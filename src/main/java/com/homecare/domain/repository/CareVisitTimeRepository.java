package com.homecare.domain.repository;

import com.homecare.domain.entity.CareVisitTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * CareVisitTime Entity의 데이터 접근 계층
 */
@Repository
public interface CareVisitTimeRepository extends JpaRepository<CareVisitTime, Long> {
    List<CareVisitTime> findByElderId(Long elderId);
    List<CareVisitTime> findByElderIdAndDayOfWeek(Long elderId, DayOfWeek dayOfWeek);
    
    /**
     * 특정 어르신의 특정 요일 방문요양 시간과 겹치는 시간대 조회
     */
    @Query("""
        SELECT c FROM CareVisitTime c 
        WHERE c.elder.id = :elderId 
        AND c.dayOfWeek = :dayOfWeek 
        AND c.startTime < :endTime 
        AND c.endTime > :startTime
    """)
    List<CareVisitTime> findOverlappingVisitTime(
        @Param("elderId") Long elderId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
    
    void deleteByElderIdAndDayOfWeek(Long elderId, DayOfWeek dayOfWeek);
}
