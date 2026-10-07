package com.ipt.siga_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private int status;          // 409
    private String error;        // "Conflict"
    private String message;      // "Career code 10450 already exists"
    private LocalDateTime timestamp;
}