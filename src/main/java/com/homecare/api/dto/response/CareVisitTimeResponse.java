package com.homecare.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 방문요양 시간 조회/응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareVisitTimeResponse {
    private Long id;
    private Long elderId;
    private String elderName;
    private DayOfWeek dayOfWeek;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime endTime;
    
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
