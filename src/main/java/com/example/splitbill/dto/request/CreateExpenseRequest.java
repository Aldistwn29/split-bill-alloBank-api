package com.example.splitbill.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.example.splitbill.model.ExpenseCategory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateExpenseRequest(
        @NotNull(message = "Payer id wajib diisi") Long payerId,

        @NotNull(message = "Amount wajib diisi") @Positive(message = "Amount harus lebih dari nol") BigDecimal amount,

        @Size(max = 255, message = "Description maksimal 255 karakter") String description,

        @NotNull(message = "Split wajib diisi") @Valid SplitRequest split,

        @NotNull(message = "Category wajib diisi") ExpenseCategory category,

        List<@NotNull Long> beneficiaryIds) {
}
