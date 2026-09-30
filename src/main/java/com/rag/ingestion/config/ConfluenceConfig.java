package com.rag.ingestion.config;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Configures the {@link RestClient} used to call Confluence with basic authentication. */
@Configuration
@EnableConfigurationProperties(ConfluenceProperties.class)
public class ConfluenceConfig {

  @Bean
  RestClient confluenceRestClient(ConfluenceProperties properties) {
    return RestClient.builder()
        .baseUrl(properties.baseUrl())
        .defaultHeaders(
            headers ->
                headers.setBasicAuth(
                    properties.email(), properties.apiToken(), StandardCharsets.UTF_8))
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
