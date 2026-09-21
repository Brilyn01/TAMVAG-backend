package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.TamvaUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TamvaUserRepository extends JpaRepository<TamvaUser, UUID> {
    Optional<TamvaUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
