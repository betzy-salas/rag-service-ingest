package com.rag.ingestion.controller.mapper;

import com.rag.ingestion.controller.dto.PageSummary;
import com.rag.ingestion.service.model.PageInfo;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Maps the page information of the service to the API response. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PageSummaryMapper {

  List<PageSummary> toSummaries(List<PageInfo> pages);
}
