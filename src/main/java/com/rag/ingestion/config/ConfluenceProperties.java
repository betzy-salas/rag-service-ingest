package com.rag.ingestion.config;

import com.rag.ingestion.client.ConfluenceApi;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Confluence connection settings bound from {@code app.confluence.*}, provided by Consul. */
@ConfigurationProperties(prefix = "app.confluence")
public record ConfluenceProperties(String baseUrl, String email, String apiToken) {

  /** Returns the wiki base URL (for example {@code https://site.atlassian.net/wiki}). */
  public String wikiUrl() {
    return baseUrl + ConfluenceApi.WIKI_PATH;
  }
}
