package com.rubrangso.finance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures the Swagger UI / OpenAPI metadata served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    /** Returns the API metadata displayed in Swagger UI. */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Personal Finance Manager API")
                        .description(
                                "REST API for tracking income, expenses, and savings goals. "
                                        + "Session-based authentication — obtain a JSESSIONID cookie "
                                        + "via POST /api/auth/login before calling protected endpoints.")
                        .version("1.0.0"));
    }
}
