package org.example.financeapp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryDTO {
    private String monthLabel;
    private BigDecimal income;
    private BigDecimal expenses;
}