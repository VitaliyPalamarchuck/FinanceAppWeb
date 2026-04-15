package org.example.financeapp.controller;

import org.example.financeapp.model.User;
import org.example.financeapp.repository.UserRepository;
import org.example.financeapp.service.CurrencyService;
import org.example.financeapp.service.FinanceService;
import org.example.financeapp.model.dto.BudgetStatusDTO; // Збережено, якщо використовується
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class DashboardController {

    private final FinanceService financeService;
    private final UserRepository userRepository;
    private final CurrencyService currencyService;

    public DashboardController(FinanceService financeService,
                               UserRepository userRepository,
                               CurrencyService currencyService) {
        this.financeService = financeService;
        this.userRepository = userRepository;
        this.currencyService = currencyService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        String userCurrency = user.getCurrency() == null ? "UAH" : user.getCurrency();

        BigDecimal totalBalanceUah = financeService.calculateTotalBalance(user.getId());
        BigDecimal incomeUah = financeService.calculateTotalIncome(user.getId());
        BigDecimal expensesUah = financeService.calculateTotalExpenses(user.getId());

        BigDecimal totalBalance = currencyService.convert(totalBalanceUah, "UAH", userCurrency);
        BigDecimal income = currencyService.convert(incomeUah, "UAH", userCurrency);
        BigDecimal expenses = currencyService.convert(expensesUah, "UAH", userCurrency);

        List<BudgetStatusDTO> budgetStatus = financeService.getBudgetStatus(user.getId());
        List<BudgetStatusDTO> convertedBudgetStatus = budgetStatus.stream()
                .map(status -> new BudgetStatusDTO(
                        status.getCategoryName(),
                        currencyService.convert(status.getLimit(), "UAH", userCurrency),
                        currencyService.convert(status.getSpent(), "UAH", userCurrency),
                        status.getPercentage(),
                        status.getStatusColor(),
                        status.getMonth(),
                        status.getYear(),
                        status.getPeriodStart(),
                        status.getPeriodEnd()
                ))
                .toList();

        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("income", income);
        model.addAttribute("expenses", expenses);
        model.addAttribute("budgetStatus", convertedBudgetStatus);
        model.addAttribute("userCurrency", userCurrency);

        return "dashboard";
    }
}
