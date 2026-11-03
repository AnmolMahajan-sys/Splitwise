
package com.example.splitwise.controller;

import com.example.splitwise.dto.CreateSettlementRequest;
import com.example.splitwise.dto.SettlementResponse;
import com.example.splitwise.exception.InvalidSettlementException;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.service.SettlementService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettlementController.class)
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettlementService settlementService;

    // 1. Successfully create a settlement
    @Test
    void shouldCreateSettlementSuccessfully() throws Exception {

        SettlementResponse response = new SettlementResponse(
                100L,
                1L,
                10L,
                "Anmol",
                11L,
                "Rahul",
                new BigDecimal("50.00")
        );

        when(settlementService.createSettlement(
                eq(1L), any(CreateSettlementRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/api/groups/1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "paidToUserId": 11,
                                    "amount": 50.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.groupId").value(1))
                .andExpect(jsonPath("$.paidByUserId").value(10))
                .andExpect(jsonPath("$.paidByName").value("Anmol"))
                .andExpect(jsonPath("$.paidToUserId").value(11))
                .andExpect(jsonPath("$.paidToName").value("Rahul"))
                .andExpect(jsonPath("$.paidAmount").value(50.00));

        verify(settlementService, times(1))
                .createSettlement(eq(1L), any(CreateSettlementRequest.class));
    }

    // 2. Retrieve settlements successfully
    @Test
    void shouldReturnGroupSettlementsSuccessfully() throws Exception {

        SettlementResponse response = new SettlementResponse(
                100L, 1L, 10L, "Anmol", 11L, "Rahul",
                new BigDecimal("50.00")
        );

        when(settlementService.getGroupSettlements(1L))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/groups/1/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].paidByName").value("Anmol"))
                .andExpect(jsonPath("$[0].paidToName").value("Rahul"))
                .andExpect(jsonPath("$[0].paidAmount").value(50.00));

        verify(settlementService).getGroupSettlements(1L);
    }

    // 3. Empty settlement list
    @Test
    void shouldReturnEmptyListWhenNoSettlementsExist() throws Exception {

        when(settlementService.getGroupSettlements(1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/groups/1/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(settlementService).getGroupSettlements(1L);
    }

    // 4. Missing payer ID
    @Test
    void shouldReturn400WhenPayerIdIsMissing() throws Exception {

        mockMvc.perform(post("/api/groups/1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidToUserId": 11,
                                    "amount": 50.00
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(settlementService);
    }

    // 5. Payer ID is zero
    @Test
    void shouldReturn400WhenPayerIdIsZero() throws Exception {

        mockMvc.perform(post("/api/groups/1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 0,
                                    "paidToUserId": 11,
                                    "amount": 50.00
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(settlementService);
    }

    // 6. Missing receiver ID
    @Test
    void shouldReturn400WhenReceiverIdIsMissing() throws Exception {

        mockMvc.perform(post("/api/groups/1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "amount": 50.00
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(settlementService);
    }

    // 7. Receiver ID is zero
    @Test
    void shouldReturn400WhenReceiverIdIsZero() throws Exception {

        mockMvc.perform(post("/api/groups/1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "paidToUserId": 0,
                                    "amount": 50.00
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(settlementService);
    }

    // 8. Missing amount
    @Test
    void shouldReturn400WhenAmountIsMissing() throws Exception {

        mockMvc.perform(post("/api/groups/1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "paidToUserId": 11
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(settlementService);
    }

    // 9. Amount is zero
    @Test
    void shouldReturn400WhenAmountIsZero() throws Exception {

        mockMvc.perform(post("/api/groups/1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "paidToUserId": 11,
                                    "amount": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(settlementService);
    }

    // 10. Amount has more than 2 decimal places
    @Test
    void shouldReturn400WhenAmountHasMoreThanTwoDecimalPlaces()
            throws Exception {

        mockMvc.perform(post("/api/groups/1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "paidToUserId": 11,
                                    "amount": 50.123
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(settlementService);
    }

    // 11. Payer and receiver cannot be the same
    @Test
    void shouldReturn400WhenPayerAndReceiverAreSame() throws Exception {

        when(settlementService.createSettlement(
                eq(1L), any(CreateSettlementRequest.class)
        )).thenThrow(new InvalidSettlementException(
                "Payer and receiver cannot be the same user"
        ));

        mockMvc.perform(post("/api/groups/1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "paidToUserId": 10,
                                    "amount": 50.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Payer and receiver cannot be the same user"));
    }

    // 12. Invalid settlement business rule
    @Test
    void shouldReturn400WhenSettlementBusinessRuleFails() throws Exception {

        when(settlementService.createSettlement(
                eq(1L), any(CreateSettlementRequest.class)
        )).thenThrow(new InvalidSettlementException(
                "Payer is not a member of this group"
        ));

        mockMvc.perform(post("/api/groups/1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "paidToUserId": 11,
                                    "amount": 50.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Payer is not a member of this group"));
    }

    // 13. Group does not exist when creating settlement
    @Test
    void shouldReturn404WhenGroupDoesNotExist() throws Exception {

        when(settlementService.createSettlement(
                eq(99L), any(CreateSettlementRequest.class)
        )).thenThrow(new ResourceNotFoundException(
                "Group not found with ID: 99"
        ));

        mockMvc.perform(post("/api/groups/99/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "paidToUserId": 11,
                                    "amount": 50.00
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Group not found with ID: 99"));
    }

    // 14. User does not exist
    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {

        when(settlementService.createSettlement(
                eq(1L), any(CreateSettlementRequest.class)
        )).thenThrow(new ResourceNotFoundException(
                "User not found with ID: 99"
        ));

        mockMvc.perform(post("/api/groups/1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 99,
                                    "paidToUserId": 11,
                                    "amount": 50.00
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("User not found with ID: 99"));
    }

    // 15. Group does not exist when retrieving settlements
    @Test
    void shouldReturn404WhenGettingSettlementsOfNonExistingGroup()
            throws Exception {

        when(settlementService.getGroupSettlements(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Group not found with ID: 99"
                ));

        mockMvc.perform(get("/api/groups/99/settlements"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Group not found with ID: 99"));
    }
}

