package com.example.splitbill.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.example.splitbill.model.Expense;
import com.example.splitbill.model.ExpenseCategory;

public record CategorySummaryResponse(ExpenseCategory category, BigDecimal totalAmount, long expenseCount) {

    public static CategorySummaryResponse from(ExpenseCategory category, List<Expense> expenses) {
        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new CategorySummaryResponse(category, totalAmount, expenses.size());
    }
}
