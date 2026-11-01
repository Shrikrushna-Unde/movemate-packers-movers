package com.movemate.service;

import com.movemate.domain.MovingService;
import com.movemate.dto.MovingServiceResponse;
import com.movemate.dto.MovingServiceUpsertRequest;
import com.movemate.exception.ConflictException;
import com.movemate.exception.ResourceNotFoundException;
import com.movemate.repository.MovingServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The service catalogue: public reads and admin writes.
 */
@Service
public class CatalogService {

    private final MovingServiceRepository repository;

    public CatalogService(MovingServiceRepository repository) {
        this.repository = repository;
    }

    /** Public: active entries only, in the admin's display order. */
    @Transactional(readOnly = true)
    public List<MovingServiceResponse> findPublicServices() {
        return repository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(MovingServiceResponse::from)
                .toList();
    }

    /** Public detail page. A deactivated service 404s rather than rendering a dead page. */
    @Transactional(readOnly = true)
    public MovingServiceResponse findPublicBySlug(String slug) {
        return repository.findBySlugAndActiveTrue(slug)
                .map(MovingServiceResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "slug", slug));
    }

    /** Admin: everything, including hidden entries. */
    @Transactional(readOnly = true)
    public List<MovingServiceResponse> findAllForAdmin() {
        return repository.findAllByOrderByDisplayOrderAsc().stream()
                .map(MovingServiceResponse::from)
                .toList();
    }

    @Transactional
    public MovingServiceResponse create(MovingServiceUpsertRequest request) {
        if (repository.existsBySlug(request.slug())) {
            throw new ConflictException("A service with the slug '" + request.slug() + "' already exists");
        }
        MovingService entity = new MovingService();
        copyFields(entity, request);
        return MovingServiceResponse.from(repository.save(entity));
    }

    @Transactional
    public MovingServiceResponse update(Long id, MovingServiceUpsertRequest request) {
        MovingService entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));

        // Changing a slug is allowed, but not onto one another row already owns. Without this
        // check the UNIQUE constraint would throw and the admin would see a generic 409 with
        // no idea which field caused it.
        repository.findBySlug(request.slug()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ConflictException("Another service already uses the slug '" + request.slug() + "'");
            }
        });

        copyFields(entity, request);
        // No save() call: inside the transaction `entity` is a MANAGED JPA instance, so
        // Hibernate's dirty checking detects the changes and flushes an UPDATE on commit.
        // Calling save() here would work identically - knowing why it is unnecessary is the
        // part interviewers probe.
        return MovingServiceResponse.from(entity);
    }

    /**
     * Soft delete by flipping {@code active}.
     *
     * <p>A hard DELETE would break detail pages customers have bookmarked and would lose the
     * price history behind existing bookings. Deactivating is almost always the right answer
     * for catalogue data.
     */
    @Transactional
    public MovingServiceResponse toggleActive(Long id) {
        MovingService entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
        entity.setActive(!entity.isActive());
        return MovingServiceResponse.from(entity);
    }

    /**
     * Copies DTO fields onto the entity.
     *
     * <p>Create and update need identical copying, and duplicating eight setter calls is how
     * the two paths silently drift apart the day someone adds a ninth field. In a larger
     * codebase this is exactly the method MapStruct would generate at compile time.
     */
    private void copyFields(MovingService entity, MovingServiceUpsertRequest request) {
        entity.setSlug(request.slug().trim());
        entity.setTitle(request.title().trim());
        entity.setShortDescription(request.shortDescription().trim());
        entity.setDescription(request.description().trim());
        entity.setIcon(request.icon().trim());
        entity.setStartingPrice(request.startingPrice());
        entity.setDisplayOrder(request.displayOrder());
        entity.setActive(request.active());
    }
}
