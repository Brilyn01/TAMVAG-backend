package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.FeatureSnapshot;
import com.tamvagbackend.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureSnapshotRepository
        extends JpaRepository<FeatureSnapshot, UUID> {

    Optional<FeatureSnapshot> findByCustomerAndPeriodStartAndPeriodEndAndFeatureVersion(
            Customer customer,
            LocalDate periodStart,
            LocalDate periodEnd,
            String featureVersion
    );

    List<FeatureSnapshot> findByCustomerOrderByComputedAtDesc(
            Customer customer
    );

    List<FeatureSnapshot> findByCustomerAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByComputedAtDesc(
            Customer customer,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}

