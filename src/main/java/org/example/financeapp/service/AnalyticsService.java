package org.example.financeapp.service;

import org.example.financeapp.model.Transaction;
import org.example.financeapp.model.dto.CategoryChartDTO;
import org.example.financeapp.model.dto.MonthlySummaryDTO;
import org.example.financeapp.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class AnalyticsService {

    private final TransactionRepository transactionRepository;

    public AnalyticsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    private Stream<Transaction> filterTransactionsByDate(Long userId, LocalDate startDate, LocalDate endDate) {
        Stream<Transaction> transactions = transactionRepository.findByUserId(userId).stream();

        if (startDate != null) {
            transactions = transactions.filter(t -> !t.getDate().isBefore(startDate));
        }
        if (endDate != null) {
            transactions = transactions.filter(t -> !t.getDate().isAfter(endDate));
        }
        return transactions;
    }

    public List<CategoryChartDTO> getExpenseByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        return filterTransactionsByDate(userId, startDate, endDate)
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ))
                .entrySet().stream()
                .map(e -> new CategoryChartDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CategoryChartDTO::getValue).reversed())
                .toList();
    }

    public List<CategoryChartDTO> getIncomeByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        return filterTransactionsByDate(userId, startDate, endDate)
                .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ))
                .entrySet().stream()
                .map(e -> new CategoryChartDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CategoryChartDTO::getValue).reversed())
                .toList();
    }

    public List<MonthlySummaryDTO> getMonthlySummary(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> incomeByMonth = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseByMonth = new LinkedHashMap<>();

        filterTransactionsByDate(userId, startDate, endDate).forEach(t -> {
            String monthLabel = Month.of(t.getDate().getMonthValue())
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + t.getDate().getYear();

            if (t.getType() == Transaction.TransactionType.INCOME) {
                incomeByMonth.merge(monthLabel, t.getAmount(), BigDecimal::add);
            } else {
                expenseByMonth.merge(monthLabel, t.getAmount(), BigDecimal::add);
            }
        });

        Set<String> allMonths = new LinkedHashSet<>();
        allMonths.addAll(incomeByMonth.keySet());
        allMonths.addAll(expenseByMonth.keySet());

        return allMonths.stream()
                .map(month -> new MonthlySummaryDTO(
                        month,
                        incomeByMonth.getOrDefault(month, BigDecimal.ZERO),
                        expenseByMonth.getOrDefault(month, BigDecimal.ZERO)
                ))
                .toList();
    }
}