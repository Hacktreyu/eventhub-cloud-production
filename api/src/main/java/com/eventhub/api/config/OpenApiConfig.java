package com.eventhub.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EventHub Cloud API")
                        .version("1.0.0")
                        .description("""
                                Event-Driven Cloud Application API

                                This API demonstrates:
                                - RESTful design with Spring Boot
                                - Event-driven architecture with Kafka/In-Memory queue
                                - PostgreSQL persistence
                                - Clean architecture patterns

                                **Modes:**
                                - `local` profile: Uses real Kafka
                                - `demo` profile: Uses in-memory queue (for free tier deployments)
                                """)
                        .contact(new Contact()
                                .name("EventHub Developer")
                                .email("developer@eventhub.cloud"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://eventhub-api.onrender.com")
                                .description("Production Server (Render)")));
    }
}
