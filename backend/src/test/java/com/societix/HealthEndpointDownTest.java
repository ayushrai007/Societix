package com.societix;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointDownTest {

    private final MockMvc mockMvc;

    @Autowired
    HealthEndpointDownTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void healthEndpointReturnsServiceUnavailableWhenDown() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @TestConfiguration
    static class DownHealthConfiguration {

        @Bean
        HealthIndicator failingHealthIndicator() {
            return () -> Health.down().build();
        }
    }
}
