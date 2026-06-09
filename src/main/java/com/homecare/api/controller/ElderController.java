package com.homecare.api.controller;

import com.homecare.api.dto.request.CreateElderRequest;
import com.homecare.api.dto.response.ElderResponse;
import com.homecare.application.service.ElderService;
import com.homecare.domain.enums.Gender;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 어르신 관리 API
 */
@RestController
@RequestMapping("/api/elders")
@RequiredArgsConstructor
public class ElderController {
    private final ElderService elderService;

    @GetMapping
    public ResponseEntity<List<ElderResponse>> getAllElders(
        @RequestParam(required = false) Gender gender,
        @RequestParam(required = false) String region) {
        
        if (gender != null) {
            return ResponseEntity.ok(elderService.getEldersByGender(gender));
        }
        if (region != null) {
            return ResponseEntity.ok(elderService.getEldersByRegion(region));
        }
        return ResponseEntity.ok(elderService.getAllElders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ElderResponse> getElder(@PathVariable Long id) {
        return ResponseEntity.ok(elderService.getElder(id));
    }

    @PostMapping
    public ResponseEntity<ElderResponse> createElder(@RequestBody CreateElderRequest request) {
        return ResponseEntity.ok(elderService.createElder(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ElderResponse> updateElder(@PathVariable Long id, @RequestBody CreateElderRequest request) {
        return ResponseEntity.ok(elderService.updateElder(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteElder(@PathVariable Long id) {
        elderService.deleteElder(id);
        return ResponseEntity.noContent().build();
    }
}
