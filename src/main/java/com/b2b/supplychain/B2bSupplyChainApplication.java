package com.b2b.supplychain;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动入口。
 */
@EnableScheduling
@SpringBootApplication
@MapperScan("com.b2b.supplychain.mapper")
public class B2bSupplyChainApplication {

    public static void main(String[] args) {
        SpringApplication.run(B2bSupplyChainApplication.class, args);
    }
}
