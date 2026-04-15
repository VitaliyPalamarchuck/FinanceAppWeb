package org.example.financeapp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetStatusDTO {

    private String categoryName;
    private BigDecimal limit;
    private BigDecimal spent;
    private int percentage;
    private String statusColor;

    private int month;
    private int year;

    private LocalDate periodStart;
    private LocalDate periodEnd;
}
