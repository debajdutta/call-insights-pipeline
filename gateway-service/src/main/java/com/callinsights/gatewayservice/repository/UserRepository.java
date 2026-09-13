package com.callinsights.gatewayservice.repository;

import com.callinsights.gatewayservice.document.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<UserDocument, String> {
}
