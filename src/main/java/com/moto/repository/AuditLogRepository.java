package com.moto.repository;

import com.moto.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findByOrderByFechaDesc();
    List<AuditLog> findFirst10ByOrderByFechaDesc();
}
