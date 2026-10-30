package com.ipt.siga_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClosingResponse {
    private String periodCode;
    private int recordsCreated;
    private int graduatedStudents;
    private int eliminatedStudents;
}