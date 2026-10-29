package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.StudentRequest;
import com.ipt.siga_backend.dto.StudentResponse;
import com.ipt.siga_backend.dto.StudentUpdateRequest;
import com.ipt.siga_backend.entity.*;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.CareerRepository;
import com.ipt.siga_backend.repository.StudentRepository;
import com.ipt.siga_backend.repository.StudyPlanRepository;
import com.ipt.siga_backend.repository.EnrollmentRepository;
import com.ipt.siga_backend.repository.AcademicRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicRecordRepository academicRecordRepository;

    public StudentService(StudentRepository studentRepository, CareerRepository careerRepository,
                          StudyPlanRepository studyPlanRepository, EnrollmentRepository enrollmentRepository,
                          AcademicRecordRepository academicRecordRepository) {
        this.studentRepository = studentRepository;
        this.careerRepository = careerRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.academicRecordRepository = academicRecordRepository;
    }

    @Transactional
    public StudentResponse createStudent(StudentRequest request) {
        validateRequiredFields(request);                               // RN-1.1 -> 400
        Career career = findCareerOrThrow(request.getCareerId());      // RN-1.4 -> 404
        validateCareerIsActive(career);                                // RN-2.5 -> 409
        validateRunIsUnique(request.getRun());                         // RN-1.2 -> 409
        validateEmailIsUnique(request.getEmail());                     // RN-1.3 -> 409
        StudyPlan currentPlan = findCurrentPlanOrThrow(career);        // RN-1.1 -> 409

        Student student = new Student();
        student.setRun(request.getRun());
        student.setFullName(request.getFullName());
        student.setEmail(request.getEmail());
        student.setStatus(StudentStatus.REGULAR);                      // RN-1.5: always REGULAR at creation
        student.setStudyPlan(currentPlan);
        return toResponse(studentRepository.save(student));
    }

    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(student -> toResponse(student))
                .toList();
    }

    public StudentResponse getStudentById(Long id) {
        return toResponse(findStudentOrThrow(id));
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentUpdateRequest request) {
        Student student = findStudentOrThrow(id);                                  // 404
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BadRequestException("Student full name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Student email is required");
        }
        // RN-1.3: only check uniqueness if the email changed (same trap as careers)
        if (!student.getEmail().equals(request.getEmail())) {
            validateEmailIsUnique(request.getEmail());                             // 409
        }
        student.setFullName(request.getFullName());
        student.setEmail(request.getEmail());
        return toResponse(studentRepository.save(student));
    }

    // RN-1.9: the admin can assign REGULAR, POSTPONED or TEMPORARY_WITHDRAWAL
    @Transactional
    public StudentResponse changeStatus(Long id, StudentStatus newStatus) {
        Student student = findStudentOrThrow(id);                                  // 404
        if (newStatus == null) {
            throw new BadRequestException("Status is required");
        }
        validateStatusCanBeAssignedManually(newStatus);                            // RN-1.7 -> 400
        validateStudentIsNotInFinalStatus(student);                                // RN-1.7 -> 409
        student.setStatus(newStatus);
        return toResponse(studentRepository.save(student));
    }

    // RN-1.8: career/plan can only change if the student has no enrollments or academic record
    @Transactional
    public StudentResponse changeCareer(Long id, Long careerId) {
        Student student = findStudentOrThrow(id);                                  // 404
        if (careerId == null) {
            throw new BadRequestException("Career is required");
        }
        if (hasEnrollmentsOrAcademicRecord(id)) {                                  // RN-1.8 -> 409
            throw new ConflictException("Student has enrollments or academic record; career cannot be changed");
        }
        Career career = findCareerOrThrow(careerId);                               // 404
        validateCareerIsActive(career);                                            // RN-2.5 -> 409
        student.setStudyPlan(findCurrentPlanOrThrow(career));                      // RN-1.1: current plan
        return toResponse(studentRepository.save(student));
    }

    // RN-1.6: a student with enrollments or academic record cannot be physically deleted
    @Transactional
    public void deleteStudent(Long id) {
        Student student = findStudentOrThrow(id);                                  // 404
        if (hasEnrollmentsOrAcademicRecord(id)) {                                  // RN-1.6 -> 409
            throw new ConflictException("Student has enrollments or academic record and cannot be deleted");
        }
        studentRepository.delete(student);
    }

    // ---------- validations ----------

    // RN-1.1: RUN, full name, email and career are required
    private void validateRequiredFields(StudentRequest request) {
        if (request.getRun() == null || request.getRun().isBlank()) {
            throw new BadRequestException("Student RUN is required");
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BadRequestException("Student full name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Student email is required");
        }
        if (request.getCareerId() == null) {
            throw new BadRequestException("Career is required");
        }
    }

    // RN-2.5: an INACTIVE career cannot receive new students
    private void validateCareerIsActive(Career career) {
        if (career.getStatus() == CareerStatus.INACTIVE) {
            throw new ConflictException("Career " + career.getCode() + " is INACTIVE and cannot receive new students");
        }
    }

    // RN-1.2: RUN must be unique
    private void validateRunIsUnique(String run) {
        if (studentRepository.existsByRun(run)) {
            throw new ConflictException("A student with RUN " + run + " already exists");
        }
    }

    // RN-1.3: email must be unique
    private void validateEmailIsUnique(String email) {
        if (studentRepository.existsByEmail(email)) {
            throw new ConflictException("A student with email " + email + " already exists");
        }
    }

    // RN-1.1: the student is assigned the CURRENT plan of the career
    private StudyPlan findCurrentPlanOrThrow(Career career) {
        return studyPlanRepository.findByCareerIdAndStatus(career.getId(), StudyPlanStatus.CURRENT)
                .orElseThrow(() -> new ConflictException("Career " + career.getCode() + " has no current study plan"));
    }

    private Career findCareerOrThrow(Long careerId) {
        return careerRepository.findById(careerId)
                .orElseThrow(() -> new NotFoundException("Career " + careerId + " not found"));
    }

    private Student findStudentOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student " + id + " not found"));
    }

    // RN-1.7: GRADUATED and ELIMINATED are only assigned automatically at period closing
    private void validateStatusCanBeAssignedManually(StudentStatus newStatus) {
        if (newStatus == StudentStatus.GRADUATED || newStatus == StudentStatus.ELIMINATED) {
            throw new BadRequestException("Status " + newStatus + " can only be assigned automatically at period closing");
        }
    }

    // RN-1.7 (decision): a GRADUATED or ELIMINATED student is in a final status
    private void validateStudentIsNotInFinalStatus(Student student) {
        if (student.getStatus() == StudentStatus.GRADUATED || student.getStatus() == StudentStatus.ELIMINATED) {
            throw new ConflictException("Student is " + student.getStatus() + " and their status cannot be changed manually");
        }
    }


    // RN-1.6 / RN-1.8: does the student have enrollments or academic record?
    private boolean hasEnrollmentsOrAcademicRecord(Long studentId) {
        return enrollmentRepository.existsByStudentId(studentId)
                || academicRecordRepository.existsByStudentId(studentId);
    }

    // ---------- mapping ----------

    private StudentResponse toResponse(Student student) {
        StudyPlan plan = student.getStudyPlan();
        Career career = plan.getCareer();
        return new StudentResponse(
                student.getId(), student.getRun(), student.getFullName(), student.getEmail(),
                student.getStatus(), career.getId(), career.getName(),
                plan.getId(), plan.getIdentifier());
    }

    // The JWT from Keycloak brings the email; we use it to find the logged student
    public Long getStudentIdByEmail(String email) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No student is linked to the user " + email));
        return student.getId();
    }
}