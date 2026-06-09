package com.homecare.application.service;

import com.homecare.api.dto.request.CreateCareVisitTimeRequest;
import com.homecare.api.dto.response.CareVisitTimeResponse;
import com.homecare.domain.entity.CareVisitTime;
import com.homecare.domain.entity.Elder;
import com.homecare.domain.repository.CareVisitTimeRepository;
import com.homecare.domain.repository.ElderRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 방문요양 시간 관리 Service (CRUD)
 */
@Service
@RequiredArgsConstructor
public class CareVisitTimeService {
    private final CareVisitTimeRepository careVisitTimeRepository;
    private final ElderRepository elderRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public CareVisitTimeResponse createCareVisitTime(CreateCareVisitTimeRequest request) {
        Elder elder = elderRepository.findById(request.getElderId())
            .orElseThrow(() -> new RuntimeException("어르신을 찾을 수 없습니다"));

        CareVisitTime careVisitTime = CareVisitTime.builder()
            .elder(elder)
            .dayOfWeek(request.getDayOfWeek())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .memo(request.getMemo())
            .build();

        CareVisitTime saved = careVisitTimeRepository.save(careVisitTime);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CareVisitTimeResponse> getCareVisitTimesByElder(Long elderId) {
        return careVisitTimeRepository.findByElderId(elderId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CareVisitTimeResponse getCareVisitTime(Long elderId, DayOfWeek dayOfWeek) {
        CareVisitTime careVisitTime = careVisitTimeRepository.findByElderIdAndDayOfWeek(elderId, dayOfWeek)
            .orElseThrow(() -> new RuntimeException("방문요양 시간을 찾을 수 없습니다"));
        return toResponse(careVisitTime);
    }

    @Transactional
    public CareVisitTimeResponse updateCareVisitTime(Long elderId, DayOfWeek dayOfWeek, CreateCareVisitTimeRequest request) {
        CareVisitTime careVisitTime = careVisitTimeRepository.findByElderIdAndDayOfWeek(elderId, dayOfWeek)
            .orElseThrow(() -> new RuntimeException("방문요양 시간을 찾을 수 없습니다"));

        careVisitTime.setStartTime(request.getStartTime());
        careVisitTime.setEndTime(request.getEndTime());
        careVisitTime.setMemo(request.getMemo());

        CareVisitTime updated = careVisitTimeRepository.save(careVisitTime);
        return toResponse(updated);
    }

    @Transactional
    public void deleteCareVisitTime(Long elderId, DayOfWeek dayOfWeek) {
        careVisitTimeRepository.deleteByElderIdAndDayOfWeek(elderId, dayOfWeek);
    }

    private CareVisitTimeResponse toResponse(CareVisitTime careVisitTime) {
        return CareVisitTimeResponse.builder()
            .id(careVisitTime.getId())
            .elderId(careVisitTime.getElder().getId())
            .elderName(careVisitTime.getElder().getName())
            .dayOfWeek(careVisitTime.getDayOfWeek())
            .startTime(careVisitTime.getStartTime())
            .endTime(careVisitTime.getEndTime())
            .memo(careVisitTime.getMemo())
            .createdAt(careVisitTime.getCreatedAt())
            .updatedAt(careVisitTime.getUpdatedAt())
            .build();
    }
}
