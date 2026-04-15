package org.example.financeapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "recurring_tasks")
public class RecurringTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "UAH";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transaction.TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private int intervalValue = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntervalUnit intervalUnit = IntervalUnit.MONTHS;

    @Column(nullable = false)
    private LocalDate nextRunDate;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum IntervalUnit {
        DAYS, WEEKS, MONTHS, YEARS
    }
}