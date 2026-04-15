package org.example.financeapp.repository;

import org.example.financeapp.model.Debt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DebtRepository extends JpaRepository<Debt, Long> {
    List<Debt> findByUserId(Long userId);
}
