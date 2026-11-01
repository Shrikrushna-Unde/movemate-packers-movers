package com.movemate.repository;

import com.movemate.domain.MovingService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for the service catalogue.
 *
 * <p>Every method here is a derived query - no SQL, no JPQL, no implementation.
 * Read the names literally and you can predict the SQL:
 * {@code findByActiveTrueOrderByDisplayOrderAsc} becomes
 * {@code WHERE active = true ORDER BY display_order ASC}.
 */
@Repository
public interface MovingServiceRepository extends JpaRepository<MovingService, Long> {

    /** What the public website shows: active services in the admin's chosen order. */
    List<MovingService> findByActiveTrueOrderByDisplayOrderAsc();

    /** What the admin screen shows: everything, including hidden services. */
    List<MovingService> findAllByOrderByDisplayOrderAsc();

    /** Detail page lookup by URL slug. */
    Optional<MovingService> findBySlugAndActiveTrue(String slug);

    Optional<MovingService> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
