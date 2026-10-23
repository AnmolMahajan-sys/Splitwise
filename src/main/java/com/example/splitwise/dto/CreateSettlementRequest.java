package com.example.splitwise.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class CreateSettlementRequest {

    @NotNull
    @Positive
    private Long paidByUserId;

    @NotNull
    @Positive
    private Long paidToUserId;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer=10,fraction = 2)
    private BigDecimal amount;

    public Long getPaidByUserId() {
        return paidByUserId;
    }
    public void setPaidByUserId(Long paidByUserId) {
        this.paidByUserId = paidByUserId;
    }
    public Long getPaidToUserId() {
        return paidToUserId;
    }
    public void setPaidToUserId(Long paidToUserId) {
        this.paidToUserId = paidToUserId;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

}
