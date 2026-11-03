
package com.example.splitwise.integration;

import com.example.splitwise.entity.User;
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
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUserAndSaveInDatabase() throws Exception {

        String request = """
                {
                    "name": "Anmol",
                    "email": "anmol@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Anmol"))
                .andExpect(jsonPath("$.email").value("anmol@example.com"));

        assertEquals(1, userRepository.count());

        User savedUser = userRepository.findByEmail("anmol@example.com")
                .orElseThrow();

        assertEquals("Anmol", savedUser.getName());
        assertEquals("anmol@example.com", savedUser.getEmail());
    }

    @Test
    void shouldNormalizeNameAndEmail() throws Exception {

        String request = """
                {
                    "name": "  Anmol Mahajan  ",
                    "email": "  ANMOL@EXAMPLE.COM  "
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Anmol Mahajan"))
                .andExpect(jsonPath("$.email").value("anmol@example.com"));

        assertEquals(1, userRepository.count());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {

        userRepository.save(new User("Anmol", "anmol@example.com"));

        String request = """
                {
                    "name": "Rahul",
                    "email": "anmol@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        assertEquals(1, userRepository.count());
    }

    @Test
    void shouldRejectMissingName() throws Exception {

        String request = """
                {
                    "email": "anmol@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, userRepository.count());
    }

    @Test
    void shouldRejectBlankName() throws Exception {

        String request = """
                {
                    "name": "   ",
                    "email": "anmol@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, userRepository.count());
    }

    @Test
    void shouldRejectMissingEmail() throws Exception {

        String request = """
                {
                    "name": "Anmol"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, userRepository.count());
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {

        String request = """
                {
                    "name": "Anmol",
                    "email": "invalid-email"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, userRepository.count());
    }

    @Test
    void shouldRejectEmailLongerThan255Characters() throws Exception {

        String longEmail = "a".repeat(250) + "@test.com";

        String request = """
                {
                    "name": "Anmol",
                    "email": "%s"
                }
                """.formatted(longEmail);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, userRepository.count());
    }
}

