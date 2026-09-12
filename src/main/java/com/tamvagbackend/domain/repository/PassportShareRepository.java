package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Passport;
import com.tamvagbackend.domain.entity.PassportShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PassportShareRepository extends JpaRepository<PassportShare, UUID> {
    Optional<PassportShare> findByShareToken(String shareToken);
    List<PassportShare> findByPassport(Passport passport);
}
