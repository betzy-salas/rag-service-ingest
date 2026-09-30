package com.rag.ingestion.controller.mapper;

import com.rag.ingestion.controller.dto.SynchronizationResult;
import com.rag.ingestion.service.model.SynchronizationSummary;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Maps the synchronization summary to the API response. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SynchronizationResultMapper {

  SynchronizationResult toResult(SynchronizationSummary summary);
}
