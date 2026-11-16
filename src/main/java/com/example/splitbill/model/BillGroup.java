package com.example.splitbill.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "bill_groups")
public class BillGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Participant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "billGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();

    protected BillGroup() {

    }

    public BillGroup(String name) {
        setName(name);
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nama group tidak boleh kosong");
        }

        this.name = name;
    }

    boolean hasSameIdentity(BillGroup other) {
        return this == other || (other != null && getId() != null && getId().equals(other.getId()));
    }

    void addParticipant(Participant participant) {
        if (participant == null) {
            throw new IllegalArgumentException("Participant tidak boleh null");
        }
        participant.assignToGroup(this);
        for (Participant existingParticipant : participants) {
            if (existingParticipant == participant) {
                return;
            }
        }
        participants.add(participant);
    }

    void addExpense(Expense expense) {
        if (expense == null || !hasSameIdentity(expense.getBillGroup())) {
            throw new IllegalArgumentException("Expense harus terikat pada group ini");
        }
        for (Expense existingExpense : expenses) {
            if (existingExpense == expense) {
                return;
            }
        }
        expenses.add(expense);
    }

    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    public Optional<Participant> findParticipantById(Long participantId) {
        return participants
                .stream()
                .filter(participant -> participantId.equals(participant.getId()))
                .findFirst();
    }

}
