package com.example.splitwise.controller;

import com.example.splitwise.dto.CreateSettlementRequest;
import com.example.splitwise.dto.SettlementResponse;
import com.example.splitwise.repository.SettlementRepository;
import com.example.splitwise.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
public class SettlementController {
    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SettlementResponse createSettlement(
        @PathVariable Long groupId,@Valid @RequestBody CreateSettlementRequest request){
        return settlementService.createSettlement(groupId, request);
    }

    @GetMapping
    public List<SettlementResponse> getSettlements(@PathVariable Long groupId){
        return settlementService.getGroupSettlements(groupId);
    }
}
