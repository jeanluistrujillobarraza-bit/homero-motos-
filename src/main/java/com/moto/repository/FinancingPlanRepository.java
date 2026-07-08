package com.moto.repository;

import com.moto.model.FinancingPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface FinancingPlanRepository extends MongoRepository<FinancingPlan, String> {
    List<FinancingPlan> findByTenantId(String tenantId);
    Optional<FinancingPlan> findByMotorcycleIdAndTenantId(String motorcycleId, String tenantId);
    List<FinancingPlan> findByBuyerCedulaAndTenantId(String cedula, String tenantId);
    List<FinancingPlan> findByEstadoCreditoAndTenantId(String estadoCredito, String tenantId);
    
    @org.springframework.data.mongodb.repository.Query("{ 'tenantId': ?0, $or: [ { 'buyer.cedula': { $regex: ?1, $options: 'i' } }, { 'buyer.nombreCompleto': { $regex: ?1, $options: 'i' } } ] }")
    List<FinancingPlan> searchPlans(String tenantId, String query);
}
