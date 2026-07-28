package com.moto.repository;

import com.moto.model.DeletedFinancingBackup;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DeletedFinancingBackupRepository extends MongoRepository<DeletedFinancingBackup, String> {
    List<DeletedFinancingBackup> findByTenantId(String tenantId);
    List<DeletedFinancingBackup> findByOrderByDeletedAtDesc();
    List<DeletedFinancingBackup> findByTenantIdOrderByDeletedAtDesc(String tenantId);
}
