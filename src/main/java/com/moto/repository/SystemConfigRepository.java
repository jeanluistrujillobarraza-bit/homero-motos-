package com.moto.repository;

import com.moto.model.SystemConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SystemConfigRepository extends MongoRepository<SystemConfig, String> {
}
