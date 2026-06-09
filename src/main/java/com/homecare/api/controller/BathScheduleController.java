package com.homecare.api.controller;

import com.homecare.api.dto.request.CreateBathScheduleRequest;
import com.homecare.api.dto.response.BathScheduleResponse;
import com.homecare.application.service.BathScheduleService;
import com.homecare.application.service.ScheduleRecommendationService;
import com.homecare.application.service.ScheduleValidationService;
import com.homecare.api.dto.response.ScheduleSlotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 목욕 일정 관리 API
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class BathScheduleController {
    private final BathScheduleService bathScheduleService;
    private final ScheduleValidationService validationService;
    private final ScheduleRecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<BathScheduleResponse>> getSchedules(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) Long elderId,
        @RequestParam(required = false) Long vehicleId) {

        if (date != null) {
            return ResponseEntity.ok(bathScheduleService.getSchedulesByDate(date));
        }
        if (elderId != null) {
            return ResponseEntity.ok(bathScheduleService.getSchedulesByElder(elderId));
        }
        if (vehicleId != null) {
            return ResponseEntity.ok(bathScheduleService.getSchedulesByVehicle(vehicleId));
        }

        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BathScheduleResponse> getSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(bathScheduleService.getSchedule(id));
    }

    @PostMapping
    public ResponseEntity<BathScheduleResponse> createSchedule(@RequestBody CreateBathScheduleRequest request) {
        return ResponseEntity.ok(bathScheduleService.createSchedule(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BathScheduleResponse> updateSchedule(@PathVariable Long id, @RequestBody CreateBathScheduleRequest request) {
        return ResponseEntity.ok(bathScheduleService.updateSchedule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        bathScheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 일정 검증 (저장 전 미리 확인)
     */
    @PostMapping("/validate")
    public ResponseEntity<ScheduleValidationService.ValidationResult> validateSchedule(
        @RequestBody CreateBathScheduleRequest request) {

        ScheduleValidationService.ValidationResult result = validationService.validateSchedule(
            request.getElderId(),
            request.getVehicleId(),
            request.getServiceDate(),
            request.getStartTime(),
            request.getEndTime()
        );

        return ResponseEntity.ok(result);
    }

    /**
     * 신규 어르신 추천 슬롯
     */
    @PostMapping("/recommend-slots")
    public ResponseEntity<List<ScheduleSlotResponse>> recommendSlots(
        @RequestParam Long elderId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate serviceDate) {

        return ResponseEntity.ok(recommendationService.recommendSlots(elderId, serviceDate));
    }
}
