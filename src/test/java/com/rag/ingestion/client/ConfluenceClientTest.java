package com.rag.ingestion.client;

import com.rag.ingestion.client.mapper.ConfluencePageMapperImpl;
import com.rag.ingestion.config.ConfluenceProperties;
import com.rag.ingestion.exception.SpaceNotFoundException;
import com.rag.ingestion.common.model.ConfluencePage;
import com.rag.ingestion.common.model.ConfluencePageVersion;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;


class ConfluenceClientTest {

  private static final String BASE_URL = "https://example.atlassian.net";
  private static final ConfluenceProperties PROPERTIES =
      new ConfluenceProperties(BASE_URL, "test@example.com", "test-token");

  @Test
  void listPagesFollowsNextLinkUntilLastPage() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(BASE_URL + "/wiki/api/v2/spaces?keys=ENG"))
        .andRespond(json("{\"results\":[{\"id\":\"123\",\"key\":\"ENG\",\"type\":\"global\"}]}"));
    server
        .expect(requestTo(BASE_URL + "/wiki/api/v2/spaces/123/pages?body-format=storage&limit=250"))
        .andRespond(
            json(
                """
                {"results":[{"id":"1","status":"current","parentId":"9","title":"Uno","version":{"number":3},"body":{"storage":{"value":"<p>uno</p>"}},
                   "_links":{"webui":"/spaces/ENG/pages/1/Uno"}}],
                 "_links":{"next":"/wiki/api/v2/spaces/123/pages?body-format=storage&limit=250&cursor=abc",
                           "base":"https://example.atlassian.net/wiki"}}
                """));
    server
        .expect(requestTo(BASE_URL + "/wiki/api/v2/spaces/123/pages?body-format=storage&limit=250&cursor=abc"))
        .andRespond(
            json(
                """
                {"results":[{"id":"2","title":"Dos","version":{"number":1},"body":{"storage":{"value":"<p>dos</p>"}},
                   "_links":{"webui":"/spaces/ENG/pages/2/Dos"}}],
                 "_links":{"base":"https://example.atlassian.net/wiki"}}
                """));

    var pages = new ConfluenceClient(builder.build(), new ConfluencePageMapperImpl(), PROPERTIES).listPages("ENG");

    assertThat(pages)
        .containsExactly(
            new ConfluencePage(
                "1", "Uno", 3, "<p>uno</p>", BASE_URL + "/wiki/spaces/ENG/pages/1/Uno"),
            new ConfluencePage(
                "2", "Dos", 1, "<p>dos</p>", BASE_URL + "/wiki/spaces/ENG/pages/2/Dos"));
    server.verify();
  }

  @Test
  void listPageVersionsDoesNotRequestBodies() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(BASE_URL + "/wiki/api/v2/spaces?keys=ENG"))
        .andRespond(json("{\"results\":[{\"id\":\"123\"}]}"));
    server
        .expect(requestTo(BASE_URL + "/wiki/api/v2/spaces/123/pages?limit=250"))
        .andRespond(
            json(
                """
                {"results":[{"id":"1","title":"Uno","version":{"number":3},
                   "_links":{"webui":"/spaces/ENG/pages/1/Uno"}}],
                 "_links":{"base":"https://example.atlassian.net/wiki"}}
                """));

    var versions =
        new ConfluenceClient(builder.build(), new ConfluencePageMapperImpl(), PROPERTIES)
            .listPageVersions("ENG");

    assertThat(versions).containsExactly(new ConfluencePageVersion("1", "Uno", 3));
    server.verify();
  }

  @Test
  void getPageReturnsBodyAndFullUrl() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(BASE_URL + "/wiki/api/v2/pages/1?body-format=storage"))
        .andRespond(
            json(
                """
                {"id":"1","title":"Uno","version":{"number":4},"body":{"storage":{"value":"<p>uno</p>"}},
                 "_links":{"webui":"/spaces/ENG/pages/1/Uno"}}
                """));

    var page =
        new ConfluenceClient(builder.build(), new ConfluencePageMapperImpl(), PROPERTIES).getPage("1");

    assertThat(page)
        .isEqualTo(
            new ConfluencePage(
                "1", "Uno", 4, "<p>uno</p>", BASE_URL + "/wiki/spaces/ENG/pages/1/Uno"));
  }

  @Test
  void unknownSpaceThrowsSpaceNotFound() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(BASE_URL + "/wiki/api/v2/spaces?keys=XX"))
        .andRespond(json("{\"results\":[]}"));
    var client = new ConfluenceClient(builder.build(), new ConfluencePageMapperImpl(), PROPERTIES);

    assertThatThrownBy(() -> client.listPageVersions("XX"))
        .isInstanceOf(SpaceNotFoundException.class)
        .hasMessage("Space 'XX' does not exist in Confluence");
  }

  private static org.springframework.test.web.client.ResponseCreator json(String body) {
    return withSuccess(body, MediaType.APPLICATION_JSON);
  }
}
