package com.homecare.api.controller;

import com.homecare.api.dto.request.CreateCareVisitTimeRequest;
import com.homecare.api.dto.response.CareVisitTimeResponse;
import com.homecare.application.service.CareVisitTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

/**
 * 방문요양 시간 관리 API
 */
@RestController
@RequestMapping("/api/care-visits")
@RequiredArgsConstructor
public class CareVisitTimeController {

    private final CareVisitTimeService careVisitTimeService;

    @GetMapping("/{elderId}")
    public ResponseEntity<List<CareVisitTimeResponse>> getCareVisitTimesByElder(
            @PathVariable Long elderId
    ) {
        return ResponseEntity.ok(careVisitTimeService.getCareVisitTimesByElder(elderId));
    }

    @GetMapping("/{elderId}/{dayOfWeek}")
    public ResponseEntity<List<CareVisitTimeResponse>> getCareVisitTimes(
            @PathVariable Long elderId,
            @PathVariable DayOfWeek dayOfWeek
    ) {
        return ResponseEntity.ok(careVisitTimeService.getCareVisitTimes(elderId, dayOfWeek));
    }

    @PostMapping
    public ResponseEntity<CareVisitTimeResponse> createCareVisitTime(
            @RequestBody CreateCareVisitTimeRequest request
    ) {
        return ResponseEntity.ok(careVisitTimeService.createCareVisitTime(request));
    }

    @PutMapping("/{elderId}/{dayOfWeek}")
    public ResponseEntity<List<CareVisitTimeResponse>> updateCareVisitTime(
            @PathVariable Long elderId,
            @PathVariable DayOfWeek dayOfWeek,
            @RequestBody CreateCareVisitTimeRequest request
    ) {
        return ResponseEntity.ok(
                careVisitTimeService.updateCareVisitTime(elderId, dayOfWeek, request)
        );
    }

    @DeleteMapping("/{elderId}/{dayOfWeek}")
    public ResponseEntity<Void> deleteCareVisitTime(
            @PathVariable Long elderId,
            @PathVariable DayOfWeek dayOfWeek
    ) {
        careVisitTimeService.deleteCareVisitTime(elderId, dayOfWeek);
        return ResponseEntity.noContent().build();
    }
}