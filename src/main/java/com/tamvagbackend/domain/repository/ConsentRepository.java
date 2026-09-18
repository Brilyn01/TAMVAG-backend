package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Consent;
import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    List<Consent> findByCustomer(Customer customer);

    List<Consent> findByCustomerAndStatus(
            Customer customer,
            String status
    );

    @Query("SELECT c FROM Consent c WHERE c.customer.customerId = :customerId AND c.institution.institutionId = :institutionId")
    List<Consent> findByCustomerIdAndInstitutionId(
            @Param("customerId") UUID customerId,
            @Param("institutionId") UUID institutionId
    );
}
