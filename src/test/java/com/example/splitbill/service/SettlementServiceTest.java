package com.example.splitbill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.splitbill.calculator.Balance;
import com.example.splitbill.calculator.ServiceCharge;
import com.example.splitbill.calculator.SettlementCalculator;
import com.example.splitbill.dto.response.SettlementResponse;
import com.example.splitbill.exception.ResourceNotFoundException;
import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Expense;
import com.example.splitbill.model.ExpenseCategory;
import com.example.splitbill.model.ExpenseSplit;
import com.example.splitbill.model.Participant;
import com.example.splitbill.model.SplitType;
import com.example.splitbill.repository.BillGroupRepository;
import com.example.splitbill.repository.ExpenseRepository;

@ExtendWith(MockitoExtension.class)
public class SettlementServiceTest {

    private static final String USERNAME = "Aldistwn29";

    @Mock
    private BillGroupRepository billGroupRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SettlementCalculator settlementCalculator;

    private SettlementService service() {
        return new SettlementService(billGroupRepository, expenseRepository,
                settlementCalculator, USERNAME);
    }

    private BillGroup groupWithParticipants() {
        BillGroup group = new BillGroup("Trip Sukabumi");
        ReflectionTestUtils.setField(group, "id", 1L);
        long participantId = 1;
        for (String name : List.of("Aldi", "Setiawan", "Damn")) {
            Participant participant = new Participant(name, group);
            ReflectionTestUtils.setField(participant, "id", participantId++);
        }
        return group;
    }

    @Test
    void getSettlement_groupNotFound_throwsNotFound() {
        when(billGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getSettlement(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Group dengan id 99 tidak ditemukan");

        verifyNoInteractions(expenseRepository, settlementCalculator);
    }

    @Test
    void getSettlement_noExpenses_returnsZeroTotals() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(expenseRepository.findAllForSettlement(1L)).thenReturn(List.of());
        when(settlementCalculator.calculateBalances(
                eq(group.getParticipants()),
                eq(List.of())))
                .thenReturn(List.of(
                        new Balance(group.getParticipants().get(0), new BigDecimal("0.00")),
                        new Balance(group.getParticipants().get(1), new BigDecimal("0.00")),
                        new Balance(group.getParticipants().get(2), new BigDecimal("0.00"))));
        when(settlementCalculator.calculateServiceCharge(
                eq(new BigDecimal("0.00")),
                eq(USERNAME)))
                .thenReturn(new ServiceCharge(7, new BigDecimal("0.00")));

        SettlementResponse response = service().getSettlement(1L);

        assertThat(response.totalExpenses()).isEqualByComparingTo("0.00");
        assertThat(response.serviceChargePct()).isEqualTo(7);
        assertThat(response.serviceChargeAmount()).isEqualByComparingTo("0.00");
        assertThat(response.balances()).hasSize(3);
        assertThat(response.balances().get(0).netBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void getSettlement_happyPath_returnsBalancesAndServiceCharge() {
        BillGroup group = groupWithParticipants();
        List<Expense> expenses = List.of(buildExpense(group, new BigDecimal("300000.00")));
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(expenseRepository.findAllForSettlement(1L)).thenReturn(expenses);
        when(settlementCalculator.calculateBalances(
                eq(group.getParticipants()),
                eq(expenses)))
                .thenReturn(List.of(
                        new Balance(group.getParticipants().get(0), new BigDecimal("199666.66")),
                        new Balance(group.getParticipants().get(1), new BigDecimal("-100333.33")),
                        new Balance(group.getParticipants().get(2), new BigDecimal("-333.33"))));
        when(settlementCalculator.calculateServiceCharge(
                eq(new BigDecimal("300000.00")),
                eq(USERNAME)))
                .thenReturn(new ServiceCharge(7, new BigDecimal("21000.00")));

        SettlementResponse response = service().getSettlement(1L);

        assertThat(response.totalExpenses()).isEqualByComparingTo("300000.00");
        assertThat(response.serviceChargePct()).isEqualTo(7);
        assertThat(response.serviceChargeAmount()).isEqualByComparingTo("21000.00");
        assertThat(response.balances()).hasSize(3);
        assertThat(response.balances().get(0).participantName()).isEqualTo("Aldi");
        assertThat(response.balances().get(0).netBalance()).isEqualByComparingTo("199666.66");
    }

    private Expense buildExpense(BillGroup group, BigDecimal amount) {
        Participant payer = group.getParticipants().get(0);
        Expense expense = new Expense(amount, payer, group, "Villa", SplitType.EQUAL, ExpenseCategory.FOOD);
        long totalCents = amount.movePointRight(2).longValueExact();
        long base = totalCents / 3;
        long remainder = totalCents % 3;
        for (int i = 0; i < 3; i++) {
            long cents = base + (i < remainder ? 1 : 0);
            new ExpenseSplit(expense, group.getParticipants().get(i), BigDecimal.valueOf(cents, 2));
        }
        return expense;
    }
}