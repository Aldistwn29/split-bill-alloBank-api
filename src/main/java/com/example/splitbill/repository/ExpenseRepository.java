package com.example.splitbill.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.splitbill.model.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    @Query("""
            select distinct expense
            from Expense expense
            join fetch expense.payer
            left join fetch expense.expenseSplits split
            left join fetch split.participant
            where expense.billGroup.id = :groupId
            order by expense.id
        """)
    List<Expense> findAllForSettlement(@Param("groupId") Long groupId);

    List<Expense> findByBillGroupId(Long groupId);
}
