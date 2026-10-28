package com.tdb.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI 3.0 / Swagger UI documentation. Provides service metadata,
 * contact details, and license information.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures the OpenAPI specification metadata for the TDB Darshan Booking Service.
     *
     * @return customized {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI darshanBookingOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("TDB Darshan Booking Service API")
                                .description(
                                        "RESTful API services for temple slot creation, ticket"
                                            + " booking, capacity management, and reservations.")
                                .version("v1.0.0")
                                .contact(
                                        new Contact()
                                                .name("TDB Darshan Support Team")
                                                .email("support@tdb-booking.org"))
                                .license(
                                        new License()
                                                .name("Apache 2.0")
                                                .url(
                                                        "https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
