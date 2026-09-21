package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tamva_user")
public class TamvaUser {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "role", nullable = false)
    private String role = "CUSTOMER";

    @Column(name = "status", nullable = false)
    private String status = "PENDING_VERIFICATION";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public TamvaUser() {
        this.userId = UUID.randomUUID();
    }

    public UUID getUserId()                        { return userId; }
    public void setUserId(UUID userId)             { this.userId = userId; }

    public UUID getCustomerId()                    { return customerId; }
    public void setCustomerId(UUID customerId)     { this.customerId = customerId; }

    public String getEmail()                       { return email; }
    public void setEmail(String email)             { this.email = email; }

    public String getPasswordHash()                { return passwordHash; }
    public void setPasswordHash(String hash)       { this.passwordHash = hash; }

    public String getFirstName()                   { return firstName; }
    public void setFirstName(String firstName)     { this.firstName = firstName; }

    public String getLastName()                    { return lastName; }
    public void setLastName(String lastName)       { this.lastName = lastName; }

    public String getPhoneNumber()                 { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRole()                        { return role; }
    public void setRole(String role)               { this.role = role; }

    public String getStatus()                      { return status; }
    public void setStatus(String status)           { this.status = status; }

    public Instant getCreatedAt()                  { return createdAt; }
    public void setCreatedAt(Instant createdAt)    { this.createdAt = createdAt; }

    public Instant getUpdatedAt()                  { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt)    { this.updatedAt = updatedAt; }
}
