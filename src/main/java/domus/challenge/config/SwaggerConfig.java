package domus.challenge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("**My Movie Management API**")
                        .version("**v1.0.0**")
                        .description("Documentation for managing directors and movies. Includes search and count endpoints.")
                        .termsOfService("https://policies.google.com/terms?hl=es")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org"))
                        .contact(new Contact().name("Support").email("support@my-company.com"))
                );
    }
}
