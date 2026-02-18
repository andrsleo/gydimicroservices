package com.affiliate.rentals.gydi.shared.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;

/**
 * Custom UserDetailsService implementation for Spring Security.
 *
 * <p>This service loads user-specific data for authentication by integrating
 * with the domain layer through the UserRepository port.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepositoryPort userRepository;

    public UserDetailsServiceImpl(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user details by email address.
     *
     * @param username the email address (username) of the user
     * @return UserDetails object containing user information and authorities
     * @throws UsernameNotFoundException if the user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Email email = Email.of(username);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        Set<GrantedAuthority> authorities = user.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name().getValue()))
                .collect(Collectors.toSet());

        // ✅ SECURITY FIX: Return CustomUserDetails to include userId for ownership validation
        return new CustomUserDetails(
            user.id(),
            user.email().address(),
            user.passwordHash(),
            authorities,
            user.isAccountVerified(),
            true // All users retrieved from repo are considered active
        );
    }
}
