package com.example.splitbill.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.splitbill.model.Expense;
import com.example.splitbill.model.ExpenseSplit;
import com.example.splitbill.model.Participant;

@Component
public class SettlementCalculator {
    public List<Balance> calculateBalances(List<Participant> participants, List<Expense> expenses) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participant tidak boleh kosong");
        }

        if (expenses == null) {
            throw new IllegalArgumentException("Expense tidak boleh null");
        }

        Map<Long, BigDecimal> netByParticipantId = new HashMap<>();
        for (Participant participant : participants) {
            netByParticipantId.put(participant.getId(), BigDecimal.ZERO.setScale(2));
        }

        for (Expense expense : expenses) {
            Long payerId = expense.getPayer().getId();
            BigDecimal payerNet = netByParticipantId.get(payerId);

            if (payerNet == null) {
                throw new IllegalArgumentException("Payer expense bukan anggota group");
            }
            netByParticipantId.put(payerId, payerNet.add(expense.getAmount()));

            for (ExpenseSplit split : expense.getExpenseSplits()) {
                Long beneficiaryId = split.getParticipant().getId();
                BigDecimal beneficiaryNet = netByParticipantId.get(beneficiaryId);

                if (beneficiaryNet == null) {
                    throw new IllegalArgumentException("Beneficiary expense bukan anggota group");
                }

                netByParticipantId.put(beneficiaryId, beneficiaryNet.subtract(split.getShareAmount()));
            }
        }

        List<Balance> balances = new ArrayList<>();

        for (Participant participant : participants) {
            balances.add(new Balance(participant, netByParticipantId.get(participant.getId())));
        }

        return balances;

    }

    public ServiceCharge calculateServiceCharge(BigDecimal totalExpenses, String githubUsername) {
        if (totalExpenses == null || totalExpenses.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total expenses tidak boleh null atau negatif");
        }

        if (githubUsername == null || githubUsername.isBlank()) {
            throw new IllegalArgumentException("Github username tidak boleh kosong");
        }

        int asciiSum = githubUsername.toLowerCase().chars().sum();
        int pct = asciiSum % 10;

        BigDecimal amount = totalExpenses.multiply(BigDecimal.valueOf(pct)).divide(BigDecimal.valueOf(100), 2,
                RoundingMode.HALF_UP);

        return new ServiceCharge(pct, amount);
    }
}
