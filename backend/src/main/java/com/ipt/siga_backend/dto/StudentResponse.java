package com.ipt.siga_backend.dto;

import com.ipt.siga_backend.entity.StudentStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private Long id;
    private String run;
    private String fullName;
    private String email;
    private StudentStatus status;
    private Long careerId;
    private String careerName;
    private Long studyPlanId;
    private String studyPlanIdentifier;
}