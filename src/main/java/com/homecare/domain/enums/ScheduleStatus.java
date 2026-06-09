package com.homecare.domain.enums;

/**
 * 목욕 일정 상태 Enum - 목욕 서비스 일정의 생명주기 상태를 나타냅니다.
 * 
 * 주요 역할:
 * - 목욕 일정의 상태 관리 (상태 머신 패턴)
 * - 상태별 쿼리 필터링 (예: 완료되지 않은 일정만 조회)
 * - 상태 전이 검증 (특정 상태에서만 다른 상태로 변경 가능)
 * 
 * === Enum 값 설명 ===
 * PENDING     : 대기중 - 일정이 처음 생성된 상태, 아직 확정되지 않음
 * CONFIRMED   : 확정 - 팀/직원이 확정하여 서비스 가능 상태
 * COMPLETED   : 완료 - 목욕 서비스가 정상적으로 완료됨
 * CANCELLED   : 취소 - 어르신이나 센터에서 일정을 취소함
 */
public enum ScheduleStatus {
    PENDING("대기중"),
    CONFIRMED("확정"),
    COMPLETED("완료"),
    CANCELLED("취소");

    private final String description;

    ScheduleStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
