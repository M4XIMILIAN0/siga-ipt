package com.ipt.siga_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherUpdateRequest {
    private String fullName;
    private String email;
    private String professionalTitle;
    private String academicDegree;
}