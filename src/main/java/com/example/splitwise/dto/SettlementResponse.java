package com.example.splitwise.dto;

import com.example.splitwise.entity.Settlement;

import java.math.BigDecimal;

public class SettlementResponse {

    private Long id;
    private Long groupId;
    private Long paidByUserId;
    private String paidByName;
    private Long paidToUserId;
    private String paidToName;
    private BigDecimal paidAmount;

    public SettlementResponse(
            Long id,
            Long groupId,
            Long paidByUserId,
            String paidByName,
            Long paidToUserId,
            String paidToName,
            BigDecimal paidAmount

    ) {
        this.id = id;
        this.groupId = groupId;
        this.paidByUserId = paidByUserId;
        this.paidByName = paidByName;
        this.paidToUserId = paidToUserId;
        this.paidToName = paidToName;
        this.paidAmount = paidAmount;
    }

    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getGroup().getId(),
                settlement.getPaidBy().getId(),
                settlement.getPaidBy().getName(),
                settlement.getPaidTo().getId(),
                settlement.getPaidTo().getName(),
                settlement.getAmount()
        );
    }

    public Long getId() {
        return id;
    }
    public Long getGroupId() {
        return groupId;
    }
    public Long getPaidByUserId() {
        return paidByUserId;
    }
    public String getPaidByName() {
        return paidByName;
    }
    public Long getPaidToUserId() {
        return paidToUserId;
    }
    public String getPaidToName() {
        return paidToName;
    }
    public BigDecimal getPaidAmount() {
        return paidAmount;
    }
}
