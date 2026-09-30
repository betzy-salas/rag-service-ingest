package com.rag.ingestion.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rag.ingestion.controller.mapper.PageSummaryMapperImpl;
import com.rag.ingestion.exception.SpaceNotFoundException;
import com.rag.ingestion.service.PageService;
import com.rag.ingestion.service.model.PageInfo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PagesController.class)
@Import(PageSummaryMapperImpl.class)
class PagesControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean PageService pageService;

  @Test
  void listPagesReturns200WithSummaries() throws Exception {
    when(pageService.listPages("DS")).thenReturn(List.of(new PageInfo("1", "Uno", 3)));

    mockMvc
        .perform(get("/pages").param("spaceKey", "DS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("1"))
        .andExpect(jsonPath("$[0].title").value("Uno"))
        .andExpect(jsonPath("$[0].version").value(3));
  }

  @Test
  void unknownSpaceReturns404() throws Exception {
    when(pageService.listPages("XX")).thenThrow(new SpaceNotFoundException("XX"));

    mockMvc.perform(get("/pages").param("spaceKey", "XX")).andExpect(status().isNotFound());
  }
}
