
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseGroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository memberRepository;

    private User creator;

    @BeforeEach
    void setUp() {

        memberRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();

        creator = userRepository.save(
                new User("Anmol", "anmol@example.com")
        );
    }

    @Test
    void shouldCreateGroupAndSaveInDatabase() throws Exception {

        String request = """
                {
                    "name": "Goa Trip",
                    "createdByUserId": %d
                }
                """.formatted(creator.getId());

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Goa Trip"))
                .andExpect(jsonPath("$.createdByUserId")
                        .value(creator.getId()))
                .andExpect(jsonPath("$.createdByName").value("Anmol"));

        assertEquals(1, groupRepository.count());

        ExpenseGroup savedGroup = groupRepository.findAll()
                .get(0);

        assertEquals("Goa Trip", savedGroup.getName());

        assertEquals(1, memberRepository.count());

        GroupMember membership = memberRepository.findAll()
                .get(0);

        assertEquals(creator.getId(), membership.getUser().getId());
        assertEquals(savedGroup.getId(), membership.getGroup().getId());
    }

    @Test
    void shouldTrimGroupName() throws Exception {

        String request = """
                {
                    "name": "   Goa Trip   ",
                    "createdByUserId": %d
                }
                """.formatted(creator.getId());

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Goa Trip"));

        assertEquals(1, groupRepository.count());
    }

    @Test
    void shouldRejectMissingGroupName() throws Exception {

        String request = """
                {
                    "createdByUserId": %d
                }
                """.formatted(creator.getId());

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, groupRepository.count());
    }

    @Test
    void shouldRejectBlankGroupName() throws Exception {

        String request = """
                {
                    "name": "   ",
                    "createdByUserId": %d
                }
                """.formatted(creator.getId());

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, groupRepository.count());
    }

    @Test
    void shouldRejectMissingCreatorId() throws Exception {

        String request = """
                {
                    "name": "Goa Trip"
                }
                """;

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, groupRepository.count());
    }

    @Test
    void shouldRejectZeroCreatorId() throws Exception {

        String request = """
                {
                    "name": "Goa Trip",
                    "createdByUserId": 0
                }
                """;

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, groupRepository.count());
    }

    @Test
    void shouldReturn404WhenCreatorDoesNotExist() throws Exception {

        String request = """
                {
                    "name": "Goa Trip",
                    "createdByUserId": 99999
                }
                """;

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        assertEquals(0, groupRepository.count());
    }

    @Test
    void shouldRejectGroupNameLongerThan100Characters() throws Exception {

        String longName = "A".repeat(101);

        String request = """
                {
                    "name": "%s",
                    "createdByUserId": %d
                }
                """.formatted(longName, creator.getId());

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, groupRepository.count());
    }
}

