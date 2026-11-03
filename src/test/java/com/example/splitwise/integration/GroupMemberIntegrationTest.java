
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupMemberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseGroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository memberRepository;

    private User creator;
    private User secondUser;
    private ExpenseGroup group;

    @BeforeEach
    void setUp() {

        memberRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();

        creator = userRepository.save(
                new User("Anmol", "anmol@example.com")
        );

        secondUser = userRepository.save(
                new User("Rahul", "rahul@example.com")
        );

        group = groupRepository.save(
                new ExpenseGroup("Goa Trip", creator)
        );
    }

    @Test
    void shouldAddMemberAndSaveInDatabase() throws Exception {

        String request = """
                {
                    "userId": %d
                }
                """.formatted(secondUser.getId());

        mockMvc.perform(post("/api/groups/" + group.getId() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value(group.getId()))
                .andExpect(jsonPath("$.userId").value(secondUser.getId()))
                .andExpect(jsonPath("$.name").value("Rahul"))
                .andExpect(jsonPath("$.email").value("rahul@example.com"));

        assertEquals(1, memberRepository.count());

        GroupMember savedMember = memberRepository.findByGroup_Id(group.getId())
                .get(0);

        assertEquals(group.getId(), savedMember.getGroup().getId());
        assertEquals(secondUser.getId(), savedMember.getUser().getId());
    }

    @Test
    void shouldRejectDuplicateMembership() throws Exception {

        memberRepository.save(
                new GroupMember(group, secondUser)
        );

        String request = """
                {
                    "userId": %d
                }
                """.formatted(secondUser.getId());

        mockMvc.perform(post("/api/groups/" + group.getId() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        assertEquals(1, memberRepository.count());
    }

    @Test
    void shouldReturn404WhenGroupDoesNotExist() throws Exception {

        String request = """
                {
                    "userId": %d
                }
                """.formatted(secondUser.getId());

        mockMvc.perform(post("/api/groups/99999/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        assertEquals(0, memberRepository.count());
    }

    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {

        String request = """
                {
                    "userId": 99999
                }
                """;

        mockMvc.perform(post("/api/groups/" + group.getId() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        assertEquals(0, memberRepository.count());
    }

    @Test
    void shouldRejectMissingUserId() throws Exception {

        String request = """
                {}
                """;

        mockMvc.perform(post("/api/groups/" + group.getId() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, memberRepository.count());
    }

    @Test
    void shouldRejectZeroUserId() throws Exception {

        String request = """
                {
                    "userId": 0
                }
                """;

        mockMvc.perform(post("/api/groups/" + group.getId() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, memberRepository.count());
    }

    @Test
    void shouldGetAllGroupMembers() throws Exception {

        memberRepository.save(new GroupMember(group, creator));
        memberRepository.save(new GroupMember(group, secondUser));

        mockMvc.perform(get("/api/groups/" + group.getId() + "/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].groupId").value(group.getId()))
                .andExpect(jsonPath("$[1].groupId").value(group.getId()));
    }

    @Test
    void shouldReturnEmptyListWhenGroupHasNoMembers() throws Exception {

        mockMvc.perform(get("/api/groups/" + group.getId() + "/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn404WhenGettingMembersOfNonExistingGroup()
            throws Exception {

        mockMvc.perform(get("/api/groups/99999/members"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}

