package com.ipt.siga_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RN-4.1: RUN must be unique
    @Column(nullable = false, unique = true, length = 12)
    private String run;

    @Column(nullable = false, length = 150)
    private String fullName;

    // RN-4.2: email must be unique
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // RN-4.3: professional title and academic degree are required
    @Column(nullable = false, length = 200)
    private String professionalTitle;

    @Column(nullable = false, length = 200)
    private String academicDegree;

    // RN-4.4 / RN-4.7: ACTIVE or INACTIVE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeacherStatus status;

}
