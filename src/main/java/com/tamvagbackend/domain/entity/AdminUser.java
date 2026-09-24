package com.tamvagbackend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_user")
public class AdminUser {

    @Id
    @Column(
            name = "admin_user_id",
            nullable = false,
            updatable = false
    )
    private UUID adminUserId;

    @Column(
            name = "email",
            nullable = false,
            unique = true
    )
    private String email;

    @Column(
            name = "password_hash",
            nullable = false
    )
    private String passwordHash;

    @Column(
            name = "first_name",
            nullable = false
    )
    private String firstName;

    @Column(
            name = "last_name",
            nullable = false
    )
    private String lastName;

    @Column(
            name = "role",
            nullable = false
    )
    private String role = "ADMIN";

    @Column(
        name = "operational_role",
        nullable = true
    )
    private String operationalRole;

    @Column(
            name = "status",
            nullable = false
    )
    private String status = "ACTIVE";

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt = Instant.now();

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt = Instant.now();

    public AdminUser() {
        this.adminUserId = UUID.randomUUID();
    }

    public UUID getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(UUID adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOperationalRole() {
        return operationalRole;
    }

    public void setOperationalRole(String operationalRole) {
        this.operationalRole = operationalRole;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}