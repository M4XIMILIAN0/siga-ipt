package com.ipt.siga_backend.controller;

import com.ipt.siga_backend.dto.SubjectRequest;
import com.ipt.siga_backend.dto.SubjectResponse;
import com.ipt.siga_backend.service.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }


    @PostMapping("/study-plans/{studyPlanId}/subjects")
    public ResponseEntity<SubjectResponse> createSubject(@PathVariable Long studyPlanId,
                                                         @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.createSubject(studyPlanId, request));
    }


    @GetMapping("/study-plans/{studyPlanId}/subjects")
    public ResponseEntity<List<SubjectResponse>> getSubjectsByPlan(@PathVariable Long studyPlanId) {
        return ResponseEntity.ok(subjectService.getSubjectsByPlan(studyPlanId));
    }



    @GetMapping("/subjects/{id}")
    public ResponseEntity<SubjectResponse> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<SubjectResponse> updateSubject(@PathVariable Long id,
                                                         @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(id, request));
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }
}