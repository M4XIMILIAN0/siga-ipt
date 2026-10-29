package com.ipt.siga_backend.controller;

import com.ipt.siga_backend.dto.PeriodRegistrationRequest;
import com.ipt.siga_backend.dto.PeriodRegistrationResponse;
import com.ipt.siga_backend.service.PeriodRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/periods/{periodId}/registrations")
public class PeriodRegistrationController {

    private final PeriodRegistrationService registrationService;

    public PeriodRegistrationController(PeriodRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<PeriodRegistrationResponse> registerStudent(@PathVariable Long periodId,
                                                                      @RequestBody PeriodRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.registerStudent(periodId, request.getStudentId()));
    }

    @GetMapping
    public ResponseEntity<List<PeriodRegistrationResponse>> getRegistrationsByPeriod(@PathVariable Long periodId) {
        return ResponseEntity.ok(registrationService.getRegistrationsByPeriod(periodId));
    }
}