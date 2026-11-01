package com.movemate.dto;

import com.movemate.domain.Role;
import com.movemate.domain.User;

/**
 * The safe, outward-facing view of a user.
 *
 * <p>Compare the fields with {@link User}: {@code passwordHash} and {@code enabled} are absent.
 * This is the strongest argument for response DTOs - a hash that is never serialised can never
 * leak through a log line, a browser devtools tab or a cached JSON response. Relying on
 * {@code @JsonIgnore} on the entity works until someone removes it in an unrelated refactor.
 */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        Role role,
        String roleLabel
) {

    /**
     * Static factory that maps entity to DTO.
     *
     * <p>Keeping the mapping next to the DTO means the response shape and its construction live
     * in one file. Larger projects generate this with MapStruct (compile-time, zero reflection);
     * for a handful of DTOs, hand-written factories are clearer and have no build-plugin setup.
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getRole().getLabel()
        );
    }
}
