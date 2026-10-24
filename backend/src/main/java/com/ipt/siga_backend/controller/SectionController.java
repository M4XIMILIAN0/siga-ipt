package com.ipt.siga_backend.controller;

import com.ipt.siga_backend.dto.SectionRequest;
import com.ipt.siga_backend.dto.SectionResponse;
import com.ipt.siga_backend.service.SectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    // Create a section in a period (academic offer) -> 201
    @PostMapping("/periods/{periodId}/sections")
    public ResponseEntity<SectionResponse> createSection(@PathVariable Long periodId,
                                                         @RequestBody SectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.createSection(periodId, request));
    }

    // Academic offer of a period -> 200
    @GetMapping("/periods/{periodId}/sections")
    public ResponseEntity<List<SectionResponse>> getSectionsByPeriod(@PathVariable Long periodId) {
        return ResponseEntity.ok(sectionService.getSectionsByPeriod(periodId));
    }

    @GetMapping("/sections/{id}")
    public ResponseEntity<SectionResponse> getSectionById(@PathVariable Long id) {
        return ResponseEntity.ok(sectionService.getSectionById(id));
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long id) {
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }
}