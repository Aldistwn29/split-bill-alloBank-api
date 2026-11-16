package com.example.splitbill.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.splitbill.calculator.Balance;
import com.example.splitbill.calculator.ServiceCharge;
import com.example.splitbill.calculator.SettlementCalculator;
import com.example.splitbill.dto.response.BalanceResponse;
import com.example.splitbill.dto.response.SettlementResponse;
import com.example.splitbill.exception.ResourceNotFoundException;
import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Expense;
import com.example.splitbill.repository.BillGroupRepository;
import com.example.splitbill.repository.ExpenseRepository;

@Service
public class SettlementService {

    private final BillGroupRepository billGroupRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementCalculator settlementCalculator;
    private final String githubUsername;

    public SettlementService(
            BillGroupRepository billGroupRepository,
            ExpenseRepository expenseRepository,
            SettlementCalculator settlementCalculator,
            @Value("${app.github-username}") String githubUsername) {
        this.billGroupRepository = billGroupRepository;
        this.expenseRepository = expenseRepository;
        this.settlementCalculator = settlementCalculator;
        this.githubUsername = githubUsername;
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long groupId) {
        BillGroup group = billGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group dengan id " + groupId + " tidak ditemukan"));

        List<Expense> expenses = expenseRepository.findAllForSettlement(groupId);
        List<Balance> balances = settlementCalculator.calculateBalances(group.getParticipants(), expenses);
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        ServiceCharge serviceCharge = settlementCalculator.calculateServiceCharge(totalExpenses, githubUsername);

        return new SettlementResponse(
                totalExpenses,
                serviceCharge.pct(),
                serviceCharge.amount(),
                balances.stream().map(BalanceResponse::from).toList());
    }
}
