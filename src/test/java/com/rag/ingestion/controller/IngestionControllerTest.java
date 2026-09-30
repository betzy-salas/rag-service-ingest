package com.rag.ingestion.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openai.errors.OpenAIIoException;
import com.rag.ingestion.controller.mapper.IngestionResultMapperImpl;
import com.rag.ingestion.controller.mapper.SynchronizationResultMapperImpl;
import com.rag.ingestion.exception.SpaceNotFoundException;
import com.rag.ingestion.service.IngestionService;
import com.rag.ingestion.service.SynchronizationService;
import com.rag.ingestion.service.model.IngestionSummary;
import com.rag.ingestion.service.model.SynchronizationSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@WebMvcTest(IngestionController.class)
@Import({IngestionResultMapperImpl.class, SynchronizationResultMapperImpl.class})
class IngestionControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean IngestionService ingestionService;

  @MockitoBean SynchronizationService synchronizationService;

  @Test
  void ingestReturns200WithResult() throws Exception {
    when(ingestionService.ingest("DS")).thenReturn(new IngestionSummary(4, 9));

    mockMvc
        .perform(post("/ingestions").param("spaceKey", "DS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pages").value(4))
        .andExpect(jsonPath("$.chunks").value(9));
  }

  @Test
  void synchronizeReturns200WithResult() throws Exception {
    when(synchronizationService.synchronize("DS")).thenReturn(new SynchronizationSummary(1, 0, 5, 2));

    mockMvc
        .perform(post("/synchronizations").param("spaceKey", "DS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.newPages").value(1))
        .andExpect(jsonPath("$.unchangedPages").value(5));
  }

  @Test
  void missingSpaceKeyReturns400() throws Exception {
    mockMvc
        .perform(post("/ingestions"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void unknownSpaceReturns404() throws Exception {
    when(ingestionService.ingest("XX")).thenThrow(new SpaceNotFoundException("XX"));

    mockMvc
        .perform(post("/ingestions").param("spaceKey", "XX"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Space 'XX' does not exist in Confluence"))
        .andExpect(jsonPath("$.instance").value("/ingestions"));
  }

  @Test
  void rejectedConfluenceCredentialsReturn502() throws Exception {
    when(ingestionService.ingest("DS"))
        .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

    mockMvc
        .perform(post("/ingestions").param("spaceKey", "DS"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value("Confluence rejected the credentials"));
  }

  @Test
  void unreachableConfluenceReturns502() throws Exception {
    when(synchronizationService.synchronize("DS")).thenThrow(new ResourceAccessException("timeout"));

    mockMvc
        .perform(post("/synchronizations").param("spaceKey", "DS"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value("Could not retrieve information from Confluence"));
  }

  @Test
  void openAiFailureReturns502() throws Exception {
    when(ingestionService.ingest("DS")).thenThrow(new OpenAIIoException("connection reset"));

    mockMvc
        .perform(post("/ingestions").param("spaceKey", "DS"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value("Could not generate embeddings with OpenAI"));
  }

  @Test
  void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
    when(ingestionService.ingest("DS")).thenThrow(new IllegalStateException("detalle interno"));

    mockMvc
        .perform(post("/ingestions").param("spaceKey", "DS"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.detail").value("Internal service error"));
  }
}
