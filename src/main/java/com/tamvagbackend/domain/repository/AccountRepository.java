package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByCustomer(Customer customer);
    Optional<Account> findByAccountRefToken(String accountRefToken);
}
