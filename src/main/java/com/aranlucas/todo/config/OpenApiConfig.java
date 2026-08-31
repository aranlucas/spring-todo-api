package com.aranlucas.todo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI todoApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Todo API")
                                .description("Authenticated REST API for managing personal todos.")
                                .version("v1"));
    }
}
