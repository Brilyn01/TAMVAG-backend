package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Connection;
import com.tamvagbackend.domain.entity.TransactionQuarantine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionQuarantineRepository
        extends JpaRepository<TransactionQuarantine, UUID> {

    List<TransactionQuarantine> findByConnectionOrderByCreatedAtDesc(
            Connection connection
    );

    List<TransactionQuarantine> findByConnectionAndStatusOrderByCreatedAtDesc(
            Connection connection,
            String status
    );
}