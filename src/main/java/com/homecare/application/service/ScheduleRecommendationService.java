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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 신규 어르신을 위한 추천 목욕 슬롯 생성 서비스
 *
 * MVP 방식:
 * 1. 성별 제약으로 가능한 차량 필터
 * 2. 방문요양 시간 제외
 * 3. 기존 목욕 일정과 겹치지 않는 후보 슬롯 생성
 * 4. region 기반 단순 이동 비용 계산
 * 5. 비용 낮은 순서로 상위 3개 추천
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleRecommendationService {

    private final BathScheduleRepository bathScheduleRepository;
    private final CareVisitTimeRepository careVisitTimeRepository;
    private final ElderRepository elderRepository;
    private final VehicleRepository vehicleRepository;

    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);
    private static final int SLOT_DURATION = 60;
    private static final int SLOT_INTERVAL = 30;
    private static final int RECOMMENDED_COUNT = 3;

    public List<ScheduleSlotResponse> recommendSlots(Long elderId, LocalDate serviceDate) {
        Elder elder = elderRepository.findById(elderId)
                .orElseThrow(() -> new RuntimeException("어르신을 찾을 수 없습니다"));

        List<Vehicle> availableVehicles = filterVehiclesByGender(elder);

        if (availableVehicles.isEmpty()) {
            log.warn("성별 제약으로 인해 사용 가능한 차량이 없습니다. elderId={}", elderId);
            return Collections.emptyList();
        }

        DayOfWeek dayOfWeek = serviceDate.getDayOfWeek();
        List<CareVisitTime> careVisits =
                careVisitTimeRepository.findByElderIdAndDayOfWeek(elderId, dayOfWeek);

        List<ScheduleSlot> allSlots =
                generateAllSlots(elder, availableVehicles, serviceDate, careVisits);

        return allSlots.stream()
                .sorted(
                        Comparator.comparing(ScheduleSlot::getTransportCost)
                                .thenComparing(ScheduleSlot::getStartTime)
                )
                .limit(RECOMMENDED_COUNT)
                .map(slot -> ScheduleSlotResponse.builder()
                        .vehicleId(slot.getVehicle().getId())
                        .vehicleName(slot.getVehicle().getName())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .score(slot.getTransportCost())
                        .reason(generateReason(slot, elder))
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<Vehicle> filterVehiclesByGender(Elder elder) {
        List<Vehicle> vehicles = vehicleRepository.findAll();

        return vehicles.stream()
                .filter(vehicle -> {
                    if (Gender.FEMALE.equals(elder.getGender()) && !elder.getAllowMaleStaff()) {
                        return !vehicle.getHasMaleStaff();
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<ScheduleSlot> generateAllSlots(
            Elder elder,
            List<Vehicle> vehicles,
            LocalDate serviceDate,
            List<CareVisitTime> careVisits
    ) {
        List<ScheduleSlot> slots = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            List<BathSchedule> vehicleSchedules =
                    bathScheduleRepository.findByServiceDateAndVehicleIdOrderByStartTimeAsc(
                            serviceDate,
                            vehicle.getId()
                    );

            for (
                    LocalTime slotStart = WORK_START;
                    !slotStart.plusMinutes(SLOT_DURATION).isAfter(WORK_END);
                    slotStart = slotStart.plusMinutes(SLOT_INTERVAL)
            ) {
                LocalTime slotEnd = slotStart.plusMinutes(SLOT_DURATION);

                if (isSlotValid(slotStart, slotEnd, vehicleSchedules, careVisits)) {
                    int transportCost =
                            calculateTransportCost(elder, slotStart, slotEnd, vehicleSchedules);

                    slots.add(new ScheduleSlot(vehicle, slotStart, slotEnd, transportCost));
                }
            }
        }

        return slots;
    }

    private boolean isSlotValid(
            LocalTime startTime,
            LocalTime endTime,
            List<BathSchedule> vehicleSchedules,
            List<CareVisitTime> careVisits
    ) {
        boolean hasConflictWithBath = vehicleSchedules.stream()
                .anyMatch(schedule ->
                        isTimeOverlap(
                                startTime,
                                endTime,
                                schedule.getStartTime(),
                                schedule.getEndTime()
                        )
                );

        if (hasConflictWithBath) {
            return false;
        }

        boolean hasConflictWithVisit = careVisits.stream()
                .anyMatch(visit ->
                        isTimeOverlap(
                                startTime,
                                endTime,
                                visit.getStartTime(),
                                visit.getEndTime()
                        )
                );

        return !hasConflictWithVisit;
    }

    private boolean isTimeOverlap(
            LocalTime newStart,
            LocalTime newEnd,
            LocalTime existingStart,
            LocalTime existingEnd
    ) {
        return newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);
    }

    /**
     * region 기반 단순 이동 비용 계산.
     *
     * 핵심:
     * - 후보 슬롯 앞 일정
     * - 신규 어르신
     * - 후보 슬롯 뒤 일정
     *
     * 이 세 구간의 지역 차이를 점수화한다.
     */
    private int calculateTransportCost(
            Elder newElder,
            LocalTime slotStart,
            LocalTime slotEnd,
            List<BathSchedule> schedules
    ) {
        BathSchedule previous = schedules.stream()
                .filter(schedule -> !schedule.getEndTime().isAfter(slotStart))
                .max(Comparator.comparing(BathSchedule::getEndTime))
                .orElse(null);

        BathSchedule next = schedules.stream()
                .filter(schedule -> !schedule.getStartTime().isBefore(slotEnd))
                .min(Comparator.comparing(BathSchedule::getStartTime))
                .orElse(null);

        int cost = 0;

        if (previous != null) {
            cost += regionCost(previous.getElder().getRegion(), newElder.getRegion());
        }

        if (next != null) {
            cost += regionCost(newElder.getRegion(), next.getElder().getRegion());
        }

        if (previous == null && next == null) {
            cost += 20;
        }

        return cost;
    }

    private int regionCost(String fromRegion, String toRegion) {
        if (fromRegion == null || toRegion == null) {
            return 10;
        }

        if (fromRegion.equals(toRegion)) {
            return 0;
        }

        return 10;
    }

    private String generateReason(ScheduleSlot slot, Elder elder) {
        return String.format(
                "%s 배정 가능, %s~%s 추천. 지역 기준 이동 비용 점수: %d",
                slot.getVehicle().getName(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getTransportCost()
        );
    }

    private static class ScheduleSlot {
        private final Vehicle vehicle;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final int transportCost;

        public ScheduleSlot(
                Vehicle vehicle,
                LocalTime startTime,
                LocalTime endTime,
                int transportCost
        ) {
            this.vehicle = vehicle;
            this.startTime = startTime;
            this.endTime = endTime;
            this.transportCost = transportCost;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public int getTransportCost() {
            return transportCost;
        }
    }
}