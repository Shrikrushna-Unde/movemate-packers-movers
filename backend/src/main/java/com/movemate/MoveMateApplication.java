package com.movemate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Application entry point.
 *
 * <p>{@code @SpringBootApplication} is three annotations in one:
 * <ul>
 *   <li>{@code @Configuration} - this class may declare {@code @Bean} methods.</li>
 *   <li>{@code @EnableAutoConfiguration} - Spring inspects the classpath and configures
 *       what it finds. Tomcat on the classpath becomes a web server; a JDBC driver plus
 *       a datasource URL becomes a connection pool. This is the single most important
 *       idea in Spring Boot and a guaranteed interview question.</li>
 *   <li>{@code @ComponentScan} - scan this package and everything below it for
 *       {@code @Component}, {@code @Service}, {@code @Repository}, {@code @RestController}.
 *       That is exactly why every class in this project lives under {@code com.movemate}.</li>
 * </ul>
 *
 * <p>{@code @EnableJpaAuditing} switches on the listener that fills {@code createdAt} and
 * {@code updatedAt} on every save - see {@link com.movemate.domain.Auditable}. Without this
 * annotation those fields stay null and inserts fail on the NOT NULL column.
 *
 * <p>{@code @ConfigurationPropertiesScan} finds our {@code @ConfigurationProperties} records
 * ({@code JwtProperties}, {@code PricingProperties}) so we do not have to register each one.
 */
@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
public class MoveMateApplication {

    public static void main(String[] args) {
        // Boots the Spring container, starts embedded Tomcat, runs Flyway migrations,
        // then runs any CommandLineRunner (our DataSeeder) before serving traffic.
        SpringApplication.run(MoveMateApplication.class, args);
    }
}
