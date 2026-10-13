package com.ipt.siga_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "subjects",
        // RN-3.2: subject code must be unique within the study plan
        uniqueConstraints = @UniqueConstraint(columnNames = {"study_plan_id", "code"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    // RN-3.3: semester between 1 and 4 (validated in SubjectService)
    @Column(nullable = false)
    private Integer semester;

    // RN-3.5 / RN-3.6: weekly hours (validated in SubjectService)
    @Column(nullable = false)
    private Integer theoryHours;

    @Column(nullable = false)
    private Integer exerciseHours;

    @Column(nullable = false)
    private Integer labHours;

    // RN-3.4: 0 < SCT <= 7 (validated in SubjectService)
    @Column(nullable = false)
    private Integer sctCredits;

    // RN-3.1: every subject belongs to an existing study plan
    @ManyToOne
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlan studyPlan;

    // RN-3.7 / RN-3.8 / RN-3.9: 0..3 prerequisites, same plan, earlier semesters
    @ManyToMany
    @JoinTable(
            name = "subject_prerequisites",
            joinColumns = @JoinColumn(name = "subject_id"),             // the subject
            inverseJoinColumns = @JoinColumn(name = "prerequisite_id")  // what it requires
    )
    private List<Subject> prerequisites = new ArrayList<>();

    // TEL total = theory + exercise + lab (calculated, not stored)
    public int getTelTotal() {
        return theoryHours + exerciseHours + labHours;
    }
}