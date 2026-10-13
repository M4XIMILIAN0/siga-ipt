package com.ipt.siga_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponse {
    private Long id;
    private String code;
    private String name;
    private Integer semester;
    private Integer theoryHours;
    private Integer exerciseHours;
    private Integer labHours;
    private Integer telTotal;
    private Integer sctCredits;
    private Long studyPlanId;
    private List<String> prerequisiteCodes;   // ["TAP201", "TAP204"] instead of nested objects
}