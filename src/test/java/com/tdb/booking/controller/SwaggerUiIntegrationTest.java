package com.tdb.booking.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SwaggerUiIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("GET /v3/api-docs - Should successfully generate OpenAPI specification JSON")
    void openApiDocs_ShouldReturnApiDefinition() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("TDB Darshan Booking Service API")))
                .andExpect(content().string(containsString("/api/v1/darshan-slots")))
                .andExpect(content().string(containsString("/api/v1/darshan-bookings")));
    }

    @Test
    @DisplayName("GET /swagger-ui/index.html - Should successfully serve Swagger UI HTML")
    void swaggerUi_ShouldReturnHtmlPage() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
