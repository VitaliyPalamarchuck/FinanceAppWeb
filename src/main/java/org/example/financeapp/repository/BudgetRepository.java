package org.example.financeapp.repository;

import org.example.financeapp.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(Long userId, Long categoryId, int month, int year);
}
