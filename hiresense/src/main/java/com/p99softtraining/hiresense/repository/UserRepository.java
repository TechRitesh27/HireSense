package com.p99softtraining.hiresense.repository;

import com.p99softtraining.hiresense.entity.User;
import com.p99softtraining.hiresense.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    Optional<User> findByIdAndCompany_Id(UUID id, UUID companyId);

    Optional<User> findByIdAndRole(UUID id, Role role);

    java.util.List<User> findByCompanyIdAndRole(UUID companyId, Role role);
}
