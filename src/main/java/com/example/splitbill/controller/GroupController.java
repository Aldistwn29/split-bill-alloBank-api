package com.example.splitbill.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.example.splitbill.dto.request.CreateGroupRequest;
import com.example.splitbill.dto.response.CategorySummaryResponse;
import com.example.splitbill.dto.response.GroupResponse;
import com.example.splitbill.dto.response.SettlementResponse;
import com.example.splitbill.service.ExpenseService;
import com.example.splitbill.service.GroupService;
import com.example.splitbill.service.SettlementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")

public class GroupController {

    private final GroupService groupService;
    private final SettlementService settlementService;
    private final ExpenseService expenseService;

    public GroupController(GroupService groupService, SettlementService settlementService,
            ExpenseService expenseService) {
        this.groupService = groupService;
        this.settlementService = settlementService;
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return groupService.createGroup(request);
    }

    @GetMapping("/{id}")
    public GroupResponse getGroup(@PathVariable Long id) {
        return groupService.getGroup(id);
    }

    @GetMapping("/{id}/settlement")
    public SettlementResponse getSettlement(@PathVariable Long id) {
        return settlementService.getSettlement(id);
    }

    @GetMapping("/{id}/expenses/summary")
    public List<CategorySummaryResponse> getCategorySummary(@PathVariable Long id) {
        return expenseService.getCategorySummary(id);
    }
}