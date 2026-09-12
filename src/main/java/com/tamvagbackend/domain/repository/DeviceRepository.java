package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findByCustomer(Customer customer);
    Optional<Device> findByCustomerAndFingerprintToken(Customer customer, String fingerprintToken);
}
