package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.TeacherRequest;
import com.ipt.siga_backend.entity.PeriodStatus;
import com.ipt.siga_backend.entity.Teacher;
import com.ipt.siga_backend.entity.TeacherStatus;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.SectionRepository;
import com.ipt.siga_backend.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ipt.siga_backend.dto.TeacherUpdateRequest;

import java.util.List;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;

    public TeacherService(TeacherRepository teacherRepository, SectionRepository sectionRepository) {
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional
    public Teacher createTeacher(TeacherRequest request) {
        validateRequiredFields(request);                 // RN-4.3 -> 400
        validateRunIsUnique(request.getRun());           // RN-4.1 -> 409
        validateEmailIsUnique(request.getEmail());       // RN-4.2 -> 409

        Teacher teacher = new Teacher();
        teacher.setRun(request.getRun());
        teacher.setFullName(request.getFullName());
        teacher.setEmail(request.getEmail());
        teacher.setProfessionalTitle(request.getProfessionalTitle());
        teacher.setAcademicDegree(request.getAcademicDegree());
        teacher.setStatus(TeacherStatus.ACTIVE);         // RN-4.4: always ACTIVE at creation
        return teacherRepository.save(teacher);
    }

    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public Teacher getTeacherById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Teacher " + id + " not found"));
    }

    @Transactional
    public Teacher updateTeacher(Long id, TeacherUpdateRequest request) {
        Teacher teacher = getTeacherById(id);                       // 404
        validateUpdateFields(request);                              // RN-4.3 -> 400

        // RN-4.2: only check uniqueness if the email changed (same trap as students)
        if (!teacher.getEmail().equals(request.getEmail())) {
            validateEmailIsUnique(request.getEmail());              // 409
        }

        teacher.setFullName(request.getFullName());
        teacher.setEmail(request.getEmail());
        teacher.setProfessionalTitle(request.getProfessionalTitle());
        teacher.setAcademicDegree(request.getAcademicDegree());
        return teacherRepository.save(teacher);
    }

    // RN-4.7: cannot become INACTIVE while having sections in an OPEN period
    @Transactional
    public Teacher changeStatus(Long id, TeacherStatus newStatus) {
        Teacher teacher = getTeacherById(id);                       // 404
        if (newStatus == null) {
            throw new BadRequestException("Status is required");
        }
        if (newStatus == TeacherStatus.INACTIVE && hasSectionsInOpenPeriod(id)) {
            throw new ConflictException("Teacher has sections in an OPEN period and cannot be set to INACTIVE");
        }
        // RN-4.8: only the status changes; sections and history are never touched
        teacher.setStatus(newStatus);
        return teacherRepository.save(teacher);
    }

    // RN-4.6: a teacher with sections cannot be physically deleted
    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = getTeacherById(id);                       // 404
        if (hasSections(id)) {
            throw new ConflictException("Teacher has sections and cannot be deleted; change the status to INACTIVE instead");
        }
        teacherRepository.delete(teacher);
    }

    // ---------- validations ----------

    // RN-4.3: required fields for creation
    private void validateRequiredFields(TeacherRequest request) {
        if (request.getRun() == null || request.getRun().isBlank()) {
            throw new BadRequestException("Teacher RUN is required");
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BadRequestException("Teacher full name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Teacher email is required");
        }
        if (request.getProfessionalTitle() == null || request.getProfessionalTitle().isBlank()) {
            throw new BadRequestException("Teacher professional title is required");
        }
        if (request.getAcademicDegree() == null || request.getAcademicDegree().isBlank()) {
            throw new BadRequestException("Teacher academic degree is required");
        }
    }

    // RN-4.3: same required fields, except RUN (it cannot be modified)
    private void validateUpdateFields(TeacherUpdateRequest request) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BadRequestException("Teacher full name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Teacher email is required");
        }
        if (request.getProfessionalTitle() == null || request.getProfessionalTitle().isBlank()) {
            throw new BadRequestException("Teacher professional title is required");
        }
        if (request.getAcademicDegree() == null || request.getAcademicDegree().isBlank()) {
            throw new BadRequestException("Teacher academic degree is required");
        }
    }

    // RN-4.6: does the teacher have any section?
    // RN-4.6: does the teacher have any section?
    private boolean hasSections(Long teacherId) {
        return sectionRepository.existsByTeacherId(teacherId);
    }

    // RN-4.7: does the teacher have sections in an OPEN period?
    private boolean hasSectionsInOpenPeriod(Long teacherId) {
        return sectionRepository.existsByTeacherIdAndPeriodStatus(teacherId, PeriodStatus.OPEN);
    }
    // RN-4.1: RUN must be unique
    private void validateRunIsUnique(String run) {
        if (teacherRepository.existsByRun(run)) {
            throw new ConflictException("A teacher with RUN " + run + " already exists");
        }
    }

    // RN-4.2: email must be unique
    private void validateEmailIsUnique(String email) {
        if (teacherRepository.existsByEmail(email)) {
            throw new ConflictException("A teacher with email " + email + " already exists");
        }
    }
}