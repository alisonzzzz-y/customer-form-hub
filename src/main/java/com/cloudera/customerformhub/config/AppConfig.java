package com.cloudera.customerformhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    // 准备一个 RestClient,供其他类用来发 HTTP 请求
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}