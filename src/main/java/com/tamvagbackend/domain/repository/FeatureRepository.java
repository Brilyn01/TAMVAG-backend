package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, UUID> {
    List<Feature> findBySubjectId(UUID subjectId);
    Optional<Feature> findBySubjectIdAndFeatureNameAndWindowPeriod(UUID subjectId, String featureName, String windowPeriod);
}
