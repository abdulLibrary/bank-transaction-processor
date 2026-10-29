package com.natwest.bank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI bankOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank Transaction Processor API")
                        .description("""
                                Production-ready banking transaction service (OAuth2 Resource Server).
                                All /api/** endpoints require a valid JWT Bearer token.
                                Roles: USER (deposit/withdraw/transfer/balance), ADMIN (also create account).
                                """)
                        .version("1.2.0")
                        .contact(new Contact().name("NatWest Candidate").email("candidate@example.com"))
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT issued by the Auth service. Include as: Authorization: Bearer <token>")));
    }
}
