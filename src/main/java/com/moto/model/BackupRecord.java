package com.moto.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "system_backups")
public class BackupRecord {
    @Id
    private String id;
    private String filename;
    private LocalDateTime createdAt;
    private Long sizeBytes;
    private String status; // SUCCESS, FAILED

    public BackupRecord() {}

    public BackupRecord(String filename, LocalDateTime createdAt, Long sizeBytes, String status) {
        this.filename = filename;
        this.createdAt = createdAt;
        this.sizeBytes = sizeBytes;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
