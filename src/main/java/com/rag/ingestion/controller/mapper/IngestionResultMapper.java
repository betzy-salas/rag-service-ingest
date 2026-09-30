package com.rag.ingestion.controller.mapper;

import com.rag.ingestion.controller.dto.IngestionResult;
import com.rag.ingestion.service.model.IngestionSummary;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Maps the ingestion summary to the API response. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface IngestionResultMapper {

  IngestionResult toResult(IngestionSummary summary);
}
