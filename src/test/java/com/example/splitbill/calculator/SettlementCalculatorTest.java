package com.example.splitbill.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Expense;
import com.example.splitbill.model.ExpenseSplit;
import com.example.splitbill.model.Participant;
import com.example.splitbill.model.SplitType;

class SettlementCalculatorTest {

    private static final String USERNAME = "Aldistwn29";

    private final SettlementCalculator calculator = new SettlementCalculator();

    @Test
    void calculateBalances_sumsOfNetBalancesAlwaysZero() {
        BillGroup group = groupWithParticipants();
        Expense expense = expenseEqual(group, new BigDecimal("300000.00"));

        List<Balance> balances = calculator.calculateBalances(group.getParticipants(), List.of(expense));

        BigDecimal sum = balances.stream()
                .map(Balance::netBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateBalances_payerPositiveBeneficiariesNegative() {
        BillGroup group = groupWithParticipants();
        Expense expense = expenseEqual(group, new BigDecimal("300000.00"));

        List<Balance> balances = calculator.calculateBalances(group.getParticipants(), List.of(expense));

        assertThat(balances.get(0).netBalance()).isEqualByComparingTo("200000.00");
        assertThat(balances.get(1).netBalance()).isEqualByComparingTo("-100000.00");
        assertThat(balances.get(2).netBalance()).isEqualByComparingTo("-100000.00");
    }

    @Test
    void calculateBalances_remainderSpreadToFirstParticipants() {
        BillGroup group = groupWithParticipants();
        Expense expense = expenseEqual(group, new BigDecimal("100.00"));

        List<Balance> balances = calculator.calculateBalances(group.getParticipants(), List.of(expense));

        assertThat(balances.get(0).netBalance()).isEqualByComparingTo("66.66");
        assertThat(balances.get(1).netBalance()).isEqualByComparingTo("-33.33");
        assertThat(balances.get(2).netBalance()).isEqualByComparingTo("-33.33");
    }

    @Test
    void calculateBalances_multipleExpensesAccumulate() {
        BillGroup group = groupWithParticipants();
        Expense first = expenseEqual(group, new BigDecimal("300000.00"));
        Expense second = expenseEqual(group, new BigDecimal("100.00"));

        List<Balance> balances = calculator.calculateBalances(group.getParticipants(), List.of(first, second));

        assertThat(balances.get(0).netBalance()).isEqualByComparingTo("200066.66");
        assertThat(balances.get(1).netBalance()).isEqualByComparingTo("-100033.33");
        assertThat(balances.get(2).netBalance()).isEqualByComparingTo("-100033.33");
    }

    @Test
    void calculateBalances_expenseWithNoSplitsKeepsOnlyPayerBalance() {
        BillGroup group = groupWithParticipants();
        Participant payer = group.getParticipants().get(0);
        Expense expense = new Expense(new BigDecimal("50000.00"), payer, group, "Tanpa split", SplitType.EQUAL);

        List<Balance> balances = calculator.calculateBalances(group.getParticipants(), List.of(expense));

        assertThat(balances.get(0).netBalance()).isEqualByComparingTo("50000.00");
        assertThat(balances.get(1).netBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateBalances_emptyExpensesReturnsZeroBalances() {
        BillGroup group = groupWithParticipants();

        List<Balance> balances = calculator.calculateBalances(group.getParticipants(), List.of());

        assertThat(balances).hasSize(3);
        assertThat(balances.get(0).netBalance()).isEqualByComparingTo("0.00");
        assertThat(balances.get(1).netBalance()).isEqualByComparingTo("0.00");
        assertThat(balances.get(2).netBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateBalances_emptyParticipantsThrows() {
        assertThatThrownBy(() -> calculator.calculateBalances(List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Participant tidak boleh kosong");
    }

    @Test
    void calculateServiceCharge_returnsPctAndAmount() {
        ServiceCharge charge = calculator.calculateServiceCharge(new BigDecimal("300000.00"), USERNAME);

        assertThat(charge.pct()).isEqualTo(7);
        assertThat(charge.amount()).isEqualByComparingTo("21000.00");
    }

    @Test
    void calculateServiceCharge_negativeTotalThrows() {
        assertThatThrownBy(() -> calculator.calculateServiceCharge(new BigDecimal("-1"), USERNAME))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private BillGroup groupWithParticipants() {
        BillGroup group = new BillGroup("Trip Sukabumi");
        long participantId = 1;
        for (String name : List.of("Aldi", "Setiawan", "Damn")) {
            Participant participant = new Participant(name, group);
            ReflectionTestUtils.setField(participant, "id", participantId++);
        }
        return group;
    }

    private Expense expenseEqual(BillGroup group, BigDecimal amount) {
        Participant payer = group.getParticipants().get(0);
        Expense expense = new Expense(amount, payer, group, "Villa", SplitType.EQUAL);
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
