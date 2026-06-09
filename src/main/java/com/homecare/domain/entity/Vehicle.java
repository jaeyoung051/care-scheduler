package com.homecare.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 차량 Entity - MVP 단순화 버전
 *
 * 관리 정보:
 * - 차량명: "1호차", "2호차"
 * - 남성 종사자 포함 여부
 * - 메모
 *
 * DB: vehicles
 */
@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 차량명: "1호차", "2호차" */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** 남성 종사자 포함 여부 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean hasMaleStaff = false;

    /** 특이사항 메모 */
    @Column(columnDefinition = "TEXT")
    private String memo;

    /** 생성 시간 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 수정 시간 */
    @Column
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