package com.ipt.siga_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

// RN-6.11: linked to student + section; subject and period are reached through the section
@Entity
@Table(
        name = "enrollments",
        // RN-6.6: the same student cannot enroll the same section twice (DB-level guard)
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "section_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    // RN-6.11: date when the enrollment was made
    @Column(nullable = false)
    private LocalDate enrollmentDate;

    // RN-7.3: at most one final grade (null = not graded yet, used by RN-7.7)
    private Double finalGrade;

    // RN-7.5: PASSED if grade >= 4.0, FAILED if < 4.0 (calculated, not stored)
    public SubjectResult getResult() {
        if (finalGrade == null) {
            return null;
        }
        return finalGrade >= 4.0 ? SubjectResult.PASSED : SubjectResult.FAILED;
    }
}