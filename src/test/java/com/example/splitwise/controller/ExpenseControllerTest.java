
package com.example.splitwise.controller;

import com.example.splitwise.dto.CreateExpenseRequest;
import com.example.splitwise.dto.ExpenseResponse;
import com.example.splitwise.dto.ExpenseSplitResponse;
import com.example.splitwise.exception.InvalidExpenseException;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.service.ExpenseService;

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

@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    // 1. Successfully create an expense
    @Test
    void shouldCreateExpenseSuccessfully() throws Exception {

        ExpenseResponse response = new ExpenseResponse(
                100L,
                1L,
                "Dinner",
                new BigDecimal("100.00"),
                10L,
                "Anmol",
                List.of(
                        new ExpenseSplitResponse(
                                10L, "Anmol", new BigDecimal("50.00")
                        ),
                        new ExpenseSplitResponse(
                                11L, "Rahul", new BigDecimal("50.00")
                        )
                )
        );

        when(expenseService.createExpense(
                eq(1L), any(CreateExpenseRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "Dinner",
                                    "amount": 100.00,
                                    "participantUserIds": [10, 11]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.groupId").value(1))
                .andExpect(jsonPath("$.description").value("Dinner"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.paidByUserId").value(10))
                .andExpect(jsonPath("$.paidByName").value("Anmol"))
                .andExpect(jsonPath("$.splits.length()").value(2))
                .andExpect(jsonPath("$.splits[0].userId").value(10))
                .andExpect(jsonPath("$.splits[0].shareAmount").value(50.00));

        verify(expenseService, times(1))
                .createExpense(eq(1L), any(CreateExpenseRequest.class));
    }

    // 2. Retrieve group expenses
    @Test
    void shouldReturnGroupExpensesSuccessfully() throws Exception {

        ExpenseResponse response = new ExpenseResponse(
                100L,
                1L,
                "Dinner",
                new BigDecimal("100.00"),
                10L,
                "Anmol",
                List.of(
                        new ExpenseSplitResponse(
                                10L, "Anmol", new BigDecimal("50.00")
                        ),
                        new ExpenseSplitResponse(
                                11L, "Rahul", new BigDecimal("50.00")
                        )
                )
        );

        when(expenseService.getGroupExpenses(1L))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/groups/1/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Dinner"))
                .andExpect(jsonPath("$[0].amount").value(100.00));

        verify(expenseService, times(1)).getGroupExpenses(1L);
    }

    // 3. Empty expense list
    @Test
    void shouldReturnEmptyListWhenGroupHasNoExpenses() throws Exception {

        when(expenseService.getGroupExpenses(1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/groups/1/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(expenseService, times(1)).getGroupExpenses(1L);
    }

    // 4. Missing payer ID (primitive long defaults to zero)
    @Test
    void shouldReturn400WhenPayerIdIsMissing() throws Exception {

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Dinner",
                                    "amount": 100.00,
                                    "participantUserIds": [10]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 5. Negative payer ID
    @Test
    void shouldReturn400WhenPayerIdIsNegative() throws Exception {

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": -10,
                                    "description": "Dinner",
                                    "amount": 100.00,
                                    "participantUserIds": [10]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 6. Missing description
    @Test
    void shouldReturn400WhenDescriptionIsMissing() throws Exception {

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "amount": 100.00,
                                    "participantUserIds": [10]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 7. Blank description
    @Test
    void shouldReturn400WhenDescriptionIsBlank() throws Exception {

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "   ",
                                    "amount": 100.00,
                                    "participantUserIds": [10]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 8. Description exceeds 255 characters
    @Test
    void shouldReturn400WhenDescriptionExceeds255Characters() throws Exception {

        String longDescription = "A".repeat(256);

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "%s",
                                    "amount": 100.00,
                                    "participantUserIds": [10]
                                }
                                """.formatted(longDescription)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 9. Missing amount
    @Test
    void shouldReturn400WhenAmountIsMissing() throws Exception {

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "Dinner",
                                    "participantUserIds": [10]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 10. Amount is zero
    @Test
    void shouldReturn400WhenAmountIsZero() throws Exception {

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "Dinner",
                                    "amount": 0,
                                    "participantUserIds": [10]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 11. Amount has more than 3 decimal places
    @Test
    void shouldReturn400WhenAmountHasMoreThanThreeDecimalPlaces()
            throws Exception {

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "Dinner",
                                    "amount": 100.1234,
                                    "participantUserIds": [10]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 12. Missing participants
    @Test
    void shouldReturn400WhenParticipantsAreMissing() throws Exception {

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "Dinner",
                                    "amount": 100.00
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 13. Empty participants list
    @Test
    void shouldReturn400WhenParticipantsListIsEmpty() throws Exception {

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "Dinner",
                                    "amount": 100.00,
                                    "participantUserIds": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 14. Zero participant ID
    @Test
    void shouldReturn400WhenParticipantIdIsZero() throws Exception {

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "Dinner",
                                    "amount": 100.00,
                                    "participantUserIds": [0]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 15. Null participant ID
    @Test
    void shouldReturn400WhenParticipantIdIsNull() throws Exception {

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "Dinner",
                                    "amount": 100.00,
                                    "participantUserIds": [null]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 16. More than 100 participants
    @Test
    void shouldReturn400WhenParticipantsExceed100() throws Exception {

        StringBuilder participants = new StringBuilder();

        for (int i = 1; i <= 101; i++) {
            if (i > 1) {
                participants.append(",");
            }
            participants.append(i);
        }

        String requestBody = """
                {
                    "paidByUserId": 10,
                    "description": "Dinner",
                    "amount": 100.00,
                    "participantUserIds": [%s]
                }
                """.formatted(participants);

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(expenseService);
    }

    // 17. Invalid expense business rule
    @Test
    void shouldReturn400WhenExpenseBusinessRuleFails() throws Exception {

        when(expenseService.createExpense(
                eq(1L), any(CreateExpenseRequest.class)
        )).thenThrow(new InvalidExpenseException("Payer is not a group member"));

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "Dinner",
                                    "amount": 100.00,
                                    "participantUserIds": [10]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Payer is not a group member"));
    }

    // 18. Group not found while creating an expense
    @Test
    void shouldReturn404WhenGroupDoesNotExist() throws Exception {

        when(expenseService.createExpense(
                eq(99L), any(CreateExpenseRequest.class)
        )).thenThrow(new ResourceNotFoundException(
                "Group not found with ID: 99"
        ));

        mockMvc.perform(post("/api/groups/99/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "paidByUserId": 10,
                                    "description": "Dinner",
                                    "amount": 100.00,
                                    "participantUserIds": [10]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Group not found with ID: 99"));
    }

    // 19. Group not found while retrieving expenses
    @Test
    void shouldReturn404WhenGettingExpensesOfNonExistingGroup()
            throws Exception {

        when(expenseService.getGroupExpenses(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Group not found with ID: 99"
                ));

        mockMvc.perform(get("/api/groups/99/expenses"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Group not found with ID: 99"));
    }
}

