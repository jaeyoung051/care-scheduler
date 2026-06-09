package com.homecare.api.dto.request;

import com.homecare.domain.enums.Gender;
import lombok.*;

/**
 * 어르신 등록/수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateElderRequest {
    private String name;
    private Gender gender;
    private String address;
    private String region;
    private Boolean allowMaleStaff;
    private String memo;
}
