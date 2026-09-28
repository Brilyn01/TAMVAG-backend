package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.CurrencyTransfer;
import com.tamvagbackend.domain.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurrencyTransferRepository extends JpaRepository<CurrencyTransfer, UUID> {
    List<CurrencyTransfer> findByWalletOrderByCreatedAtDesc(Wallet wallet);
    Optional<CurrencyTransfer> findByWalletAndReference(Wallet wallet, String reference);
}

