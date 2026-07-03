package com.moto.repository;

import com.moto.model.BackupRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BackupRecordRepository extends MongoRepository<BackupRecord, String> {
    List<BackupRecord> findByOrderByCreatedAtDesc();
}
