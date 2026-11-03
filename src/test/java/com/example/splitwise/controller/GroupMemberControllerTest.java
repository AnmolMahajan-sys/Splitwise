package com.example.splitwise.controller;

import com.example.splitwise.dto.AddGroupMemberRequest;
import com.example.splitwise.dto.GroupMemberResponse;
import com.example.splitwise.exception.DuplicateGroupMembershipException;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.service.GroupMemberService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupMemberController.class)
class GroupMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupMemberService memberService;

    // 1. Successfully add a member
    @Test
    void shouldAddMemberSuccessfully() throws Exception {

        when(memberService.addMember(eq(1L), any(AddGroupMemberRequest.class)))
                .thenReturn(new GroupMemberResponse(
                        100L, 1L, 10L, "Anmol", "anmol@gmail.com"
                ));

        mockMvc.perform(post("/api/groups/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipId").value(100))
                .andExpect(jsonPath("$.groupId").value(1))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.name").value("Anmol"))
                .andExpect(jsonPath("$.email").value("anmol@gmail.com"));

        verify(memberService, times(1))
                .addMember(eq(1L), any(AddGroupMemberRequest.class));
    }

    // 2. Missing user ID
    @Test
    void shouldReturn400WhenUserIdIsMissing() throws Exception {

        mockMvc.perform(post("/api/groups/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(memberService);
    }

    // 3. User ID is zero
    @Test
    void shouldReturn400WhenUserIdIsZero() throws Exception {

        mockMvc.perform(post("/api/groups/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(memberService);
    }

    // 4. User ID is negative
    @Test
    void shouldReturn400WhenUserIdIsNegative() throws Exception {

        mockMvc.perform(post("/api/groups/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": -10
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(memberService);
    }

    // 5. Retrieve members successfully
    @Test
    void shouldReturnAllMembersSuccessfully() throws Exception {

        when(memberService.getMembers(1L))
                .thenReturn(List.of(
                        new GroupMemberResponse(
                                100L, 1L, 10L, "Anmol", "anmol@gmail.com"
                        ),
                        new GroupMemberResponse(
                                101L, 1L, 11L, "Rahul", "rahul@gmail.com"
                        )
                ));

        mockMvc.perform(get("/api/groups/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(10))
                .andExpect(jsonPath("$[0].name").value("Anmol"))
                .andExpect(jsonPath("$[1].userId").value(11))
                .andExpect(jsonPath("$[1].name").value("Rahul"));

        verify(memberService, times(1)).getMembers(1L);
    }

    // 6. Retrieve members when group has no members
    @Test
    void shouldReturnEmptyListWhenGroupHasNoMembers() throws Exception {

        when(memberService.getMembers(1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/groups/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(memberService, times(1)).getMembers(1L);
    }

    // 7. Duplicate membership
    @Test
    void shouldReturn409WhenUserIsAlreadyMember() throws Exception {

        when(memberService.addMember(eq(1L), any(AddGroupMemberRequest.class)))
                .thenThrow(new DuplicateGroupMembershipException(
                        "User 10 is already member of group 1"
                ));

        mockMvc.perform(post("/api/groups/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "userId": 10
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("User 10 is already member of group 1"));
    }

    // 8. Group does not exist when adding a member
    @Test
    void shouldReturn404WhenGroupDoesNotExist() throws Exception {

        when(memberService.addMember(eq(99L), any(AddGroupMemberRequest.class)))
                .thenThrow(new ResourceNotFoundException(
                        "Group not found with ID: 99"
                ));

        mockMvc.perform(post("/api/groups/99/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "userId": 10
                            }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Group not found with ID: 99"));
    }

    // 9. User does not exist
    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {

        when(memberService.addMember(eq(1L), any(AddGroupMemberRequest.class)))
                .thenThrow(new ResourceNotFoundException(
                        "User not found with ID: 99"
                ));

        mockMvc.perform(post("/api/groups/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "userId": 99
                            }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("User not found with ID: 99"));
    }

    // 10. Group does not exist when retrieving members
    @Test
    void shouldReturn404WhenGettingMembersOfNonExistingGroup() throws Exception {

        when(memberService.getMembers(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Group not found with ID: 99"
                ));

        mockMvc.perform(get("/api/groups/99/members"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Group not found with ID: 99"));
    }
}
