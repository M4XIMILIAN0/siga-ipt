package com.ipt.siga_backend.controller;

import com.ipt.siga_backend.dto.*;
import com.ipt.siga_backend.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable Long id,
                                                         @RequestBody StudentUpdateRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<StudentResponse> changeStatus(@PathVariable Long id,
                                                        @RequestBody StudentStatusRequest request) {
        return ResponseEntity.ok(studentService.changeStatus(id, request.getStatus()));
    }

    @PutMapping("/{id}/career")
    public ResponseEntity<StudentResponse> changeCareer(@PathVariable Long id,
                                                        @RequestBody StudentCareerRequest request) {
        return ResponseEntity.ok(studentService.changeCareer(id, request.getCareerId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}