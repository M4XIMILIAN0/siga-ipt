package com.ipt.siga_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeriodRegistrationResponse {
    private Long id;
    private Long periodId;
    private String periodCode;
    private Long studentId;
    private String studentRun;
    private String studentName;
}