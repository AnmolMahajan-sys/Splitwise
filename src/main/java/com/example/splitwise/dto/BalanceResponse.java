package com.example.splitwise.dto;

import java.math.BigDecimal;

public class BalanceResponse {
    private Long userId;
    private String name;
    private BigDecimal netBalance;

    public BalanceResponse(Long userId, String name, BigDecimal netBalance) {
        this.userId = userId;
        this.name = name;
        this.netBalance = netBalance;
    }

    public Long getUserId(){
        return userId;
    }
    public String getName(){
        return name;
    }
    public BigDecimal getNetBalance(){
        return netBalance;
    }
}
