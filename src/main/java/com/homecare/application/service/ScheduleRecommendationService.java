package com.homecare.application.service;

import com.homecare.api.dto.response.ScheduleSlotResponse;
import com.homecare.domain.entity.BathSchedule;
import com.homecare.domain.entity.CareVisitTime;
import com.homecare.domain.entity.Elder;
import com.homecare.domain.entity.Vehicle;
import com.homecare.domain.enums.Gender;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 신규 어르신을 위한 추천 목욕 슬롯 생성 서비스
 * 
 * 알고리즘:
 * 1. 성별 제약으로 가능한 차량 필터
 * 2. 해당 요일의 방문요양 시간 제외
 * 3. 기존 BathSchedule 조회
 * 4. 30분 단위 슬롯 생성 (09:00 ~ 18:00)
 * 5. 각 슬롯의 이동 비용 계산 (region 기반 단순 점수)
 * 6. 비용 낮은 순서로 상위 2-3개 반환
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleRecommendationService {
    private final BathScheduleRepository bathScheduleRepository;
    private final CareVisitTimeRepository careVisitTimeRepository;
    private final ElderRepository elderRepository;
    private final VehicleRepository vehicleRepository;
    private final ScheduleValidationService validationService;

    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);
    private static final int SLOT_DURATION = 60; // 60분 목욕
    private static final int SLOT_INTERVAL = 30; // 30분 단위 슬롯
    private static final int RECOMMENDED_COUNT = 3; // 추천할 슬롯 개수

    /**
     * 신규 어르신을 위한 추천 슬롯 생성
     */
    public List<ScheduleSlotResponse> recommendSlots(Long elderId, LocalDate serviceDate) {
        Elder elder = elderRepository.findById(elderId)
            .orElseThrow(() -> new RuntimeException("어르신을 찾을 수 없습니다"));

        // Step 1: 성별 제약으로 가능한 차량 필터
        List<Vehicle> availableVehicles = filterVehiclesByGender(elder);
        if (availableVehicles.isEmpty()) {
            log.warn("성별 제약으로 인해 사용 가능한 차량이 없습니다 (elderId: {})", elderId);
            return Collections.emptyList();
        }

        // Step 2: 해당 요일의 방문요양 시간 조회
        DayOfWeek dayOfWeek = serviceDate.getDayOfWeek();
        List<CareVisitTime> careVisits = careVisitTimeRepository.findByElderIdAndDayOfWeek(elderId, dayOfWeek)
            .map(Collections::singletonList)
            .orElse(Collections.emptyList());

        // Step 3: 모든 가능한 슬롯 생성
        List<ScheduleSlot> allSlots = generateAllSlots(availableVehicles, serviceDate, careVisits);

        // Step 4: 이동 비용으로 정렬
        List<ScheduleSlot> sortedSlots = allSlots.stream()
            .sorted(Comparator.comparing(ScheduleSlot::getTransportCost).thenComparing(ScheduleSlot::getStartTime))
            .limit(RECOMMENDED_COUNT)
            .collect(Collectors.toList());

        // Step 5: Response 변환
        return sortedSlots.stream()
            .map(slot -> ScheduleSlotResponse.builder()
                .vehicleId(slot.getVehicle().getId())
                .vehicleName(slot.getVehicle().getName())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .score(slot.getTransportCost())
                .reason(generateReason(slot, elder))
                .build())
            .collect(Collectors.toList());
    }

    /**
     * 성별 제약으로 가능한 차량 필터
     */
    private List<Vehicle> filterVehiclesByGender(Elder elder) {
        List<Vehicle> vehicles = vehicleRepository.findAll();

        return vehicles.stream()
            .filter(v -> {
                // 여성 + 남성 거부 → 남성 미포함 차량만 가능
                if (Gender.FEMALE.equals(elder.getGender()) && !elder.getAllowMaleStaff()) {
                    return !v.getHasMaleStaff();
                }
                // 남성 또는 남성 허용 → 모든 차량 가능
                return true;
            })
            .collect(Collectors.toList());
    }

    /**
     * 가능한 모든 슬롯 생성
     */
    private List<ScheduleSlot> generateAllSlots(List<Vehicle> vehicles, LocalDate serviceDate, List<CareVisitTime> careVisits) {
        List<ScheduleSlot> slots = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            // 해당 차량의 기존 일정 조회
            List<BathSchedule> vehicleSchedules = bathScheduleRepository.findByServiceDateAndVehicleIdOrderByStartTimeAsc(serviceDate, vehicle.getId());

            // 30분 단위 슬롯 생성
            for (LocalTime slotStart = WORK_START; 
                 slotStart.plusMinutes(SLOT_DURATION).isBefore(WORK_END) || slotStart.plusMinutes(SLOT_DURATION).equals(WORK_END);
                 slotStart = slotStart.plusMinutes(SLOT_INTERVAL)) {

                LocalTime slotEnd = slotStart.plusMinutes(SLOT_DURATION);

                // 슬롯 유효성 검사
                if (isSlotValid(slotStart, slotEnd, vehicleSchedules, careVisits)) {
                    int transportCost = calculateTransportCost(vehicle, vehicleSchedules);
                    slots.add(new ScheduleSlot(vehicle, slotStart, slotEnd, transportCost));
                }
            }
        }

        return slots;
    }

    /**
     * 슬롯 유효성 검사 (기존 일정 및 방문요양과 겹치지 않음)
     */
    private boolean isSlotValid(LocalTime startTime, LocalTime endTime, 
                                List<BathSchedule> vehicleSchedules, 
                                List<CareVisitTime> careVisits) {
        // 기존 목욕 일정과 겹치는지 확인
        boolean hasConflictWithBath = vehicleSchedules.stream()
            .anyMatch(schedule -> !(schedule.getEndTime().isBefore(startTime) || schedule.getStartTime().isAfter(endTime)));

        if (hasConflictWithBath) {
            return false;
        }

        // 방문요양 시간과 겹치는지 확인
        boolean hasConflictWithVisit = careVisits.stream()
            .anyMatch(visit -> !(visit.getEndTime().isBefore(startTime) || visit.getStartTime().isAfter(endTime)));

        return !hasConflictWithVisit;
    }

    /**
     * 이동 비용 계산 (region 기반 단순 점수)
     * 추가 이동 비용 기반: 앞 어르신 region → 신규 어르신 → 뒤 어르신 region
     */
    private int calculateTransportCost(Vehicle vehicle, List<BathSchedule> schedules) {
        // MVP 단순화: 차량의 최근 일정과의 이동 거리 대략 계산
        if (schedules.isEmpty()) {
            return 0; // 기존 일정이 없으면 비용 0
        }

        // 마지막 일정 이후 차량의 위치 고려
        // (실제로는 Kakao Map API로 계산하지만, MVP에서는 단순화)
        return schedules.size() * 10; // 슬롯 수에 따른 단순 점수
    }

    /**
     * 추천 이유 생성
     */
    private String generateReason(ScheduleSlot slot, Elder elder) {
        // TODO: 더 상세한 이유 작성
        return String.format("추천 차량: %s, 시간: %02d:%02d~%02d:%02d",
            slot.getVehicle().getName(),
            slot.getStartTime().getHour(), slot.getStartTime().getMinute(),
            slot.getEndTime().getHour(), slot.getEndTime().getMinute());
    }

    /**
     * 슬롯 정보를 담는 내부 클래스
     */
    private static class ScheduleSlot {
        private final Vehicle vehicle;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final int transportCost;

        public ScheduleSlot(Vehicle vehicle, LocalTime startTime, LocalTime endTime, int transportCost) {
            this.vehicle = vehicle;
            this.startTime = startTime;
            this.endTime = endTime;
            this.transportCost = transportCost;
        }

        public Vehicle getVehicle() { return vehicle; }
        public LocalTime getStartTime() { return startTime; }
        public LocalTime getEndTime() { return endTime; }
        public int getTransportCost() { return transportCost; }
    }
}
