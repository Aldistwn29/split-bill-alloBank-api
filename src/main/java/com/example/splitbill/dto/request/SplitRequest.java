package com.example.splitbill.dto.request;

import java.util.List;

import com.example.splitbill.model.SplitType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SplitRequest(
        @NotNull(message = "Split wajib diisi") SplitType type,
        List<@Valid SplitAllocationRequest> allocations) {
}
