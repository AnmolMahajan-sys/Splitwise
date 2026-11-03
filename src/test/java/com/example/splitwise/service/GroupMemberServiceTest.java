
package com.example.splitwise.service;

import com.example.splitwise.dto.AddGroupMemberRequest;
import com.example.splitwise.dto.GroupMemberResponse;
import com.example.splitwise.entity.ExpenseGroup;
import com.example.splitwise.entity.GroupMember;
import com.example.splitwise.entity.User;
import com.example.splitwise.exception.DuplicateGroupMembershipException;
import com.example.splitwise.exception.InvalidExpenseException;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.repository.ExpenseGroupRepository;
import com.example.splitwise.repository.GroupMemberRepository;
import com.example.splitwise.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GroupMemberServiceTest {

    private ExpenseGroupRepository groupRepository;
    private UserRepository userRepository;
    private GroupMemberRepository memberRepository;

    private GroupMemberService groupMemberService;

    @BeforeEach
    void setUp() {

        groupRepository = mock(ExpenseGroupRepository.class);
        userRepository = mock(UserRepository.class);
        memberRepository = mock(GroupMemberRepository.class);

        groupMemberService = new GroupMemberService(
                groupRepository,
                userRepository,
                memberRepository
        );
    }

    private AddGroupMemberRequest createRequest(Long userId) {

        AddGroupMemberRequest request = new AddGroupMemberRequest();
        request.setUserId(userId);

        return request;
    }

    private User createUser(Long id, String name, String email) {

        User user = mock(User.class);

        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.getEmail()).thenReturn(email);

        return user;
    }

    private GroupMember createSavedMember(
            Long membershipId,
            ExpenseGroup group,
            User user
    ) {

        GroupMember member = mock(GroupMember.class);

        when(member.getId()).thenReturn(membershipId);
        when(member.getGroup()).thenReturn(group);
        when(member.getUser()).thenReturn(user);

        return member;
    }

    // TEST 1: Successfully add a member

    @Test
    void shouldAddMemberSuccessfully() {

        ExpenseGroup group = mock(ExpenseGroup.class);
        when(group.getId()).thenReturn(10L);

        User user = createUser(1L, "Anmol", "anmol@gmail.com");

        AddGroupMemberRequest request = createRequest(1L);

        GroupMember savedMember = createSavedMember(100L, group, user);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(false);

        when(memberRepository.save(any(GroupMember.class)))
                .thenReturn(savedMember);

        GroupMemberResponse response =
                groupMemberService.addMember(10L, request);

        assertNotNull(response);
        assertEquals(100L, response.getMembershipId());
        assertEquals(10L, response.getGroupId());
        assertEquals(1L, response.getUserId());
        assertEquals("Anmol", response.getName());
        assertEquals("anmol@gmail.com", response.getEmail());

        verify(memberRepository).save(any(GroupMember.class));
    }

    // TEST 2: Group does not exist

    @Test
    void shouldThrowExceptionWhenGroupDoesNotExist() {

        AddGroupMemberRequest request = createRequest(1L);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> groupMemberService.addMember(10L, request)
        );

        verifyNoInteractions(userRepository);
        verify(memberRepository, never()).save(any());
    }

    // TEST 3: User does not exist

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        ExpenseGroup group = mock(ExpenseGroup.class);

        AddGroupMemberRequest request = createRequest(1L);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> groupMemberService.addMember(10L, request)
        );

        verify(memberRepository, never())
                .existsByGroup_IdAndUser_Id(anyLong(), anyLong());

        verify(memberRepository, never()).save(any());
    }

    // TEST 4: Reject duplicate membership

    @Test
    void shouldRejectDuplicateGroupMembership() {

        ExpenseGroup group = mock(ExpenseGroup.class);

        User user = createUser(1L, "Anmol", "anmol@gmail.com");

        AddGroupMemberRequest request = createRequest(1L);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        assertThrows(
                DuplicateGroupMembershipException.class,
                () -> groupMemberService.addMember(10L, request)
        );

        verify(memberRepository, never()).save(any());
    }

    // TEST 5: Reject null request

    @Test
    void shouldRejectNullRequest() {

        assertThrows(
                InvalidExpenseException.class,
                () -> groupMemberService.addMember(10L, null)
        );

        verifyNoInteractions(groupRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(memberRepository);
    }

    // TEST 6: Successfully retrieve all group members

    @Test
    void shouldReturnAllGroupMembers() {

        ExpenseGroup group = mock(ExpenseGroup.class);
        when(group.getId()).thenReturn(10L);

        User user1 = createUser(1L, "Anmol", "anmol@gmail.com");
        User user2 = createUser(2L, "Rahul", "rahul@gmail.com");

        GroupMember member1 = createSavedMember(100L, group, user1);
        GroupMember member2 = createSavedMember(101L, group, user2);

        when(groupRepository.existsById(10L))
                .thenReturn(true);

        when(memberRepository.findByGroup_Id(10L))
                .thenReturn(List.of(member1, member2));

        List<GroupMemberResponse> response =
                groupMemberService.getMembers(10L);

        assertNotNull(response);
        assertEquals(2, response.size());

        assertEquals(1L, response.get(0).getUserId());
        assertEquals("Anmol", response.get(0).getName());

        assertEquals(2L, response.get(1).getUserId());
        assertEquals("Rahul", response.get(1).getName());

        verify(memberRepository).findByGroup_Id(10L);
    }

    // TEST 7: Group has no members

    @Test
    void shouldReturnEmptyListWhenGroupHasNoMembers() {

        when(groupRepository.existsById(10L))
                .thenReturn(true);

        when(memberRepository.findByGroup_Id(10L))
                .thenReturn(List.of());

        List<GroupMemberResponse> response =
                groupMemberService.getMembers(10L);

        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(memberRepository).findByGroup_Id(10L);
    }

    // TEST 8: Group does not exist while retrieving members

    @Test
    void shouldThrowExceptionWhenGettingMembersOfNonexistentGroup() {

        when(groupRepository.existsById(10L))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> groupMemberService.getMembers(10L)
        );

        verify(memberRepository, never()).findByGroup_Id(anyLong());
    }
}

