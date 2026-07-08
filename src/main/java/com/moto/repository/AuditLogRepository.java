package com.moto.repository;

import com.moto.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findByTenantId(String tenantId);
    List<AuditLog> findByTenantIdOrderByFechaDesc(String tenantId);
    List<AuditLog> findFirst10ByTenantIdOrderByFechaDesc(String tenantId);
    
    // Original methods for system/superadmin views
    List<AuditLog> findByOrderByFechaDesc();
    List<AuditLog> findFirst10ByOrderByFechaDesc();
}
