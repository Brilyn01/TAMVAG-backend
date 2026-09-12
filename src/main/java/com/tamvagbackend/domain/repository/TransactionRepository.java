package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Account;
import com.tamvagbackend.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByAccountAndSourceEventId(Account account, String sourceEventId);
    List<Transaction> findByAccountOrderByOccurredAtDesc(Account account);

    @Query("SELECT t FROM Transaction t WHERE t.account.customer.customerId = :customerId AND t.occurredAt >= :since ORDER BY t.occurredAt DESC")
    List<Transaction> findRecentByCustomer(@Param("customerId") UUID customerId, @Param("since") Instant since);

    @Query("SELECT t FROM Transaction t WHERE t.account.customer.customerId = :customerId ORDER BY t.occurredAt DESC")
    List<Transaction> findAllByCustomerId(@Param("customerId") UUID customerId);
}
