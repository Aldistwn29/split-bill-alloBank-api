package com.example.splitbill.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.splitbill.calculator.Allocation;
import com.example.splitbill.calculator.Share;
import com.example.splitbill.calculator.SplitCalculator;
import com.example.splitbill.dto.request.CreateExpenseRequest;
import com.example.splitbill.dto.request.SplitAllocationRequest;
import com.example.splitbill.dto.response.CategorySummaryResponse;
import com.example.splitbill.dto.response.ExpenseResponse;
import com.example.splitbill.exception.BusinessException;
import com.example.splitbill.exception.ResourceNotFoundException;
import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Expense;
import com.example.splitbill.model.ExpenseCategory;
import com.example.splitbill.model.ExpenseSplit;
import com.example.splitbill.model.Participant;
import com.example.splitbill.repository.BillGroupRepository;
import com.example.splitbill.repository.ExpenseRepository;

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

        Participant payer = group.findParticipantById(request.payerId())
                .orElseThrow(
                        () -> new BusinessException("Payer dengan id " + request.payerId() + " bukan anggota group"));

        List<Share> shares = switch (request.split().type()) {
            case EQUAL -> splitEqual(group, request);
            case EXACT -> splitExact(group, request);
            case PERCENTAGE -> splitPercentage(group, request);
        };

        Expense expense = new Expense(request.amount(), payer, group, request.description(), request.split().type(),
                request.category());

        for (Share share : shares) {
            new ExpenseSplit(expense, share.participant(), share.shareAmount());
        }

        Expense savedExpense = expenseRepository.save(expense);

        return ExpenseResponse.from(savedExpense);

    }

    private List<Share> splitEqual(BillGroup group, CreateExpenseRequest request) {
        if (request.beneficiaryIds() == null || request.beneficiaryIds().isEmpty()) {
            throw new BusinessException("Beneficiary wajib diisi untuk split EQUAL");
        }

        Set<Long> seenIds = new HashSet<>();
        for (Long beneficiaryId : request.beneficiaryIds()) {
            if (!seenIds.add(beneficiaryId)) {
                throw new BusinessException("Beneficiary tidak boleh memiliki duplikat");
            }
        }

        List<Participant> beneficiaries = request.beneficiaryIds().stream()
                .map(id -> group.findParticipantById(id).orElseThrow(
                        () -> new BusinessException("Beneficiary dengan id " + id + " bukan anggota group")))
                .toList();

        return splitCalculator.calculateEqual(request.amount(), beneficiaries);
    }

    private List<Share> splitExact(BillGroup group, CreateExpenseRequest request) {
        List<SplitAllocationRequest> allocations = request.split().allocations();
        if (allocations == null || allocations.isEmpty()) {
            throw new BusinessException("Allocations wajib diisi untuk split EXACT");
        }

        if (request.beneficiaryIds() != null && !request.beneficiaryIds().isEmpty()) {
            throw new BusinessException("BeneficiaryIds tidak digunakan untuk split EXACT");
        }

        Set<Long> seenIds = new HashSet<>();
        for (SplitAllocationRequest allocation : allocations) {
            if (!seenIds.add(allocation.participantId())) {
                throw new BusinessException("Beneficiary tidak boleh memiliki duplikat pada split EXACT");
            }
        }

        List<Allocation> resolvedAllocations = allocations.stream().map(
                allocationRequest -> {
                    Participant participant = group.findParticipantById(allocationRequest.participantId())
                            .orElseThrow(() -> new BusinessException("Beneficiary dengan id "
                                    + allocationRequest.participantId() + " bukan anggota group"));
                    return new Allocation(participant, allocationRequest.value());
                }).toList();

        return splitCalculator.calculateExact(request.amount(), resolvedAllocations);
    }

    private List<Share> splitPercentage(BillGroup group, CreateExpenseRequest request) {
        List<SplitAllocationRequest> allocations = request.split().allocations();
        if (allocations == null || allocations.isEmpty()) {
            throw new BusinessException("Allocations wajib diisi untuk split PERCENTAGE");
        }

        if (request.beneficiaryIds() != null && !request.beneficiaryIds().isEmpty()) {
            throw new BusinessException("BeneficiaryIds tidak digunakan untuk split PERCENTAGE");
        }

        Set<Long> seenIds = new HashSet<>();
        for (SplitAllocationRequest allocation : allocations) {
            if (!seenIds.add(allocation.participantId())) {
                throw new BusinessException("Beneficiary tidak boleh memiliki duplikat pada split PERCENTAGE");
            }
        }

        List<Allocation> resolvedAllocations = allocations.stream().map(
                allocationRequest -> {
                    Participant participant = group.findParticipantById(allocationRequest.participantId())
                            .orElseThrow(() -> new BusinessException("Beneficiary dengan id "
                                    + allocationRequest.participantId() + " bukan anggota group"));
                    return new Allocation(participant, allocationRequest.value());
                }).toList();

        return splitCalculator.calculatePercentage(request.amount(), resolvedAllocations);
    }

    public List<CategorySummaryResponse> getCategorySummary(Long groupId) {
        billGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group dengan id " + groupId + " tidak ditemukan"));

        List<Expense> expenses = expenseRepository.findByBillGroupId(groupId);

        return expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ExpenseCategory::name)))
                .map(entry -> CategorySummaryResponse.from(entry.getKey(), entry.getValue()))
                .toList();
    }

}