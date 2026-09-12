package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Connection;
import com.tamvagbackend.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, UUID> {
    List<Connection> findByCustomer(Customer customer);
}
