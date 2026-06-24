package com.moto.repository;

import com.moto.model.FinancingPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface FinancingPlanRepository extends MongoRepository<FinancingPlan, String> {
    Optional<FinancingPlan> findByMotorcycleId(String motorcycleId);
    List<FinancingPlan> findByBuyerCedula(String cedula);
    List<FinancingPlan> findByBuyerNombreCompletoContainingIgnoreCase(String nombreCompleto);
    List<FinancingPlan> findByEstadoCredito(String estadoCredito);
    
    // Joint searches
    List<FinancingPlan> findByBuyerCedulaContainingIgnoreCaseOrBuyerNombreCompletoContainingIgnoreCase(String cedula, String nombreCompleto);
}
