package com.rag.ingestion.service.model;

import com.rag.ingestion.common.model.ConfluencePageVersion;
import java.util.Map;

/** Change of a Confluence page compared with the version stored in {@code confluence_page}. */
public enum PageChange {
  NEW,
  UPDATED,
  UNCHANGED;

  /** Classifies a page by comparing its version with the stored versions, keyed by page id. */
  public static PageChange of(ConfluencePageVersion page, Map<String, Integer> storedVersions) {
    Integer stored = storedVersions.get(page.id());
    if (stored == null) {
      return NEW;
    }
    return stored.equals(page.version()) ? UNCHANGED : UPDATED;
  }
}
