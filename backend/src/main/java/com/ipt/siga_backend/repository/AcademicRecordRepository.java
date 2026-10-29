package com.ipt.siga_backend.repository;

import com.ipt.siga_backend.entity.AcademicRecord;
import com.ipt.siga_backend.entity.SubjectResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, Long> {

    // RN-6.4 / RN-6.5: did the student pass this subject?
    boolean existsByStudentIdAndSubjectIdAndResult(Long studentId, Long subjectId, SubjectResult result);

    // RN-6.7: how many subjects has the student passed? (to know how many are pending)
    long countByStudentIdAndResult(Long studentId, SubjectResult result);

    // RN-7.9: how many times did the student fail this subject?
    long countByStudentIdAndSubjectIdAndResult(Long studentId, Long subjectId, SubjectResult result);

    List<AcademicRecord> findByStudentId(Long studentId);   // student's history view
    boolean existsByStudentId(Long studentId);              // RN-1.6 / RN-1.8
    boolean existsBySubjectId(Long subjectId);              // RN-3.10
}