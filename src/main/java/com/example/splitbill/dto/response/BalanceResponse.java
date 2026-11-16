package com.example.splitbill.dto.response;

import java.math.BigDecimal;
import com.example.splitbill.calculator.Balance;

public record BalanceResponse(
        Long participantId,
        String participantName,
        BigDecimal netBalance) {

    public static BalanceResponse from(Balance balance) {
        return new BalanceResponse(
                balance.participant().getId(),
                balance.participant().getName(),
                balance.netBalance());
    }
}
