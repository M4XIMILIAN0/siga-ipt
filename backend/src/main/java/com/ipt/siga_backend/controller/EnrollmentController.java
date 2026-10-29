package com.ipt.siga_backend.controller;

import com.ipt.siga_backend.dto.EnrollmentRequest;
import com.ipt.siga_backend.dto.EnrollmentResponse;
import com.ipt.siga_backend.dto.OfferItemResponse;
import com.ipt.siga_backend.service.EnrollmentService;
import com.ipt.siga_backend.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final StudentService studentService;

    public EnrollmentController(EnrollmentService enrollmentService, StudentService studentService) {
        this.enrollmentService = enrollmentService;
        this.studentService = studentService;
    }

    // The logged student comes from the JWT (email claim), never from the request
    private Long currentStudentId(Jwt jwt) {
        return studentService.getStudentIdByEmail(jwt.getClaimAsString("email"));
    }

    // ---------- STUDENT ----------

    @GetMapping("/me/offer")
    public ResponseEntity<List<OfferItemResponse>> getMyOffer(@AuthenticationPrincipal Jwt jwt,
                                                              @RequestParam Long periodId) {
        return ResponseEntity.ok(enrollmentService.getAvailableOffer(currentStudentId(jwt), periodId));
    }

    @GetMapping("/me/enrollments")
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments(@AuthenticationPrincipal Jwt jwt,
                                                                     @RequestParam Long periodId) {
        return ResponseEntity.ok(enrollmentService.getStudentEnrollments(currentStudentId(jwt), periodId));
    }

    @PostMapping("/me/enrollments")
    public ResponseEntity<List<EnrollmentResponse>> enroll(@AuthenticationPrincipal Jwt jwt,
                                                           @RequestBody EnrollmentRequest request) {
        List<EnrollmentResponse> created =
                enrollmentService.enroll(currentStudentId(jwt), request.getPeriodId(), request.getSectionIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/me/enrollments/{id}")
    public ResponseEntity<Void> cancelEnrollment(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        enrollmentService.cancelEnrollment(currentStudentId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    // ---------- ADMIN ----------

    @GetMapping("/periods/{periodId}/enrollments")
    public ResponseEntity<List<EnrollmentResponse>> getPeriodEnrollments(@PathVariable Long periodId) {
        return ResponseEntity.ok(enrollmentService.getPeriodEnrollments(periodId));
    }
}