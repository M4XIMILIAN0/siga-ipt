package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.CareerRequest;
import com.ipt.siga_backend.entity.Career;
import com.ipt.siga_backend.entity.CareerStatus;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.CareerRepository;
import com.ipt.siga_backend.repository.StudyPlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CareerService {

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;

    public CareerService(CareerRepository careerRepository, StudyPlanRepository studyPlanRepository) {
        this.careerRepository = careerRepository;
        this.studyPlanRepository = studyPlanRepository;
    }

    public Career createCareer(CareerRequest request) {
        validateRequiredFields(request);          // RN-2.2 -> 400
        validateCodeIsUnique(request.getCode());  // RN-2.1 -> 409

        Career career = new Career();
        career.setCode(request.getCode());
        career.setName(request.getName());
        career.setDescription(request.getDescription());
        career.setStatus(CareerStatus.ACTIVE);
        return careerRepository.save(career);
    }

    public List<Career> getAllCareers() {
        return careerRepository.findAll();
    }

    public Career getCareerById(Long id) {
        return careerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Career " + id + " not found"));
    }

    public Career updateCareer(Long id, CareerRequest request) {
        Career career = getCareerById(id);        // 404
        validateRequiredFields(request);          // RN-2.2 -> 400

        // RN-2.1: only check uniqueness if the code actually changed
        if (!career.getCode().equals(request.getCode())) {
            validateCodeIsUnique(request.getCode());
        }

        career.setCode(request.getCode());
        career.setName(request.getName());
        career.setDescription(request.getDescription());
        return careerRepository.save(career);
    }

    // RN-2.3: change status (ACTIVE / INACTIVE)
    public Career changeStatus(Long id, CareerStatus newStatus) {
        Career career = getCareerById(id);        // 404
        if (newStatus == null) {
            throw new BadRequestException("Status is required");
        }
        career.setStatus(newStatus);
        return careerRepository.save(career);
    }

    // RN-2.9: a career with study plans cannot be physically deleted
    public void deleteCareer(Long id) {
        Career career = getCareerById(id);        // 404
        if (studyPlanRepository.existsByCareerId(id)) {
            throw new ConflictException("Career has study plans; change its status to INACTIVE instead");
        }
        // TODO RN-2.9 (Epic 1): also check if the career has students
        careerRepository.delete(career);
    }

    // RN-2.2: code, name and description are required
    private void validateRequiredFields(CareerRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("Career code is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Career name is required");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new BadRequestException("Career description is required");
        }
    }

    // RN-2.1: career code must be unique
    private void validateCodeIsUnique(String code) {
        if (careerRepository.existsByCode(code)) {
            throw new ConflictException("Career code " + code + " already exists");
        }
    }
}