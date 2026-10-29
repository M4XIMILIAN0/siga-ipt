package com.ipt.siga_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequest {
    private Long periodId;
    private List<Long> sectionIds;   // all the sections chosen by the student, in one request
}