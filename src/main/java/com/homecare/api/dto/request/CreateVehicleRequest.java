package com.homecare.api.dto.request;

import lombok.*;

/**
 * 차량 등록/수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVehicleRequest {
    private String name;
    private Boolean hasMaleStaff;
    private String memo;
}
