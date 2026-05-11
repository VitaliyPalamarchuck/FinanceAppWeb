package org.example.financeapp.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CurrencyService {
    private final NbuCurrencyService nbuCurrencyService;
    public CurrencyService(NbuCurrencyService nbuCurrencyService) {
        this.nbuCurrencyService = nbuCurrencyService;
    }
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        String from = normalize(fromCurrency);
        String to = normalize(toCurrency);
        if (from.equals(to)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal amountInUah = toUah(amount, from);
        return fromUah(amountInUah, to);
    }

    public BigDecimal convert(double amount, String fromCurrency, String toCurrency) {
        return convert(BigDecimal.valueOf(amount), fromCurrency, toCurrency);
    }

    private BigDecimal toUah(BigDecimal amount, String fromCurrency) {
        BigDecimal rate = nbuCurrencyService.getRateToUah(fromCurrency);
        return amount.multiply(rate);
    }

    private BigDecimal fromUah(BigDecimal amountUah, String toCurrency) {
        BigDecimal rate = nbuCurrencyService.getRateToUah(toCurrency);
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return amountUah.setScale(2, RoundingMode.HALF_UP);
        }
        return amountUah.divide(rate, 2, RoundingMode.HALF_UP);
    }

    private String normalize(String currency) {
        return currency == null || currency.isBlank() ? "UAH" : currency.toUpperCase();
    }
}
