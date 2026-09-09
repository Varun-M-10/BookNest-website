package com.booknest.service;

import com.booknest.dto.UserRegistrationDto;
import com.booknest.entity.Role;
import com.booknest.entity.User;
import com.booknest.repository.RoleRepository;
import com.booknest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Service class for User operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (registrationDto.getPhone() != null && 
            userRepository.existsByPhone(registrationDto.getPhone())) {
            throw new RuntimeException("Phone number already registered");
        }

        User user = new User();
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setPhone(registrationDto.getPhone());
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("ROLE_USER");
                    newRole.setDescription("Standard user role");
                    return roleRepository.save(newRole);
                });
        roles.add(userRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            String email = authentication.getName();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    @SuppressWarnings("null")
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @SuppressWarnings("null")
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public void updateLastLogin(User user) {
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    public void changePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public java.util.List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllUsers();
        }
        return userRepository.searchUsers(query.trim());
    }

    public User toggleUserStatus(Long userId) {
        User user = getUserById(userId);
        if (user != null) {
            user.setEnabled(!user.isEnabled());
            return userRepository.save(user);
        }
        return null;
    }

    public User toggleUserAdminRole(Long userId) {
        User user = getUserById(userId);
        if (user != null) {
            boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
            if (isAdmin) {
                user.getRoles().removeIf(r -> "ROLE_ADMIN".equals(r.getName()));
            } else {
                Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                    Role r = new Role();
                    r.setName("ROLE_ADMIN");
                    r.setDescription("Administrator role");
                    return roleRepository.save(r);
                });
                user.getRoles().add(adminRole);
            }
            return userRepository.save(user);
        }
        return null;
    }
}
