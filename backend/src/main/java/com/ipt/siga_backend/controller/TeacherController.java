package com.ipt.siga_backend.controller;

import com.ipt.siga_backend.dto.TeacherRequest;
import com.ipt.siga_backend.dto.TeacherStatusRequest;
import com.ipt.siga_backend.dto.TeacherUpdateRequest;
import com.ipt.siga_backend.entity.Teacher;
import com.ipt.siga_backend.service.TeacherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping
    public ResponseEntity<Teacher> createTeacher(@RequestBody TeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.createTeacher(request));
    }

    @GetMapping
    public ResponseEntity<List<Teacher>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.getAllTeachers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Teacher> getTeacherById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getTeacherById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Teacher> updateTeacher(@PathVariable Long id,
                                                 @RequestBody TeacherUpdateRequest request) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Teacher> changeStatus(@PathVariable Long id,
                                                @RequestBody TeacherStatusRequest request) {
        return ResponseEntity.ok(teacherService.changeStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }
}