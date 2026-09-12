package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Wallet;
import com.tamvagbackend.domain.entity.WalletBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletBalanceRepository extends JpaRepository<WalletBalance, UUID> {
    List<WalletBalance> findByWallet(Wallet wallet);
    Optional<WalletBalance> findByWalletAndCurrency(Wallet wallet, String currency);
}
