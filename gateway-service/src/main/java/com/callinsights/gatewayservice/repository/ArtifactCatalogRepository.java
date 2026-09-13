package com.callinsights.gatewayservice.repository;

import com.callinsights.gatewayservice.document.ArtifactCatalogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ArtifactCatalogRepository extends MongoRepository<ArtifactCatalogDocument, String> {
    List<ArtifactCatalogDocument> findByCallId(String callId);
}
