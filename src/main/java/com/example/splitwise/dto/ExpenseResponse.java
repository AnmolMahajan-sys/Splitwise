package com.example.splitwise.dto;

import com.example.splitwise.entity.Expense;
import com.example.splitwise.entity.ExpenseSplit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExpenseResponse {

    private Long id;
    private Long groupId;
    private String description;
    private BigDecimal amount;
    private Long paidByUserId;
    private String paidByName;
    private List<ExpenseSplitResponse> splits;

    public ExpenseResponse(
            Long id,
            Long groupId,
            String description,
            BigDecimal amount,
            Long paidByUserId,
            String paidByName,
            List<ExpenseSplitResponse> splits
    ) {
        this.id = id;
        this.groupId = groupId;
        this.description = description;
        this.amount = amount;
        this.paidByUserId = paidByUserId;
        this.paidByName = paidByName;
        this.splits = splits;
    }
    public static ExpenseResponse from(
            Expense expense, List<ExpenseSplit> splits
    ){
        List<ExpenseSplitResponse> splitResponses = splits.stream().map(ExpenseSplitResponse::from).toList();

        return new ExpenseResponse(
                expense.getId(),
                expense.getGroup().getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getPaidBy().getId(),
                expense.getPaidBy().getName(),
                splitResponses
        );
    }
    public Long getId() {
        return id;
    }
    public Long getGroupId() {
        return groupId;
    }
    public String getDescription() {
        return description;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public Long getPaidByUserId() {
        return paidByUserId;
    }
    public String getPaidByName() {
        return paidByName;
    }
    public List<ExpenseSplitResponse> getSplits() {
        return splits;
    }


}
