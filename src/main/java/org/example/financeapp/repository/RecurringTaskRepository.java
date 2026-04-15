package org.example.financeapp.repository;

import org.example.financeapp.model.RecurringTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTaskRepository extends JpaRepository<RecurringTask, Long> {
    List<RecurringTask> findByActiveTrueAndNextRunDateLessThanEqual(LocalDate date);
    List<RecurringTask> findByUserId(Long userId);
}
