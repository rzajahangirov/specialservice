package az.techvibeds.specialservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    // Bu metod, "public-apis" qrupu Ã¼Ã§Ã¼n Swagger JSON-u qaytaracaq
    @Bean
    public GroupedOpenApi publicApis() {
        return GroupedOpenApi
                .builder()
                .group("public-apis") // public-apis qrupu
                .pathsToMatch("/**") // HÉ™r hansÄ± bir yol
                .build();
    }

    // Bu metodda Swagger UI vÉ™ OpenAPI Ã¼Ã§Ã¼n qlobal parametrlÉ™ri konfiqurasiya edirik
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Special Service").version("v 1.0.1").description("Special Service API").version("v3"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(
                        new Components()
                                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development Server")
                ));
    }
}

