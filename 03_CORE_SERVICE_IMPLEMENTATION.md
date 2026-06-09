# 핵심 Service 구현 가이드

## ScheduleValidationService (일정 검증 서비스)

이 서비스는 목욕 일정 생성 시 다섯 가지 검증을 수행합니다:
1. 성별 제약
2. 방문요양 시간 충돌
3. 팀 시간 중복
4. 목욕 시간 유효성
5. 최소 이동시간

### 구현 코드

```java
package com.homecare.service;

import com.homecare.domain.entity.*;
import com.homecare.domain.enums.Gender;
import com.homecare.domain.enums.ScheduleStatus;
import com.homecare.dto.request.CreateBathScheduleRequest;
import com.homecare.dto.response.ScheduleValidationResponse;
import com.homecare.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ScheduleValidationService {

    private final BathScheduleRepository bathScheduleRepository;
    private final CareVisitTimeRepository careVisitTimeRepository;
    private final ElderRepository elderRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ScheduleRuleRepository scheduleRuleRepository;

    /**
     * 목욕 일정 종합 검증
     * @return 검증 결과 (유효성, 경고, 에러)
     */
    public ScheduleValidationResponse validateSchedule(CreateBathScheduleRequest request) {
        ScheduleValidationResponse response = new ScheduleValidationResponse();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // Step 1: 기본 데이터 검증
            validateBasicData(request, errors);
            if (!errors.isEmpty()) {
                response.setIsValid(false);
                response.setErrors(errors);
                response.setWarnings(warnings);
                return response;
            }

            // 데이터 조회
            Elder elder = elderRepository.findById(request.getElderId()).orElse(null);
            Team team = teamRepository.findById(request.getTeamId()).orElse(null);

            // Step 2: 성별 제약 검증 (경고)
            validateGenderMatch(elder, team, warnings);

            // Step 3: 방문요양 시간 충돌 검증 (에러)
            validateCareVisitConflict(request, elder, errors);

            // Step 4: 팀 시간 중복 검증 (에러)
            validateTeamTimeConflict(request, team, errors);

            // Step 5: 목욕시간 유효성 검증 (에러)
            validateBathDuration(request, errors);

            // Step 6: 최소 이동시간 검증 (에러)
            validateTravelTime(request, errors);

            response.setIsValid(errors.isEmpty());
            response.setWarnings(warnings);
            response.setErrors(errors);

        } catch (Exception e) {
            log.error("일정 검증 중 예외 발생", e);
            response.setIsValid(false);
            response.setErrors(List.of("검증 중 오류 발생: " + e.getMessage()));
        }

        return response;
    }

    /**
     * Step 1: 기본 데이터 검증
     * - Elder 존재 여부
     * - Team 존재 여부
     * - 날짜 유효성 (미래 날짜인지 확인)
     */
    private void validateBasicData(CreateBathScheduleRequest request, List<String> errors) {
        // Elder 확인
        if (!elderRepository.existsById(request.getElderId())) {
            errors.add("ERROR_ELDER_NOT_FOUND: 어르신 정보가 존재하지 않습니다.");
        }

        // Team 확인
        if (!teamRepository.existsById(request.getTeamId())) {
            errors.add("ERROR_TEAM_NOT_FOUND: 팀 정보가 존재하지 않습니다.");
        }

        // 날짜 확인 (최소한 내일 이후)
        if (request.getServiceDate().isBefore(LocalDate.now())) {
            errors.add("ERROR_INVALID_DATE: 과거 날짜는 선택할 수 없습니다.");
        }

        // 시간 확인
        if (request.getStartTime().isAfter(request.getEndTime())) {
            errors.add("ERROR_INVALID_TIME: 시작 시간이 종료 시간보다 클 수 없습니다.");
        }
    }

    /**
     * Step 2: 성별 제약 검증
     * 
     * 규칙: 여성 어르신(FEMALE)에게 남성(MALE) 팀원만 배치되면 경고
     * 
     * 예시:
     * - 여성 어르신 + 여성 팀원 1명 이상 → OK
     * - 여성 어르신 + 남성 팀원만 → WARNING
     */
    private void validateGenderMatch(Elder elder, Team team, List<String> warnings) {
        if (elder == null || team == null) return;

        // 여성 어르신인 경우만 체크
        if (elder.getGender() == Gender.FEMALE) {
            // 해당 팀의 현재 활성 팀원 조회
            List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndEndDateIsNull(team.getId());

            // 팀원이 없으면 경고
            if (activeMembers.isEmpty()) {
                warnings.add("WARN_NO_TEAM_MEMBERS: 팀에 할당된 팀원이 없습니다.");
                return;
            }

            // 모든 팀원이 남성인지 확인
            boolean hasFemaleMember = activeMembers.stream()
                .anyMatch(member -> member.getEmployee().getGender() == Gender.FEMALE);

            if (!hasFemaleMember) {
                String teamMemberNames = activeMembers.stream()
                    .map(m -> m.getEmployee().getName())
                    .collect(Collectors.joining(", "));
                warnings.add(String.format(
                    "WARN_GENDER_MISMATCH: 여성 어르신(%s)에 남성 팀원만 배치됩니다. 팀원: %s",
                    elder.getName(), teamMemberNames
                ));
            }
        }
    }

    /**
     * Step 3: 방문요양 시간 충돌 검증
     * 
     * 규칙: 같은 날짜에 이미 예약된 방문요양 시간과 목욕 시간이 겹쳐서는 안 됨
     * 
     * 예시:
     * - 방문요양: 09:00 ~ 10:00
     * - 신청 목욕: 09:30 ~ 10:30 → ERROR (30분 겹침)
     * - 신청 목욕: 10:00 ~ 11:00 → OK (정확히 끝나고 시작)
     */
    private void validateCareVisitConflict(CreateBathScheduleRequest request, Elder elder, List<String> errors) {
        if (elder == null) return;

        // 같은 날짜의 방문요양 시간 중 시간이 겹치는 것 찾기
        List<CareVisitTime> conflictingTimes = careVisitTimeRepository.findConflictingTimes(
            elder.getId(),
            request.getServiceDate(),
            request.getStartTime(),
            request.getEndTime()
        );

        if (!conflictingTimes.isEmpty()) {
            for (CareVisitTime careVisit : conflictingTimes) {
                errors.add(String.format(
                    "ERROR_CARE_VISIT_CONFLICT: 방문요양 시간과 중복됩니다. " +
                    "(방문요양: %s ~ %s, 신청: %s ~ %s)",
                    careVisit.getStartTime(), careVisit.getEndTime(),
                    request.getStartTime(), request.getEndTime()
                ));
            }
        }
    }

    /**
     * Step 4: 팀 시간 중복 검증
     * 
     * 규칙: 같은 팀이 같은 날짜에 시간이 겹치는 두 개의 목욕 일정을 가질 수 없음
     *       (한 팀은 한 시간에 한 명의 어르신만 방문 가능)
     * 
     * 예시:
     * - 기존 일정: 오전 09:00 ~ 10:00 (어르신 A)
     * - 신청 일정: 오전 09:30 ~ 10:30 (어르신 B) → ERROR
     * - 신청 일정: 오전 10:00 ~ 11:00 (어르신 B) → OK
     */
    private void validateTeamTimeConflict(CreateBathScheduleRequest request, Team team, List<String> errors) {
        if (team == null) return;

        // 같은 팀의 같은 날짜 일정 조회
        List<BathSchedule> existingSchedules = bathScheduleRepository.findByTeamIdAndServiceDate(
            team.getId(),
            request.getServiceDate()
        );

        for (BathSchedule existing : existingSchedules) {
            // CANCELLED 상태는 무시
            if (existing.getStatus() == ScheduleStatus.CANCELLED) {
                continue;
            }

            // 시간 충돌 확인
            if (isTimeConflict(
                request.getStartTime(), request.getEndTime(),
                existing.getStartTime(), existing.getEndTime()
            )) {
                errors.add(String.format(
                    "ERROR_TEAM_TIME_CONFLICT: 같은 팀의 다른 방문과 시간이 중복됩니다. " +
                    "(어르신: %s, 기존: %s ~ %s, 신청: %s ~ %s)",
                    existing.getElder().getName(),
                    existing.getStartTime(), existing.getEndTime(),
                    request.getStartTime(), request.getEndTime()
                ));
            }
        }
    }

    /**
     * Step 5: 목욕 시간 유효성 검증
     * 
     * 규칙: 목욕 시간은 60 ~ 65분 범위 내여야 함
     */
    private void validateBathDuration(CreateBathScheduleRequest request, List<String> errors) {
        Integer minDuration = 60;
        Integer maxDuration = 65;

        // 규칙에서 가져오기 (선택)
        ScheduleRule minRule = scheduleRuleRepository.findByRuleCode("MIN_BATH_DURATION").orElse(null);
        ScheduleRule maxRule = scheduleRuleRepository.findByRuleCode("MAX_BATH_DURATION").orElse(null);

        if (minRule != null) {
            minDuration = Integer.parseInt(minRule.getRuleValue());
        }
        if (maxRule != null) {
            maxDuration = Integer.parseInt(maxRule.getRuleValue());
        }

        if (request.getBathDuration() < minDuration || request.getBathDuration() > maxDuration) {
            errors.add(String.format(
                "ERROR_INVALID_BATH_DURATION: 목욕 시간은 %d~%d분이어야 합니다. (입력: %d분)",
                minDuration, maxDuration, request.getBathDuration()
            ));
        }
    }

    /**
     * Step 6: 최소 이동시간 검증
     * 
     * 규칙: 이동시간은 최소 10분 이상이어야 함
     */
    private void validateTravelTime(CreateBathScheduleRequest request, List<String> errors) {
        Integer minTravelTime = 10;

        // 규칙에서 가져오기
        ScheduleRule rule = scheduleRuleRepository.findByRuleCode("MIN_TRAVEL_TIME").orElse(null);
        if (rule != null) {
            minTravelTime = Integer.parseInt(rule.getRuleValue());
        }

        if (request.getTravelTimeAfter() != null && request.getTravelTimeAfter() < minTravelTime) {
            errors.add(String.format(
                "ERROR_INVALID_TRAVEL_TIME: 이동시간은 최소 %d분이어야 합니다. (입력: %d분)",
                minTravelTime, request.getTravelTimeAfter()
            ));
        }
    }

    /**
     * 두 시간대가 겹치는지 판정
     * 
     * 예시:
     * - 09:00 ~ 10:00 vs 09:30 ~ 10:30 → true (겹침)
     * - 09:00 ~ 10:00 vs 10:00 ~ 11:00 → false (정확히 끝남)
     * - 09:00 ~ 10:00 vs 08:00 ~ 09:00 → false (정확히 끝남)
     */
    private boolean isTimeConflict(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}
```

---

## ScheduleService (일정 관리 서비스)

```java
package com.homecare.service;

import com.homecare.domain.entity.*;
import com.homecare.domain.enums.ScheduleStatus;
import com.homecare.dto.request.CreateBathScheduleRequest;
import com.homecare.dto.request.UpdateBathScheduleRequest;
import com.homecare.dto.response.BathScheduleResponse;
import com.homecare.dto.response.ScheduleValidationResponse;
import com.homecare.repository.*;
import com.homecare.exception.ScheduleConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduleService {

    private final BathScheduleRepository bathScheduleRepository;
    private final ScheduleValidationService validationService;
    private final ElderRepository elderRepository;
    private final TeamRepository teamRepository;
    private final ModelMapper modelMapper;

    /**
     * 목욕 일정 생성 (검증 포함)
     */
    public BathScheduleResponse create(CreateBathScheduleRequest request) {
        // Step 1: 검증
        ScheduleValidationResponse validation = validationService.validateSchedule(request);

        // 에러가 있으면 즉시 실패
        if (!validation.getIsValid()) {
            String errorMessage = String.join(" | ", validation.getErrors());
            log.warn("일정 생성 실패 - 검증 오류: {}", errorMessage);
            throw new ScheduleConflictException("일정 검증 실패: " + errorMessage);
        }

        // 경고가 있으면 로깅
        if (!validation.getWarnings().isEmpty()) {
            log.warn("일정 생성 경고: {}", String.join(" | ", validation.getWarnings()));
        }

        // Step 2: 엔티티 생성 및 저장
        Elder elder = elderRepository.findById(request.getElderId()).orElse(null);
        Team team = teamRepository.findById(request.getTeamId()).orElse(null);

        BathSchedule schedule = BathSchedule.builder()
            .elder(elder)
            .team(team)
            .serviceDate(request.getServiceDate())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .bathDuration(request.getBathDuration())
            .travelTimeAfter(request.getTravelTimeAfter())
            .status(ScheduleStatus.PENDING)
            .notes(request.getNotes())
            .build();

        BathSchedule saved = bathScheduleRepository.save(schedule);
        log.info("목욕 일정 생성: id={}, 어르신={}, 팀={}, 날짜={}",
            saved.getId(), elder.getName(), team.getName(), request.getServiceDate());

        return convertToResponse(saved);
    }

    /**
     * 목욕 일정 조회 (상세)
     */
    @Transactional(readOnly = true)
    public BathScheduleResponse getById(Long id) {
        BathSchedule schedule = bathScheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));
        return convertToResponse(schedule);
    }

    /**
     * 특정 날짜의 모든 일정 조회
     */
    @Transactional(readOnly = true)
    public List<BathScheduleResponse> getSchedulesByDate(LocalDate date) {
        return bathScheduleRepository.findByServiceDateOrderByStartTime(date)
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * 어르신의 모든 일정 조회
     */
    @Transactional(readOnly = true)
    public List<BathScheduleResponse> getSchedulesByElder(Long elderId) {
        return bathScheduleRepository.findByElderId(elderId)
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * 주간 일정 조회
     */
    @Transactional(readOnly = true)
    public List<BathScheduleResponse> getWeeklySchedules(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return bathScheduleRepository.findByDateRange(weekStart, weekEnd)
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * 일정 수정
     */
    public BathScheduleResponse update(Long id, UpdateBathScheduleRequest request) {
        BathSchedule schedule = bathScheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        // 변경할 필드 업데이트
        if (request.getServiceDate() != null) schedule.setServiceDate(request.getServiceDate());
        if (request.getStartTime() != null) schedule.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) schedule.setEndTime(request.getEndTime());
        if (request.getBathDuration() != null) schedule.setBathDuration(request.getBathDuration());
        if (request.getTravelTimeAfter() != null) schedule.setTravelTimeAfter(request.getTravelTimeAfter());
        if (request.getNotes() != null) schedule.setNotes(request.getNotes());

        BathSchedule updated = bathScheduleRepository.save(schedule);
        log.info("일정 수정: id={}", id);

        return convertToResponse(updated);
    }

    /**
     * 일정 확정 (PENDING → CONFIRMED)
     */
    public BathScheduleResponse confirm(Long id) {
        BathSchedule schedule = bathScheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        if (schedule.getStatus() != ScheduleStatus.PENDING) {
            throw new IllegalArgumentException("대기중인 일정만 확정할 수 있습니다.");
        }

        schedule.setStatus(ScheduleStatus.CONFIRMED);
        BathSchedule updated = bathScheduleRepository.save(schedule);
        log.info("일정 확정: id={}", id);

        return convertToResponse(updated);
    }

    /**
     * 일정 완료 (any → COMPLETED)
     */
    public BathScheduleResponse complete(Long id) {
        BathSchedule schedule = bathScheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        if (schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new IllegalArgumentException("취소된 일정은 완료할 수 없습니다.");
        }

        schedule.setStatus(ScheduleStatus.COMPLETED);
        BathSchedule updated = bathScheduleRepository.save(schedule);
        log.info("일정 완료: id={}", id);

        return convertToResponse(updated);
    }

    /**
     * 일정 취소 (any → CANCELLED)
     */
    public BathScheduleResponse cancel(Long id) {
        BathSchedule schedule = bathScheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            throw new IllegalArgumentException("완료된 일정은 취소할 수 없습니다.");
        }

        schedule.setStatus(ScheduleStatus.CANCELLED);
        BathSchedule updated = bathScheduleRepository.save(schedule);
        log.info("일정 취소: id={}", id);

        return convertToResponse(updated);
    }

    /**
     * 일정 삭제
     */
    public void delete(Long id) {
        if (!bathScheduleRepository.existsById(id)) {
            throw new IllegalArgumentException("일정이 존재하지 않습니다.");
        }
        bathScheduleRepository.deleteById(id);
        log.info("일정 삭제: id={}", id);
    }

    /**
     * Entity → Response DTO 변환
     */
    private BathScheduleResponse convertToResponse(BathSchedule schedule) {
        return BathScheduleResponse.builder()
            .id(schedule.getId())
            .elderId(schedule.getElder().getId())
            .elderName(schedule.getElder().getName())
            .teamId(schedule.getTeam().getId())
            .teamName(schedule.getTeam().getName())
            .serviceDate(schedule.getServiceDate())
            .startTime(schedule.getStartTime())
            .endTime(schedule.getEndTime())
            .bathDuration(schedule.getBathDuration())
            .travelTimeAfter(schedule.getTravelTimeAfter())
            .status(schedule.getStatus())
            .notes(schedule.getNotes())
            .createdAt(schedule.getCreatedAt())
            .build();
    }
}
```

---

## 예외 처리 클래스

```java
package com.homecare.exception;

public class ScheduleConflictException extends RuntimeException {
    public ScheduleConflictException(String message) {
        super(message);
    }

    public ScheduleConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
package com.homecare.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "성공", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}
```

---

## 유틸리티 클래스

```java
package com.homecare.util;

import java.time.LocalTime;

public class ScheduleValidator {

    /**
     * 두 시간대가 겹치는지 판정
     */
    public static boolean isTimeConflict(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * 시간 간격 계산 (분)
     */
    public static Integer calculateMinutesBetween(LocalTime start, LocalTime end) {
        return (end.getHour() * 60 + end.getMinute()) - (start.getHour() * 60 + start.getMinute());
    }

    /**
     * 목욕 시간이 유효한지 확인 (60 ~ 65분)
     */
    public static boolean isValidBathDuration(Integer minutes) {
        return minutes >= 60 && minutes <= 65;
    }
}
```

---

## 테스트 케이스 예시

```java
package com.homecare.service;

import com.homecare.domain.entity.*;
import com.homecare.domain.enums.*;
import com.homecare.dto.request.CreateBathScheduleRequest;
import com.homecare.dto.response.ScheduleValidationResponse;
import com.homecare.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ScheduleValidationServiceTest {

    @Autowired private ScheduleValidationService validationService;
    @Autowired private ElderRepository elderRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private CareVisitTimeRepository careVisitTimeRepository;
    @Autowired private BathScheduleRepository bathScheduleRepository;

    private Elder femaleElder;
    private Team team;
    private LocalDate testDate;

    @BeforeEach
    public void setup() {
        // 테스트 데이터 생성
        testDate = LocalDate.now().plusDays(1);

        // 어르신 생성
        femaleElder = elderRepository.save(Elder.builder()
            .name("김영희")
            .gender(Gender.FEMALE)
            .phoneNumber("010-1111-1111")
            .address("서울시")
            .createdAt(LocalDateTime.now())
            .isActive(true)
            .build());

        // 차량 생성
        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
            .vehicleNumber("12가1234")
            .model("그랜저")
            .createdAt(LocalDateTime.now())
            .isActive(true)
            .build());

        // 팀 생성
        team = teamRepository.save(Team.builder()
            .name("오전팀")
            .vehicle(vehicle)
            .createdAt(LocalDateTime.now())
            .isActive(true)
            .build());

        // 직원 생성 (남성)
        Employee maleCaregiver = employeeRepository.save(Employee.builder()
            .name("박준호")
            .gender(Gender.MALE)
            .phoneNumber("010-2222-2222")
            .role(EmployeeRole.CAREGIVER)
            .createdAt(LocalDateTime.now())
            .isActive(true)
            .build());

        // 팀원 추가
        teamMemberRepository.save(TeamMember.builder()
            .team(team)
            .employee(maleCaregiver)
            .startDate(testDate.minusDays(10))
            .assignmentOrder(1)
            .createdAt(LocalDateTime.now())
            .isActive(true)
            .build());
    }

    /**
     * Test 1: 성별 제약 경고 검증
     */
    @Test
    public void testGenderMismatchWarning() {
        CreateBathScheduleRequest request = CreateBathScheduleRequest.builder()
            .elderId(femaleElder.getId())
            .teamId(team.getId())
            .serviceDate(testDate)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 10))
            .bathDuration(60)
            .build();

        ScheduleValidationResponse response = validationService.validateSchedule(request);

        assertTrue(response.getIsValid(), "검증이 통과해야 함");
        assertFalse(response.getWarnings().isEmpty(), "경고가 있어야 함");
        assertTrue(response.getWarnings().stream()
            .anyMatch(w -> w.contains("WARN_GENDER_MISMATCH")), "성별 경고가 있어야 함");
    }

    /**
     * Test 2: 방문요양 시간 충돌 검증
     */
    @Test
    public void testCareVisitConflict() {
        // 방문요양 시간 등록: 09:00 ~ 10:00
        careVisitTimeRepository.save(CareVisitTime.builder()
            .elder(femaleElder)
            .serviceDate(testDate)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .caregiver("이소영")
            .createdAt(LocalDateTime.now())
            .build());

        // 충돌하는 목욕 일정 신청: 09:30 ~ 10:30
        CreateBathScheduleRequest request = CreateBathScheduleRequest.builder()
            .elderId(femaleElder.getId())
            .teamId(team.getId())
            .serviceDate(testDate)
            .startTime(LocalTime.of(9, 30))
            .endTime(LocalTime.of(10, 30))
            .bathDuration(60)
            .build();

        ScheduleValidationResponse response = validationService.validateSchedule(request);

        assertFalse(response.getIsValid(), "검증이 실패해야 함");
        assertFalse(response.getErrors().isEmpty(), "에러가 있어야 함");
        assertTrue(response.getErrors().stream()
            .anyMatch(e -> e.contains("ERROR_CARE_VISIT_CONFLICT")), "방문요양 충돌 에러가 있어야 함");
    }

    /**
     * Test 3: 목욕 시간 유효성 검증
     */
    @Test
    public void testInvalidBathDuration() {
        CreateBathScheduleRequest request = CreateBathScheduleRequest.builder()
            .elderId(femaleElder.getId())
            .teamId(team.getId())
            .serviceDate(testDate)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 50))  // 110분
            .bathDuration(110)  // 60~65분 범위 벗어남
            .build();

        ScheduleValidationResponse response = validationService.validateSchedule(request);

        assertFalse(response.getIsValid(), "검증이 실패해야 함");
        assertTrue(response.getErrors().stream()
            .anyMatch(e -> e.contains("ERROR_INVALID_BATH_DURATION")), "목욕 시간 에러가 있어야 함");
    }

    /**
     * Test 4: 정상적인 일정 신청
     */
    @Test
    public void testValidSchedule() {
        CreateBathScheduleRequest request = CreateBathScheduleRequest.builder()
            .elderId(femaleElder.getId())
            .teamId(team.getId())
            .serviceDate(testDate)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 5))
            .bathDuration(60)
            .build();

        ScheduleValidationResponse response = validationService.validateSchedule(request);

        assertTrue(response.getIsValid(), "검증이 통과해야 함");
        assertTrue(response.getErrors().isEmpty(), "에러가 없어야 함");
    }
}
```

---

## 다음 단계

1. GlobalExceptionHandler 구현
2. SecurityConfig & JWT 설정
3. Controller 작성
4. 실제 테스트 수행
5. Swagger 문서 생성
