package com.dmg.moviebooking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DMG Movie Ticket Booking API")
                        .description("REST API for the DMG Movie Ticket Booking System. Supports city, theater, and show management with seat-level booking, pricing tiers, discount codes, and configurable refund policies.\n\n### Authentication\n1. Call `POST /api/auth/login` with your credentials to obtain a JWT token.\n2. Click the **Authorize** button and paste your token as `Bearer <token>`.\n3. All subsequent API calls will include the token automatically.")
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
                        .url("https://github.com/CpBruceMeena/dmg-movie-ticket-booking"))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token as: **Bearer &lt;token&gt;**. Get a token by calling `POST /api/auth/login`.")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SCHEME_NAME));
    }
}
