package com.example.splitbill.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateExpenseRequest(
        @NotNull(message = "Payer id wajib diisi")
        Long payerId,

        @NotNull(message = "Amount wajib diisi")
        @Positive(message = "Amount harus lebih dari nol")
        BigDecimal amount,

        @Size(max = 255, message = "Description maksimal 255 karakter")
        String description,

        @NotNull(message = "Split wajib diisi")
        @Valid
        SplitRequest split,

        @NotEmpty(message = "Beneficiary wajib diisi")
        List<@NotNull Long> beneficiaryIds
) {}
