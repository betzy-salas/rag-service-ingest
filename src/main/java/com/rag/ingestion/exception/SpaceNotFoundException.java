package com.rag.ingestion.exception;

/** Thrown when a space key does not exist in Confluence. */
public class SpaceNotFoundException extends RuntimeException {

  public SpaceNotFoundException(String spaceKey) {
    super(ErrorMessages.SPACE_NOT_FOUND.formatted(spaceKey));
  }
}
