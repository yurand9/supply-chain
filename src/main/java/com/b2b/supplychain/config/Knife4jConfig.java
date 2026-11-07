package com.b2b.supplychain.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置（文档页标题/描述/版本）。
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("b2b-supply-chain 接口文档")
                .description("企业级 B2B 数据交换网关与供应链协同平台")
                .version("1.0.0"));
    }
}
