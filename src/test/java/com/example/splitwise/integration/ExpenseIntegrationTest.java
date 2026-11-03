
package com.example.splitwise.integration;

import com.example.splitwise.entity.ExpenseGroup;
import com.example.splitwise.entity.GroupMember;
import com.example.splitwise.entity.User;
import com.example.splitwise.repository.ExpenseGroupRepository;
import com.example.splitwise.repository.ExpenseRepository;
import com.example.splitwise.repository.ExpenseSplitRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExpenseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseGroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository memberRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseSplitRepository splitRepository;

    private User creator;
    private User secondUser;
    private User thirdUser;
    private User outsider;
    private ExpenseGroup group;

    @BeforeEach
    void setUp() {

        splitRepository.deleteAll();
        expenseRepository.deleteAll();
        memberRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();

        creator = userRepository.save(
                new User("Anmol", "anmol@example.com")
        );

        secondUser = userRepository.save(
                new User("Rahul", "rahul@example.com")
        );

        thirdUser = userRepository.save(
                new User("Aman", "aman@example.com")
        );

        outsider = userRepository.save(
                new User("Karan", "karan@example.com")
        );

        group = groupRepository.save(
                new ExpenseGroup("Goa Trip", creator)
        );

        memberRepository.save(new GroupMember(group, creator));
        memberRepository.save(new GroupMember(group, secondUser));
        memberRepository.save(new GroupMember(group, thirdUser));

        // outsider is deliberately not a group member
    }

    private String createRequest(
            Long payerId,
            String description,
            String amount,
            String participantIds) {

        return """
                {
                    "paidByUserId": %d,
                    "description": "%s",
                    "amount": %s,
                    "participantUserIds": %s
                }
                """.formatted(
                payerId,
                description,
                amount,
                participantIds
        );
    }

    @Test
    void shouldCreateExpenseAndSaveInDatabase() throws Exception {

        String request = createRequest(
                creator.getId(),
                "Dinner",
                "100.00",
                "[" + creator.getId() + "," + secondUser.getId() + "]"
        );

        mockMvc.perform(post("/api/groups/" + group.getId() + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.groupId").value(group.getId()))
                .andExpect(jsonPath("$.description").value("Dinner"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.paidByUserId").value(creator.getId()))
                .andExpect(jsonPath("$.splits.length()").value(2));

        assertEquals(1, expenseRepository.count());
        assertEquals(2, splitRepository.count());
    }

    @Test
    void shouldCalculateEqualSplitsCorrectly() throws Exception {

        String request = createRequest(
                creator.getId(),
                "Lunch",
                "90.00",
                "[" + creator.getId() + "," +
                        secondUser.getId() + "," +
                        thirdUser.getId() + "]"
        );

        mockMvc.perform(post("/api/groups/" + group.getId() + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.splits.length()").value(3))
                .andExpect(jsonPath("$.splits[0].shareAmount").value(30.00))
                .andExpect(jsonPath("$.splits[1].shareAmount").value(30.00))
                .andExpect(jsonPath("$.splits[2].shareAmount").value(30.00));
    }

    @Test
    void shouldDistributeRemainingCentsCorrectly() throws Exception {

        String request = createRequest(
                creator.getId(),
                "Hotel",
                "100.00",
                "[" + creator.getId() + "," +
                        secondUser.getId() + "," +
                        thirdUser.getId() + "]"
        );

        mockMvc.perform(post("/api/groups/" + group.getId() + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.splits[0].shareAmount").value(33.34))
                .andExpect(jsonPath("$.splits[1].shareAmount").value(33.33))
                .andExpect(jsonPath("$.splits[2].shareAmount").value(33.33));

        assertEquals(3, splitRepository.count());
    }

    @Test
    void shouldTrimExpenseDescription() throws Exception {

        String request = createRequest(
                creator.getId(),
                "  Dinner  ",
                "200.00",
                "[" + creator.getId() + "]"
        );

        mockMvc.perform(post("/api/groups/" + group.getId() + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Dinner"));
    }

    @Test
    void shouldRejectDuplicateParticipants() throws Exception {

        String request = createRequest(
                creator.getId(),
                "Dinner",
                "100.00",
                "[" + secondUser.getId() + "," +
                        secondUser.getId() + "]"
        );

        mockMvc.perform(post("/api/groups/" + group.getId() + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, expenseRepository.count());
        assertEquals(0, splitRepository.count());
    }

    @Test
    void shouldRejectPayerWhoIsNotGroupMember() throws Exception {

        String request = createRequest(
                outsider.getId(),
                "Dinner",
                "100.00",
                "[" + secondUser.getId() + "]"
        );

        mockMvc.perform(post("/api/groups/" + group.getId() + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, expenseRepository.count());
    }

    @Test
    void shouldRejectParticipantWhoIsNotGroupMember() throws Exception {

        String request = createRequest(
                creator.getId(),
                "Dinner",
                "100.00",
                "[" + secondUser.getId() + "," +
                        outsider.getId() + "]"
        );

        mockMvc.perform(post("/api/groups/" + group.getId() + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, expenseRepository.count());
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() throws Exception {

        String request = createRequest(
                creator.getId(),
                "Dinner",
                "100.123",
                "[" + creator.getId() + "]"
        );

        mockMvc.perform(post("/api/groups/" + group.getId() + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, expenseRepository.count());
    }

    @Test
    void shouldReturn404WhenGroupDoesNotExist() throws Exception {

        String request = createRequest(
                creator.getId(),
                "Dinner",
                "100.00",
                "[" + creator.getId() + "]"
        );

        mockMvc.perform(post("/api/groups/99999/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());

        assertEquals(0, expenseRepository.count());
    }

    @Test
    void shouldGetAllGroupExpenses() throws Exception {

        String firstRequest = createRequest(
                creator.getId(),
                "Dinner",
                "100.00",
                "[" + creator.getId() + "," + secondUser.getId() + "]"
        );

        String secondRequest = createRequest(
                secondUser.getId(),
                "Taxi",
                "60.00",
                "[" + secondUser.getId() + "," + thirdUser.getId() + "]"
        );

        mockMvc.perform(post("/api/groups/" + group.getId() + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/groups/" + group.getId() + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondRequest))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/groups/" + group.getId() + "/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Dinner"))
                .andExpect(jsonPath("$[1].description").value("Taxi"))
                .andExpect(jsonPath("$[0].splits.length()").value(2))
                .andExpect(jsonPath("$[1].splits.length()").value(2));
    }

    @Test
    void shouldReturnEmptyListWhenGroupHasNoExpenses() throws Exception {

        mockMvc.perform(get("/api/groups/" + group.getId() + "/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

