package com.ipt.siga_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "academic_periods",
        // RN-5.1: a period (year + semester) exists only once
        uniqueConstraints = @UniqueConstraint(columnNames = {"period_year", "semester"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademicPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "year" is an SQL keyword, so the column gets a safer name
    @Column(name = "period_year", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer semester;

    // RN-5.2 / RN-6.3 / RN-7.6
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PeriodStatus status;

    // RN-5.1: code like "2027-1" (calculated, not stored, same idea as telTotal)
    public String getCode() {
        return year + "-" + semester;
    }
}