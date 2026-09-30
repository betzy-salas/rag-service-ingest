package com.rag.ingestion.repository;

import com.rag.ingestion.entity.ConfluencePageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfluencePageRepository extends JpaRepository<ConfluencePageEntity, String> {

  List<ConfluencePageEntity> findBySpaceKey(String spaceKey);
}
