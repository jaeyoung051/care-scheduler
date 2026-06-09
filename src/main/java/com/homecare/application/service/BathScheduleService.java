package com.homecare.application.service;

import com.homecare.api.dto.request.CreateBathScheduleRequest;
import com.homecare.api.dto.response.BathScheduleResponse;
import com.homecare.domain.entity.BathSchedule;
import com.homecare.domain.entity.Elder;
import com.homecare.domain.entity.Vehicle;
import com.homecare.domain.repository.BathScheduleRepository;
import com.homecare.domain.repository.ElderRepository;
import com.homecare.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 목욕 일정 관리 Service (CRUD + 검증)
 */
@Service
@RequiredArgsConstructor
public class BathScheduleService {
    private final BathScheduleRepository bathScheduleRepository;
    private final ElderRepository elderRepository;
    private final VehicleRepository vehicleRepository;
    private final ScheduleValidationService validationService;
    private final ModelMapper modelMapper;

    @Transactional
    public BathScheduleResponse createSchedule(CreateBathScheduleRequest request) {
        // 엔티티 조회
        Elder elder = elderRepository.findById(request.getElderId())
            .orElseThrow(() -> new RuntimeException("어르신을 찾을 수 없습니다"));
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
            .orElseThrow(() -> new RuntimeException("차량을 찾을 수 없습니다"));

        // 검증
        ScheduleValidationService.ValidationResult validation = validationService.validateSchedule(
            request.getElderId(),
            request.getVehicleId(),
            request.getServiceDate(),
            request.getStartTime(),
            request.getEndTime()
        );

        if (!validation.isValid()) {
            throw new RuntimeException("일정 검증 실패: " + String.join(", ", validation.getErrors()));
        }

        // 저장
        BathSchedule schedule = BathSchedule.builder()
            .elder(elder)
            .vehicle(vehicle)
            .serviceDate(request.getServiceDate())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .memo(request.getMemo())
            .build();

        BathSchedule saved = bathScheduleRepository.save(schedule);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BathScheduleResponse getSchedule(Long id) {
        BathSchedule schedule = bathScheduleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다"));
        return toResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<BathScheduleResponse> getSchedulesByDate(LocalDate date) {
        return bathScheduleRepository.findByServiceDate(date).stream()
            .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BathScheduleResponse> getSchedulesByElder(Long elderId) {
        return bathScheduleRepository.findByElderIdOrderByServiceDateAscStartTimeAsc(elderId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BathScheduleResponse> getSchedulesByVehicle(Long vehicleId) {
        return bathScheduleRepository.findByVehicleIdOrderByServiceDateAscStartTimeAsc(vehicleId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public BathScheduleResponse updateSchedule(Long id, CreateBathScheduleRequest request) {
        BathSchedule schedule = bathScheduleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다"));

        Elder elder = elderRepository.findById(request.getElderId())
            .orElseThrow(() -> new RuntimeException("어르신을 찾을 수 없습니다"));
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
            .orElseThrow(() -> new RuntimeException("차량을 찾을 수 없습니다"));

        // 검증
        ScheduleValidationService.ValidationResult validation = validationService.validateSchedule(
            request.getElderId(),
            request.getVehicleId(),
            request.getServiceDate(),
            request.getStartTime(),
            request.getEndTime()
        );

        if (!validation.isValid()) {
            throw new RuntimeException("일정 검증 실패: " + String.join(", ", validation.getErrors()));
        }

        schedule.setElder(elder);
        schedule.setVehicle(vehicle);
        schedule.setServiceDate(request.getServiceDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setMemo(request.getMemo());

        BathSchedule updated = bathScheduleRepository.save(schedule);
        return toResponse(updated);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        bathScheduleRepository.deleteById(id);
    }

    private BathScheduleResponse toResponse(BathSchedule schedule) {
        return BathScheduleResponse.builder()
            .id(schedule.getId())
            .elderId(schedule.getElder().getId())
            .elderName(schedule.getElder().getName())
            .vehicleId(schedule.getVehicle().getId())
            .vehicleName(schedule.getVehicle().getName())
            .serviceDate(schedule.getServiceDate())
            .startTime(schedule.getStartTime())
            .endTime(schedule.getEndTime())
            .memo(schedule.getMemo())
            .createdAt(schedule.getCreatedAt())
            .updatedAt(schedule.getUpdatedAt())
            .build();
    }
}
