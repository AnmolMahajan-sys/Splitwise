package com.example.splitwise.dto;

import com.example.splitwise.entity.ExpenseGroup;
import com.example.splitwise.entity.ExpenseSplit;

import java.math.BigDecimal;

public class ExpenseSplitResponse {

    private Long userId;
    private String userName;
    private BigDecimal shareAmount;

    public ExpenseSplitResponse(Long userId, String userName, BigDecimal shareAmount) {
        this.userId = userId;
        this.userName = userName;
        this.shareAmount = shareAmount;
    }
    public static ExpenseSplitResponse from(ExpenseSplit split) {
        return new ExpenseSplitResponse(
                split.getUser().getId(),
                split.getUser().getName(),
                split.getShareamount()
        );
    }
    public Long getUserId() {
        return userId;
    }
    public String getUserName() {
        return userName;
    }
    public BigDecimal getShareAmount() {
        return shareAmount;
    }
}
