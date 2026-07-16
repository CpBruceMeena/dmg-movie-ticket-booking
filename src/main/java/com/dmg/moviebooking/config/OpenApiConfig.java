package com.dmg.moviebooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DMG Movie Ticket Booking API")
                        .description("REST API for the DMG Movie Ticket Booking System. Supports city, theater, and show management with seat-level booking, pricing tiers, discount codes, and configurable refund policies.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DMG Development Team")
                                .email("dev@dmg.com"))
                        .license(new License()
                                .name("Private")
                                .url("https://github.com/CpBruceMeena/dmg-movie-ticket-booking")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")
                ))
                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                        .description("Project Repository")
                        .url("https://github.com/CpBruceMeena/dmg-movie-ticket-booking"));
    }
}
