package com.example.splitwise;

import com.example.splitwise.dto.CreateExpenseRequest;
import com.example.splitwise.dto.ExpenseResponse;
import com.example.splitwise.entity.*;
import com.example.splitwise.exception.InvalidExpenseException;
import com.example.splitwise.repository.*;
import com.example.splitwise.service.ExpenseService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExpenseServiceTest {

    private static ExpenseGroupRepository groupRepository;
    private UserRepository userRepository;
    private GroupMemberRepository memberRepository;
    private static ExpenseRepository expenseRepository;
    private ExpenseSplitRepository splitRepository;

    private static ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        groupRepository = mock(ExpenseGroupRepository.class);
        userRepository = mock(UserRepository.class);
        memberRepository = mock(GroupMemberRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        splitRepository = mock(ExpenseSplitRepository.class);

        expenseService = new ExpenseService(
                groupRepository,
                userRepository,
                memberRepository,
                expenseRepository,
                splitRepository
        );
    }

    @Test
    void shouldCreateExpenseAndSplitAmountCorrectly() {
        // Arrange: create group and users
        ExpenseGroup group = mock(ExpenseGroup.class);
        when(group.getId()).thenReturn(10L);

        User payer = mock(User.class);
        when(payer.getId()).thenReturn(1L);
        when(payer.getName()).thenReturn("Anmol");

        User rahul = mock(User.class);
        when(rahul.getId()).thenReturn(2L);
        when(rahul.getName()).thenReturn("Rahul");

        User priya = mock(User.class);
        when(priya.getId()).thenReturn(3L);
        when(priya.getName()).thenReturn("Priya");

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(rahul));
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(priya));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 2L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 3L))
                .thenReturn(true);

        // Arrange: prepare the request
        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(1L, 2L, 3L));

        // Simulate saving the expense
        when(expenseRepository.save(any(Expense.class)))
                .thenAnswer(invocation -> {
                    Expense expense = invocation.getArgument(0);
                    // Expense ID is assumed to be assigned by persistence.
                    // If the entity has no setId(), see the note below.
                    return expense;
                });

        // Simulate saving the splits
        when(splitRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ExpenseResponse response =
                expenseService.createExpense(10L, request);

        // Assert: check the response
        assertNotNull(response);
        assertEquals(10L, response.getGroupId());
        assertEquals("Dinner", response.getDescription());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals(1L, response.getPaidByUserId());
        assertEquals("Anmol", response.getPaidByName());

        // Assert: check the three participant shares
        assertEquals(3, response.getSplits().size());

        assertEquals(
                new BigDecimal("33.34"),
                response.getSplits().get(0).getShareAmount()
        );
        assertEquals(
                new BigDecimal("33.33"),
                response.getSplits().get(1).getShareAmount()
        );
        assertEquals(
                new BigDecimal("33.33"),
                response.getSplits().get(2).getShareAmount()
        );

        // All three shares must add up to exactly ₹100.00
        BigDecimal totalShares = response.getSplits().stream()
                .map(split -> split.getShareAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("100.00"), totalShares);

        verify(expenseRepository).save(any(Expense.class));
        verify(splitRepository).saveAll(any());
    }


    @Test
    void shouldRejectParticipantWhoIsNotGroupMember() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        User payer = mock(User.class);
        when(payer.getId()).thenReturn(1L);
        when(payer.getName()).thenReturn("Anmol");

        User outsider = mock(User.class);
        when(outsider.getId()).thenReturn(99L);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));
        when(userRepository.findById(99L))
                .thenReturn(Optional.of(outsider));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 2L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 3L))
                .thenReturn(true);


        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 99L))
                .thenReturn(false);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(99L));

        assertThrows(
                com.example.splitwise.exception.InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
        verify(splitRepository, never()).saveAll(any());
    }


    @Test
    void shouldRejectDuplicateParticipants() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        User payer = mock(User.class);
        when(payer.getId()).thenReturn(1L);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));
        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(1L, 2L, 2L));

        assertThrows(
                com.example.splitwise.exception.InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
        verify(splitRepository, never()).saveAll(any());
    }


    @Test
    void shouldRejectExpenseWithNoParticipants() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        User payer = mock(User.class);
        when(payer.getId()).thenReturn(1L);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));
        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of());

        assertThrows(
                com.example.splitwise.exception.InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
        verify(splitRepository, never()).saveAll(any());
    }


    @Test
    void shouldRejectNullParticipantList() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        User payer = mock(User.class);
        when(payer.getId()).thenReturn(1L);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));
        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(null);

        assertThrows(
                com.example.splitwise.exception.InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
        verify(splitRepository, never()).saveAll(any());
    }



    @Test
    void shouldThrowExceptionWhenGroupDoesNotExist() {
        when(groupRepository.findById(10L))
                .thenReturn(Optional.empty());

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(1L));

        assertThrows(
                com.example.splitwise.exception.ResourceNotFoundException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(userRepository, never()).findById(any());
        verify(expenseRepository, never()).save(any(Expense.class));
        verify(splitRepository, never()).saveAll(any());
    }



    @Test
    void shouldThrowExceptionWhenPayerDoesNotExist() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(99L);
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(99L));

        assertThrows(
                com.example.splitwise.exception.ResourceNotFoundException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(memberRepository, never())
                .existsByGroup_IdAndUser_Id(any(), any());

        verify(expenseRepository, never()).save(any(Expense.class));
        verify(splitRepository, never()).saveAll(any());
    }


    @Test
    void shouldThrowExceptionWhenParticipantDoesNotExist() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        User payer = mock(User.class);
        when(payer.getId()).thenReturn(1L);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));
        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        // Participant 99 does not exist.
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(99L));

        assertThrows(
                com.example.splitwise.exception.ResourceNotFoundException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
        verify(splitRepository, never()).saveAll(any());
    }


    @Test
    void shouldReturnEmptyListWhenGroupHasNoExpenses() {
        when(groupRepository.existsById(10L))
                .thenReturn(true);

        when(expenseRepository.findByGroup_Id(10L))
                .thenReturn(List.of());

        List<ExpenseResponse> responses =
                expenseService.getGroupExpenses(10L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(expenseRepository).findByGroup_Id(10L);
        verify(splitRepository, never()).findByExpense_IdIn(anyList());
    }


    @Test
    void shouldThrowExceptionWhenListingExpensesForNonexistentGroup() {
        when(groupRepository.existsById(10L))
                .thenReturn(false);

        assertThrows(
                com.example.splitwise.exception.ResourceNotFoundException.class,
                () -> expenseService.getGroupExpenses(10L)
        );

        verify(expenseRepository, never()).findByGroup_Id(any());
        verify(splitRepository, never()).findByExpense_IdIn(anyList());
    }


    @Test
    void shouldReturnExpensesWithTheirCorrectSplits() {
        // Arrange: create a group and two expenses
        ExpenseGroup group = mock(ExpenseGroup.class);
        when(group.getId()).thenReturn(10L);

        User payer = mock(User.class);
        when(payer.getId()).thenReturn(1L);
        when(payer.getName()).thenReturn("Anmol");

        User rahul = mock(User.class);
        when(rahul.getId()).thenReturn(2L);
        when(rahul.getName()).thenReturn("Rahul");

        Expense expense1 = mock(Expense.class);
        when(expense1.getId()).thenReturn(101L);
        when(expense1.getGroup()).thenReturn(group);
        when(expense1.getPaidBy()).thenReturn(payer);
        when(expense1.getDescription()).thenReturn("Dinner");
        when(expense1.getAmount()).thenReturn(new BigDecimal("100.00"));

        Expense expense2 = mock(Expense.class);
        when(expense2.getId()).thenReturn(102L);
        when(expense2.getGroup()).thenReturn(group);
        when(expense2.getPaidBy()).thenReturn(rahul);
        when(expense2.getDescription()).thenReturn("Taxi");
        when(expense2.getAmount()).thenReturn(new BigDecimal("60.00"));

        when(groupRepository.existsById(10L)).thenReturn(true);
        when(expenseRepository.findByGroup_Id(10L))
                .thenReturn(List.of(expense1, expense2));

        // Splits for expense 101
        ExpenseSplit split1 = mock(ExpenseSplit.class);
        when(split1.getExpense()).thenReturn(expense1);
        when(split1.getUser()).thenReturn(payer);
        when(split1.getShareamount()).thenReturn(new BigDecimal("50.00"));

        ExpenseSplit split2 = mock(ExpenseSplit.class);
        when(split2.getExpense()).thenReturn(expense1);
        when(split2.getUser()).thenReturn(rahul);
        when(split2.getShareamount()).thenReturn(new BigDecimal("50.00"));

        // Splits for expense 102
        ExpenseSplit split3 = mock(ExpenseSplit.class);
        when(split3.getExpense()).thenReturn(expense2);
        when(split3.getUser()).thenReturn(payer);
        when(split3.getShareamount()).thenReturn(new BigDecimal("30.00"));

        ExpenseSplit split4 = mock(ExpenseSplit.class);
        when(split4.getExpense()).thenReturn(expense2);
        when(split4.getUser()).thenReturn(rahul);
        when(split4.getShareamount()).thenReturn(new BigDecimal("30.00"));

        when(splitRepository.findByExpense_IdIn(List.of(101L, 102L)))
                .thenReturn(List.of(split1, split2, split3, split4));

        // Act
        List<ExpenseResponse> responses =
                expenseService.getGroupExpenses(10L);

        // Assert: both expenses are returned
        assertEquals(2, responses.size());

        assertEquals(101L, responses.get(0).getId());
        assertEquals("Dinner", responses.get(0).getDescription());
        assertEquals(new BigDecimal("100.00"), responses.get(0).getAmount());
        assertEquals(2, responses.get(0).getSplits().size());

        assertEquals(102L, responses.get(1).getId());
        assertEquals("Taxi", responses.get(1).getDescription());
        assertEquals(new BigDecimal("60.00"), responses.get(1).getAmount());
        assertEquals(2, responses.get(1).getSplits().size());

        // Verify that splits were not mixed between expenses
        assertEquals(
                new BigDecimal("50.00"),
                responses.get(0).getSplits().get(0).getShareAmount()
        );
        assertEquals(
                new BigDecimal("30.00"),
                responses.get(1).getSplits().get(0).getShareAmount()
        );

        verify(splitRepository).findByExpense_IdIn(List.of(101L, 102L));
    }


    @Test
    void shouldRejectZeroExpenseAmount() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        User payer = mock(User.class);
        when(payer.getId()).thenReturn(1L);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));
        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 2L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 3L))
                .thenReturn(true);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("0.00"));
        request.setParticipantUserIds(List.of(1L));

        assertThrows(
                com.example.splitwise.exception.InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void shouldRejectNegativeExpenseAmount() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        User payer = mock(User.class);
        when(payer.getId()).thenReturn(1L);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));
        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("-50.00"));
        request.setParticipantUserIds(List.of(1L));

        assertThrows(
                com.example.splitwise.exception.InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
        verify(splitRepository, never()).saveAll(any());
    }


    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        User payer = mock(User.class);
        when(payer.getId()).thenReturn(1L);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));
        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 2L))
                .thenReturn(true);

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 3L))
                .thenReturn(true);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("100.123"));
        request.setParticipantUserIds(List.of(1L));

        assertThrows(
                com.example.splitwise.exception.InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
        verify(splitRepository, never()).saveAll(any());
    }

    @Test
    void shouldRejectBlankExpenseDescription() {
        ExpenseGroup group = mock(ExpenseGroup.class);
        User payer = mock(User.class);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("   ");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(1L));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        assertThrows(
                InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );
    }

    @Test
    void shouldRejectNullExpenseDescription() {
        ExpenseGroup group = mock(ExpenseGroup.class);
        User payer = mock(User.class);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription(null);
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(1L));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        assertThrows(
                InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
    }


    @Test
    void shouldRejectDescriptionLongerThan255Characters() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("a".repeat(256));
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(1L));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        assertThrows(
                InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
    }


    @Test
    void shouldRejectNullExpenseRequest() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        assertThrows(
                InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, null)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
    }


    @Test
    void shouldRejectNullPayerId() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setDescription("Lunch");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(1L));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        assertThrows(
                InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void shouldRejectNullExpenseAmount() {
        ExpenseGroup group = mock(ExpenseGroup.class);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Lunch");
        request.setAmount(null);
        request.setParticipantUserIds(List.of(1L));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        assertThrows(
                InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
    }


    @Test
    void shouldRejectPayerWhoIsNotGroupMember() {
        ExpenseGroup group = mock(ExpenseGroup.class);
        User payer = mock(User.class);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("Lunch");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(1L));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(false);

        assertThrows(
                InvalidExpenseException.class,
                () -> expenseService.createExpense(10L, request)
        );

        verify(expenseRepository, never()).save(any(Expense.class));
    }




    @Test
    void shouldTrimExpenseDescriptionBeforeSaving() {
        ExpenseGroup group = mock(ExpenseGroup.class);
        User payer = mock(User.class);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setPaidByUserId(1L);
        request.setDescription("  Lunch with friends  ");
        request.setAmount(new BigDecimal("100.00"));
        request.setParticipantUserIds(List.of(1L));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(payer));

        when(memberRepository.existsByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(true);

        when(expenseRepository.save(any(Expense.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(splitRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        expenseService.createExpense(10L, request);

        verify(expenseRepository).save(
                argThat(expense ->
                        "Lunch with friends".equals(expense.getDescription()))
        );
    }


    static class CreateExpenseRequestTest {

        private static ValidatorFactory factory;
        private static Validator validator;

        @BeforeAll
        static void setUp() {
            factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @AfterAll
        static void tearDown() {
            factory.close();
        }

        @Test
        void shouldRejectBlankDescription() {
            CreateExpenseRequest request = new CreateExpenseRequest();
            request.setDescription("   ");

            assertFalse(validator.validate(request).isEmpty());
        }

        @Test
        void shouldRejectDescriptionLongerThan255Characters() {
            CreateExpenseRequest request = new CreateExpenseRequest();
            request.setDescription("a".repeat(256));

            assertFalse(validator.validate(request).isEmpty());
        }


        @Test
        void shouldAcceptDescriptionWithExactly255Characters() {
            CreateExpenseRequest request = new CreateExpenseRequest();
            request.setDescription("a".repeat(255));

            assertTrue(
                    validator.validate(request).stream()
                            .noneMatch(error ->
                                    error.getPropertyPath().toString()
                                            .equals("description"))
            );
        }


    }
}

