package com.movemate.security;

import com.movemate.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges Spring Security to our database.
 *
 * <p>{@link UserDetailsService} has exactly one method. Spring Security's
 * {@code DaoAuthenticationProvider} calls it during login to fetch the stored hash, then
 * compares it with the submitted password using the configured {@code PasswordEncoder}.
 * Our {@link JwtAuthenticationFilter} calls it on each authenticated request too.
 *
 * <p>Implementing this interface is the hook that lets a framework look up users in ANY store -
 * JPA here, LDAP or an HTTP call elsewhere - without the framework knowing anything about it.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Constructor injection. No {@code @Autowired} needed since Spring 4.3 when a class has a
     * single constructor.
     *
     * <p>Why constructor injection rather than field injection?
     * <ul>
     *   <li>the field can be {@code final}, so the dependency cannot be swapped at runtime;</li>
     *   <li>the object is impossible to construct in an invalid state;</li>
     *   <li>unit tests can pass a mock with {@code new AppUserDetailsService(mockRepo)} -
     *       no reflection, no Spring context.</li>
     * </ul>
     * This is the single most common "what would you improve in this code" answer, so get it
     * right everywhere.
     */
    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * {@code readOnly = true} tells Hibernate not to bother with dirty checking and lets the
     * driver flag the transaction read-only - a small, free win on a path that runs on every
     * single authenticated request.
     *
     * @param email the login identifier (Spring calls it "username")
     * @throws UsernameNotFoundException when no such account exists. Spring translates it into
     *         {@code BadCredentialsException} by default, so the API never reveals whether an
     *         email is registered.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for " + email));
    }
}
