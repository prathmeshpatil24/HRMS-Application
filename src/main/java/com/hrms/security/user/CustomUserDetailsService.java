package com.hrms.security.user;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmailWithRolesAndPermissions(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format("User not found with username or email: %s", usernameOrEmail)));

        return CustomUserDetails.build(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format("User not found with id: %d", id)));

        return CustomUserDetails.build(user);
    }
}
