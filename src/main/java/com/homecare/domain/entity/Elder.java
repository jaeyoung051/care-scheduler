package com.homecare.domain.entity;

import com.homecare.domain.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 어르신 Entity - MVP 단순화 버전
 * 
 * 관리 정보:
 * - 기본 정보: 이름, 성별, 주소/지역
 * - 서비스 제약: 남성 종사자 허용 여부
 * - 특수 사항: 메모
 * 
 * 관계: 1명의 어르신 → 여러 목욕 일정 + 여러 방문요양 시간
 * 
 * DB: elders
 */
@Entity
@Table(name = "elders", indexes = {
    @Index(name = "idx_gender", columnList = "gender"),
    @Index(name = "idx_region", columnList = "region")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Elder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어르신 이름 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 성별: MALE 또는 FEMALE */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    /** 기본 주소 */
    @Column(nullable = false, length = 200)
    private String address;

    /** 지역 코드: "광안", "수영", "전포" 등 (추천 슬롯 계산에 사용) */
    @Column(length = 50)
    private String region;

    /** 남성 종사자 허용 여부 (false면 여성만 배정) */
    @Column(nullable = false)
    @Builder.Default
    private Boolean allowMaleStaff = true;

    /** 특수 요청사항 메모 */
    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

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
