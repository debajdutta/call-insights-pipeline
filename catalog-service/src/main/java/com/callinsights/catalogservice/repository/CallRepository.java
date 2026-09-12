package com.callinsights.catalogservice.repository;

import com.callinsights.catalogservice.document.CallDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CallRepository extends MongoRepository<CallDocument, String> {
}
