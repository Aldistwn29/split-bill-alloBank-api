package com.example.splitbill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.splitbill.calculator.Allocation;
import com.example.splitbill.calculator.Share;
import com.example.splitbill.calculator.SplitCalculator;
import com.example.splitbill.dto.request.CreateExpenseRequest;
import com.example.splitbill.dto.request.SplitAllocationRequest;
import com.example.splitbill.dto.request.SplitRequest;
import com.example.splitbill.dto.response.CategorySummaryResponse;
import com.example.splitbill.dto.response.ExpenseResponse;
import com.example.splitbill.exception.ResourceNotFoundException;
import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Expense;
import com.example.splitbill.model.ExpenseCategory;
import com.example.splitbill.model.Participant;
import com.example.splitbill.model.SplitType;
import com.example.splitbill.repository.BillGroupRepository;
import com.example.splitbill.repository.ExpenseRepository;

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {
    private static final BigDecimal AMOUNT = new BigDecimal("300000.00");

    @Mock
    private BillGroupRepository billGroupRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SplitCalculator splitCalculator;

    private ExpenseService service() {
        return new ExpenseService(expenseRepository, billGroupRepository, splitCalculator);
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

    private CreateExpenseRequest request(Long payerId, List<Long> beneficiaryIds) {
        return new CreateExpenseRequest(payerId, AMOUNT, "Villa",
                new SplitRequest(SplitType.EQUAL, null), ExpenseCategory.ACCOMMODATION, beneficiaryIds);
    }

    @Test
    void createExpense_happyPath() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(splitCalculator.calculateEqual(eq(AMOUNT), anyList()))
                .thenAnswer(inovaction -> {
                    List<Participant> participants = inovaction.getArgument(1);
                    return List.of(
                            new Share(participants.get(0), new BigDecimal("100000.00")),
                            new Share(participants.get(1), new BigDecimal("100000.00")),
                            new Share(participants.get(2), new BigDecimal("100000.00")));
                });
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inovation -> {
            Expense expense = inovation.getArgument(0);
            ReflectionTestUtils.setField(expense, "id", 3L);
            return expense;
        });

        ExpenseResponse response = service().createExpense(1L, request(3L, List.of(1L, 2L, 3L)));

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.payer().name()).isEqualTo("Damn");
        assertThat(response.splitType()).isEqualTo(SplitType.EQUAL);
        assertThat(response.beneficiaries()).hasSize(3);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().getExpenseSplits()).hasSize(3);
        assertThat(captor.getValue().getPayer().getName()).isEqualTo("Damn");

    }

    @Test
    void createExpense_percentage_happyPath() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(splitCalculator.calculatePercentage(eq(AMOUNT), anyList())).thenAnswer(invocation -> {
            List<Allocation> allocations = invocation.getArgument(1);
            return List.of(
                    new Share(allocations.get(0).participant(), new BigDecimal("150000.00")),
                    new Share(allocations.get(1).participant(), new BigDecimal("150000.00")));
        });
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SplitRequest splitRequest = new SplitRequest(SplitType.PERCENTAGE, List.of(
                new SplitAllocationRequest(1L, new BigDecimal("50.00")),
                new SplitAllocationRequest(2L, new BigDecimal("50.00"))));
        CreateExpenseRequest request = new CreateExpenseRequest(3L, AMOUNT, "Villa",
                splitRequest, ExpenseCategory.FOOD, null);

        ExpenseResponse response = service().createExpense(1L, request);

        assertThat(response.splitType()).isEqualTo(SplitType.PERCENTAGE);
        assertThat(response.beneficiaries()).hasSize(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Allocation>> allocationCaptor = ArgumentCaptor.forClass(List.class);
        verify(splitCalculator).calculatePercentage(eq(AMOUNT), allocationCaptor.capture());
        assertThat(allocationCaptor.getValue())
                .extracting(allocation -> allocation.participant().getId())
                .containsExactly(1L, 2L);
    }

    @Test
    void createExpense_unknownGroup() {
        when(billGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().createExpense(99L, request(1L, List.of(1L, 2L, 3L))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Group dengan id 99 tidak ditemukan");

        verifyNoInteractions(expenseRepository, splitCalculator);
    }

    @Test
    void createExpense_subCentAmount() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service().createExpense(1L,
                new CreateExpenseRequest(1L, new BigDecimal("100.005"), "Villa",
                        new SplitRequest(SplitType.EQUAL, null), ExpenseCategory.FOOD, List.of(1L, 2L, 3L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount expense tidak boleh memiliki pecahan sub-sen");

        verify(expenseRepository, never()).save(any());
    }

    private Expense expense(BillGroup group, String amount, ExpenseCategory category) {
        Participant payer = new Participant("Aldi", group);
        return new Expense(new BigDecimal(amount), payer, group, "Makan", SplitType.EQUAL, category);
    }

    @Test
    void getCategorySummary_groupsAndSortsByCategory() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(expenseRepository.findByBillGroupId(1L)).thenReturn(List.of(
                expense(group, "300000.00", ExpenseCategory.FOOD),
                expense(group, "100000.00", ExpenseCategory.TRANSPORT),
                expense(group, "50000.00", ExpenseCategory.FOOD),
                expense(group, "25000.00", ExpenseCategory.ACCOMMODATION)));

        List<CategorySummaryResponse> summary = service().getCategorySummary(1L);

        // urut alfabetis by category name: ACCOMMODATION, FOOD, TRANSPORT
        assertThat(summary).hasSize(3);
        assertThat(summary.get(0).category()).isEqualTo(ExpenseCategory.ACCOMMODATION);
        assertThat(summary.get(0).totalAmount()).isEqualByComparingTo("25000.00");
        assertThat(summary.get(0).expenseCount()).isEqualTo(1);
        assertThat(summary.get(1).category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(summary.get(1).totalAmount()).isEqualByComparingTo("350000.00");
        assertThat(summary.get(1).expenseCount()).isEqualTo(2);
        assertThat(summary.get(2).category()).isEqualTo(ExpenseCategory.TRANSPORT);
        assertThat(summary.get(2).totalAmount()).isEqualByComparingTo("100000.00");
        assertThat(summary.get(2).expenseCount()).isEqualTo(1);
    }
}