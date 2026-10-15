package com.ipt.siga_backend.dto;

import com.ipt.siga_backend.entity.TeacherStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherStatusRequest {
    private TeacherStatus status;
}