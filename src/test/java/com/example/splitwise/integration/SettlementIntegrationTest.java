package com.example.splitwise.integration;

import com.example.splitwise.entity.ExpenseGroup;
import com.example.splitwise.entity.GroupMember;
import com.example.splitwise.entity.Settlement;
import com.example.splitwise.entity.User;
import com.example.splitwise.repository.ExpenseGroupRepository;
import com.example.splitwise.repository.GroupMemberRepository;
import com.example.splitwise.repository.SettlementRepository;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SettlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseGroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository memberRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    private User firstUser;
    private User secondUser;
    private User thirdUser;
    private User outsider;

    private ExpenseGroup group;

    @BeforeEach
    void setUp() {
        settlementRepository.deleteAll();
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

        outsider = userRepository.save(
                new User("Karan", "karan@example.com")
        );

        group = groupRepository.save(
                new ExpenseGroup("Goa Trip", firstUser)
        );

        memberRepository.save(
                new GroupMember(group, firstUser)
        );

        memberRepository.save(
                new GroupMember(group, secondUser)
        );

        memberRepository.save(
                new GroupMember(group, thirdUser)
        );
    }

    private String createRequest(
            Long paidByUserId,
            Long paidToUserId,
            String amount
    ) {
        return """
                {
                    "paidByUserId": %d,
                    "paidToUserId": %d,
                    "amount": %s
                }
                """.formatted(paidByUserId, paidToUserId, amount);
    }

    @Test
    void shouldCreateSettlementAndSaveInDatabase() throws Exception {

        String request = createRequest(
                firstUser.getId(),
                secondUser.getId(),
                "500.00"
        );

        mockMvc.perform(post(
                        "/api/groups/" + group.getId() + "/settlements"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value(group.getId()))
                .andExpect(jsonPath("$.paidByUserId").value(firstUser.getId()))
                .andExpect(jsonPath("$.paidToUserId").value(secondUser.getId()))
                .andExpect(jsonPath("$.paidAmount").value(500.00));

        assert settlementRepository.count() == 1;
    }

    @Test
    void shouldRejectSettlementBetweenSameUsers() throws Exception {

        String request = createRequest(
                firstUser.getId(),
                firstUser.getId(),
                "100.00"
        );

        mockMvc.perform(post(
                        "/api/groups/" + group.getId() + "/settlements"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assert settlementRepository.count() == 0;
    }

    @Test
    void shouldRejectPayerWhoIsNotGroupMember() throws Exception {

        String request = createRequest(
                outsider.getId(),
                secondUser.getId(),
                "100.00"
        );

        mockMvc.perform(post(
                        "/api/groups/" + group.getId() + "/settlements"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assert settlementRepository.count() == 0;
    }

    @Test
    void shouldRejectReceiverWhoIsNotGroupMember() throws Exception {

        String request = createRequest(
                firstUser.getId(),
                outsider.getId(),
                "100.00"
        );

        mockMvc.perform(post(
                        "/api/groups/" + group.getId() + "/settlements"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assert settlementRepository.count() == 0;
    }

    @Test
    void shouldReturn404WhenGroupDoesNotExist() throws Exception {

        String request = createRequest(
                firstUser.getId(),
                secondUser.getId(),
                "100.00"
        );

        mockMvc.perform(post("/api/groups/99999/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());

        assert settlementRepository.count() == 0;
    }

    @Test
    void shouldReturn404WhenPayerDoesNotExist() throws Exception {

        String request = createRequest(
                99999L,
                secondUser.getId(),
                "100.00"
        );

        mockMvc.perform(post(
                        "/api/groups/" + group.getId() + "/settlements"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());

        assert settlementRepository.count() == 0;
    }

    @Test
    void shouldReturn404WhenReceiverDoesNotExist() throws Exception {

        String request = createRequest(
                firstUser.getId(),
                99999L,
                "100.00"
        );

        mockMvc.perform(post(
                        "/api/groups/" + group.getId() + "/settlements"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());

        assert settlementRepository.count() == 0;
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() throws Exception {

        String request = createRequest(
                firstUser.getId(),
                secondUser.getId(),
                "100.123"
        );

        mockMvc.perform(post(
                        "/api/groups/" + group.getId() + "/settlements"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assert settlementRepository.count() == 0;
    }

    @Test
    void shouldRejectZeroSettlementAmount() throws Exception {

        String request = createRequest(
                firstUser.getId(),
                secondUser.getId(),
                "0.00"
        );

        mockMvc.perform(post(
                        "/api/groups/" + group.getId() + "/settlements"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assert settlementRepository.count() == 0;
    }

    @Test
    void shouldGetAllGroupSettlements() throws Exception {

        Settlement firstSettlement = new Settlement(
                group,
                firstUser,
                secondUser,
                new BigDecimal("500.00")
        );

        Settlement secondSettlement = new Settlement(
                group,
                secondUser,
                thirdUser,
                new BigDecimal("250.00")
        );

        settlementRepository.save(firstSettlement);
        settlementRepository.save(secondSettlement);

        mockMvc.perform(get(
                        "/api/groups/" + group.getId() + "/settlements"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldReturnEmptyListWhenGroupHasNoSettlements() throws Exception {

        mockMvc.perform(get(
                        "/api/groups/" + group.getId() + "/settlements"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn404WhenGettingSettlementsForMissingGroup() throws Exception {

        mockMvc.perform(get("/api/groups/99999/settlements"))
                .andExpect(status().isNotFound());
    }
}