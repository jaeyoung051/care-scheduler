package com.homecare.application.service;

import com.homecare.api.dto.request.CreateElderRequest;
import com.homecare.api.dto.response.ElderResponse;
import com.homecare.domain.entity.Elder;
import com.homecare.domain.enums.Gender;
import com.homecare.domain.repository.ElderRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 어르신 관리 Service (CRUD)
 */
@Service
@RequiredArgsConstructor
public class ElderService {
    private final ElderRepository elderRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ElderResponse createElder(CreateElderRequest request) {
        Elder elder = Elder.builder()
            .name(request.getName())
            .gender(request.getGender())
            .address(request.getAddress())
            .region(request.getRegion())
            .allowMaleStaff(request.getAllowMaleStaff() != null ? request.getAllowMaleStaff() : true)
            .memo(request.getMemo())
            .build();

        Elder saved = elderRepository.save(elder);
        return modelMapper.map(saved, ElderResponse.class);
    }

    @Transactional(readOnly = true)
    public ElderResponse getElder(Long id) {
        Elder elder = elderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("어르신을 찾을 수 없습니다"));
        return modelMapper.map(elder, ElderResponse.class);
    }

    @Transactional(readOnly = true)
    public List<ElderResponse> getAllElders() {
        return elderRepository.findAll().stream()
            .map(e -> modelMapper.map(e, ElderResponse.class))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ElderResponse> getEldersByGender(Gender gender) {
        return elderRepository.findByGender(gender).stream()
            .map(e -> modelMapper.map(e, ElderResponse.class))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ElderResponse> getEldersByRegion(String region) {
        return elderRepository.findByRegion(region).stream()
            .map(e -> modelMapper.map(e, ElderResponse.class))
            .collect(Collectors.toList());
    }

    @Transactional
    public ElderResponse updateElder(Long id, CreateElderRequest request) {
        Elder elder = elderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("어르신을 찾을 수 없습니다"));

        if (request.getName() != null) elder.setName(request.getName());
        if (request.getGender() != null) elder.setGender(request.getGender());
        if (request.getAddress() != null) elder.setAddress(request.getAddress());
        if (request.getRegion() != null) elder.setRegion(request.getRegion());
        if (request.getAllowMaleStaff() != null) elder.setAllowMaleStaff(request.getAllowMaleStaff());
        if (request.getMemo() != null) elder.setMemo(request.getMemo());

        Elder updated = elderRepository.save(elder);
        return modelMapper.map(updated, ElderResponse.class);
    }

    @Transactional
    public void deleteElder(Long id) {
        elderRepository.deleteById(id);
    }
}
