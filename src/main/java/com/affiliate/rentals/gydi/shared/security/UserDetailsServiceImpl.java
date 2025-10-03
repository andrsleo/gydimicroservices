package com.affiliate.rentals.gydi.shared.security;

import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

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

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
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

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.email().address())
                .password(user.passwordHash())
                .authorities(authorities)
                .build();
    }
}
