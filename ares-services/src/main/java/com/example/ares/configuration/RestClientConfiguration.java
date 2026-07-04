package com.example.ares.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP client configuration used by adapters that observe external service health.
 */
@Configuration
public class RestClientConfiguration {

    @Bean
    RestTemplate restTemplate(ObservationProperties properties) {
        Duration timeout = Duration.ofMillis(properties.getActuatorTimeoutMs());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return new RestTemplate(requestFactory);
    }
}
