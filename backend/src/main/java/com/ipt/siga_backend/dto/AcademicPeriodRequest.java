package com.ipt.siga_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademicPeriodRequest {
    private Integer year;       // 2027
    private Integer semester;   // 1 or 2
}