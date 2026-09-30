package com.rag.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rag.ingestion.TestInfrastructure;
import com.rag.ingestion.client.ConfluenceClient;
import com.rag.ingestion.common.model.ConfluencePage;
import com.rag.ingestion.common.model.ConfluencePageVersion;
import com.rag.ingestion.entity.ConfluencePageEntity;
import com.rag.ingestion.repository.ConfluencePageRepository;
import com.rag.ingestion.service.model.SynchronizationSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestInfrastructure.class)
class SynchronizationServiceTest {

  @MockitoBean ConfluenceClient confluenceClient;

  @Autowired SynchronizationService synchronizationService;

  @Autowired ConfluencePageRepository pageRepository;

  @Autowired JdbcClient jdbcClient;

  @Test
  void firstSynchronizationIndexesEveryPageAsNew() {
    confluenceHas("NEW", version("n1", 1), version("n2", 1));

    var summary = synchronizationService.synchronize("NEW");

    assertThat(summary).isEqualTo(new SynchronizationSummary(2, 0, 0, 2));
    assertThat(pageRepository.findBySpaceKey("NEW")).hasSize(2);
  }

  @Test
  void synchronizeIndexesOnlyNewAndChangedPages() {
    confluenceHas("SYNC", version("s1", 1), version("s2", 1));
    synchronizationService.synchronize("SYNC");
    clearInvocations(confluenceClient);
    confluenceHas("SYNC", version("s1", 1), version("s2", 2), version("s3", 1));
    when(confluenceClient.getPage("s2")).thenReturn(page("s2", 2, "<p>Editada.</p>"));

    var summary = synchronizationService.synchronize("SYNC");

    assertThat(summary).isEqualTo(new SynchronizationSummary(1, 1, 1, 2));
    verify(confluenceClient, never()).getPage("s1");
    assertThat(
            jdbcClient
                .sql("SELECT content FROM confluence_chunk WHERE metadata->>'pageId' = 's2'")
                .query(String.class)
                .list())
        .containsExactly("Página s2\nEditada.");
    assertThat(pageRepository.findById("s2"))
        .get()
        .extracting(ConfluencePageEntity::getVersion)
        .isEqualTo(2);
  }

  @Test
  void synchronizationWithoutChangesDoesNotDownloadPages() {
    confluenceHas("SAME", version("u1", 1));
    synchronizationService.synchronize("SAME");
    clearInvocations(confluenceClient);

    var summary = synchronizationService.synchronize("SAME");

    assertThat(summary).isEqualTo(new SynchronizationSummary(0, 0, 1, 0));
    verify(confluenceClient, never()).getPage("u1");
  }

  /** Stubs the lightweight listing of the space; each page is returned with one paragraph of text. */
  private void confluenceHas(String spaceKey, ConfluencePageVersion... versions) {
    when(confluenceClient.listPageVersions(spaceKey)).thenReturn(List.of(versions));
    for (ConfluencePageVersion version : versions) {
      when(confluenceClient.getPage(version.id()))
          .thenReturn(page(version.id(), version.version(), "<p>Texto de " + version.id() + ".</p>"));
    }
  }

  private static ConfluencePageVersion version(String id, int version) {
    return new ConfluencePageVersion(id, "Página " + id, version);
  }

  private static ConfluencePage page(String id, int version, String body) {
    return new ConfluencePage(
        id, "Página " + id, version, body, "https://example.atlassian.net/wiki/spaces/X/pages/" + id);
  }
}
