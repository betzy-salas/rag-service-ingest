package com.rag.ingestion.service.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rag.ingestion.common.model.ConfluencePageVersion;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PageChangeTest {

  private final Map<String, Integer> stored = Map.of("1", 3);

  @Test
  void pageNotStoredIsNew() {
    assertThat(PageChange.of(new ConfluencePageVersion("2", "Dos", 1), stored)).isEqualTo(PageChange.NEW);
  }

  @Test
  void pageWithSameVersionIsUnchanged() {
    assertThat(PageChange.of(new ConfluencePageVersion("1", "Uno", 3), stored))
        .isEqualTo(PageChange.UNCHANGED);
  }

  @Test
  void pageWithOtherVersionIsUpdated() {
    assertThat(PageChange.of(new ConfluencePageVersion("1", "Uno", 4), stored))
        .isEqualTo(PageChange.UPDATED);
  }
}
