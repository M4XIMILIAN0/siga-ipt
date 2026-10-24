package com.ipt.siga_backend.controller;

import com.ipt.siga_backend.dto.AcademicPeriodRequest;
import com.ipt.siga_backend.entity.AcademicPeriod;
import com.ipt.siga_backend.service.AcademicPeriodService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/periods")
public class AcademicPeriodController {

    private final AcademicPeriodService periodService;

    public AcademicPeriodController(AcademicPeriodService periodService) {
        this.periodService = periodService;
    }

    @PostMapping
    public ResponseEntity<AcademicPeriod> createPeriod(@RequestBody AcademicPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodService.createPeriod(request));
    }

    @GetMapping
    public ResponseEntity<List<AcademicPeriod>> getAllPeriods() {
        return ResponseEntity.ok(periodService.getAllPeriods());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicPeriod> getPeriodById(@PathVariable Long id) {
        return ResponseEntity.ok(periodService.getPeriodById(id));
    }
}