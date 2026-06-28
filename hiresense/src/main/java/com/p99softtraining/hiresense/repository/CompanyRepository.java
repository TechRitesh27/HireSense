package com.p99softtraining.hiresense.repository;

import com.p99softtraining.hiresense.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByEmail(String email);

    // Used by Super Admin to list all companies
    java.util.List<Company> findAllByOrderByCreatedAtDesc();
}
