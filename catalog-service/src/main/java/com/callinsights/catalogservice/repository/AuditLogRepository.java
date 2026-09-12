package com.callinsights.catalogservice.repository;

import com.callinsights.catalogservice.document.AuditLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogRepository extends MongoRepository<AuditLogDocument, String> {
}
