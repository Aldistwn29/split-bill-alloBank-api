package com.example.splitbill.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Expense;
import com.example.splitbill.model.ExpenseSplit;
import com.example.splitbill.model.Participant;
import com.example.splitbill.model.SplitType;

public class SplitCalculatorTest {
    private final SplitCalculator calculator = new SplitCalculator();

    private List<Participant> participants(String... names) {
        BillGroup group = new BillGroup("Trip");
        return Arrays.stream(names)
                .map(name -> new Participant(name, group))
                .toList();
    }

    @Test
    void divideExactlyWhenAmountDivisible() {
        List<Share> shares = calculator.calculateEqual(
                new BigDecimal("90.00"), participants("Aldi", "Sujuran", "Sudahan"));

        assertEquals(3, shares.size());
        assertEquals(new BigDecimal("30.00"), shares.get(0).shareAmount());
        assertEquals(new BigDecimal("30.00"), shares.get(1).shareAmount());
        assertEquals(new BigDecimal("30.00"), shares.get(2).shareAmount());
    }

    @Test
    void spreadsRemainderToFirstParticipants() {
        List<Share> shares = calculator.calculateEqual(
                new BigDecimal("100.00"), participants("Aldi", "Sujuran", "Sudahan"));

        // Sisa 1 sen -> participants pertama dapat base + 1
        assertEquals(new BigDecimal("33.34"), shares.get(0).shareAmount());
        assertEquals(new BigDecimal("33.33"), shares.get(1).shareAmount());
        assertEquals(new BigDecimal("33.33"), shares.get(2).shareAmount());
    }

    @Test
    void sumOfSharesAlwaysEqualsAmount() {
        BigDecimal amount = new BigDecimal("12345.67");
        List<Share> shares = calculator.calculateEqual(
                amount, participants("A", "B", "C", "D", "E", "F", "G"));

        BigDecimal sum = shares.stream()
                .map(Share::shareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(amount));
    }

    @Test
    void rejectNullZeroAndNegativeAmount() {
        List<Participant> two = participants("Aldi", "Sujuran");
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateEqual(null, two));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateEqual(BigDecimal.ZERO, two));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateEqual(new BigDecimal("-1.00"), two));
    }

    @Test
    void rejectEmptyAndNullParticipants() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateEqual(new BigDecimal("100.00"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateEqual(new BigDecimal("10.00"), null));
    }

    @Test
    void rejectDuplicateParticipantSplit() {
        BillGroup group = new BillGroup("Trip");
        Participant aldi = new Participant("Aldi", group);
        Expense expense = new Expense(new BigDecimal("100.00"), aldi, group, "Makan siang", SplitType.EQUAL);

        new ExpenseSplit(expense, aldi, new BigDecimal("100.00"));
        assertThrows(IllegalArgumentException.class, () -> new ExpenseSplit(expense, aldi, new BigDecimal("50.00")));
        assertEquals(1, expense.getExpenseSplits().size());
    }

    @Test
    void rejectSubCentAmount() {
        List<Participant> two = participants("Aldi", "Sujuran");
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateEqual(new BigDecimal("100.005"), two));
    }

    @Test
    void deterministicFromSameInput() {
        BigDecimal amount = new BigDecimal("100.000");
        List<Participant> two = participants("Aldi", "Sujuran");

        List<Share> first = calculator.calculateEqual(amount, two);
        List<Share> second = calculator.calculateEqual(amount, two);

        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).shareAmount(), second.get(i).shareAmount());
        }
    }

}
