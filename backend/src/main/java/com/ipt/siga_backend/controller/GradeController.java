package com.ipt.siga_backend.controller;

import com.ipt.siga_backend.dto.GradeRequest;
import com.ipt.siga_backend.dto.GradeResponse;
import com.ipt.siga_backend.dto.SectionResponse;
import com.ipt.siga_backend.service.GradeService;
import com.ipt.siga_backend.service.SectionService;
import com.ipt.siga_backend.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// TEACHER endpoints: the teacher always comes from the JWT
@RestController
@RequestMapping("/api/teacher")
public class GradeController {

    private final GradeService gradeService;
    private final SectionService sectionService;
    private final TeacherService teacherService;

    public GradeController(GradeService gradeService, SectionService sectionService, TeacherService teacherService) {
        this.gradeService = gradeService;
        this.sectionService = sectionService;
        this.teacherService = teacherService;
    }

    private Long currentTeacherId(Jwt jwt) {
        return teacherService.getTeacherIdByEmail(jwt.getClaimAsString("email"));
    }

    @GetMapping("/sections")
    public ResponseEntity<List<SectionResponse>> getMySections(@AuthenticationPrincipal Jwt jwt,
                                                               @RequestParam Long periodId) {
        return ResponseEntity.ok(sectionService.getSectionsByTeacher(currentTeacherId(jwt), periodId));
    }

    @GetMapping("/sections/{sectionId}/grades")
    public ResponseEntity<List<GradeResponse>> getSectionGrades(@AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable Long sectionId) {
        return ResponseEntity.ok(gradeService.getSectionGrades(currentTeacherId(jwt), sectionId));
    }

    @PutMapping("/enrollments/{enrollmentId}/grade")
    public ResponseEntity<GradeResponse> registerFinalGrade(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable Long enrollmentId,
                                                            @RequestBody GradeRequest request) {
        return ResponseEntity.ok(
                gradeService.registerFinalGrade(currentTeacherId(jwt), enrollmentId, request.getFinalGrade()));
    }
}