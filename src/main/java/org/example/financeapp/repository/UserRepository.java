package org.example.financeapp.repository;

import org.example.financeapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username); // Змінено на Optional
    Optional<User> findByEmail(String email); // Змінено на Optional
}
