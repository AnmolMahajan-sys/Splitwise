
package com.example.splitwise.service;

import com.example.splitwise.dto.CreateUserRequest;
import com.example.splitwise.dto.UserResponse;
import com.example.splitwise.entity.User;
import com.example.splitwise.exception.EmailAlreadyExistsException;
import com.example.splitwise.exception.InvalidExpenseException;
import com.example.splitwise.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    private CreateUserRequest createRequest(String name, String email) {
        CreateUserRequest request = new CreateUserRequest();
        request.setName(name);
        request.setEmail(email);
        return request;
    }

    private User createSavedUser(Long id, String name, String email) {
        User user = mock(User.class);

        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.getEmail()).thenReturn(email);

        return user;
    }

    // TEST 1: Successful user creation

    @Test
    void shouldCreateUserSuccessfully() {

        CreateUserRequest request =
                createRequest("Anmol", "anmol@gmail.com");

        User savedUser =
                createSavedUser(1L, "Anmol", "anmol@gmail.com");

        when(userRepository.existsByEmail("anmol@gmail.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Anmol", response.getName());
        assertEquals("anmol@gmail.com", response.getEmail());

        verify(userRepository).save(any(User.class));
    }

    // TEST 2: Reject duplicate email

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {

        CreateUserRequest request =
                createRequest("Rahul", "rahul@gmail.com");

        when(userRepository.existsByEmail("rahul@gmail.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any());
    }

    // TEST 3: Trim name before saving

    @Test
    void shouldTrimNameBeforeSaving() {

        CreateUserRequest request =
                createRequest("  Anmol Mahajan  ", "anmol@gmail.com");

        when(userRepository.existsByEmail("anmol@gmail.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {

                    User user = invocation.getArgument(0);

                    assertEquals("Anmol Mahajan", user.getName());

                    return createSavedUser(
                            1L,
                            user.getName(),
                            user.getEmail()
                    );
                });

        UserResponse response = userService.createUser(request);

        assertEquals("Anmol Mahajan", response.getName());
    }

    // TEST 4: Convert email to lowercase

    @Test
    void shouldConvertEmailToLowercase() {

        CreateUserRequest request =
                createRequest("Anmol", "ANMOL@GMAIL.COM");

        when(userRepository.existsByEmail("anmol@gmail.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {

                    User user = invocation.getArgument(0);

                    assertEquals("anmol@gmail.com", user.getEmail());

                    return createSavedUser(
                            1L,
                            user.getName(),
                            user.getEmail()
                    );
                });

        UserResponse response = userService.createUser(request);

        assertEquals("anmol@gmail.com", response.getEmail());
    }

    // TEST 5: Trim spaces around email

    @Test
    void shouldTrimEmailBeforeSaving() {

        CreateUserRequest request =
                createRequest("Anmol", "  anmol@gmail.com  ");

        when(userRepository.existsByEmail("anmol@gmail.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {

                    User user = invocation.getArgument(0);

                    assertEquals("anmol@gmail.com", user.getEmail());

                    return createSavedUser(
                            1L,
                            user.getName(),
                            user.getEmail()
                    );
                });

        UserResponse response = userService.createUser(request);

        assertEquals("anmol@gmail.com", response.getEmail());
    }

    // TEST 6: Reject null request

    @Test
    void shouldRejectNullRequest() {

        assertThrows(
                InvalidExpenseException.class,
                () -> userService.createUser(null)
        );

        verifyNoInteractions(userRepository);
    }

    // TEST 7: Reject null name

    @Test
    void shouldRejectNullName() {

        CreateUserRequest request =
                createRequest(null, "anmol@gmail.com");

        assertThrows(
                InvalidExpenseException.class,
                () -> userService.createUser(request)
        );

        verifyNoInteractions(userRepository);
    }

    // TEST 8: Reject blank name

    @Test
    void shouldRejectBlankName() {

        CreateUserRequest request =
                createRequest("   ", "anmol@gmail.com");

        assertThrows(
                InvalidExpenseException.class,
                () -> userService.createUser(request)
        );

        verifyNoInteractions(userRepository);
    }

    // TEST 9: Reject null email

    @Test
    void shouldRejectNullEmail() {

        CreateUserRequest request =
                createRequest("Anmol", null);

        assertThrows(
                InvalidExpenseException.class,
                () -> userService.createUser(request)
        );

        verifyNoInteractions(userRepository);
    }

    // TEST 10: Reject blank email

    @Test
    void shouldRejectBlankEmail() {

        CreateUserRequest request =
                createRequest("Anmol", "   ");

        assertThrows(
                InvalidExpenseException.class,
                () -> userService.createUser(request)
        );

        verifyNoInteractions(userRepository);
    }
}

