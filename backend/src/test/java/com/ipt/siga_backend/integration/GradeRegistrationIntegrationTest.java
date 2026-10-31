package com.ipt.siga_backend.integration;

import com.ipt.siga_backend.dto.EnrollmentResponse;
import com.ipt.siga_backend.entity.AcademicPeriod;
import com.ipt.siga_backend.entity.PeriodStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Process 3 (Epic 7): the responsible TEACHER registers final grades
class GradeRegistrationIntegrationTest extends IntegrationTestBase {

    private Long periodId;
    private Long tap101EnrollmentId;   // section of María
    private Long tap102EnrollmentId;   // section of Juan

    // Pedro enrolled in TAP101 (María), TAP102 and TAP103 (Juan)
    @BeforeEach
    void setUp() {
        periodId = createOpenPeriod();
        Long tap101 = createSection(periodId, "TAP101", MARIA, "L2", "W3", "V1");
        Long tap102 = createSection(periodId, "TAP102", JUAN, "M1", "J4");
        Long tap103 = createSection(periodId, "TAP103", JUAN, "M2", "J3");
        registerStudent(periodId, PEDRO);
        List<EnrollmentResponse> enrollments =
                enrollmentService.enroll(studentId(PEDRO), periodId, List.of(tap101, tap102, tap103));
        tap101EnrollmentId = enrollments.get(0).getId();
        tap102EnrollmentId = enrollments.get(1).getId();
    }

    private ResultActions putGrade(Long enrollmentId, String grade, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(put("/api/teacher/enrollments/" + enrollmentId + "/grade")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"finalGrade\": " + grade + "}"));
    }

    // ===== 3 success scenarios =====

    // RN-7.5: 5.5 -> PASSED
    @Test
    void registerGrade_passingGrade_returns200Passed() throws Exception {
        putGrade(tap101EnrollmentId, "5.5", asTeacher(MARIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalGrade").value(5.5))
                .andExpect(jsonPath("$.result").value("PASSED"));
    }

    // RN-7.5: 3.0 -> FAILED
    @Test
    void registerGrade_failingGrade_returns200Failed() throws Exception {
        putGrade(tap101EnrollmentId, "3.0", asTeacher(MARIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("FAILED"));
    }

    // RN-7.3 / RN-7.4: while OPEN, the grade can be modified (it stays ONE grade)
    @Test
    void registerGrade_modifyExistingGrade_returns200() throws Exception {
        putGrade(tap101EnrollmentId, "4.0", asTeacher(MARIA)).andExpect(status().isOk());

        putGrade(tap101EnrollmentId, "6.5", asTeacher(MARIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalGrade").value(6.5));
    }

    // ===== 3 rejection scenarios =====

    // RN-7.1: María cannot grade Juan's section
    @Test
    void registerGrade_notResponsibleTeacher_returns403() throws Exception {
        putGrade(tap102EnrollmentId, "5.0", asTeacher(MARIA))
                .andExpect(status().isForbidden());
    }

    // RN-7.3: grade out of range
    @Test
    void registerGrade_gradeAboveSeven_returns400() throws Exception {
        putGrade(tap101EnrollmentId, "7.5", asTeacher(MARIA))
                .andExpect(status().isBadRequest());
    }

    // RN-7.12: no grades in a CLOSED period
    @Test
    void registerGrade_closedPeriod_returns409() throws Exception {
        AcademicPeriod period = periodRepository.findById(periodId).orElseThrow();
        period.setStatus(PeriodStatus.CLOSED);
        periodRepository.save(period);

        putGrade(tap101EnrollmentId, "6.0", asTeacher(MARIA))
                .andExpect(status().isConflict());
    }
}