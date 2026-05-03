package org.example.financeapp.service;

import org.example.financeapp.model.Budget;
import org.example.financeapp.model.Transaction;
import org.example.financeapp.model.dto.BudgetStatusDTO;
import org.example.financeapp.repository.BudgetRepository;
import org.example.financeapp.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class FinanceService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public FinanceService(TransactionRepository transactionRepository, BudgetRepository budgetRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    public BigDecimal calculateTotalBalance(Long userId) {
        BigDecimal income = calculateTotalIncome(userId);
        BigDecimal expenses = calculateTotalExpenses(userId);
        return income.subtract(expenses);
    }

    public BigDecimal calculateTotalIncome(Long userId) {
        return transactionRepository.findByUserId(userId).stream()
                .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotalExpenses(Long userId) {
        return transactionRepository.findByUserId(userId).stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<BudgetStatusDTO> getBudgetStatus(Long userId) {
        LocalDate today = LocalDate.now();

        List<Budget> budgets = budgetRepository.findByUserId(userId).stream()
                .filter(budget -> budget.getMonth() == today.getMonthValue())
                .filter(budget -> budget.getYear() == today.getYear())
                .toList();

        return budgets.stream().map(budget -> {
            List<Transaction> monthTransactions = transactionRepository.findByUserId(userId).stream()
                    .filter(t -> t.getCategory() != null)
                    .filter(t -> t.getCategory().getId().equals(budget.getCategory().getId()))
                    .filter(t -> t.getDate().getMonthValue() == budget.getMonth())
                    .filter(t -> t.getDate().getYear() == budget.getYear())
                    .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                    .toList();

            BigDecimal currentSpending = monthTransactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int usagePercentage = 0;
            if (budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0) {
                usagePercentage = currentSpending.multiply(BigDecimal.valueOf(100))
                        .divide(budget.getLimitAmount(), 0, RoundingMode.HALF_UP).intValue();
            }

            String statusColor;
            if (usagePercentage > 99) {
                statusColor = "danger";
            } else if (usagePercentage > 74) {
                statusColor = "warning";
            } else {
                statusColor = "success";
            }

            LocalDate periodStart = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
            LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

            return new BudgetStatusDTO(
                    budget.getCategory().getName(),
                    budget.getLimitAmount(),
                    currentSpending,
                    usagePercentage,
                    statusColor,
                    budget.getMonth(),
                    budget.getYear(),
                    periodStart,
                    periodEnd
            );
        }).collect(Collectors.toList());
    }

    public boolean isBudgetExceeded(Transaction transaction) {
        if (transaction.getType() == Transaction.TransactionType.EXPENSE) {
            LocalDate date = transaction.getDate();
            Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                    transaction.getUser().getId(),
                    transaction.getCategory().getId(),
                    date.getMonthValue(),
                    date.getYear()
            );

            if (budgetOpt.isPresent()) {
                Budget budget = budgetOpt.get();
                List<Transaction> monthTransactions = transactionRepository.findByUserId(transaction.getUser().getId())
                        .stream()
                        .filter(t -> t.getCategory() != null)
                        .filter(t -> t.getCategory().getId().equals(transaction.getCategory().getId()))
                        .filter(t -> t.getDate().getMonthValue() == date.getMonthValue())
                        .filter(t -> t.getDate().getYear() == date.getYear())
                        .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                        .toList();

                BigDecimal currentSpending = monthTransactions.stream()
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                return currentSpending.add(transaction.getAmount()).compareTo(budget.getLimitAmount()) > 0;
            }
        }
        return false;
    }
}
