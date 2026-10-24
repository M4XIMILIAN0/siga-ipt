package com.ipt.siga_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponse {
    private Long id;
    private Long periodId;
    private String periodCode;        // "2027-1"
    private Long subjectId;
    private String subjectCode;       // "TAP101"
    private String subjectName;
    private Integer semester;
    private Long teacherId;
    private String teacherName;
    private Integer capacity;
    private List<String> blocks;      // ["L2", "W3", "V1"]
}