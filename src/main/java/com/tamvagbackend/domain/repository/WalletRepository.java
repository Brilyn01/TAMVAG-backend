package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByCustomer(Customer customer);
    Optional<Wallet> findByCustomerCustomerId(UUID customerId);
}
