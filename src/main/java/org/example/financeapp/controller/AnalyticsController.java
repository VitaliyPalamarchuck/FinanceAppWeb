package org.example.financeapp.controller;

import org.example.financeapp.model.User;
import org.example.financeapp.model.dto.CategoryChartDTO;
import org.example.financeapp.model.dto.MonthlySummaryDTO;
import org.example.financeapp.repository.UserRepository;
import org.example.financeapp.service.AnalyticsService;
import org.example.financeapp.service.CurrencyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class AnalyticsController {

    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final CurrencyService currencyService;

    public AnalyticsController(UserRepository userRepository,
                               AnalyticsService analyticsService,
                               CurrencyService currencyService) {
        this.userRepository = userRepository;
        this.analyticsService = analyticsService;
        this.currencyService = currencyService;
    }

    @GetMapping("/analytics")
    public String analytics(Model model,
                            @AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        String userCurrency = user.getCurrency() == null ? "UAH" : user.getCurrency();

        List<CategoryChartDTO> expenseByCategory = analyticsService.getExpenseByCategory(user.getId(), startDate, endDate).stream()
                .map(dto -> new CategoryChartDTO(
                        dto.getLabel(),
                        currencyService.convert(dto.getValue(), "UAH", userCurrency)
                ))
                .toList();

        List<CategoryChartDTO> incomeByCategory = analyticsService.getIncomeByCategory(user.getId(), startDate, endDate).stream()
                .map(dto -> new CategoryChartDTO(
                        dto.getLabel(),
                        currencyService.convert(dto.getValue(), "UAH", userCurrency)
                ))
                .toList();

        List<MonthlySummaryDTO> monthlySummariesRaw = analyticsService.getMonthlySummary(user.getId(), startDate, endDate);

        List<MonthlySummaryDTO> monthlySummary = monthlySummariesRaw.stream()
                .map(dto -> new MonthlySummaryDTO(
                        dto.getMonthLabel(),
                        currencyService.convert(dto.getIncome(), "UAH", userCurrency),
                        currencyService.convert(dto.getExpenses(), "UAH", userCurrency)
                ))
                .toList();

        BigDecimal totalIncome = monthlySummariesRaw.stream()
                .map(MonthlySummaryDTO::getIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = monthlySummariesRaw.stream()
                .map(MonthlySummaryDTO::getExpenses)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        model.addAttribute("userCurrency", userCurrency);
        model.addAttribute("expenseByCategory", expenseByCategory);
        model.addAttribute("incomeByCategory", incomeByCategory);
        model.addAttribute("monthlySummary", monthlySummary);
        model.addAttribute("totalIncome", currencyService.convert(totalIncome, "UAH", userCurrency));
        model.addAttribute("totalExpenses", currencyService.convert(totalExpenses, "UAH", userCurrency));
        model.addAttribute("netBalance", currencyService.convert(netBalance, "UAH", userCurrency));
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);


        return "analytics";
    }
}
