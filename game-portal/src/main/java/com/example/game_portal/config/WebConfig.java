package com.example.game_portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

/*
 * Without Spring, there is no RestTemplate. Every class that needs to call
 * an external API creates its own HttpURLConnection from scratch:
 *
 *   URL url = new URL("https://api.example.com/data");
 *   HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 *   conn.setRequestMethod("GET");
 *   conn.setRequestProperty("Accept", "application/json");
 *   // ... read the response, close the connection manually
 *
 * With Spring, RestTemplate is declared once here as a @Bean and then injected
 * wherever needed. It handles connection management, JSON parsing via Jackson,
 * and HTTP error responses automatically.
 */
