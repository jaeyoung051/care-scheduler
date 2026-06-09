package com.homecare.domain.repository;

import com.homecare.domain.entity.BathSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * BathSchedule Entity의 데이터 접근 계층
 */
@Repository
public interface BathScheduleRepository extends JpaRepository<BathSchedule, Long> {
    List<BathSchedule> findByServiceDate(LocalDate serviceDate);
    List<BathSchedule> findByElderIdOrderByServiceDateAscStartTimeAsc(Long elderId);
    List<BathSchedule> findByVehicleIdOrderByServiceDateAscStartTimeAsc(Long vehicleId);
    List<BathSchedule> findByServiceDateAndVehicleIdOrderByStartTimeAsc(LocalDate serviceDate, Long vehicleId);
    
    /**
     * 동일 차량에서 시간이 겹치는 일정 조회
     * NOT (A.end_time <= ? OR A.start_time >= ?)와 동일
     */
    @Query("""
        SELECT b FROM BathSchedule b 
        WHERE b.vehicle.id = :vehicleId 
        AND b.serviceDate = :serviceDate 
        AND b.startTime < :endTime 
        AND b.endTime > :startTime
    """)
    List<BathSchedule> findOverlappingSchedules(
        @Param("vehicleId") Long vehicleId,
        @Param("serviceDate") LocalDate serviceDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
}
