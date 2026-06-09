package com.homecare.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 방문요양 시간 Entity - 목욕 일정과 방문요양 시간 충돌 검사용
 *
 * 예:
 * - 김○○ 어르신
 * - 월/수/금 09:00~12:00 방문요양
 *
 * 목욕 일정 등록 시 같은 요일, 같은 시간대에 겹치면 배정 불가 처리한다.
 */
@Entity
@Table(name = "care_visit_times", indexes = {
        @Index(name = "idx_care_visit_elder_day", columnList = "elder_id,day_of_week")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareVisitTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 방문요양을 받는 어르신 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elder_id", nullable = false)
    private Elder elder;

    /** 방문 요일 */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /** 방문요양 시작 시간 */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** 방문요양 종료 시간 */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** 메모 */
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