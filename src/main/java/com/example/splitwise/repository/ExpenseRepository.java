package com.example.splitwise.repository;

import com.example.splitwise.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroup_Id(Long groupId);
}
