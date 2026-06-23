package com.cloudera.customerformhub.repository;

import com.cloudera.customerformhub.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
}
