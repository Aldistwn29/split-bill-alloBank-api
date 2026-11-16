package com.example.splitbill.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.splitbill.model.BillGroup;

public interface BillGroupRepository extends JpaRepository<BillGroup, Long> {

    @Override
    @EntityGraph(attributePaths = "participants")
    Optional<BillGroup> findById(Long id);
}