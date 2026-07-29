package com.example.demo.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClientCustomizer timeoutRestClientCustomizer() {
        return restClientBuilder -> {
            ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                    .withConnectTimeout(Duration.ofSeconds(10))
                    .withReadTimeout(Duration.ofSeconds(290));

            restClientBuilder.requestFactory(
                    ClientHttpRequestFactoryBuilder.simple().build(settings)
            );
        };
    }
}
