package com.movemate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A registered account - customer or admin.
 *
 * <p>Deliberate design choices worth defending in an interview:
 * <ul>
 *   <li>{@code @Table(name = "users")} - "user" is a reserved word in PostgreSQL and
 *       several other engines, so the plural avoids a future portability trap.</li>
 *   <li>{@code GenerationType.IDENTITY} - maps to MySQL AUTO_INCREMENT. Note the tradeoff:
 *       Hibernate cannot batch inserts with IDENTITY because it must read the generated key
 *       back row by row. For a booking site that is irrelevant; for bulk imports you would
 *       choose SEQUENCE (on PostgreSQL) or a TABLE generator.</li>
 *   <li>This entity does NOT implement {@code UserDetails}. Mixing a JPA entity with a
 *       Spring Security interface couples your database model to your security framework;
 *       {@code AppUserDetailsService} adapts between them instead.</li>
 * </ul>
 */
@Entity
@Table(name = "users")
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    /**
     * The login identifier. {@code unique = true} documents the intent in Java, but the real
     * guarantee is the UNIQUE constraint created in V1__init_schema.sql - Hibernate only
     * creates constraints when it generates the schema, which we have disabled.
     */
    @Column(nullable = false, length = 180, unique = true)
    private String email;

    @Column(length = 20)
    private String phone;

    /** BCrypt hash. Always 60 characters; the column allows 100 for algorithm changes. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /**
     * {@code EnumType.STRING} stores "ROLE_ADMIN" in the column.
     * The default, {@code EnumType.ORDINAL}, stores 0/1 - which breaks the day someone
     * reorders the enum constants and silently promotes every customer to admin.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.ROLE_CUSTOMER;

    /** Soft disable. We deactivate accounts instead of deleting them so bookings keep their owner. */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * JPA requires a no-argument constructor (it instantiates entities reflectively before
     * populating fields). It is protected rather than public to discourage application code
     * from creating a half-built User.
     */
    protected User() {
    }

    public User(String fullName, String email, String phone, String passwordHash, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAdmin() {
        return role == Role.ROLE_ADMIN;
    }
}
