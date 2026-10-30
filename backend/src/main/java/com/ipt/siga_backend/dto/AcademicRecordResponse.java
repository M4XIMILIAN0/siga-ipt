package com.ipt.siga_backend.dto;

import com.ipt.siga_backend.entity.SubjectResult;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademicRecordResponse {
    private String periodCode;
    private String subjectCode;
    private String subjectName;
    private Integer semester;
    private Integer sctCredits;
    private Double finalGrade;
    private SubjectResult result;
}