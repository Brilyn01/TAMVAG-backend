package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Connection;
import com.tamvagbackend.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, UUID> {

    @Query("SELECT c FROM Connection c WHERE c.connectionId = :connectionId AND c.customer.customerId = :customerId AND c.institution.institutionId = :institutionId")
    Optional<Connection> findByConnectionIdAndCustomerIdAndInstitutionId(
            @Param("connectionId") UUID connectionId,
            @Param("customerId") UUID customerId,
            @Param("institutionId") UUID institutionId
    );

    List<Connection> findByCustomer(Customer customer);
}