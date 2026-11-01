package com.movemate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Shared timestamp columns for every entity.
 *
 * <p>{@code @MappedSuperclass} means: "these fields become columns on each subclass's own
 * table". There is no {@code auditable} table and no join - the inheritance exists only in
 * Java. Compare that with {@code @Entity} + {@code @Inheritance}, which really does create
 * extra tables or a discriminator column. We want code reuse, not a table, so
 * {@code @MappedSuperclass} is the right tool.
 *
 * <p>{@code @EntityListeners(AuditingEntityListener.class)} hooks Spring Data's callback in,
 * which populates the two fields on insert and update. The alternative is a JPA
 * {@code @PrePersist} / {@code @PreUpdate} method - fine too, but then you repeat it per entity.
 *
 * <p>Why {@link Instant} and not {@code LocalDateTime}? An Instant is an unambiguous point on
 * the global timeline (UTC). LocalDateTime has no timezone, so "2026-10-04 14:30" means
 * different real moments in Pune and in London. Audit data must never be ambiguous.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    /** Set once on insert. {@code updatable = false} makes Hibernate leave it out of UPDATE statements. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Rewritten on every update. Null until the row is first modified. */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

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
