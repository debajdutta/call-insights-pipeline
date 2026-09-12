package com.callinsights.catalogservice.repository;

import com.callinsights.catalogservice.document.ArtifactCatalogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ArtifactCatalogRepository extends MongoRepository<ArtifactCatalogDocument, String> {
}
