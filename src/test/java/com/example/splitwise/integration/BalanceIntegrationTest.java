package com.example.splitwise.integration;

import com.example.splitwise.entity.ExpenseGroup;
import com.example.splitwise.entity.GroupMember;
import com.example.splitwise.entity.User;
import com.example.splitwise.repository.ExpenseGroupRepository;
import com.example.splitwise.repository.GroupMemberRepository;
import com.example.splitwise.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BalanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseGroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository memberRepository;

    private User firstUser;
    private User secondUser;
    private User thirdUser;

    private ExpenseGroup group;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();

        firstUser = userRepository.save(
                new User("Anmol", "anmol@example.com")
        );

        secondUser = userRepository.save(
                new User("Rahul", "rahul@example.com")
        );

        thirdUser = userRepository.save(
                new User("Aman", "aman@example.com")
        );

        group = groupRepository.save(
                new ExpenseGroup("Goa Trip", firstUser)
        );

        memberRepository.save(new GroupMember(group, firstUser));
        memberRepository.save(new GroupMember(group, secondUser));
        memberRepository.save(new GroupMember(group, thirdUser));
    }

    private String expenseRequest(
            Long payerId,
            String description,
            String amount,
            String participantIds
    ) {
        return """
                {
                    "paidByUserId": %d,
                    "description": "%s",
                    "amount": %s,
                    "participantUserIds": [%s]
                }
                """.formatted(payerId, description, amount, participantIds);
    }

    private String settlementRequest(
            Long paidById,
            Long paidToId,
            String amount
    ) {
        return """
                {
                    "paidByUserId": %d,
                    "paidToUserId": %d,
                    "amount": %s
                }
                """.formatted(paidById, paidToId, amount);
    }

    private void createExpense(
            Long payerId,
            String description,
            String amount,
            String participantIds
    ) throws Exception {
        mockMvc.perform(post(
                        "/api/groups/" + group.getId() + "/expenses"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseRequest(
                                payerId, description, amount, participantIds
                        )))
                .andExpect(status().isCreated());
    }

    private void createSettlement(
            Long paidById,
            Long paidToId,
            String amount
    ) throws Exception {
        mockMvc.perform(post(
                        "/api/groups/" + group.getId() + "/settlements"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settlementRequest(
                                paidById, paidToId, amount
                        )))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnZeroBalancesWhenGroupHasNoExpenses() throws Exception {

        mockMvc.perform(get(
                        "/api/groups/" + group.getId() + "/balances"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].netBalance").value(0.00))
                .andExpect(jsonPath("$[1].netBalance").value(0.00))
                .andExpect(jsonPath("$[2].netBalance").value(0.00));
    }

    @Test
    void shouldCalculateEqualExpenseSplitsCorrectly() throws Exception {

        // Anmol pays ₹90, shared equally by all three users.
        createExpense(
                firstUser.getId(),
                "Dinner",
                "90.00",
                firstUser.getId() + "," +
                        secondUser.getId() + "," +
                        thirdUser.getId()
        );

        mockMvc.perform(get(
                        "/api/groups/" + group.getId() + "/balances"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                // Anmol paid 90 and owes 30: net +60.
                .andExpect(jsonPath("$[0].netBalance").value(60.00))
                // Rahul owes 30.
                .andExpect(jsonPath("$[1].netBalance").value(-30.00))
                // Aman owes 30.
                .andExpect(jsonPath("$[2].netBalance").value(-30.00));
    }

    @Test
    void shouldCalculateBalancesForMultipleExpenses() throws Exception {

        // Anmol pays ₹90, shared by all three.
        createExpense(
                firstUser.getId(),
                "Dinner",
                "90.00",
                firstUser.getId() + "," +
                        secondUser.getId() + "," +
                        thirdUser.getId()
        );

        // Rahul pays ₹60, shared by Rahul and Aman.
        createExpense(
                secondUser.getId(),
                "Taxi",
                "60.00",
                secondUser.getId() + "," + thirdUser.getId()
        );

        mockMvc.perform(get(
                        "/api/groups/" + group.getId() + "/balances"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))

                .andExpect(jsonPath("$[0].netBalance").value(60.00))

                .andExpect(jsonPath("$[1].netBalance").value(0.00))

                .andExpect(jsonPath("$[2].netBalance").value(-60.00));
    }

    @Test
    void shouldAdjustBalancesAfterSettlement() throws Exception {

        // Anmol pays ₹90, shared by all three.
        createExpense(
                firstUser.getId(),
                "Dinner",
                "90.00",
                firstUser.getId() + "," +
                        secondUser.getId() + "," +
                        thirdUser.getId()
        );

        // Rahul pays Anmol ₹20 as a settlement.
        createSettlement(
                secondUser.getId(),
                firstUser.getId(),
                "20.00"
        );

        mockMvc.perform(get(
                        "/api/groups/" + group.getId() + "/balances"
                ))
                .andExpect(status().isOk())
                // Anmol: +60 - 20 = +40.
                .andExpect(jsonPath("$[0].netBalance").value(40.00))
                // Rahul: -30 + 20 = -10.
                .andExpect(jsonPath("$[1].netBalance").value(-10.00))
                // Aman: -30.
                .andExpect(jsonPath("$[2].netBalance").value(-30.00));
    }

    @Test
    void shouldReturnZeroBalancesWhenExpenseIsFullySettled() throws Exception {

        // Anmol pays ₹90, shared by Anmol and Rahul.
        createExpense(
                firstUser.getId(),
                "Dinner",
                "90.00",
                firstUser.getId() + "," + secondUser.getId()
        );

        // Rahul pays Anmol ₹45.
        createSettlement(
                secondUser.getId(),
                firstUser.getId(),
                "45.00"
        );

        mockMvc.perform(get(
                        "/api/groups/" + group.getId() + "/balances"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].netBalance").value(0.00))
                .andExpect(jsonPath("$[1].netBalance").value(0.00))
                .andExpect(jsonPath("$[2].netBalance").value(0.00));
    }

    @Test
    void shouldReturn404WhenGroupDoesNotExist() throws Exception {

        mockMvc.perform(get("/api/groups/99999/balances"))
                .andExpect(status().isNotFound());
    }
}
