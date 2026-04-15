package org.example.financeapp.service;

import org.example.financeapp.model.Category;
import org.example.financeapp.model.RecurringTask;
import org.example.financeapp.model.Transaction;
import org.example.financeapp.repository.RecurringTaskRepository;
import org.example.financeapp.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringTaskService {

    private final RecurringTaskRepository recurringTaskRepository;
    private final TransactionRepository transactionRepository;
    private final CurrencyService currencyService;

    public RecurringTaskService(RecurringTaskRepository recurringTaskRepository,
                                TransactionRepository transactionRepository,
                                CurrencyService currencyService) {
        this.recurringTaskRepository = recurringTaskRepository;
        this.transactionRepository = transactionRepository;
        this.currencyService = currencyService;
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void processRecurringTasks() {
        LocalDate today = LocalDate.now();
        List<RecurringTask> dueTasks = recurringTaskRepository.findByActiveTrueAndNextRunDateLessThanEqual(today);

        for (RecurringTask task : dueTasks) {
            executeTask(task, today);
        }
    }

    @Transactional
    public void runNow(Long taskId) {
        RecurringTask task = recurringTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("Recurring task not found"));

        if (!task.isActive()) {
            return;
        }

        executeTask(task, LocalDate.now());
    }

    @Transactional
    public void executeTask(RecurringTask task, LocalDate executionDate) {
        Transaction transaction = new Transaction();
        transaction.setDescription(task.getTitle());
        transaction.setType(task.getType());
        transaction.setDate(executionDate);
        transaction.setUser(task.getUser());
        transaction.setCategory(task.getCategory());
        transaction.setAmount(currencyService.convert(task.getAmount(), task.getCurrency(), "UAH"));

        transactionRepository.save(transaction);

        LocalDate nextDate = calculateNextDate(
                task.getNextRunDate() != null ? task.getNextRunDate() : task.getStartDate(),
                task
        );

        task.setNextRunDate(nextDate);

        if (nextDate.isAfter(task.getEndDate())) {
            task.setActive(false);
        }

        recurringTaskRepository.save(task);
    }

    private LocalDate calculateNextDate(LocalDate baseDate, RecurringTask task) {
        return switch (task.getIntervalUnit()) {
            case DAYS -> baseDate.plusDays(task.getIntervalValue());
            case WEEKS -> baseDate.plusWeeks(task.getIntervalValue());
            case MONTHS -> baseDate.plusMonths(task.getIntervalValue());
            case YEARS -> baseDate.plusYears(task.getIntervalValue());
        };
    }
}
