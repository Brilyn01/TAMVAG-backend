package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.RiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RiskEventRepository extends JpaRepository<RiskEvent, UUID> {
    List<RiskEvent> findByCustomerOrderByCreatedAtDesc(Customer customer);
    List<RiskEvent> findTop50ByOrderByCreatedAtDesc();
}
