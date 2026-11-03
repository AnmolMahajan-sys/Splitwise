
package com.example.splitwise.controller;

import com.example.splitwise.dto.BalanceResponse;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.service.BalanceService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BalanceController.class)
class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BalanceService balanceService;

    // 1. Retrieve balances successfully
    @Test
    void shouldReturnGroupBalancesSuccessfully() throws Exception {

        when(balanceService.getGroupBalances(1L))
                .thenReturn(List.of(
                        new BalanceResponse(
                                10L, "Anmol", new BigDecimal("50.00")
                        ),
                        new BalanceResponse(
                                11L, "Rahul", new BigDecimal("-50.00")
                        )
                ));

        mockMvc.perform(get("/api/groups/1/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(10))
                .andExpect(jsonPath("$[0].name").value("Anmol"))
                .andExpect(jsonPath("$[0].netBalance").value(50.00))
                .andExpect(jsonPath("$[1].userId").value(11))
                .andExpect(jsonPath("$[1].name").value("Rahul"))
                .andExpect(jsonPath("$[1].netBalance").value(-50.00));

        verify(balanceService, times(1)).getGroupBalances(1L);
    }

    // 2. Empty balance list
    @Test
    void shouldReturnEmptyListWhenNoBalancesExist() throws Exception {

        when(balanceService.getGroupBalances(1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/groups/1/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(balanceService, times(1)).getGroupBalances(1L);
    }

    // 3. Group not found
    @Test
    void shouldReturn404WhenGroupDoesNotExist() throws Exception {

        when(balanceService.getGroupBalances(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Group not found with ID: 99"
                ));

        mockMvc.perform(get("/api/groups/99/balances"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Group not found with ID: 99"));

        verify(balanceService, times(1)).getGroupBalances(99L);
    }

    // 4. Zero balance
    @Test
    void shouldReturnZeroBalanceCorrectly() throws Exception {

        when(balanceService.getGroupBalances(1L))
                .thenReturn(List.of(
                        new BalanceResponse(
                                10L, "Anmol", new BigDecimal("0.00")
                        )
                ));

        mockMvc.perform(get("/api/groups/1/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(10))
                .andExpect(jsonPath("$[0].netBalance").value(0.00));

        verify(balanceService, times(1)).getGroupBalances(1L);
    }
}

