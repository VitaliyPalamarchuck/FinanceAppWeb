package org.example.financeapp.controller;

import org.example.financeapp.model.Debt;
import org.example.financeapp.model.User;
import org.example.financeapp.repository.DebtRepository;
import org.example.financeapp.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/debts")
public class DebtController {

    private final DebtRepository debtRepository;
    private final UserRepository userRepository;

    public DebtController(DebtRepository debtRepository, UserRepository userRepository) {
        this.debtRepository = debtRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listDebts(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        List<Debt> allDebts = debtRepository.findByUserId(user.getId());

        List<Debt> lentDebts = allDebts.stream()
                .filter(d -> d.getType() == Debt.DebtType.LENT && !d.isSettled())
                .collect(Collectors.toList());

        List<Debt> borrowedDebts = allDebts.stream()
                .filter(d -> d.getType() == Debt.DebtType.BORROWED && !d.isSettled())
                .collect(Collectors.toList());

        model.addAttribute("lentDebts", lentDebts);
        model.addAttribute("borrowedDebts", borrowedDebts);
        model.addAttribute("newDebt", new Debt());

        return "debts";
    }

    @PostMapping
    public String addDebt(@ModelAttribute Debt newDebt, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        newDebt.setUser(user);
        newDebt.setPaidAmount(BigDecimal.ZERO);
        newDebt.setSettled(false);
        debtRepository.save(newDebt);
        return "redirect:/debts";
    }

    @PostMapping("/settle/{id}")
    public String settleDebt(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        Debt debt = debtRepository.findById(id).orElseThrow();

        if (debt.getUser().getId().equals(user.getId())) {
            debt.setPaidAmount(debt.getAmount());
            debt.setSettled(true);
            debtRepository.save(debt);
        }

        return "redirect:/debts";
    }

    @PostMapping("/pay/{id}")
    public String payPartialDebt(@PathVariable Long id,
                                 @RequestParam("paymentAmount") BigDecimal paymentAmount,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        Debt debt = debtRepository.findById(id).orElseThrow();

        if (!debt.getUser().getId().equals(user.getId())) {
            return "redirect:/debts";
        }

        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "redirect:/debts";
        }

        BigDecimal newPaidAmount = debt.getPaidAmount().add(paymentAmount);

        if (newPaidAmount.compareTo(debt.getAmount()) >= 0) {
            debt.setPaidAmount(debt.getAmount());
            debt.setSettled(true);
        } else {
            debt.setPaidAmount(newPaidAmount);
            debt.setSettled(false);
        }

        debtRepository.save(debt);
        return "redirect:/debts";
    }

    public BigDecimal getRemainingDebt(Debt debt) {
        return debt.getAmount().subtract(debt.getPaidAmount()).max(BigDecimal.ZERO);
    }

    public int getProgressPercent(Debt debt) {
        if (debt.getAmount() == null || debt.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return debt.getPaidAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(debt.getAmount(), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}