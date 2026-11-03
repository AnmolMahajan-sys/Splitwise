
package com.example.splitwise.service;

import com.example.splitwise.dto.CreateGroupRequest;
import com.example.splitwise.dto.GroupResponse;
import com.example.splitwise.entity.ExpenseGroup;
import com.example.splitwise.entity.GroupMember;
import com.example.splitwise.entity.User;
import com.example.splitwise.exception.InvalidExpenseException;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.repository.ExpenseGroupRepository;
import com.example.splitwise.repository.GroupMemberRepository;
import com.example.splitwise.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GroupServiceTest {

    private ExpenseGroupRepository groupRepository;
    private UserRepository userRepository;
    private GroupMemberRepository memberRepository;

    private GroupService groupService;

    @BeforeEach
    void setUp() {

        groupRepository = mock(ExpenseGroupRepository.class);
        userRepository = mock(UserRepository.class);
        memberRepository = mock(GroupMemberRepository.class);

        groupService = new GroupService(
                groupRepository,
                userRepository,
                memberRepository
        );
    }

    private CreateGroupRequest createRequest(String name, Long userId) {

        CreateGroupRequest request = new CreateGroupRequest();
        request.setName(name);
        request.setCreatedByUserId(userId);

        return request;
    }

    private User createUser() {

        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(user.getName()).thenReturn("Anmol");

        return user;
    }

    // TEST 1: Successful group creation

    @Test
    void shouldCreateGroupSuccessfully() {

        User creator = createUser();

        CreateGroupRequest request = createRequest("Trip", 1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(creator));

        when(groupRepository.save(any(ExpenseGroup.class)))
                .thenAnswer(invocation -> {

                    ExpenseGroup group = invocation.getArgument(0);

                    return mockSavedGroup(group, 10L);
                });

        GroupResponse response = groupService.createGroup(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Trip", response.getName());
        assertEquals(1L, response.getCreatedByUserId());
        assertEquals("Anmol", response.getCreatedByName());

        verify(groupRepository).save(any(ExpenseGroup.class));
        verify(memberRepository).save(any(GroupMember.class));
    }

    // TEST 2: Creator does not exist

    @Test
    void shouldThrowExceptionWhenCreatorDoesNotExist() {

        CreateGroupRequest request = createRequest("Trip", 1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> groupService.createGroup(request)
        );

        verify(groupRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
    }

    // TEST 3: Group name should be trimmed

    @Test
    void shouldTrimGroupNameBeforeSaving() {

        User creator = createUser();

        CreateGroupRequest request = createRequest("  Goa Trip  ", 1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(creator));

        when(groupRepository.save(any(ExpenseGroup.class)))
                .thenAnswer(invocation -> {

                    ExpenseGroup group = invocation.getArgument(0);

                    assertEquals("Goa Trip", group.getName());

                    return mockSavedGroup(group, 10L);
                });

        groupService.createGroup(request);

        verify(groupRepository).save(any(ExpenseGroup.class));
    }

    // TEST 4: Creator should automatically become group member

    @Test
    void shouldAddCreatorAsGroupMember() {

        User creator = createUser();

        CreateGroupRequest request = createRequest("Trip", 1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(creator));

        when(groupRepository.save(any(ExpenseGroup.class)))
                .thenAnswer(invocation -> {

                    ExpenseGroup group = invocation.getArgument(0);

                    return mockSavedGroup(group, 10L);
                });

        groupService.createGroup(request);

        verify(memberRepository).save(argThat(member ->
                member.getGroup().getId().equals(10L)
                        && member.getUser().getId().equals(1L)
        ));
    }

    // TEST 5: Null request

    @Test
    void shouldRejectNullRequest() {

        assertThrows(
                InvalidExpenseException.class,
                () -> groupService.createGroup(null)
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(groupRepository);
        verifyNoInteractions(memberRepository);
    }

    // TEST 6: Null group name

    @Test
    void shouldRejectNullGroupName() {

        CreateGroupRequest request = createRequest(null, 1L);

        assertThrows(
                InvalidExpenseException.class,
                () -> groupService.createGroup(request)
        );

        verifyNoInteractions(userRepository);
    }

    // TEST 7: Empty group name

    @Test
    void shouldRejectEmptyGroupName() {

        CreateGroupRequest request = createRequest("", 1L);

        assertThrows(
                InvalidExpenseException.class,
                () -> groupService.createGroup(request)
        );

        verifyNoInteractions(userRepository);
    }

    // TEST 8: Whitespace-only group name

    @Test
    void shouldRejectWhitespaceOnlyGroupName() {

        CreateGroupRequest request = createRequest("     ", 1L);

        assertThrows(
                InvalidExpenseException.class,
                () -> groupService.createGroup(request)
        );

        verifyNoInteractions(userRepository);
    }

    // TEST 9: Group name with surrounding spaces should be stored trimmed

    @Test
    void shouldSaveTrimmedGroupName() {

        User creator = createUser();

        CreateGroupRequest request = createRequest("  Family  ", 1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(creator));

        when(groupRepository.save(any(ExpenseGroup.class)))
                .thenAnswer(invocation -> {

                    ExpenseGroup group = invocation.getArgument(0);

                    assertEquals("Family", group.getName());

                    return mockSavedGroup(group, 20L);
                });

        GroupResponse response = groupService.createGroup(request);

        assertEquals("Family", response.getName());
    }

    // TEST 10: Membership should not be saved if group creation fails

    @Test
    void shouldNotSaveMembershipWhenGroupSaveFails() {

        User creator = createUser();

        CreateGroupRequest request = createRequest("Trip", 1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(creator));

        when(groupRepository.save(any(ExpenseGroup.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(
                RuntimeException.class,
                () -> groupService.createGroup(request)
        );

        verify(memberRepository, never()).save(any());
    }

    // Helper method to simulate a database-saved group

    private ExpenseGroup mockSavedGroup(ExpenseGroup original, Long groupId) {

        ExpenseGroup savedGroup = mock(ExpenseGroup.class);

        when(savedGroup.getId()).thenReturn(groupId);
        when(savedGroup.getName()).thenReturn(original.getName());
        when(savedGroup.getCreatedBy()).thenReturn(original.getCreatedBy());

        return savedGroup;
    }
}

