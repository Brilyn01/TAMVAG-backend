package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Wallet;
import com.tamvagbackend.domain.entity.WalletBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletBalanceRepository extends JpaRepository<WalletBalance, UUID> {
    List<WalletBalance> findByWallet(Wallet wallet);
    Optional<WalletBalance> findByWalletAndCurrency(Wallet wallet, String currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.wallet = :wallet AND wb.currency = :currency")
    Optional<WalletBalance> findByWalletAndCurrencyForUpdate(@Param("wallet") Wallet wallet, @Param("currency") String currency);
}

