package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Beneficiary;
import com.tamvagbackend.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {
    List<Beneficiary> findByCustomer(Customer customer);
    Optional<Beneficiary> findByCustomerAndToken(Customer customer, String token);
}
