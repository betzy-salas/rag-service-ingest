package com.rag.ingestion.client;

/** Paths of the Confluence Cloud REST API v2. */
public final class ConfluenceApi {

  public static final String WIKI_PATH = "/wiki";
  public static final int PAGE_LIMIT = 250;

  public static final String SPACES_BY_KEY = "/wiki/api/v2/spaces?keys={key}";
  public static final String SPACE_PAGES = "/wiki/api/v2/spaces/%s/pages?limit=%d";
  public static final String SPACE_PAGES_WITH_BODY =
      "/wiki/api/v2/spaces/%s/pages?body-format=storage&limit=%d";
  public static final String PAGE_WITH_BODY = "/wiki/api/v2/pages/{id}?body-format=storage";

  private ConfluenceApi() {}
}
