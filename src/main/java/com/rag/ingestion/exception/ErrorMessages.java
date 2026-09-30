package com.rag.ingestion.exception;

/** Error messages returned to API clients in the {@code detail} field of the problem details. */
public final class ErrorMessages {

  public static final String SPACE_NOT_FOUND = "Space '%s' does not exist in Confluence";
  public static final String CONFLUENCE_CREDENTIALS_REJECTED = "Confluence rejected the credentials";
  public static final String CONFLUENCE_UNAVAILABLE = "Could not retrieve information from Confluence";
  public static final String EMBEDDINGS_FAILED = "Could not generate embeddings with OpenAI";
  public static final String INTERNAL_ERROR = "Internal service error";

  private ErrorMessages() {}
}
