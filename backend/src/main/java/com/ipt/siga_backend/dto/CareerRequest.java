package com.ipt.siga_backend.dto;

import com.ipt.siga_backend.entity.CareerStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CareerRequest {
    private String code;
    private String name;
    private String description;
}