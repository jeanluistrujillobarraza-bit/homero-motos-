package com.moto.repository;

import com.moto.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByTenantId(String tenantId);
    List<Payment> findByFinancingPlanIdAndTenantIdOrderByFechaPagoAsc(String financingPlanId, String tenantId);
    List<Payment> findByTenantIdAndFechaPagoBetween(String tenantId, java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<Payment> findFirst10ByTenantIdOrderByFechaPagoDesc(String tenantId);
    
    // Original methods for system/superadmin views
    List<Payment> findByFinancingPlanIdOrderByFechaPagoAsc(String financingPlanId);
    List<Payment> findByFechaPagoBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<Payment> findFirst10ByOrderByFechaPagoDesc();
}
