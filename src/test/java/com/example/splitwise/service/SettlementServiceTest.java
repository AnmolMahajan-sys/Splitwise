
package com.example.splitwise.service;

import com.example.splitwise.dto.CreateSettlementRequest;
import com.example.splitwise.dto.SettlementResponse;
import com.example.splitwise.entity.ExpenseGroup;
import com.example.splitwise.entity.Settlement;
import com.example.splitwise.entity.User;
import com.example.splitwise.exception.InvalidExpenseException;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.repository.ExpenseGroupRepository;
import com.example.splitwise.repository.GroupMemberRepository;
import com.example.splitwise.repository.SettlementRepository;
import com.example.splitwise.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettlementServiceTest {

    private ExpenseGroupRepository groupRepository;
    private UserRepository userRepository;
    private GroupMemberRepository memberRepository;
    private SettlementRepository settlementRepository;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        groupRepository = mock(ExpenseGroupRepository.class);
        userRepository = mock(UserRepository.class);
        memberRepository = mock(GroupMemberRepository.class);
        settlementRepository = mock(SettlementRepository.class);

        settlementService = new SettlementService(
                groupRepository,
                userRepository,
                memberRepository,
                settlementRepository
        );
    }

    @Test
    void shouldCreateSettlementSuccessfully() {

        ExpenseGroup group = mock(ExpenseGroup.class);
        User paidBy = mock(User.class);
        User paidTo = mock(User.class);

        when(paidBy.getId()).thenReturn(1L);
        when(paidTo.getId()).thenReturn(2L);

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setPaidByUserId(1L);
        request.setPaidToUserId(2L);
        request.setAmount(new BigDecimal("500.00"));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(paidBy));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(paidTo));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 2L))
                .thenReturn(true);

        when(settlementRepository.save(any(Settlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SettlementResponse response =
                settlementService.createSettlement(10L, request);

        assertNotNull(response);

        verify(settlementRepository).save(any(Settlement.class));
    }


    @Test
    void shouldThrowExceptionWhenGroupDoesNotExist() {

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setPaidByUserId(1L);
        request.setPaidToUserId(2L);
        request.setAmount(new BigDecimal("500.00"));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> settlementService.createSettlement(10L, request)
        );

        verify(settlementRepository, never()).save(any(Settlement.class));
    }


    @Test
    void shouldThrowExceptionWhenPayerDoesNotExist() {

        ExpenseGroup group = mock(ExpenseGroup.class);

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setPaidByUserId(1L);
        request.setPaidToUserId(2L);
        request.setAmount(new BigDecimal("500.00"));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> settlementService.createSettlement(10L, request)
        );

        verify(settlementRepository, never()).save(any(Settlement.class));
    }


    @Test
    void shouldThrowExceptionWhenReceiverDoesNotExist() {

        ExpenseGroup group = mock(ExpenseGroup.class);
        User paidBy = mock(User.class);

        when(paidBy.getId()).thenReturn(1L);

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setPaidByUserId(1L);
        request.setPaidToUserId(2L);
        request.setAmount(new BigDecimal("500.00"));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(paidBy));

        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> settlementService.createSettlement(10L, request)
        );

        verify(settlementRepository, never()).save(any(Settlement.class));
    }


    @Test
    void shouldRejectSettlementWhenPayerAndReceiverAreSame() {

        ExpenseGroup group = mock(ExpenseGroup.class);
        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setPaidByUserId(1L);
        request.setPaidToUserId(1L);
        request.setAmount(new BigDecimal("500.00"));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidExpenseException.class,
                () -> settlementService.createSettlement(10L, request)
        );

        verify(settlementRepository, never()).save(any(Settlement.class));
    }



    @Test
    void shouldRejectPayerWhoIsNotGroupMember() {

        ExpenseGroup group = mock(ExpenseGroup.class);
        User paidBy = mock(User.class);
        User paidTo = mock(User.class);

        when(paidBy.getId()).thenReturn(1L);
        when(paidTo.getId()).thenReturn(2L);

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setPaidByUserId(1L);
        request.setPaidToUserId(2L);
        request.setAmount(new BigDecimal("500.00"));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(paidBy));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(paidTo));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(false);

        assertThrows(
                InvalidExpenseException.class,
                () -> settlementService.createSettlement(10L, request)
        );

        verify(settlementRepository, never()).save(any(Settlement.class));
    }


    @Test
    void shouldRejectReceiverWhoIsNotGroupMember() {

        ExpenseGroup group = mock(ExpenseGroup.class);
        User paidBy = mock(User.class);
        User paidTo = mock(User.class);

        when(paidBy.getId()).thenReturn(1L);
        when(paidTo.getId()).thenReturn(2L);

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setPaidByUserId(1L);
        request.setPaidToUserId(2L);
        request.setAmount(new BigDecimal("500.00"));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(paidBy));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(paidTo));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 2L))
                .thenReturn(false);

        assertThrows(
                InvalidExpenseException.class,
                () -> settlementService.createSettlement(10L, request)
        );

        verify(settlementRepository, never()).save(any(Settlement.class));
    }


    @Test
    void shouldRejectZeroSettlementAmount() {

        ExpenseGroup group = mock(ExpenseGroup.class);
        User paidBy = mock(User.class);
        User paidTo = mock(User.class);

        when(paidBy.getId()).thenReturn(1L);
        when(paidTo.getId()).thenReturn(2L);

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setPaidByUserId(1L);
        request.setPaidToUserId(2L);
        request.setAmount(new BigDecimal("0.00"));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(paidBy));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(paidTo));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 2L))
                .thenReturn(true);

        assertThrows(
                InvalidExpenseException.class,
                () -> settlementService.createSettlement(10L, request)
        );

        verify(settlementRepository, never()).save(any(Settlement.class));
    }


    @Test
    void shouldRejectNegativeSettlementAmount() {

        ExpenseGroup group = mock(ExpenseGroup.class);
        User paidBy = mock(User.class);
        User paidTo = mock(User.class);

        when(paidBy.getId()).thenReturn(1L);
        when(paidTo.getId()).thenReturn(2L);

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setPaidByUserId(1L);
        request.setPaidToUserId(2L);
        request.setAmount(new BigDecimal("-100.00"));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(paidBy));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(paidTo));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 2L))
                .thenReturn(true);

        assertThrows(
                InvalidExpenseException.class,
                () -> settlementService.createSettlement(10L, request)
        );

        verify(settlementRepository, never()).save(any(Settlement.class));
    }



    @Test
    void shouldRejectSettlementAmountWithMoreThanTwoDecimalPlaces() {

        ExpenseGroup group = mock(ExpenseGroup.class);
        User paidBy = mock(User.class);
        User paidTo = mock(User.class);

        when(paidBy.getId()).thenReturn(1L);
        when(paidTo.getId()).thenReturn(2L);

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setPaidByUserId(1L);
        request.setPaidToUserId(2L);
        request.setAmount(new BigDecimal("100.123"));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(paidBy));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(paidTo));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 2L))
                .thenReturn(true);

        assertThrows(
                InvalidExpenseException.class,
                () -> settlementService.createSettlement(10L, request)
        );

        verify(settlementRepository, never()).save(any(Settlement.class));
    }


    @Test
    void shouldAcceptSettlementAmountWithTwoDecimalPlaces() {

        ExpenseGroup group = mock(ExpenseGroup.class);
        User paidBy = mock(User.class);
        User paidTo = mock(User.class);

        when(paidBy.getId()).thenReturn(1L);
        when(paidTo.getId()).thenReturn(2L);

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setPaidByUserId(1L);
        request.setPaidToUserId(2L);
        request.setAmount(new BigDecimal("100.50"));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(paidBy));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(paidTo));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 2L))
                .thenReturn(true);

        when(settlementRepository.save(any(Settlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SettlementResponse response =
                settlementService.createSettlement(10L, request);

        assertNotNull(response);

        verify(settlementRepository).save(
                argThat(settlement ->
                        settlement.getAmount().compareTo(
                                new BigDecimal("100.50")
                        ) == 0)
        );
    }



    @Test
    void shouldReturnEmptyListWhenGroupHasNoSettlements() {

        when(groupRepository.existsById(10L))
                .thenReturn(true);

        when(settlementRepository.findByGroup_Id(10L))
                .thenReturn(List.of());

        List<SettlementResponse> response =
                settlementService.getGroupSettlements(10L);

        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(settlementRepository).findByGroup_Id(10L);
    }

    @Test
    void shouldThrowExceptionWhenGettingSettlementsForNonexistentGroup() {

        when(groupRepository.existsById(10L))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> settlementService.getGroupSettlements(10L)
        );

        verify(settlementRepository, never()).findByGroup_Id(anyLong());
    }

    @Test
    void shouldReturnGroupSettlements() {

        ExpenseGroup group = mock(ExpenseGroup.class);

        User user1 = mock(User.class);
        User user2 = mock(User.class);
        User user3 = mock(User.class);

        when(user1.getId()).thenReturn(1L);
        when(user2.getId()).thenReturn(2L);
        when(user3.getId()).thenReturn(3L);

        Settlement settlement1 = new Settlement(
                group,
                user1,
                user2,
                new BigDecimal("100.00")
        );

        Settlement settlement2 = new Settlement(
                group,
                user2,
                user3,
                new BigDecimal("250.50")
        );

        when(groupRepository.existsById(10L))
                .thenReturn(true);

        when(settlementRepository.findByGroup_Id(10L))
                .thenReturn(List.of(settlement1, settlement2));

        List<SettlementResponse> response =
                settlementService.getGroupSettlements(10L);

        assertNotNull(response);
        assertEquals(2, response.size());

        assertNotNull(response.get(0));
        assertNotNull(response.get(1));

        verify(groupRepository).existsById(10L);
        verify(settlementRepository).findByGroup_Id(10L);
    }
}

