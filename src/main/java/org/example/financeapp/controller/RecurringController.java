package org.example.financeapp.controller;

import org.example.financeapp.model.Category;
import org.example.financeapp.model.RecurringTask;
import org.example.financeapp.model.Transaction;
import org.example.financeapp.model.User;
import org.example.financeapp.repository.CategoryRepository;
import org.example.financeapp.repository.RecurringTaskRepository;
import org.example.financeapp.repository.UserRepository;
import org.example.financeapp.service.CurrencyService;
import org.example.financeapp.service.RecurringTaskService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/recurring")
public class RecurringController {

    private final RecurringTaskRepository recurringTaskRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CurrencyService currencyService;
    private final RecurringTaskService recurringTaskService;

    public RecurringController(RecurringTaskRepository recurringTaskRepository,
                               CategoryRepository categoryRepository,
                               UserRepository userRepository,
                               CurrencyService currencyService,
                               RecurringTaskService recurringTaskService) {
        this.recurringTaskRepository = recurringTaskRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.currencyService = currencyService;
        this.recurringTaskService = recurringTaskService;
    }

    @GetMapping
    public String listRecurring(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        String userCurrency = user.getCurrency() == null ? "UAH" : user.getCurrency();

        List<RecurringTask> tasks = recurringTaskRepository.findByUserId(user.getId());
        List<Category> categories = categoryRepository.findByUserId(user.getId());

        model.addAttribute("tasks", tasks);
        model.addAttribute("categories", categories);
        model.addAttribute("newTask", new RecurringTask());
        model.addAttribute("userCurrency", userCurrency);
        model.addAttribute("currencyService", currencyService);

        return "recurring";
    }

    @PostMapping
    public String addRecurringTask(@ModelAttribute RecurringTask newTask,
                                   @RequestParam("currency") String currency,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        if (newTask.getCategory() != null && newTask.getCategory().getId() != null) {
            Category category = categoryRepository.findById(newTask.getCategory().getId()).orElseThrow();
            newTask.setCategory(category);
        }

        BigDecimal amountInUah = currencyService.convert(newTask.getAmount(), currency, "UAH");
        newTask.setAmount(amountInUah);
        newTask.setUser(user);

        if (newTask.getStartDate() == null) {
            newTask.setStartDate(LocalDate.now());
        }
        if (newTask.getEndDate() == null) {
            newTask.setEndDate(newTask.getStartDate().plusMonths(1));
        }

        newTask.setNextRunDate(newTask.getStartDate());
        newTask.setActive(true);

        recurringTaskRepository.save(newTask);
        return "redirect:/recurring";
    }

    @GetMapping("/edit/{id}")
    public String editRecurringTaskForm(@PathVariable Long id,
                                        Model model,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        RecurringTask task = recurringTaskRepository.findById(id).orElseThrow();

        if (!task.getUser().getId().equals(user.getId())) {
            return "redirect:/recurring";
        }

        model.addAttribute("task", task);
        model.addAttribute("categories", categoryRepository.findByUserId(user.getId()));
        model.addAttribute("userCurrency", user.getCurrency() == null ? "UAH" : user.getCurrency());

        return "recurring-edit";
    }

    @PostMapping("/update")
    public String updateRecurringTask(@RequestParam("id") Long id,
                                      @RequestParam("title") String title,
                                      @RequestParam("amount") BigDecimal amount,
                                      @RequestParam("currency") String currency,
                                      @RequestParam("type") Transaction.TransactionType type,
                                      @RequestParam("categoryId") Long categoryId,
                                      @RequestParam("startDate") LocalDate startDate,
                                      @RequestParam("endDate") LocalDate endDate,
                                      @RequestParam("intervalValue") int intervalValue,
                                      @RequestParam("intervalUnit") RecurringTask.IntervalUnit intervalUnit,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        RecurringTask task = recurringTaskRepository.findById(id).orElseThrow();

        if (!task.getUser().getId().equals(user.getId())) {
            return "redirect:/recurring";
        }

        Category category = categoryRepository.findById(categoryId).orElseThrow();

        task.setTitle(title);
        task.setAmount(currencyService.convert(amount, currency, "UAH"));
        task.setType(type);
        task.setCategory(category);
        task.setStartDate(startDate);
        task.setEndDate(endDate);
        task.setIntervalValue(intervalValue);
        task.setIntervalUnit(intervalUnit);

        if (task.getNextRunDate() == null || task.getNextRunDate().isBefore(startDate)) {
            task.setNextRunDate(startDate);
        }

        if (task.getNextRunDate().isAfter(endDate)) {
            task.setActive(false);
        }

        recurringTaskRepository.save(task);
        return "redirect:/recurring";
    }

    @PostMapping("/run/{id}")
    public String runNow(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        RecurringTask task = recurringTaskRepository.findById(id).orElseThrow();

        if (task.getUser().getId().equals(user.getId())) {
            recurringTaskService.runNow(id);
        }

        return "redirect:/recurring";
    }

    @PostMapping("/toggle/{id}")
    public String toggleTask(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        RecurringTask task = recurringTaskRepository.findById(id).orElseThrow();

        if (task.getUser().getId().equals(user.getId())) {
            task.setActive(!task.isActive());
            recurringTaskRepository.save(task);
        }

        return "redirect:/recurring";
    }

    @PostMapping("/delete/{id}")
    public String deleteTask(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        RecurringTask task = recurringTaskRepository.findById(id).orElseThrow();

        if (task.getUser().getId().equals(user.getId())) {
            recurringTaskRepository.delete(task);
        }

        return "redirect:/recurring";
    }
}