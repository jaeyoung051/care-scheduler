package com.homecare.domain.repository;

import com.homecare.domain.entity.Elder;
import com.homecare.domain.enums.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elder Entity의 데이터 접근 계층
 */
@Repository
public interface ElderRepository extends JpaRepository<Elder, Long> {
    List<Elder> findByGender(Gender gender);
    List<Elder> findByRegion(String region);
    List<Elder> findByNameContaining(String name);
    List<Elder> findByGenderAndRegion(Gender gender, String region);
}
