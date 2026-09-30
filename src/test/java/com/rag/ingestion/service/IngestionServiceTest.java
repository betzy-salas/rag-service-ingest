package com.rag.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.rag.ingestion.TestInfrastructure;
import com.rag.ingestion.client.ConfluenceClient;
import com.rag.ingestion.common.model.ConfluencePage;
import com.rag.ingestion.entity.ConfluencePageEntity;
import com.rag.ingestion.repository.ConfluencePageRepository;
import com.rag.ingestion.service.model.IngestionSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestInfrastructure.class)
class IngestionServiceTest {

  @MockitoBean ConfluenceClient confluenceClient;

  @Autowired IngestionService ingestionService;

  @Autowired ConfluencePageRepository pageRepository;

  @Autowired JdbcClient jdbcClient;

  @Test
  void ingestStoresChunksWithMetadataAndRegistersPages() {
    when(confluenceClient.listPages("DS"))
        .thenReturn(List.of(page("1", 2, "<h1>Vault</h1><p>Usamos Vault para secretos.</p>")));

    var result = ingestionService.ingest("DS");

    assertThat(result).isEqualTo(new IngestionSummary(1, 1));
    assertThat(
            jdbcClient
                .sql(
                    "SELECT metadata->>'title' || '|' || (metadata->>'url') FROM confluence_chunk"
                        + " WHERE metadata->>'pageId' = '1'")
                .query(String.class)
                .single())
        .isEqualTo("Página 1|https://example.atlassian.net/wiki/spaces/DS/pages/1");
    assertThat(pageRepository.findById("1"))
        .get()
        .extracting(ConfluencePageEntity::getVersion, ConfluencePageEntity::getChunkCount)
        .containsExactly(2, 1);
  }

  @Test
  void ingestAgainReplacesPreviousChunksOfThePage() {
    when(confluenceClient.listPages("DS")).thenReturn(List.of(page("7", 1, "<p>Primera versión.</p>")));
    ingestionService.ingest("DS");
    when(confluenceClient.listPages("DS")).thenReturn(List.of(page("7", 2, "<p>Segunda versión.</p>")));

    ingestionService.ingest("DS");

    assertThat(
            jdbcClient
                .sql("SELECT content FROM confluence_chunk WHERE metadata->>'pageId' = '7'")
                .query(String.class)
                .list())
        .containsExactly("Página 7\nSegunda versión.");
  }

  @Test
  void ingestCreatesOneChunkPerSectionPrefixedWithTitleAndHeading() {
    when(confluenceClient.listPages("DS"))
        .thenReturn(
            List.of(
                page(
                    "9",
                    1,
                    "<h2>Rollback</h2><p>Revierte primero.</p><h2>CI/CD</h2><p>GitHub Actions.</p>")));

    ingestionService.ingest("DS");

    assertThat(
            jdbcClient
                .sql("SELECT content FROM confluence_chunk WHERE metadata->>'pageId' = '9' ORDER BY content")
                .query(String.class)
                .list())
        .containsExactly("Página 9 > CI/CD\nGitHub Actions.", "Página 9 > Rollback\nRevierte primero.");
  }

  private static ConfluencePage page(String id, int version, String body) {
    return new ConfluencePage(
        id, "Página " + id, version, body, "https://example.atlassian.net/wiki/spaces/DS/pages/" + id);
  }
}
