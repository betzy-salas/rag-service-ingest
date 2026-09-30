package com.rag.ingestion.controller;

import com.rag.ingestion.controller.dto.PageSummary;
import com.rag.ingestion.controller.mapper.PageSummaryMapper;
import com.rag.ingestion.service.PageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Temporary endpoint to check that the service can read a Confluence space. */
@RestController
@RequiredArgsConstructor
public class PagesController {

  private final PageService pageService;
  private final PageSummaryMapper summaryMapper;

  @GetMapping(ApiPaths.PAGES)
  ResponseEntity<List<PageSummary>> listPages(@RequestParam String spaceKey) {
    return ResponseEntity.ok(summaryMapper.toSummaries(pageService.listPages(spaceKey)));
  }
}
