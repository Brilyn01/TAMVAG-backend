package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.RefreshTokenSession;
import com.tamvagbackend.domain.entity.TamvaUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, UUID> {
    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);
    List<RefreshTokenSession> findByUser(TamvaUser user);
    void deleteByUser(TamvaUser user);
}
