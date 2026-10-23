package com.example.splitwise.controller;

import com.example.splitwise.dto.BalanceResponse;
import com.example.splitwise.service.BalanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/balances")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping
    public List<BalanceResponse> getGroupBalances(@PathVariable Long groupId) {
        return balanceService.getGroupBalances(groupId);
    }
}
