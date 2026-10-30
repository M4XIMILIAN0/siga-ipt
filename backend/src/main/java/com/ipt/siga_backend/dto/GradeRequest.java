package com.ipt.siga_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradeRequest {
    private Double finalGrade;   // 1.0 .. 7.0
}