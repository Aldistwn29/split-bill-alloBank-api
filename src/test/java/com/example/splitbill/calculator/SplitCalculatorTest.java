package com.example.splitbill.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Expense;
import com.example.splitbill.model.ExpenseCategory;
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
    void calculateEqual_dividesAmountEvenly() {
        List<Share> shares = calculator.calculateEqual(
                new BigDecimal("90.00"), participants("Aldi", "Sujuran", "Sudahan"));

        assertEquals(3, shares.size());
        assertEquals(new BigDecimal("30.00"), shares.get(0).shareAmount());
        assertEquals(new BigDecimal("30.00"), shares.get(1).shareAmount());
        assertEquals(new BigDecimal("30.00"), shares.get(2).shareAmount());
    }

    @Test
    void calculateEqual_spreadsRemainderToFirstParticipants() {
        List<Share> shares = calculator.calculateEqual(
                new BigDecimal("100.00"), participants("Aldi", "Sujuran", "Sudahan"));

        // Sisa 1 sen -> participants pertama dapat base + 1
        assertEquals(new BigDecimal("33.34"), shares.get(0).shareAmount());
        assertEquals(new BigDecimal("33.33"), shares.get(1).shareAmount());
        assertEquals(new BigDecimal("33.33"), shares.get(2).shareAmount());
    }

    @Test
    void calculateEqual_sumOfSharesAlwaysEqualsAmount() {
        BigDecimal amount = new BigDecimal("12345.67");
        List<Share> shares = calculator.calculateEqual(
                amount, participants("A", "B", "C", "D", "E", "F", "G"));

        BigDecimal sum = shares.stream()
                .map(Share::shareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(amount));
    }

    @Test
    void calculateEqual_rejectsNullZeroAndNegativeAmount() {
        List<Participant> two = participants("Aldi", "Sujuran");
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateEqual(null, two));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateEqual(BigDecimal.ZERO, two));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateEqual(new BigDecimal("-1.00"), two));
    }

    @Test
    void calculateEqual_rejectsEmptyAndNullParticipants() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateEqual(new BigDecimal("100.00"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateEqual(new BigDecimal("10.00"), null));
    }

    @Test
    void calculateEqual_rejectsDuplicateParticipantSplit() {
        BillGroup group = new BillGroup("Trip");
        Participant aldi = new Participant("Aldi", group);
        Expense expense = new Expense(new BigDecimal("100.00"), aldi, group, "Makan siang", SplitType.EQUAL,
                ExpenseCategory.FOOD);

        new ExpenseSplit(expense, aldi, new BigDecimal("100.00"));
        assertThrows(IllegalArgumentException.class, () -> new ExpenseSplit(expense, aldi, new BigDecimal("50.00")));
        assertEquals(1, expense.getExpenseSplits().size());
    }

    @Test
    void calculateEqual_rejectsSubCentAmount() {
        List<Participant> two = participants("Aldi", "Sujuran");
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateEqual(new BigDecimal("100.005"), two));
    }

    @Test
    void calculateEqual_deterministicFromSameInput() {
        BigDecimal amount = new BigDecimal("100.000");
        List<Participant> two = participants("Aldi", "Sujuran");

        List<Share> first = calculator.calculateEqual(amount, two);
        List<Share> second = calculator.calculateEqual(amount, two);

        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).shareAmount(), second.get(i).shareAmount());
        }
    }

    @Test
    void calculateExact_sharesMatchExplicitAllocations() {
        // Arrange
        List<Participant> two = participants("Aldi", "Sujuran");
        List<Allocation> allocations = List.of(
                new Allocation(two.get(0), new BigDecimal("60.00")),
                new Allocation(two.get(1), new BigDecimal("40.00")));

        // Act
        List<Share> shares = calculator.calculateExact(new BigDecimal("100.00"), allocations);

        // Assert — nilai per share, urutan input, dan invariant Σ == amount
        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).participant()).isEqualTo(two.get(0));
        assertThat(shares.get(0).shareAmount()).isEqualByComparingTo("60.00");
        assertThat(shares.get(1).participant()).isEqualTo(two.get(1));
        assertThat(shares.get(1).shareAmount()).isEqualByComparingTo("40.00");
        assertThat(shares.stream().map(Share::shareAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void calculateExact_rejectsSumMismatch() {
        List<Participant> two = participants("Aldi", "Sujuran");
        List<Allocation> allocations = List.of(
                new Allocation(two.get(0), new BigDecimal("50.00")),
                new Allocation(two.get(1), new BigDecimal("40.00")));

        assertThatThrownBy(() -> calculator.calculateExact(new BigDecimal("100.00"), allocations))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total value allocation (90.00) harus sama dengan amount expense (100.00)");
    }

    @Test
    void calculateExact_rejectsSubCentValue() {
        List<Participant> two = participants("Aldi", "Sujuran");
        List<Allocation> allocations = List.of(
                new Allocation(two.get(0), new BigDecimal("33.333")),
                new Allocation(two.get(1), new BigDecimal("66.667")));

        assertThatThrownBy(() -> calculator.calculateExact(new BigDecimal("100.00"), allocations))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value allocation tidak boleh memiliki pecahan sub-sen");
    }

    @Test
    void calculatePercentage_happyPath_spreadsRemainder() {
        List<Participant> three = participants("Aldi", "Sujuran", "Sudahan");
        List<Allocation> allocations = List.of(
                new Allocation(three.get(0), new BigDecimal("33.33")),
                new Allocation(three.get(1), new BigDecimal("33.33")),
                new Allocation(three.get(2), new BigDecimal("33.34")));

        List<Share> shares = calculator.calculatePercentage(new BigDecimal("10.00"), allocations);

        // floor 3.33 x3, remainder 1 sen -> participant pertama urutan input
        assertThat(shares.get(0).shareAmount()).isEqualByComparingTo("3.34");
        assertThat(shares.get(1).shareAmount()).isEqualByComparingTo("3.33");
        assertThat(shares.get(2).shareAmount()).isEqualByComparingTo("3.33");
        assertThat(shares.stream().map(Share::shareAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("10.00");
    }

    @Test
    void calculatePercentage_rejectsSumPctMismatch() {
        List<Participant> two = participants("Aldi", "Sujuran");
        List<Allocation> allocations = List.of(
                new Allocation(two.get(0), new BigDecimal("50.00")),
                new Allocation(two.get(1), new BigDecimal("40.00")));

        assertThatThrownBy(() -> calculator.calculatePercentage(new BigDecimal("100.00"), allocations))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total percentage (90.00) harus sama dengan 100.00");
    }

    @Test
    void calculatePercentage_rejectsSubCentPctValue() {
        List<Participant> two = participants("Aldi", "Sujuran");
        List<Allocation> allocations = List.of(
                new Allocation(two.get(0), new BigDecimal("33.333")),
                new Allocation(two.get(1), new BigDecimal("66.667")));

        assertThatThrownBy(() -> calculator.calculatePercentage(new BigDecimal("100.00"), allocations))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value percentage tidak boleh memiliki lebih dari dua desimal");
    }
}
