package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    Optional<Application> findByClientId(String clientId);

    Optional<Application> findByApplicationIdAndInstitution_InstitutionId(
            UUID applicationId,
            UUID institutionId
    );

    List<Application> findByInstitution_InstitutionId(UUID institutionId);
}