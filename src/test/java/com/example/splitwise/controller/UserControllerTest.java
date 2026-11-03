
package com.example.splitwise.controller;

import com.example.splitwise.dto.CreateUserRequest;
import com.example.splitwise.dto.UserResponse;
import com.example.splitwise.exception.EmailAlreadyExistsException;
import com.example.splitwise.service.UserService;

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

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // TEST 1: Successful user creation

    @Test
    void shouldCreateUserSuccessfully() throws Exception {

        UserResponse response =
                new UserResponse(1L, "Anmol", "anmol@gmail.com");

        when(userService.createUser(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Anmol",
                                    "email": "anmol@gmail.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Anmol"))
                .andExpect(jsonPath("$.email").value("anmol@gmail.com"));

        verify(userService).createUser(any());
    }

    // TEST 2: Missing name

    @Test
    void shouldRejectRequestWhenNameIsMissing() throws Exception {

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "anmol@gmail.com"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // TEST 3: Missing email

    @Test
    void shouldRejectRequestWhenEmailIsMissing() throws Exception {

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Anmol"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // TEST 4: Invalid email format

    @Test
    void shouldRejectInvalidEmail() throws Exception {

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Anmol",
                                    "email": "invalid-email"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // TEST 5: Name exceeds 100 characters

    @Test
    void shouldRejectNameExceeding100Characters() throws Exception {

        String longName = "A".repeat(101);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "%s",
                                    "email": "anmol@gmail.com"
                                }
                                """.formatted(longName)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // TEST 6: Email exceeds 255 characters

    @Test
    void shouldRejectEmailExceeding255Characters() throws Exception {

        String longEmail = "a".repeat(245) + "@gmail.com";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Anmol",
                                    "email": "%s"
                                }
                                """.formatted(longEmail)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // TEST 7: Blank name

    @Test
    void shouldRejectBlankName() throws Exception {

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "   ",
                                    "email": "anmol@gmail.com"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // TEST 8: Blank email

    @Test
    void shouldRejectBlankEmail() throws Exception {

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Anmol",
                                    "email": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("test@gmail.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Anmol",
                                "email": "test@gmail.com"
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("An account with email test@gmail.com already exists"));

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }
}

