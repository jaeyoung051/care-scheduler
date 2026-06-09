package com.homecare.application.service;

import com.homecare.domain.entity.BathSchedule;
import com.homecare.domain.entity.CareVisitTime;
import com.homecare.domain.entity.Elder;
import com.homecare.domain.entity.Vehicle;
import com.homecare.domain.repository.BathScheduleRepository;
import com.homecare.domain.repository.CareVisitTimeRepository;
import com.homecare.domain.repository.ElderRepository;
import com.homecare.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 목욕 일정 검증 서비스
 * 
 * 검증 규칙:
 * 1. 동일 차량 시간 겹침: 같은 차량/날짜에서 기존 일정과 시간 중복 확인
 * 2. 목욕 시간 검사: startTime ~ endTime >= 60분 확인
 * 3. 방문요양 시간 겹침: 해당 어르신의 방문요양 시간과 중복 확인
 * 4. 성별/차량 제약: 여성 어르신 + 남성 거부 + 차량에 남성 포함 = 거절
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleValidationService {
    private final BathScheduleRepository bathScheduleRepository;
    private final CareVisitTimeRepository careVisitTimeRepository;
    private final ElderRepository elderRepository;
    private final VehicleRepository vehicleRepository;

    private static final int MIN_BATH_DURATION = 60; // 최소 목욕 시간 (분)
    private static final int MAX_BATH_DURATION = 90; // 최대 목욕 시간 (분) - 경고 기준

    /**
     * 목욕 일정 전체 검증
     */
    public ValidationResult validateSchedule(Long elderId, Long vehicleId, LocalDate serviceDate, LocalTime startTime, LocalTime endTime) {
        ValidationResult result = new ValidationResult();

        // Rule 1: 동일 차량 시간 겹침 검사
        if (!validateVehicleTimeConflict(vehicleId, serviceDate, startTime, endTime)) {
            result.addError("Rule1", "같은 차량의 해당 시간대에 이미 예약된 일정이 있습니다");
        }

        // Rule 2: 목욕 시간 검사
        String durationWarning = validateBathDuration(startTime, endTime);
        if (durationWarning != null) {
            result.addWarning("Rule2", durationWarning);
        }

        // Rule 3: 방문요양 시간 겹침 검사
        if (!validateCareVisitTimeConflict(elderId, serviceDate, startTime, endTime)) {
            result.addError("Rule3", "해당 요일의 방문요양 시간과 겹칩니다");
        }

        // Rule 4: 성별/차량 제약 검사
        String genderWarning = validateGenderConstraint(elderId, vehicleId);
        if (genderWarning != null) {
            result.addWarning("Rule4", genderWarning);
        }

        return result;
    }

    /**
     * Rule 1: 동일 차량에서 시간 겹침 확인
     * @return 겹침이 없으면 true
     */
    public boolean validateVehicleTimeConflict(Long vehicleId, LocalDate serviceDate, LocalTime startTime, LocalTime endTime) {
        List<BathSchedule> conflicts = bathScheduleRepository.findOverlappingSchedules(vehicleId, serviceDate, startTime, endTime);
        return conflicts.isEmpty();
    }

    /**
     * Rule 2: 목욕 시간 검사 (60~65분 권장, 60분 미만은 경고)
     * @return 문제가 없으면 null, 경고 메시지가 있으면 문자열 반환
     */
    public String validateBathDuration(LocalTime startTime, LocalTime endTime) {
        long durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime);
        
        if (durationMinutes < MIN_BATH_DURATION) {
            return String.format("목욕 시간이 %d분 미만입니다 (최소 %d분 필요)", durationMinutes, MIN_BATH_DURATION);
        }
        
        if (durationMinutes > MAX_BATH_DURATION) {
            return String.format("목욕 시간이 %d분으로 표준(%d~65분)을 초과합니다", durationMinutes, MIN_BATH_DURATION);
        }
        
        return null;
    }

    /**
     * Rule 3: 해당 어르신의 방문요양 시간과 겹침 확인
     * @return 겹침이 없으면 true
     */
    public boolean validateCareVisitTimeConflict(Long elderId, LocalDate serviceDate, LocalTime startTime, LocalTime endTime) {
        DayOfWeek dayOfWeek = serviceDate.getDayOfWeek();
        List<CareVisitTime> conflicts = careVisitTimeRepository.findOverlappingVisitTime(elderId, dayOfWeek, startTime, endTime);
        return conflicts.isEmpty();
    }

    /**
     * Rule 4: 성별/차량 제약 확인
     * 여성 어르신 + 남성 거부 + 차량에 남성 포함 = 경고 또는 거절
     * @return 문제가 없으면 null, 경고/오류 메시지가 있으면 문자열 반환
     */
    public String validateGenderConstraint(Long elderId, Long vehicleId) {
        Elder elder = elderRepository.findById(elderId).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);

        if (elder == null || vehicle == null) {
            return null;
        }

        // 여성 + 남성 거부 + 차량에 남성 포함 = 경고
        if (elder.getGender().equals(com.homecare.domain.enums.Gender.FEMALE) &&
            !elder.getAllowMaleStaff() &&
            vehicle.getHasMaleStaff()) {
            return "여성 어르신이 남성 종사자를 거부하는데, 해당 차량(2호차)에 남성 기사가 포함됩니다. 확인 후 진행해주세요.";
        }

        return null;
    }

    /**
     * 검증 결과를 담는 내부 클래스
     */
    public static class ValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();

        public void addError(String rule, String message) {
            errors.add(String.format("[%s] %s", rule, message));
        }

        public void addWarning(String rule, String message) {
            warnings.add(String.format("[%s] %s", rule, message));
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public java.util.List<String> getErrors() {
            return errors;
        }

        public java.util.List<String> getWarnings() {
            return warnings;
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "errors=" + errors +
                    ", warnings=" + warnings +
                    '}';
        }
    }
}
