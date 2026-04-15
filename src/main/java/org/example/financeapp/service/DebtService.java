package org.example.financeapp.service;

import org.example.financeapp.model.Debt;
import org.example.financeapp.model.Transaction;
import org.example.financeapp.repository.DebtRepository;
import org.example.financeapp.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class DebtService {

    private final DebtRepository debtRepository;
    private final TransactionRepository transactionRepository;

    public DebtService(DebtRepository debtRepository, TransactionRepository transactionRepository) {
        this.debtRepository = debtRepository;
        this.transactionRepository = transactionRepository;
    }

    public void settleDebt(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid debt Id:" + debtId));

        if (!debt.isSettled()) {
            debt.setSettled(true);

            // Create a corresponding transaction
            Transaction transaction = new Transaction();
            transaction.setAmount(debt.getAmount());
            transaction.setDate(LocalDate.now());
            transaction.setUser(debt.getUser());
            // The category for debt settlement should probably be a generic one.
            // For now, we'll leave it to be set by the user or a default.
            // transaction.setCategory(...); 

            if (debt.getType() == Debt.DebtType.LENT) {
                // Money you lent is returned to you.
                transaction.setType(Transaction.TransactionType.INCOME);
                transaction.setDescription("Settled debt: " + debt.getPersonName() + " paid you back.");
            } else { // BORROWED
                // You are paying back money you borrowed.
                transaction.setType(Transaction.TransactionType.EXPENSE);
                transaction.setDescription("Settled debt: You paid back " + debt.getPersonName() + ".");
            }
            
            transactionRepository.save(transaction);
            debtRepository.save(debt);
        }
    }
}
