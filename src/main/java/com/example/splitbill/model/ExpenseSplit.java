package com.example.splitbill.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "expense_splits", uniqueConstraints = {
    @UniqueConstraint(
        columnNames = {"expense_id", "participant_id"}
        )
    }
)


public class ExpenseSplit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="expense_id", nullable = false)
    private Expense expense;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="participant_id", nullable = false)
    private Participant participant;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal shareAmount;

    protected ExpenseSplit() {

    }

    public ExpenseSplit(Expense expense, Participant participant, BigDecimal shareAmount){
        if (expense == null || participant == null) {
            throw new IllegalArgumentException("Expense dan participant tidak boleh null");
        }
        if (shareAmount == null || shareAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Share amount tidak boleh null atau negatif");
        }
        BigDecimal validatedShareAmount;
        try {
            validatedShareAmount = shareAmount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Share amount tidak boleh memiliki pecahan sub-sen", exception);
        }
        if (validatedShareAmount.precision() > 19) {
            throw new IllegalArgumentException("Share amount melebihi precision 19 dan scale 2");
        }
        if (!expense.getBillGroup().hasSameIdentity(participant.getGroup())) {
            throw new IllegalArgumentException("Beneficiary harus berasal dari group expense");
        }
        this.expense = expense;
        this.participant = participant;
        this.shareAmount = validatedShareAmount;
        expense.addExpenseSplit(this);
    }

    public Long getId(){
        return id;
    }

    public Expense getExpense(){
        return expense;
    }

    public Participant getParticipant(){
        return participant;
    }

    public BigDecimal getShareAmount(){
        return shareAmount;
    }

}
