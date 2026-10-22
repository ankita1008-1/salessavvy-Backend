package com.example.learn.adminservices;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.learn.entity.Role;
import com.example.learn.entity.User;
import com.example.learn.repository.JWTTokenRepository;
import com.example.learn.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final JWTTokenRepository jwtTokenRepository;

    public AdminUserService(UserRepository userRepository,
                            JWTTokenRepository jwtTokenRepository) {
        this.userRepository = userRepository;
        this.jwtTokenRepository = jwtTokenRepository;
    }

    @Transactional
    public User modifyUser(Integer userId, String username,
                           String email, String role) {

        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User existingUser = userOptional.get();

        if (username != null && !username.isEmpty()) {
            existingUser.setUsername(username);
        }

        if (email != null && !email.isEmpty()) {
            existingUser.setEmail(email);
        }

        if (role != null && !role.isEmpty()) {
            try {
                existingUser.setRole(Role.valueOf(role.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + role);
            }
        }

        jwtTokenRepository.deleteByUserId(userId);

        return userRepository.save(existingUser);
    }

    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }
}