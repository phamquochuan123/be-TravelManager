package com.example.travelManager.repository.destination;

import com.example.travelManager.domain.destination.Destination;
import com.example.travelManager.util.constant.destination.DestinationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DestinationRepository extends JpaRepository<Destination, Long> {
    List<Destination> findByIsActiveTrue();
    List<Destination> findByCityIgnoreCaseAndIsActiveTrue(String city);
    List<Destination> findByDestinationTypeAndIsActiveTrue(DestinationType type);
    List<Destination> findByProvinceIgnoreCaseAndIsActiveTrue(String province);
}
