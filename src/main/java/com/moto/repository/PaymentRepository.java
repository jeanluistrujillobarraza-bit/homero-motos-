package com.moto.repository;

import com.moto.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByFinancingPlanIdOrderByFechaPagoAsc(String financingPlanId);
    List<Payment> findByFechaPagoBetween(LocalDateTime start, LocalDateTime end);
    List<Payment> findFirst10ByOrderByFechaPagoDesc();
}
