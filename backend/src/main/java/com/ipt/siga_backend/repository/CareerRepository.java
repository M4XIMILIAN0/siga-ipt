package com.ipt.siga_backend.repository;

import com.ipt.siga_backend.entity.Career;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerRepository extends JpaRepository<Career, Long> {

    // RN-2.1: check if a career code is already taken
    boolean existsByCode(String code);
}