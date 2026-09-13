package com.callinsights.gatewayservice.repository;

import com.callinsights.gatewayservice.document.CallDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CallRepository extends MongoRepository<CallDocument, String> {
}
