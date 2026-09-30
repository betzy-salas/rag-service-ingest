package com.rag.ingestion.controller;

import com.rag.ingestion.controller.dto.IngestionResult;
import com.rag.ingestion.controller.dto.SynchronizationResult;
import com.rag.ingestion.controller.mapper.IngestionResultMapper;
import com.rag.ingestion.controller.mapper.SynchronizationResultMapper;
import com.rag.ingestion.service.IngestionService;
import com.rag.ingestion.service.SynchronizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints to ingest or synchronize a Confluence space. Temporary: both run synchronously. */
@RestController
@RequiredArgsConstructor
public class IngestionController {

  private final IngestionService ingestionService;
  private final SynchronizationService synchronizationService;
  private final IngestionResultMapper resultMapper;
  private final SynchronizationResultMapper synchronizationResultMapper;

  @PostMapping(ApiPaths.INGESTIONS)
  ResponseEntity<IngestionResult> ingest(@RequestParam String spaceKey) {
    return ResponseEntity.ok(resultMapper.toResult(ingestionService.ingest(spaceKey)));
  }

  @PostMapping(ApiPaths.SYNCHRONIZATIONS)
  ResponseEntity<SynchronizationResult> synchronize(@RequestParam String spaceKey) {
    return ResponseEntity.ok(
        synchronizationResultMapper.toResult(synchronizationService.synchronize(spaceKey)));
  }
}
