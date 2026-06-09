package com.homecare.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 목욕 일정 조회/응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BathScheduleResponse {
    private Long id;
    private Long elderId;
    private String elderName;
    private Long vehicleId;
    private String vehicleName;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate serviceDate;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime endTime;
    
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
