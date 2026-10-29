package com.ipt.siga_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Period registration ("matrícula"): the student is allowed to enroll in this period (RN-6.1)
@Entity
@Table(
        name = "period_registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "period_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeriodRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "period_id", nullable = false)
    private AcademicPeriod period;
}