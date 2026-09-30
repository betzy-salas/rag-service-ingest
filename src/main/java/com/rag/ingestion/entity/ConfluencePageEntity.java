package com.rag.ingestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "confluence_page")
@Getter
@Setter
public class ConfluencePageEntity {

  @Id
  @Column(name = "page_id")
  private String pageId;

  @Column(name = "space_key")
  private String spaceKey;

  private String title;

  private int version;

  @Column(name = "chunk_count")
  private int chunkCount;

  @Column(name = "indexed_at")
  private OffsetDateTime indexedAt;
}
