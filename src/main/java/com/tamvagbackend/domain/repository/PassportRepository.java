package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.Passport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PassportRepository extends JpaRepository<Passport, UUID> {
    List<Passport> findByCustomer(Customer customer);
    List<Passport> findByCustomerAndStatus(Customer customer, String status);
}
