package com.ipt.siga_backend.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    private Long id;
    private Long studentId;
    private String studentRun;
    private String studentName;
    private Long periodId;
    private String periodCode;
    private Long sectionId;
    private String subjectCode;
    private String subjectName;
    private Integer semester;
    private String teacherName;
    private List<String> blocks;
    private LocalDate enrollmentDate;
    private Double finalGrade;
}