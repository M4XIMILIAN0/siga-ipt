package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.StudyPlanRequest;
import com.ipt.siga_backend.entity.Career;
import com.ipt.siga_backend.entity.StudyPlan;
import com.ipt.siga_backend.entity.StudyPlanStatus;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.CareerRepository;
import com.ipt.siga_backend.repository.StudyPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final CareerRepository careerRepository;

    public StudyPlanService(StudyPlanRepository studyPlanRepository, CareerRepository careerRepository) {
        this.studyPlanRepository = studyPlanRepository;
        this.careerRepository = careerRepository;
    }

    @Transactional
    public StudyPlan createStudyPlan(Long careerId, StudyPlanRequest request) {
        Career career = findCareerOrThrow(careerId);                    // 1. search
        validateIdentifierRequired(request);                            // 2. data
        validateIdentifierIsUnique(careerId, request.getIdentifier());  // 3. DB rule
        markCurrentPlanAsNotCurrent(careerId);                          // 4. side effect (RN-2.10)

        StudyPlan plan = new StudyPlan();                               // 5. create + save
        plan.setIdentifier(request.getIdentifier());
        plan.setStatus(StudyPlanStatus.CURRENT);
        plan.setCareer(career);
        return studyPlanRepository.save(plan);
    }

    public List<StudyPlan> getPlansByCareer(Long careerId) {
        findCareerOrThrow(careerId);                         // 404 if the career doesn't exist
        return studyPlanRepository.findByCareerId(careerId);
    }

    private Career findCareerOrThrow(Long careerId) {
        return careerRepository.findById(careerId)
                .orElseThrow(() -> new NotFoundException("Career " + careerId + " not found"));
    }

    // Identifier is required -> 400
    private void validateIdentifierRequired(StudyPlanRequest request) {
        if(request.getIdentifier() == null || request.getIdentifier().isBlank()) {
            throw new BadRequestException("Identifier required");
        }
    }

    // RN-2.6: identifier must be unique within the career -> 409
    private void validateIdentifierIsUnique(Long careerId, String identifier) {
        if (studyPlanRepository.existsByCareerIdAndIdentifier(careerId, identifier)) {
            throw new ConflictException("Study plan identifier " + identifier + " already exists for career " + careerId);
        }
    }

    // RN-2.10: the previous CURRENT plan becomes NOT_CURRENT
    private void markCurrentPlanAsNotCurrent(Long careerId) {
        Optional<StudyPlan> currentPlan =
                studyPlanRepository.findByCareerIdAndStatus(careerId, StudyPlanStatus.CURRENT);
        if (currentPlan.isPresent()) {
            StudyPlan oldPlan = currentPlan.get();
            oldPlan.setStatus(StudyPlanStatus.NOT_CURRENT);
            studyPlanRepository.save(oldPlan);
        }
    }
}