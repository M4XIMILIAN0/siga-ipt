package com.ipt.siga_backend.controller;

import com.ipt.siga_backend.dto.StudyPlanRequest;
import com.ipt.siga_backend.entity.StudyPlan;
import com.ipt.siga_backend.service.StudyPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    public StudyPlanController(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    // Create a study plan for a career -> 201
    @PostMapping("/{careerId}/study-plans")
    public ResponseEntity<StudyPlan> createStudyPlan(@PathVariable Long careerId,
                                                     @RequestBody StudyPlanRequest request) {
        StudyPlan created = studyPlanService.createStudyPlan(careerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // List the study plans of a career -> 200
    @GetMapping("/{careerId}/study-plans")
    public ResponseEntity<List<StudyPlan>> getPlansByCareer(@PathVariable Long careerId) {
        return ResponseEntity.ok(studyPlanService.getPlansByCareer(careerId));
    }
}