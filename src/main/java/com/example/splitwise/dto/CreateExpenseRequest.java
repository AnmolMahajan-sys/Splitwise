package com.example.splitwise.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public class CreateExpenseRequest {

    @NotNull(message = "Payer user ID is required")
    @Positive(message = "Payer user ID >0")
    private long paidByUserId;

    @NotBlank(message = "Description is required")
    @Size(max=255, message = "Description<255 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value="0.01",message = "Amount must be >0")
    @Digits(integer = 10,fraction = 3,message = "Amount must have at most 10 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotEmpty(message = "At least 1 participant is required")
    @Size(max=100, message = "An expense can have at most 100 participants")
    private List<@NotNull @Positive Long> participantUserIds;

    public  CreateExpenseRequest() {
    }

    public Long getPaidByUserId() {
        return paidByUserId;
    }
    public void setPaidByUserId(Long paidByUserId) {
        this.paidByUserId = paidByUserId;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public List<Long> getParticipantUserIds() {
        return participantUserIds;
    }
    public void setParticipantUserIds(List<Long> participantUserIds) {
        this.participantUserIds = participantUserIds;
    }
}
