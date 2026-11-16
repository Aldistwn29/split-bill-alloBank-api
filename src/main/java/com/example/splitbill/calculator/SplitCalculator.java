package com.example.splitbill.calculator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.splitbill.model.Participant;

@Component
public class SplitCalculator {
    public List<Share> calculateEqual(BigDecimal amount, List<Participant> participants) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount expense harus lebih besar dari nol");
        }

        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Beneficiary tidak boleh kosong");
        }

        long totalCents;
        try {
            totalCents = amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount expense tidak boleh memiliki pecahan sub-sen", exception);
        }

        int n = participants.size();
        long base = totalCents / n;
        long remainder = totalCents % n;

        List<Share> shares = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long shareCents = base + (i < remainder ? 1 : 0);
            shares.add(new Share(participants.get(i), BigDecimal.valueOf(shareCents, 2)));
        }

        return shares;
    }
}
