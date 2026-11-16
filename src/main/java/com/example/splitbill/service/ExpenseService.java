package com.example.splitbill.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.splitbill.calculator.Share;
import com.example.splitbill.calculator.SplitCalculator;
import com.example.splitbill.dto.request.CreateExpenseRequest;
import com.example.splitbill.dto.response.ExpenseResponse;
import com.example.splitbill.exception.BusinessException;
import com.example.splitbill.exception.ResourceNotFoundException;
import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Expense;
import com.example.splitbill.model.ExpenseSplit;
import com.example.splitbill.model.Participant;
import com.example.splitbill.repository.BillGroupRepository;
import com.example.splitbill.repository.ExpenseRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final BillGroupRepository billGroupRepository;
    private final SplitCalculator splitCalculator;

    public ExpenseService(ExpenseRepository expenseRepository, BillGroupRepository billGroupRepository,
            SplitCalculator splitCalculator) {
        this.expenseRepository = expenseRepository;
        this.billGroupRepository = billGroupRepository;
        this.splitCalculator = splitCalculator;
    }

    @Transactional
    public ExpenseResponse createExpense(Long groupId, CreateExpenseRequest request) {
        BillGroup group = billGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group dengan id " + groupId + " tidak ditemukan"));

        Set<Long> seenIds = new HashSet<>();
        for (Long beneficiaryId : request.beneficiaryIds()) {
            if (!seenIds.add(beneficiaryId)) {
                throw new BusinessException("Beneficiary tidak boleh memiliki duplikat");
            }
        }

        Participant payer = group.findParticipantById(request.payerId())
                .orElseThrow(
                        () -> new BusinessException("Payer dengan id " + request.payerId() + " bukan anggota group"));

        List<Participant> beneficiaries = request.beneficiaryIds().stream()
                .map(id -> group.findParticipantById(id).orElseThrow(
                        () -> new BusinessException("Beneficiary dengan id " + id + " bukan anggota group")))
                .toList();

        List<Share> shares = splitCalculator.calculateEqual(request.amount(), beneficiaries);

        Expense expense = new Expense(request.amount(), payer, group, request.description(), request.split().type());
        for (Share share : shares) {
            new ExpenseSplit(expense, share.participant(), share.shareAmount());
        }

        Expense savedExpense = expenseRepository.save(expense);
        return ExpenseResponse.from(savedExpense);
    }
}