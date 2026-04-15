package org.example.financeapp.service;

import org.example.financeapp.model.User;
import org.example.financeapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User saveUser(User user) {
        userRepository.findByUsername(user.getUsername()).ifPresent(u -> {
            throw new RuntimeException("User with this username already exists");
        });
        userRepository.findByEmail(user.getEmail()).ifPresent(u -> {
            throw new RuntimeException("User with this email already exists");
        });
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public void updateUserCurrency(String username, String currency) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setCurrency(currency);
        userRepository.save(user);
    }
}
