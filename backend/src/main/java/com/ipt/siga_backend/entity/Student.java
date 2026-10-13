package com.ipt.siga_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RN-1.2: RUN must be unique
    @Column(nullable = false, unique = true, length = 12)
    private String run;

    @Column(nullable = false, length = 150)
    private String fullName;

    // RN-1.3: email must be unique
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // RN-1.5 / RN-1.7 / RN-1.9
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StudentStatus status;

    // RN-1.4: the student belongs to a study plan; career = studyPlan.getCareer()
    @ManyToOne
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlan studyPlan;
}