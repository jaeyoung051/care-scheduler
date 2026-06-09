package com.homecare.domain.enums;

/**
 * 사용자 역할 Enum - 시스템 사용자(관리자/매니저)의 권한 레벨을 나타냅니다.
 * 
 * 주요 역할:
 * - 로그인 사용자의 권한 레벨 지정
 * - 메뉴/기능 접근 제어 (예: 관리자만 사용자 관리 가능)
 * - API 엔드포인트 권한 검증 (@PreAuthorize)
 * - 감사 로깅 (누가 어떤 작업을 했는가 추적)
 * 
 * === Enum 값 설명 ===
 * ADMIN    : 관리자 - 시스템 전체 관리 권한 (사용자, 규칙, 설정 모두 관리 가능)
 * MANAGER  : 매니저 - 일정 관리 및 팀/직원 관리 권한 (전체 시스템 설정은 불가)
 */
public enum UserRole {
    ADMIN("관리자"),
    MANAGER("매니저");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
