package com.moto.repository;

import com.moto.model.Motorcycle;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface MotorcycleRepository extends MongoRepository<Motorcycle, String> {
    Optional<Motorcycle> findByPlaca(String placa);
    Optional<Motorcycle> findByVin(String vin);
    Optional<Motorcycle> findByNumeroMotor(String numeroMotor);
    List<Motorcycle> findByEstado(String estado);
    
    // For search queries
    List<Motorcycle> findByPlacaContainingIgnoreCaseOrMarcaContainingIgnoreCaseOrModeloContainingIgnoreCase(String placa, String marca, String modelo);
}
