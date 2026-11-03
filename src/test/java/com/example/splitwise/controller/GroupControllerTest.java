package com.example.splitwise.controller;

import com.example.splitwise.dto.CreateGroupRequest;
import com.example.splitwise.dto.GroupResponse;
import com.example.splitwise.service.GroupService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupService groupService;

    // 1. Successful group creation
    @Test
    void shouldCreateGroupSuccessfully() throws Exception {

        when(groupService.createGroup(any(CreateGroupRequest.class)))
                .thenReturn(new GroupResponse(1L, "Trip", 10L, "Anmol"));

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Trip",
                                    "createdByUserId": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Trip"))
                .andExpect(jsonPath("$.createdByUserId").value(10))
                .andExpect(jsonPath("$.createdByName").value("Anmol"));

        verify(groupService, times(1))
                .createGroup(any(CreateGroupRequest.class));
    }

    // 2. Missing group name
    @Test
    void shouldReturn400WhenGroupNameIsMissing() throws Exception {

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "createdByUserId": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(groupService);
    }

    // 3. Blank group name
    @Test
    void shouldReturn400WhenGroupNameIsBlank() throws Exception {

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "   ",
                                    "createdByUserId": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(groupService);
    }

    // 4. Group name exceeds 100 characters
    @Test
    void shouldReturn400WhenGroupNameExceeds100Characters() throws Exception {

        String longName = "A".repeat(101);

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "%s",
                                    "createdByUserId": 10
                                }
                                """.formatted(longName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(groupService);
    }

    // 5. Missing creator user ID
    @Test
    void shouldReturn400WhenCreatorUserIdIsMissing() throws Exception {

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Trip"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(groupService);
    }

    // 6. Creator user ID is zero
    @Test
    void shouldReturn400WhenCreatorUserIdIsZero() throws Exception {

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Trip",
                                    "createdByUserId": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(groupService);
    }

    // 7. Creator user ID is negative
    @Test
    void shouldReturn400WhenCreatorUserIdIsNegative() throws Exception {

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Trip",
                                    "createdByUserId": -5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(groupService);
    }
}
