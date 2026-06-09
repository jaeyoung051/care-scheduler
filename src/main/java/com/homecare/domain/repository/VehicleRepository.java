package com.homecare.domain.repository;

import com.homecare.domain.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Vehicle Entity의 데이터 접근 계층
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByName(String name);
    List<Vehicle> findByHasMaleStaff(Boolean hasMaleStaff);
}
