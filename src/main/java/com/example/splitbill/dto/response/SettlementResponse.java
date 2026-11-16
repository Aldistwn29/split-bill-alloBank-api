package com.example.splitbill.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SettlementResponse(
        BigDecimal totalExpenses,
        @JsonProperty("service_charge_pct") int serviceChargePct,
        @JsonProperty("service_charge_amount") BigDecimal serviceChargeAmount,
        List<BalanceResponse> balances) {
}
