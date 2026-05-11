package org.example.financeapp.service;

import org.example.financeapp.model.Budget;
import org.example.financeapp.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BudgetService {
    private final BudgetRepository budgetRepository;
    @Autowired
    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }
    public List<Budget> findByUserId(Long userId) {
        return budgetRepository.findByUserId(userId);
    }
    public Optional<Budget> findById(Long id) {
        return budgetRepository.findById(id);
    }
    public void deleteById(Long id) {
        budgetRepository.deleteById(id);
    }
    public Budget save(Budget budget) {
        return budgetRepository.save(budget);
    }
}
