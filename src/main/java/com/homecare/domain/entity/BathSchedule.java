package com.homecare.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 목욕 일정 Entity - MVP 단순화 버전
 * 
 * 관리 정보:
 * - 어르신, 차량, 날짜, 시작/종료 시간, 메모
 * - 상태 추적 없음 (최종 확정 일정만 저장)
 * 
 * 관계: 1명 어르신 + 1대 차량 → 1개 목욕 일정
 * 
 * DB: bath_schedules
 */
@Entity
@Table(name = "bath_schedules", indexes = {
    @Index(name = "idx_service_date", columnList = "service_date"),
    @Index(name = "idx_elder_id", columnList = "elder_id"),
    @Index(name = "idx_vehicle_id", columnList = "vehicle_id"),
    @Index(name = "idx_vehicle_date", columnList = "vehicle_id,service_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BathSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 목욕 서비스를 받는 어르신 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elder_id", nullable = false)
    private Elder elder;

    /** 목욕 서비스를 담당할 차량 (기존 team 대신 vehicle 직접 연결) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /** 서비스 날짜 */
    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    /** 서비스 시작 시간 */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** 서비스 종료 시간 (검증: endTime - startTime >= 60분) */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** 특이사항 메모 */
    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}