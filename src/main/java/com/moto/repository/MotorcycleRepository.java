package com.moto.repository;

import com.moto.model.Motorcycle;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface MotorcycleRepository extends MongoRepository<Motorcycle, String> {
    List<Motorcycle> findByTenantId(String tenantId);
    Optional<Motorcycle> findByPlacaAndTenantId(String placa, String tenantId);
    Optional<Motorcycle> findByVinAndTenantId(String vin, String tenantId);
    Optional<Motorcycle> findByNumeroMotorAndTenantId(String numeroMotor, String tenantId);
    List<Motorcycle> findByEstadoAndTenantId(String estado, String tenantId);
    
    @org.springframework.data.mongodb.repository.Query("{ 'tenantId': ?0, $or: [ { 'placa': { $regex: ?1, $options: 'i' } }, { 'marca': { $regex: ?1, $options: 'i' } }, { 'modelo': { $regex: ?1, $options: 'i' } } ] }")
    List<Motorcycle> searchMotorcycles(String tenantId, String query);
}
