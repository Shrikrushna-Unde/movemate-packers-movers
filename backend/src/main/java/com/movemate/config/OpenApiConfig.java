package com.movemate.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI / OpenAPI metadata.
 *
 * <p>springdoc already generates the endpoint list by scanning our controllers. This class
 * adds the two things it cannot infer: the title/description a reader sees first, and the
 * "Authorize" button that lets them paste a JWT and actually call the protected endpoints.
 *
 * <p>Worth knowing for interviews: this is <b>documentation generated from code</b>, so it can
 * never drift out of date the way a hand-written API doc does. Put the Swagger URL on your
 * resume next to the live site - it shows the API works without the reviewer installing
 * anything.
 */
@Configuration
public class OpenApiConfig {

    /** Must match the name used in {@code addSecurityItem} below. */
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI moveMateOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MoveMate Packers & Movers API")
                        .version("1.0.0")
                        .description("""
                                REST API for a packers and movers platform.

                                **Built with:** Java 17, Spring Boot 3.3, Spring Security (JWT), \
                                Spring Data JPA, MySQL 8, Flyway.

                                **How to try a protected endpoint**
                                1. `POST /api/v1/auth/login` with the seeded admin account.
                                2. Copy the `token` from the response.
                                3. Click **Authorize** (top right) and paste it.
                                4. Every `/api/v1/admin/**` call now carries the Bearer header.
                                """)
                        .contact(new Contact()
                                .name("MoveMate Engineering")
                                .email("support@movemate.in"))
                        .license(new License().name("MIT")))

                // Declares HOW to authenticate: a bearer token in the Authorization header.
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                // bearerFormat is free text; "JWT" tells humans what to expect.
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("Paste the JWT returned by /api/v1/auth/login")))

                // Applies the scheme globally so Swagger UI sends the header to every endpoint.
                // Public endpoints simply ignore it.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
