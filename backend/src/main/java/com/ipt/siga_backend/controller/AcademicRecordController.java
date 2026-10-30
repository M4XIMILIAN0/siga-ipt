package com.ipt.siga_backend.controller;

import com.ipt.siga_backend.dto.AcademicRecordResponse;
import com.ipt.siga_backend.dto.ClosingResponse;
import com.ipt.siga_backend.service.AcademicRecordService;
import com.ipt.siga_backend.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AcademicRecordController {

    private final AcademicRecordService academicRecordService;
    private final StudentService studentService;

    public AcademicRecordController(AcademicRecordService academicRecordService, StudentService studentService) {
        this.academicRecordService = academicRecordService;
        this.studentService = studentService;
    }

    // ADMIN: period closing (RN-7.6 .. RN-7.11)
    @PostMapping("/periods/{periodId}/close")
    public ResponseEntity<ClosingResponse> closePeriod(@PathVariable Long periodId) {
        return ResponseEntity.ok(academicRecordService.closePeriod(periodId));
    }

    // STUDENT: his own academic record
    @GetMapping("/me/record")
    public ResponseEntity<List<AcademicRecordResponse>> getMyRecord(@AuthenticationPrincipal Jwt jwt) {
        Long studentId = studentService.getStudentIdByEmail(jwt.getClaimAsString("email"));
        return ResponseEntity.ok(academicRecordService.getStudentRecord(studentId));
    }
}