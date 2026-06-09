package com.homecare.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalTime;

/**
 * 추천 목욕 슬롯 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleSlotResponse {
    private Long vehicleId;
    private String vehicleName;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime endTime;
    
    /** 이동 비용 점수 (낮을수록 좋음) */
    private int score;
    
    /** 추천 이유 설명 */
    private String reason;
}
