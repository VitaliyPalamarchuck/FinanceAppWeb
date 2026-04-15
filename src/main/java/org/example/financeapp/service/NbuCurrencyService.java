package org.example.financeapp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NbuCurrencyService {

    private final RestClient restClient;

    public NbuCurrencyService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://bank.gov.ua/NBUStatService/v1/statdirectory")
                .build();
    }

    public BigDecimal getRateToUah(String currencyCode) {
        String currency = normalize(currencyCode);

        if ("UAH".equals(currency)) {
            return BigDecimal.ONE;
        }

        try {
            CurrencyRate[] rates = restClient.get()
                    .uri("/exchange?json")
                    .retrieve()
                    .body(CurrencyRate[].class);

            if (rates == null) {
                return fallbackRate(currency);
            }

            Map<String, BigDecimal> map = Arrays.stream(rates)
                    .collect(Collectors.toMap(
                            r -> normalize(r.cc),
                            r -> BigDecimal.valueOf(r.rate),
                            (a, b) -> a
                    ));

            return map.getOrDefault(currency, fallbackRate(currency));
        } catch (RestClientException ex) {
            return fallbackRate(currency);
        }
    }

    private BigDecimal fallbackRate(String currency) {
        return switch (currency) {
            case "USD" -> BigDecimal.valueOf(40);
            case "EUR" -> BigDecimal.valueOf(43);
            default -> BigDecimal.ONE;
        };
    }

    private String normalize(String currency) {
        return currency == null || currency.isBlank() ? "UAH" : currency.toUpperCase();
    }

    public static class CurrencyRate {
        public String cc;
        public double rate;
    }
}
