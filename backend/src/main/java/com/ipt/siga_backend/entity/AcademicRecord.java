package com.ipt.siga_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Academic record ("historial"): created only at period closing (RN-7.8)
@Entity
@Table(
        name = "academic_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject_id", "period_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademicRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Kept for the business rules (RN-6.4, RN-6.5, RN-7.9)
    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "period_id", nullable = false)
    private AcademicPeriod period;

    // RN-3.11: a COPY of the subject data at closing time, so later edits do not change history
    @Column(nullable = false, length = 20)
    private String subjectCode;

    @Column(nullable = false, length = 150)
    private String subjectName;

    @Column(nullable = false)
    private Integer semester;

    @Column(nullable = false)
    private Integer sctCredits;

    @Column(nullable = false)
    private Double finalGrade;

    // RN-7.5: PASSED or FAILED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SubjectResult result;
}