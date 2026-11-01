package com.movemate.repository;

import com.movemate.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for {@link User}.
 *
 * <p>There is no implementation class anywhere in this project, and that is the point.
 * Spring Data creates a proxy at startup and derives the SQL from the method NAME:
 * {@code findByEmail} becomes {@code SELECT ... FROM users WHERE email = ?}. The parser
 * understands {@code And}, {@code Or}, {@code Like}, {@code Between}, {@code OrderBy},
 * {@code True}/{@code False}, {@code IgnoreCase} and more.
 *
 * <p>{@code JpaRepository<User, Long>} - first type is the entity, second is the type of its
 * {@code @Id}. Inheriting it gives us save, findById, findAll, delete, count, plus paging
 * and sorting, for free.
 *
 * <p>{@code @Repository} is technically optional here (Spring Data registers the interface
 * anyway) but it documents the layer and enables exception translation, which converts
 * vendor-specific {@code SQLException}s into Spring's {@code DataAccessException} hierarchy.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * {@link Optional} rather than {@code User} so the absence of a row is part of the method
     * signature. Returning null here is how NullPointerExceptions get into login code.
     */
    Optional<User> findByEmail(String email);

    /**
     * Cheaper than {@code findByEmail(...).isPresent()}: Spring Data issues
     * {@code SELECT COUNT(*)} and never materialises the entity.
     * Used by registration to return a clean 409 Conflict before attempting the insert.
     */
    boolean existsByEmail(String email);
}
