package com.homecare.application.service;

import com.homecare.api.dto.request.CreateVehicleRequest;
import com.homecare.api.dto.response.VehicleResponse;
import com.homecare.domain.entity.Vehicle;
import com.homecare.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 차량 관리 Service (CRUD)
 */
@Service
@RequiredArgsConstructor
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        Vehicle vehicle = Vehicle.builder()
            .name(request.getName())
            .hasMaleStaff(request.getHasMaleStaff() != null ? request.getHasMaleStaff() : false)
            .memo(request.getMemo())
            .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        return modelMapper.map(saved, VehicleResponse.class);
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("차량을 찾을 수 없습니다"));
        return modelMapper.map(vehicle, VehicleResponse.class);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
            .map(v -> modelMapper.map(v, VehicleResponse.class))
            .collect(Collectors.toList());
    }

    @Transactional
    public VehicleResponse updateVehicle(Long id, CreateVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("차량을 찾을 수 없습니다"));

        if (request.getName() != null) vehicle.setName(request.getName());
        if (request.getHasMaleStaff() != null) vehicle.setHasMaleStaff(request.getHasMaleStaff());
        if (request.getMemo() != null) vehicle.setMemo(request.getMemo());

        Vehicle updated = vehicleRepository.save(vehicle);
        return modelMapper.map(updated, VehicleResponse.class);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }
}
