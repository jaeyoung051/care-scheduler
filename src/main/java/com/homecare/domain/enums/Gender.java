package com.homecare.domain.enums;

/**
 * 성별 Enum - 시스템의 모든 사람(어르신, 직원)
의 성별을 나타냅니다.
 * 
 * 주요 역할:
 * - 어르신의 성별 정보 저장
 * - 직원의 성별 정보 저장
 * - 팀 배정 시 성별 제약 조건 검증 (어르신이 거부하는 성별 확인)
 * - 성별 기반 필터링 쿼리에서 사용
 * 
 * === Enum 값 설명 ===
 * MALE    : 남성 - 남성 요양보호사, 남성 기사, 남성 어르신
 * FEMALE  : 여성 - 여성 요양보호사, 여성 기사, 여성 어르신
 */
public enum Gender {
    MALE("남성"),
    FEMALE("여성");

    private final String description;

    Gender(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
