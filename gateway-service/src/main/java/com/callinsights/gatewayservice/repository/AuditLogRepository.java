package com.callinsights.gatewayservice.repository;

import com.callinsights.gatewayservice.document.AuditLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLogDocument, String> {
    List<AuditLogDocument> findByCallIdOrderByTimestampDesc(String callId);
}
