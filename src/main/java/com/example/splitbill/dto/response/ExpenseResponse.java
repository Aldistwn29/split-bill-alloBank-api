package com.example.splitbill.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.splitbill.model.Expense;
import com.example.splitbill.model.SplitType;

public record ExpenseResponse(
        Long id,
        BigDecimal amount,
        String description,
        ParticipantResponse payer,
        List<ParticipantResponse> beneficiaries,
        SplitType splitType,
        LocalDate createdAt) {
    public static ExpenseResponse from(Expense expense) {
        List<ParticipantResponse> beneficiaries = expense.getExpenseSplits().stream()
                .map(split -> ParticipantResponse.from(split.getParticipant()))
                .toList();
        return new ExpenseResponse(expense.getId(), expense.getAmount(),
                expense.getDescription(),
                ParticipantResponse.from(expense.getPayer()),
                beneficiaries,
                expense.getSplitType(),
                expense.getCreatedAt());
    }
}
