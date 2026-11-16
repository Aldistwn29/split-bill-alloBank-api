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

import com.example.splitbill.calculator.Share;
import com.example.splitbill.calculator.SplitCalculator;
import com.example.splitbill.dto.request.CreateExpenseRequest;
import com.example.splitbill.dto.request.SplitRequest;
import com.example.splitbill.dto.response.ExpenseResponse;
import com.example.splitbill.exception.BusinessException;
import com.example.splitbill.exception.ResourceNotFoundException;
import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Expense;
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
                new SplitRequest(SplitType.EQUAL, null), beneficiaryIds);
    }

    @Test
    void createExpense_happyPath_persistsExpenseWithSplits() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(splitCalculator.calculateEqual(eq(AMOUNT), anyList())).thenAnswer(invocation -> {
            List<Participant> participants = invocation.getArgument(1);
            return List.of(
                    new Share(participants.get(0), new BigDecimal("100000.00")),
                    new Share(participants.get(1), new BigDecimal("100000.00")),
                    new Share(participants.get(2), new BigDecimal("100000.00")));
        });
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
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
    void createExpense_unknownGroup_throwsResourceNotFoundException() {
        when(billGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().createExpense(99L, request(1L, List.of(1L, 2L, 3L))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Group dengan id 99 tidak ditemukan");

        verifyNoInteractions(expenseRepository, splitCalculator);
    }

    @Test
    void createExpense_payerNotMember_throwsBusinessException() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service().createExpense(1L, request(999L, List.of(1L, 2L, 3L))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Payer dengan id 999 bukan anggota group");

        verifyNoInteractions(expenseRepository, splitCalculator);
    }

    @Test
    void createExpense_beneficiaryNotMember_throwsBusinessException() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service().createExpense(1L, request(1L, List.of(1L, 2L, 999L))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Beneficiary dengan id 999 bukan anggota group");

        verifyNoInteractions(expenseRepository);
    }

    @Test
    void createExpense_duplicateBeneficiary_throwsBusinessException() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service().createExpense(1L, request(1L, List.of(1L, 2L, 1L))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Beneficiary tidak boleh memiliki duplikat");

        verifyNoInteractions(splitCalculator, expenseRepository);
    }

    @Test
    void createExpense_subCentAmount_throwsIllegalArgumentException() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service().createExpense(1L,
                new CreateExpenseRequest(1L, new BigDecimal("100.005"), "Villa",
                        new SplitRequest(SplitType.EQUAL, null), List.of(1L, 2L, 3L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount expense tidak boleh memiliki pecahan sub-sen");

        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpense_resolvesCalculatorWithCorrectParticipants() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(splitCalculator.calculateEqual(eq(AMOUNT), anyList())).thenReturn(List.of());
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().createExpense(1L, request(3L, List.of(2L, 3L, 1L)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Participant>> beneficiaryCaptor = ArgumentCaptor.forClass(List.class);
        verify(splitCalculator).calculateEqual(eq(AMOUNT), beneficiaryCaptor.capture());
        assertThat(beneficiaryCaptor.getValue())
                .extracting(Participant::getId)
                .containsExactly(2L, 3L, 1L);
    }

    @Test
    void createExpense_payerIsAlsoBeneficiary_isValid() {
        BillGroup group = groupWithParticipants();
        when(billGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(splitCalculator.calculateEqual(eq(AMOUNT), anyList())).thenAnswer(invocation -> {
            List<Participant> participants = invocation.getArgument(1);
            return List.of(new Share(participants.get(0), AMOUNT));
        });
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = service().createExpense(1L, request(1L, List.of(1L)));

        assertThat(response.payer().id()).isEqualTo(1L);
        assertThat(response.beneficiaries()).hasSize(1);
    }
}