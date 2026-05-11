package org.example.financeapp.controller;

import org.example.financeapp.model.Budget;
import org.example.financeapp.model.Category;
import org.example.financeapp.model.Transaction;
import org.example.financeapp.model.User;
import org.example.financeapp.repository.BudgetRepository;
import org.example.financeapp.repository.CategoryRepository;
import org.example.financeapp.repository.TransactionRepository;
import org.example.financeapp.repository.UserRepository;
import org.example.financeapp.service.CurrencyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class BudgetController {
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CurrencyService currencyService;
    public BudgetController(BudgetRepository budgetRepository,
                            CategoryRepository categoryRepository,
                            TransactionRepository transactionRepository,
                            UserRepository userRepository,
                            CurrencyService currencyService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.currencyService = currencyService;
    }
    @GetMapping("/budgets")
    public String budgets(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        String userCurrency = user.getCurrency() == null ? "UAH" : user.getCurrency();
        List<Category> expenseCategories = categoryRepository.findByUserId(user.getId()).stream()
                .filter(category -> category.getType() == Category.CategoryType.EXPENSE)
                .toList();
        List<Budget> budgets = budgetRepository.findByUserId(user.getId()).stream()
                .filter(budget -> budget.getCategory() != null)
                .filter(budget -> budget.getCategory().getType() == Category.CategoryType.EXPENSE)
                .toList();
        Map<Integer, String> monthOptions = new LinkedHashMap<>();
        for (Month month : Month.values()) {
            monthOptions.put(
                    month.getValue(),
                    month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            );
        }

        model.addAttribute("categories", expenseCategories);
        model.addAttribute("budgets", budgets);
        model.addAttribute("userCurrency", userCurrency);
        model.addAttribute("monthOptions", monthOptions);
        model.addAttribute("currentMonth", LocalDate.now().getMonthValue());
        model.addAttribute("currentYear", LocalDate.now().getYear());
        model.addAttribute("newBudget", new Budget());

        return "budgets";
    }

    @PostMapping("/budgets")
    public String addBudget(@ModelAttribute Budget newBudget,
                            @RequestParam("currency") String currency,
                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        if (newBudget.getCategory() != null && newBudget.getCategory().getId() != null) {
            Category category = categoryRepository.findById(newBudget.getCategory().getId()).orElseThrow();

            if (category.getType() != Category.CategoryType.EXPENSE) {
                return "redirect:/budgets?error=incomeCategoryNotAllowed";
            }

            newBudget.setCategory(category);
        }

        BigDecimal limitInUah = currencyService.convert(newBudget.getLimitAmount(), currency, "UAH");
        newBudget.setLimitAmount(limitInUah);
        newBudget.setYear(LocalDate.now().getYear());
        newBudget.setUser(user);

        budgetRepository.save(newBudget);
        return "redirect:/budgets";
    }

    @GetMapping("/budgets/edit")
    public String editBudgetForm(@RequestParam("id") Long id,
                                 Model model,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        Budget budget = budgetRepository.findById(id).orElseThrow();

        if (!budget.getUser().getId().equals(user.getId())) {
            return "redirect:/budgets";
        }

        if (budget.getCategory() != null && budget.getCategory().getType() != Category.CategoryType.EXPENSE) {
            return "redirect:/budgets";
        }

        List<Category> expenseCategories = categoryRepository.findByUserId(user.getId()).stream()
                .filter(category -> category.getType() == Category.CategoryType.EXPENSE)
                .toList();

        Map<Integer, String> monthOptions = new LinkedHashMap<>();
        for (Month month : Month.values()) {
            monthOptions.put(
                    month.getValue(),
                    month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            );
        }

        model.addAttribute("budget", budget);
        model.addAttribute("categories", expenseCategories);
        model.addAttribute("monthOptions", monthOptions);
        model.addAttribute("userCurrency", user.getCurrency() == null ? "UAH" : user.getCurrency());

        return "budgets-edit";
    }

    @PostMapping("/budgets/update")
    public String updateBudget(@RequestParam("id") Long id,
                               @RequestParam("categoryId") Long categoryId,
                               @RequestParam("month") int month,
                               @RequestParam("year") int year,
                               @RequestParam("limitAmount") BigDecimal limitAmount,
                               @RequestParam("currency") String currency,
                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        Budget budget = budgetRepository.findById(id).orElseThrow();

        if (!budget.getUser().getId().equals(user.getId())) {
            return "redirect:/budgets";
        }

        Category category = categoryRepository.findById(categoryId).orElseThrow();
        if (category.getType() != Category.CategoryType.EXPENSE) {
            return "redirect:/budgets?error=incomeCategoryNotAllowed";
        }

        budget.setCategory(category);
        budget.setMonth(month);
        budget.setYear(year);
        budget.setLimitAmount(currencyService.convert(limitAmount, currency, "UAH"));

        budgetRepository.save(budget);
        return "redirect:/budgets";
    }

    @PostMapping("/budgets/delete")
    public String deleteBudget(@RequestParam("id") Long id,
                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        Budget budget = budgetRepository.findById(id).orElseThrow();

        if (budget.getUser().getId().equals(user.getId())) {
            budgetRepository.delete(budget);
        }

        return "redirect:/budgets";
    }

    public BigDecimal getSpentForBudget(Budget budget) {
        List<Transaction> transactions = transactionRepository.findByUserId(budget.getUser().getId());

        return transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> t.getCategory().getId().equals(budget.getCategory().getId()))
                .filter(t -> t.getDate().getMonthValue() == budget.getMonth())
                .filter(t -> t.getDate().getYear() == budget.getYear())
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRemainingForBudget(Budget budget) {
        return budget.getLimitAmount().subtract(getSpentForBudget(budget));
    }
}
