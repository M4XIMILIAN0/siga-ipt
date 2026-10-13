package com.ipt.siga_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectRequest {
    private String code;
    private String name;
    private Integer semester;
    private Integer theoryHours;
    private Integer exerciseHours;
    private Integer labHours;
    private Integer sctCredits;
    private List<String> prerequisiteCodes;   // e.g. ["TAP201", "TAP204"]
}