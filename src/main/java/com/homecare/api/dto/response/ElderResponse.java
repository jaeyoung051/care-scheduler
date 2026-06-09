package com.homecare.api.dto.response;

import com.homecare.domain.enums.Gender;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 어르신 조회/응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElderResponse {
    private Long id;
    private String name;
    private Gender gender;
    private String address;
    private String region;
    private Boolean allowMaleStaff;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
