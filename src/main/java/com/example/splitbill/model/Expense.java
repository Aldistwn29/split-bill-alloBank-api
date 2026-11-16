package com.example.splitbill.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(updatable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private BillGroup billGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private Participant payer;

    @Column(nullable = false, updatable = false)
    private LocalDate createdAt;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private SplitType splitType;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ExpenseSplit> expenseSplits = new ArrayList<>();

    protected Expense() {

    }

    public Expense(BigDecimal amount, Participant payer, BillGroup billGroup, String description, SplitType splitType) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount expense harus lebih besar dari nol");
        }

        if (splitType == null) {
            throw new IllegalArgumentException("Split type tidak boleh null");
        }

        if (description != null && description.isBlank()) {
            throw new IllegalArgumentException("Description tidak boleh kosong");
        }

        BigDecimal validatedAmount;
        try {
            validatedAmount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount expense tidak boleh memiliki pecahan sub-sen", exception);
        }
        if (validatedAmount.precision() > 19) {
            throw new IllegalArgumentException("Amount expense melebihi precision 19 dan scale 2");
        }
        if (payer == null || billGroup == null) {
            throw new IllegalArgumentException("Payer dan group tidak boleh null");
        }
        if (!billGroup.hasSameIdentity(payer.getGroup())) {
            throw new IllegalArgumentException("Payer harus berasal dari group expense");
        }
        this.amount = validatedAmount;
        this.payer = payer;
        this.description = description;
        this.splitType = splitType;
        this.billGroup = billGroup;
        this.createdAt = LocalDate.now();
        billGroup.addExpense(this);
    }

    public void addExpenseSplit(ExpenseSplit expenseSplit) {
        // Lapisan guard untuk input dasar
        if (expenseSplit == null) {
            throw new IllegalArgumentException("Expense split tidak boleh null");
        }

        Expense splitExpense = expenseSplit.getExpense();
        if (splitExpense != this
                && (getId() == null || splitExpense == null || !getId().equals(splitExpense.getId()))) {
            throw new IllegalArgumentException("Expense split harus terkait pada expense ini");
        }

        Participant participant = expenseSplit.getParticipant();
        if (participant == null || !billGroup.hasSameIdentity(participant.getGroup())) {
            throw new IllegalArgumentException("Beneficiary harus berasal dari group expense");
        }

        for (ExpenseSplit existSplit : expenseSplits) {
            if (existSplit == expenseSplit) {
                return;
            }
            Participant exParticipant = existSplit.getParticipant();
            if (exParticipant == participant || (participant.getId() != null
                    && participant.getId().equals(exParticipant.getId()))) {
                throw new IllegalArgumentException("Participant sudah memiliki split pada expense ini");
            }
        }
        expenseSplits.add(expenseSplit);
    }

    public List<ExpenseSplit> getExpenseSplits() {
        return Collections.unmodifiableList(expenseSplits);
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BillGroup getBillGroup() {
        return billGroup;
    }

    public Participant getPayer() {
        return payer;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public SplitType getSplitType() {
        return splitType;
    }
}
