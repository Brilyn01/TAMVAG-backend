package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Consent;
import com.tamvagbackend.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, UUID> {
    List<Consent> findByCustomer(Customer customer);
    List<Consent> findByCustomerAndStatus(Customer customer, String status);
}
