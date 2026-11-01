package com.movemate.security;

import com.movemate.domain.Role;
import com.movemate.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The Spring Security view of a logged-in user: the "principal".
 *
 * <p>Why a separate class instead of making {@link User} implement {@link UserDetails}?
 * Because the entity would then be coupled to Spring Security forever - it would have to carry
 * {@code isAccountNonLocked()}, {@code getUsername()} and friends into every JPA query, test
 * and JSON response. This adapter keeps the two models independent: the entity is about
 * persistence, this class is about authentication.
 *
 * <p>Controllers receive it with {@code @AuthenticationPrincipal AuthenticatedUser me}, which
 * is how {@code /bookings/me} knows whose bookings to return without trusting a client-supplied
 * user id.
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;

    public AuthenticatedUser(Long id, String fullName, String email, String passwordHash,
                             Role role, boolean enabled) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
    }

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.isEnabled());
    }

    /**
     * The authorities this user holds.
     *
     * <p>Because {@link Role} constants are already named {@code ROLE_ADMIN} /
     * {@code ROLE_CUSTOMER}, {@code name()} can be used directly and
     * {@code hasRole("ADMIN")} (which prepends {@code ROLE_}) matches.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /** Spring compares the submitted password against this hash via the PasswordEncoder. */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** "Username" in Spring's vocabulary means "login identifier" - ours is the email. */
    @Override
    public String getUsername() {
        return email;
    }

    /*
     * The three "non-expired / non-locked" flags are hardcoded true because this product has
     * no password-ageing or lockout policy. They are left explicit (rather than removed) so a
     * future reviewer sees the decision was made, not forgotten - the interface's default
     * methods would return true silently.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Honours the soft-disable flag: a deactivated account cannot authenticate. */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ROLE_ADMIN;
    }
}
