package com.ipt.siga_backend.dto;

import com.ipt.siga_backend.entity.StudentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatusRequest {
    private StudentStatus status;
}