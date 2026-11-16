package com.example.splitbill.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SplitAllocationRequest(
        @NotNull(message = "Participant id wajib diisi") Long participantId,

        @NotNull(message = "Value wajib diisi") @Positive(message = "Value harus lebih dari nol") BigDecimal value) {
}