package org.example.financeapp.controller;

import org.example.financeapp.model.Category;
import org.example.financeapp.model.Transaction;
import org.example.financeapp.model.User;
import org.example.financeapp.repository.CategoryRepository;
import org.example.financeapp.repository.TransactionRepository;
import org.example.financeapp.repository.UserRepository;
import org.example.financeapp.service.CurrencyService;
import org.example.financeapp.service.FinanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FinanceService financeService;
    private final CurrencyService currencyService;

    public TransactionController(TransactionRepository transactionRepository,
                                 CategoryRepository categoryRepository,
                                 UserRepository userRepository,
                                 FinanceService financeService,
                                 CurrencyService currencyService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.financeService = financeService;
        this.currencyService = currencyService;
    }

    @GetMapping
    public String listTransactions(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        List<Transaction> transactions = transactionRepository.findByUserId(user.getId());
        List<Category> categories = categoryRepository.findByUserId(user.getId());
        String userCurrency = user.getCurrency() == null || user.getCurrency().isBlank()
                ? "UAH"
                : user.getCurrency();

        List<TransactionView> transactionViews = transactions.stream()
                .map(transaction -> new TransactionView(
                        transaction,
                        currencyService.convert(transaction.getAmount(), "UAH", userCurrency)
                ))
                .toList();

        model.addAttribute("transactions", transactionViews);
        model.addAttribute("categories", categories);
        model.addAttribute("newTransaction", new Transaction());
        model.addAttribute("userCurrency", userCurrency);

        return "transactions";
    }

    @PostMapping
    public String addTransaction(@ModelAttribute Transaction transaction,
                                 @RequestParam("currency") String currency,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        BigDecimal amountInUah = currencyService.convert(transaction.getAmount(), currency, "UAH");
        transaction.setAmount(amountInUah);
        transaction.setUser(user);

        if (transaction.getCategory() != null && transaction.getCategory().getId() != null) {
            Category category = categoryRepository.findById(transaction.getCategory().getId()).orElseThrow();
            transaction.setCategory(category);
        }

        transactionRepository.save(transaction);
        return "redirect:/transactions";
    }

    @PostMapping("/check-budget")
    @ResponseBody
    public ResponseEntity<?> checkBudget(@RequestBody Transaction transaction, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        transaction.setUser(user);

        Category category = categoryRepository.findById(transaction.getCategory().getId()).orElse(null);
        if (category == null) {
            return ResponseEntity.badRequest().body("Invalid Category");
        }
        transaction.setCategory(category);

        boolean exceeded = financeService.isBudgetExceeded(transaction);
        return ResponseEntity.ok().body(java.util.Map.of("exceeded", exceeded));
    }

    public record TransactionView(Transaction transaction, BigDecimal convertedAmount) {
    }
}
