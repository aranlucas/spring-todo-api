package com.aranlucas.todo.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer transactionAwareRedisCache() {
        return builder -> builder.transactionAware();
    }
}
