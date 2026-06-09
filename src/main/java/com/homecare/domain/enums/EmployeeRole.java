package com.homecare.domain.enums;

/**
 * 직원 역할 Enum - 직원의 직책/담당 역할을 나타냅니다.
 * 
 * 주요 역할:
 * - 직원의 직책 구분 (요양보호사 vs 기사)
 * - 역할별 권한/기능 제한 (요양보호사만 어르신 담당, 기사만 운전)
 * - 역할별 자격증 요구사항 정의 (요양보호사 자격증 필수 vs 운전면허 필수)
 * - 팀 구성 시 역할 검증 (팀에 최소 1명 이상의 요양보호사 포함 필수)
 * 
 * === Enum 값 설명 ===
 * CAREGIVER : 요양보호사 - 어르신의 목욕 서비스를 직접 제공하는 직원
 * DRIVER    : 기사 - 차량을 운전하고 어르신 이동을 담당하는 직원
 */
public enum EmployeeRole {
    CAREGIVER("요양보호사"),
    DRIVER("기사");

    private final String description;

    EmployeeRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
