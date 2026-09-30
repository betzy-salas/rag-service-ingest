package com.rag.ingestion.common.model;

/** Metadata keys stored with every chunk. They are a contract with the query service. */
public final class ChunkMetadata {

  public static final String PAGE_ID = "pageId";
  public static final String SPACE_KEY = "spaceKey";
  public static final String TITLE = "title";
  public static final String URL = "url";

  private ChunkMetadata() {}
}
