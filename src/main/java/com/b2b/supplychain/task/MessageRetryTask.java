package com.b2b.supplychain.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.b2b.supplychain.common.enums.MessageStatus;
import com.b2b.supplychain.entity.LocalMessage;
import com.b2b.supplychain.mapper.LocalMessageMapper;
import com.b2b.supplychain.mq.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息重试定时任务：扫描「待发/失败」且「已到重试时间」的本地消息，重新投递。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRetryTask {

    /** 最大重试次数，超过则放弃 */
    private static final int MAX_RETRY_COUNT = 10;

    /** 失败后下次重试的间隔（秒） */
    private static final int RETRY_DELAY_SECONDS = 10;

    private final LocalMessageMapper localMessageMapper;
    private final MessageProducer messageProducer;

    /**
     * 每 5 秒执行一次（fixedDelay：上一次执行完成后再等 5 秒，避免重叠）。
     */
    @Scheduled(fixedDelay = 5000)
    public void retryFailedMessages() {
        LocalDateTime now = LocalDateTime.now();

        List<LocalMessage> messages = localMessageMapper.selectList(
                new LambdaQueryWrapper<LocalMessage>()
                        .in(LocalMessage::getStatus, MessageStatus.PENDING.getCode(), MessageStatus.FAILED.getCode())
                        .le(LocalMessage::getNextRetryTime, now)
                        .last("LIMIT 50")); // 每次最多处理 50 条

        if (messages.isEmpty()) {
            return;
        }

        log.info("定时任务开始重试，待处理消息数：{}", messages.size());
        for (LocalMessage message : messages) {
            int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();

            // 重试次数达到上限，放弃
            if (retryCount >= MAX_RETRY_COUNT) {
                log.warn("消息重试次数达到上限({})，放弃：{}", MAX_RETRY_COUNT, message.getMessageId());
                message.setStatus(MessageStatus.DEAD.getCode()); // 标记为「最终失败」，不再被扫描
                message.setErrorMsg("重试次数达到上限，放弃");
                message.setUpdateTime(now);
                localMessageMapper.updateById(message);
                continue;
            }

            try {
                messageProducer.send(message.getPayload());
                // 重试成功
                message.setStatus(MessageStatus.SUCCESS.getCode());
                message.setErrorMsg(null);
                message.setUpdateTime(now);
                localMessageMapper.updateById(message);
                log.info("消息重试成功：{}", message.getMessageId());
            } catch (Exception e) {
                // 重试失败：次数 +1，安排下次重试
                log.error("消息重试失败：{}", message.getMessageId(), e);
                message.setStatus(MessageStatus.FAILED.getCode());
                message.setRetryCount(retryCount + 1);
                message.setErrorMsg(e.getMessage());
                message.setNextRetryTime(now.plusSeconds(RETRY_DELAY_SECONDS));
                message.setUpdateTime(now);
                localMessageMapper.updateById(message);
            }
        }
    }
}
