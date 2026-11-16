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

    public List<Share> calculateExact(BigDecimal amount, List<Allocation> allocations) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount expense harus lebih besar dari nol");
        }

        long amountCents;

        try {
            amountCents = amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount expense tidak boleh memiliki pecahan sub-sen", exception);
        }

        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("Allocations wajib diisi untuk split EXACT");
        }

        long sumCents = 0;
        List<Long> valueCentsList = new ArrayList<>();
        for (Allocation allocation : allocations) {
            long valueCents;
            try {
                valueCents = allocation.value().movePointRight(2).longValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Value allocation tidak boleh memiliki pecahan sub-sen", exception);
            }
            if (valueCents <= 0) {
                throw new IllegalArgumentException("Value allocation harus lebih besar dari nol");
            }

            sumCents += valueCents;
            valueCentsList.add(valueCents);
        }

        // Invariant utama
        if (sumCents != amountCents) {
            throw new IllegalArgumentException("Total value allocation (" + BigDecimal.valueOf(sumCents, 2)
                    + ") harus sama dengan amount expense (" + BigDecimal.valueOf(amountCents, 2) + ")");
        }

        // bangun hasil
        List<Share> shares = new ArrayList<>();
        for (int i = 0; i < allocations.size(); i++) {
            shares.add(new Share(allocations.get(i).participant(), BigDecimal.valueOf(valueCentsList.get(i), 2)));
        }

        return shares;
    }

    public List<Share> calculatePercentage(BigDecimal amount, List<Allocation> allocations) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount expense harus lebih besar dari nol");
        }

        long amountCents;

        try {
            amountCents = amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount expense tidak boleh memiliki pecahan sub-sen", exception);
        }

        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("Allocations wajib diisi untuk split PERCENTAGE");
        }

        long sumPctCents = 0;

        List<Long> pctCentsList = new ArrayList<>();

        for (Allocation allocation : allocations) {
            long pctCents;
            try {
                pctCents = allocation.value().movePointRight(2).longValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Value percentage tidak boleh memiliki lebih dari dua desimal",
                        exception);
            }

            if (pctCents <= 0) {
                throw new IllegalArgumentException("Value percentage harus lebih besar dari nol");
            }

            sumPctCents += pctCents;
            pctCentsList.add(pctCents);
        }

        if (sumPctCents != 10000) {
            throw new IllegalArgumentException(
                    "Total percentage (" + BigDecimal.valueOf(sumPctCents, 2) + ") harus sama dengan 100.00");
        }

        List<Share> shares = new ArrayList<>();
        long sumFloorCents = 0;

        for (long pctCents : pctCentsList) {
            sumFloorCents += amountCents * pctCents / 10000;
        }

        long remainder = amountCents - sumFloorCents;
        for (int i = 0; i < allocations.size(); i++) {
            long shareCents = amountCents * pctCentsList.get(i) / 10000 + (i < remainder ? 1 : 0);
            shares.add(new Share(allocations.get(i).participant(), BigDecimal.valueOf(shareCents, 2)));
        }

        return shares;
    }
}
