package com.ipt.siga_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequest {
    private String run;
    private String fullName;
    private String email;
    private Long careerId;     // NO studyPlanId: the system assigns the current plan (RN-1.1)
}