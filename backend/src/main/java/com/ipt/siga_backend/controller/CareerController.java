package com.ipt.siga_backend.controller;

import com.ipt.siga_backend.dto.CareerRequest;
import com.ipt.siga_backend.dto.CareerStatusRequest;
import com.ipt.siga_backend.entity.Career;
import com.ipt.siga_backend.service.CareerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/careers")
public class CareerController {

    private final CareerService careerService;

    public CareerController(CareerService careerService) {
        this.careerService = careerService;
    }

    // Create a career -> 201 Created
    @PostMapping
    public ResponseEntity<Career> createCareer(@RequestBody CareerRequest request) {
        Career created = careerService.createCareer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // List all careers -> 200 OK
    @GetMapping
    public ResponseEntity<List<Career>> getAllCareers() {
        return ResponseEntity.ok(careerService.getAllCareers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Career> getCareerById(@PathVariable Long id) {
        return ResponseEntity.ok(careerService.getCareerById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Career> updateCareer(@PathVariable Long id,
                                               @RequestBody CareerRequest request) {
        return ResponseEntity.ok(careerService.updateCareer(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Career> changeStatus(@PathVariable Long id,
                                               @RequestBody CareerStatusRequest request) {
        return ResponseEntity.ok(careerService.changeStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCareer(@PathVariable Long id) {
        careerService.deleteCareer(id);
        return ResponseEntity.noContent().build();          // 204 no body
    }
}