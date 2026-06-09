package com.homecare.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 차량 조회/응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {
    private Long id;
    private String name;
    private Boolean hasMaleStaff;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
