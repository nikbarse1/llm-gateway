package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public RestClientCustomizer restClientCustomizer(
            @Value("${llm.http.connect-timeout:10000}") int connectTimeoutMs,
            @Value("${llm.http.read-timeout:90000}") int readTimeoutMs) {

        ClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        ((SimpleClientHttpRequestFactory) factory).setConnectTimeout(connectTimeoutMs);
        ((SimpleClientHttpRequestFactory) factory).setReadTimeout(readTimeoutMs);

        return new RestClientCustomizer() {
            @Override
            public void customize(RestClient.Builder builder) {
                builder.requestFactory(factory);
            }
        };
    }

}
