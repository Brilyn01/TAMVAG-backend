package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.LedgerEntry;
import com.tamvagbackend.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByTransaction(Transaction transaction);

    @Query("SELECT le FROM LedgerEntry le WHERE le.transaction.account.customer.customerId = :customerId AND le.createdAt >= :since ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByCustomerSince(@Param("customerId") UUID customerId, @Param("since") Instant since);
}
