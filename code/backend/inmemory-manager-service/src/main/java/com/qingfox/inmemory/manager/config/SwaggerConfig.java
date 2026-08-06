package com.qingfox.inmemory.manager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inmemory Manager API")
                        .description("Inmemory Manager Service API Documentation")
                        .version("0.0.1")
                        .contact(new Contact()
                                .name("qingfox")));
    }
}
