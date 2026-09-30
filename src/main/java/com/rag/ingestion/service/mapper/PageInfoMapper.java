package com.rag.ingestion.service.mapper;

import com.rag.ingestion.common.model.ConfluencePageVersion;
import com.rag.ingestion.service.model.PageInfo;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Maps the lightweight Confluence page listing to the service model. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PageInfoMapper {

  List<PageInfo> toPageInfos(List<ConfluencePageVersion> pages);
}
