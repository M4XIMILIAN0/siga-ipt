package com.ipt.siga_backend.dto;

import com.ipt.siga_backend.entity.SubjectResult;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradeResponse {
    private Long enrollmentId;
    private String studentRun;
    private String studentName;
    private String subjectCode;
    private Double finalGrade;       // null = pending
    private SubjectResult result;    // null = pending
}