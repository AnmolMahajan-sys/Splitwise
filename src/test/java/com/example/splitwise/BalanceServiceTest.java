package com.example.splitwise;

import com.example.splitwise.dto.BalanceResponse;
import com.example.splitwise.entity.*;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.repository.*;
import com.example.splitwise.service.BalanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BalanceServiceTest {

    @Mock
    private ExpenseGroupRepository groupRepository;

    @Mock
    private GroupMemberRepository memberRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseSplitRepository splitRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private BalanceService balanceService;

    @Test
    void balanceShouldBeZeroAfterFullSettlement() {

        ExpenseGroup group=org.mockito.Mockito.mock(ExpenseGroup.class);

        User anmol =  org.mockito.Mockito.mock(User.class);
        User rahul =  org.mockito.Mockito.mock(User.class);

        when(anmol.getId()).thenReturn(1L);
        when(anmol.getName()).thenReturn("Anmol");

        when(rahul.getId()).thenReturn(2L);
        when(rahul.getName()).thenReturn("Rahul");

        GroupMember anmolMember = org.mockito.Mockito.mock(GroupMember.class);
        GroupMember rahulMember = org.mockito.Mockito.mock(GroupMember.class);

        when(anmolMember.getUser()).thenReturn(anmol);
        when(rahulMember.getUser()).thenReturn(rahul);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(memberRepository.findByGroup_Id(10L)).thenReturn(List.of(anmolMember, rahulMember));

        Expense expense = org.mockito.Mockito.mock(Expense.class);

        when(expense.getId()).thenReturn(100L);
        when(expense.getPaidBy()).thenReturn(anmol);
        when(expense.getAmount()).thenReturn(new BigDecimal("900.00"));

        when(expenseRepository.findByGroup_Id(10L)).thenReturn(List.of(expense));

        ExpenseSplit anmolSplit = org.mockito.Mockito.mock(ExpenseSplit.class);
        ExpenseSplit rahulSplit = org.mockito.Mockito.mock(ExpenseSplit.class);

        when(anmolSplit.getUser()).thenReturn(anmol);
        when(anmolSplit.getShareamount()).thenReturn(new BigDecimal("450.00"));

        when(rahulSplit.getUser()).thenReturn(rahul);
        when(rahulSplit.getShareamount()).thenReturn(new BigDecimal("450.00"));

        when(splitRepository.findByExpense_IdIn(List.of(100L))).thenReturn(List.of(anmolSplit, rahulSplit));

        Settlement settlement = org.mockito.Mockito.mock(Settlement.class);

        when(settlement.getPaidBy()).thenReturn(rahul);
        when(settlement.getPaidTo()).thenReturn(anmol);
        when(settlement.getAmount()).thenReturn(new BigDecimal("450.00"));
        when(settlementRepository.findByGroup_Id(10L)).thenReturn(List.of(settlement));

        List<BalanceResponse> balances=balanceService.getGroupBalances(10L);
        assertEquals(2,balances.size());

        assertEquals(new BigDecimal("0.00"),balances.get(0).getNetBalance());
        assertEquals(new BigDecimal("0.00"),balances.get(1).getNetBalance());

        verify(groupRepository).findById(10L);
        verify(memberRepository).findByGroup_Id(10L);
        verify(expenseRepository).findByGroup_Id(10L);
        verify(settlementRepository).findByGroup_Id(10L);

    }

    @Test
    void shouldThrowExceptionWhenGroupDoesNotExist() {
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->balanceService.getGroupBalances(999L));
    }

    @Test
    void shouldThrowExceptionWhenExpensePayerIsNotGroupMember() {
        ExpenseGroup group=org.mockito.Mockito.mock(ExpenseGroup.class);
        User anmol=  org.mockito.Mockito.mock(User.class);
        User outsider=  org.mockito.Mockito.mock(User.class);

        when(anmol.getId()).thenReturn(1L);


        when(outsider.getId()).thenReturn(3L);

        GroupMember member=org.mockito.Mockito.mock(GroupMember.class);

        when(member.getUser()).thenReturn(anmol);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(memberRepository.findByGroup_Id(10L)).thenReturn(List.of(member));

        Expense expense = org.mockito.Mockito.mock(Expense.class);


        when(expense.getPaidBy()).thenReturn(outsider);

        when(expenseRepository.findByGroup_Id(10L)).thenReturn(List.of(expense));

        assertThrows(
                IllegalStateException.class,()->balanceService.getGroupBalances(10L)
        );
    }

    @Test
    void shouldCalculateBalancesCorrectlyForUnequalSplits(){

        ExpenseGroup group=org.mockito.Mockito.mock(ExpenseGroup.class);

        User anmol=  org.mockito.Mockito.mock(User.class);
        User rahul=  org.mockito.Mockito.mock(User.class);
        User priya=  org.mockito.Mockito.mock(User.class);

        when(anmol.getId()).thenReturn(1L);
        when(anmol.getName()).thenReturn("Anmol");

        when(rahul.getId()).thenReturn(2L);
        when(rahul.getName()).thenReturn("Rahul");

        when(priya.getId()).thenReturn(3L);
        when(priya.getName()).thenReturn("Priya");

        GroupMember anmolMember = org.mockito.Mockito.mock(GroupMember.class);
        GroupMember rahulMember = org.mockito.Mockito.mock(GroupMember.class);
        GroupMember priyaMember = org.mockito.Mockito.mock(GroupMember.class);

        when(anmolMember.getUser()).thenReturn(anmol);
        when(rahulMember.getUser()).thenReturn(rahul);
        when(priyaMember.getUser()).thenReturn(priya);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(memberRepository.findByGroup_Id(10L)).thenReturn(List.of(anmolMember, rahulMember, priyaMember));

        Expense expense = org.mockito.Mockito.mock(Expense.class);
        when(expense.getId()).thenReturn(100L);
        when(expense.getPaidBy()).thenReturn(anmol);
        when(expense.getAmount()).thenReturn(new BigDecimal("1000.00"));

        when(expenseRepository.findByGroup_Id(10L)).thenReturn(List.of(expense));

        ExpenseSplit anmolSplit = org.mockito.Mockito.mock(ExpenseSplit.class);
        ExpenseSplit rahulSplit = org.mockito.Mockito.mock(ExpenseSplit.class);
        ExpenseSplit priyaSplit = org.mockito.Mockito.mock(ExpenseSplit.class);

        when(anmolSplit.getUser()).thenReturn(anmol);
        when(anmolSplit.getShareamount()).thenReturn(new BigDecimal("500.00"));

        when(rahulSplit.getUser()).thenReturn(rahul);
        when(rahulSplit.getShareamount()).thenReturn(new BigDecimal("300.00"));

        when(priyaSplit.getUser()).thenReturn(priya);
        when(priyaSplit.getShareamount()).thenReturn(new BigDecimal("200.00"));

        when(splitRepository.findByExpense_IdIn(List.of(100L))).thenReturn(List.of(
                anmolSplit,rahulSplit,priyaSplit
        ));

        when(settlementRepository.findByGroup_Id(10L)).thenReturn(List.of());

        List<BalanceResponse> balances=balanceService.getGroupBalances(10L);

        assertEquals(3,balances.size());

        assertEquals(new BigDecimal("500.00"),balances.get(0).getNetBalance());
        assertEquals(new BigDecimal("-300.00"),balances.get(1).getNetBalance());
        assertEquals(new BigDecimal("-200.00"),balances.get(2).getNetBalance());

        BigDecimal total= balances.stream().map(BalanceResponse::getNetBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("0.00"),total);
    }

    @Test
    void shouldCalculateBalancesCorrectlyForMultipleExpenses() {


        ExpenseGroup group = org.mockito.Mockito.mock(ExpenseGroup.class);


        User anmol = org.mockito.Mockito.mock(User.class);
        User rahul = org.mockito.Mockito.mock(User.class);
        User priya = org.mockito.Mockito.mock(User.class);

        when(anmol.getId()).thenReturn(1L);
        when(anmol.getName()).thenReturn("Anmol");

        when(rahul.getId()).thenReturn(2L);
        when(rahul.getName()).thenReturn("Rahul");

        when(priya.getId()).thenReturn(3L);
        when(priya.getName()).thenReturn("Priya");


        GroupMember anmolMember = org.mockito.Mockito.mock(GroupMember.class);
        GroupMember rahulMember = org.mockito.Mockito.mock(GroupMember.class);
        GroupMember priyaMember = org.mockito.Mockito.mock(GroupMember.class);

        when(anmolMember.getUser()).thenReturn(anmol);
        when(rahulMember.getUser()).thenReturn(rahul);
        when(priyaMember.getUser()).thenReturn(priya);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(memberRepository.findByGroup_Id(10L))
                .thenReturn(List.of(anmolMember, rahulMember, priyaMember));


        Expense expense1 = org.mockito.Mockito.mock(Expense.class);
        when(expense1.getId()).thenReturn(100L);
        when(expense1.getPaidBy()).thenReturn(anmol);
        when(expense1.getAmount()).thenReturn(new BigDecimal("1000.00"));

        Expense expense2 = org.mockito.Mockito.mock(Expense.class);
        when(expense2.getId()).thenReturn(200L);
        when(expense2.getPaidBy()).thenReturn(rahul);
        when(expense2.getAmount()).thenReturn(new BigDecimal("600.00"));

        when(expenseRepository.findByGroup_Id(10L))
                .thenReturn(List.of(expense1, expense2));


        ExpenseSplit split1Anmol = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(split1Anmol.getUser()).thenReturn(anmol);
        when(split1Anmol.getShareamount()).thenReturn(new BigDecimal("400.00"));

        ExpenseSplit split1Rahul = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(split1Rahul.getUser()).thenReturn(rahul);
        when(split1Rahul.getShareamount()).thenReturn(new BigDecimal("300.00"));

        ExpenseSplit split1Priya = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(split1Priya.getUser()).thenReturn(priya);
        when(split1Priya.getShareamount()).thenReturn(new BigDecimal("300.00"));


        ExpenseSplit split2Anmol = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(split2Anmol.getUser()).thenReturn(anmol);
        when(split2Anmol.getShareamount()).thenReturn(new BigDecimal("0.00"));

        ExpenseSplit split2Rahul = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(split2Rahul.getUser()).thenReturn(rahul);
        when(split2Rahul.getShareamount()).thenReturn(new BigDecimal("0.00"));

        ExpenseSplit split2Priya = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(split2Priya.getUser()).thenReturn(priya);
        when(split2Priya.getShareamount()).thenReturn(new BigDecimal("600.00"));

        when(splitRepository.findByExpense_IdIn(List.of(100L, 200L)))
                .thenReturn(List.of(
                        split1Anmol, split1Rahul, split1Priya,
                        split2Anmol, split2Rahul, split2Priya
                ));


        when(settlementRepository.findByGroup_Id(10L))
                .thenReturn(List.of());


        List<BalanceResponse> balances = balanceService.getGroupBalances(10L);


        assertEquals(3, balances.size());

        assertEquals(
                new BigDecimal("600.00"),
                balances.get(0).getNetBalance()
        );

        assertEquals(
                new BigDecimal("300.00"),
                balances.get(1).getNetBalance()
        );

        assertEquals(
                new BigDecimal("-900.00"),
                balances.get(2).getNetBalance()
        );


        BigDecimal total = balances.stream()
                .map(BalanceResponse::getNetBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("0.00"), total);
    }

    @Test
    void shouldThrowExceptionWhenExpenseParticipantIsNotGroupMember() {

        ExpenseGroup group = org.mockito.Mockito.mock(ExpenseGroup.class);

        User anmol = org.mockito.Mockito.mock(User.class);
        User outsider = org.mockito.Mockito.mock(User.class);

        when(anmol.getId()).thenReturn(1L);


        when(outsider.getId()).thenReturn(3L);


        GroupMember anmolMember = org.mockito.Mockito.mock(GroupMember.class);
        when(anmolMember.getUser()).thenReturn(anmol);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(memberRepository.findByGroup_Id(10L))
                .thenReturn(List.of(anmolMember));

        Expense expense = org.mockito.Mockito.mock(Expense.class);
        when(expense.getId()).thenReturn(100L);
        when(expense.getPaidBy()).thenReturn(anmol);
        when(expense.getAmount()).thenReturn(new BigDecimal("100.00"));

        when(expenseRepository.findByGroup_Id(10L))
                .thenReturn(List.of(expense));

        ExpenseSplit validSplit = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(validSplit.getUser()).thenReturn(anmol);
        when(validSplit.getShareamount()).thenReturn(new BigDecimal("50.00"));

        ExpenseSplit invalidSplit = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(invalidSplit.getUser()).thenReturn(outsider);

        when(splitRepository.findByExpense_IdIn(List.of(100L)))
                .thenReturn(List.of(validSplit, invalidSplit));

        assertThrows(
                IllegalStateException.class,
                () -> balanceService.getGroupBalances(10L)
        );
    }

    @Test
    void shouldThrowExceptionWhenSettlementPayerIsNotGroupMember() {

        ExpenseGroup group = org.mockito.Mockito.mock(ExpenseGroup.class);

        User anmol = org.mockito.Mockito.mock(User.class);
        User outsider = org.mockito.Mockito.mock(User.class);

        when(anmol.getId()).thenReturn(1L);
        when(outsider.getId()).thenReturn(3L);

        GroupMember anmolMember = org.mockito.Mockito.mock(GroupMember.class);
        when(anmolMember.getUser()).thenReturn(anmol);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(memberRepository.findByGroup_Id(10L))
                .thenReturn(List.of(anmolMember));

        when(expenseRepository.findByGroup_Id(10L))
                .thenReturn(List.of());

        Settlement settlement = org.mockito.Mockito.mock(Settlement.class);
        when(settlement.getPaidBy()).thenReturn(outsider);
        when(settlement.getPaidTo()).thenReturn(anmol);

        when(settlementRepository.findByGroup_Id(10L))
                .thenReturn(List.of(settlement));

        assertThrows(
                IllegalStateException.class,
                () -> balanceService.getGroupBalances(10L)
        );
    }

    @Test
    void shouldThrowExceptionWhenSettlementReceiverIsNotGroupMember() {

        ExpenseGroup group = org.mockito.Mockito.mock(ExpenseGroup.class);

        User anmol = org.mockito.Mockito.mock(User.class);
        User outsider = org.mockito.Mockito.mock(User.class);

        when(anmol.getId()).thenReturn(1L);
        when(outsider.getId()).thenReturn(3L);

        GroupMember anmolMember = org.mockito.Mockito.mock(GroupMember.class);
        when(anmolMember.getUser()).thenReturn(anmol);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(memberRepository.findByGroup_Id(10L))
                .thenReturn(List.of(anmolMember));

        when(expenseRepository.findByGroup_Id(10L))
                .thenReturn(List.of());

        Settlement settlement = org.mockito.Mockito.mock(Settlement.class);
        when(settlement.getPaidBy()).thenReturn(anmol);
        when(settlement.getPaidTo()).thenReturn(outsider);
        when(settlement.getAmount()).thenReturn(new BigDecimal("50.00"));

        when(settlementRepository.findByGroup_Id(10L))
                .thenReturn(List.of(settlement));

        assertThrows(
                IllegalStateException.class,
                () -> balanceService.getGroupBalances(10L)
        );
    }

    @Test
    void shouldReturnZeroBalancesWhenGroupHasNoExpensesOrSettlements() {

        ExpenseGroup group = org.mockito.Mockito.mock(ExpenseGroup.class);

        User anmol = org.mockito.Mockito.mock(User.class);
        User rahul = org.mockito.Mockito.mock(User.class);

        when(anmol.getId()).thenReturn(1L);
        when(anmol.getName()).thenReturn("Anmol");

        when(rahul.getId()).thenReturn(2L);
        when(rahul.getName()).thenReturn("Rahul");

        GroupMember anmolMember = org.mockito.Mockito.mock(GroupMember.class);
        GroupMember rahulMember = org.mockito.Mockito.mock(GroupMember.class);

        when(anmolMember.getUser()).thenReturn(anmol);
        when(rahulMember.getUser()).thenReturn(rahul);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(memberRepository.findByGroup_Id(10L))
                .thenReturn(List.of(anmolMember, rahulMember));

        when(expenseRepository.findByGroup_Id(10L))
                .thenReturn(List.of());

        when(settlementRepository.findByGroup_Id(10L))
                .thenReturn(List.of());

        List<BalanceResponse> balances = balanceService.getGroupBalances(10L);

        assertEquals(2, balances.size());

        assertEquals(
                new BigDecimal("0.00"),
                balances.get(0).getNetBalance()
        );

        assertEquals(
                new BigDecimal("0.00"),
                balances.get(1).getNetBalance()
        );
    }

    @Test
    void shouldReturnEmptyListWhenGroupHasNoMembers() {

        ExpenseGroup group = org.mockito.Mockito.mock(ExpenseGroup.class);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(memberRepository.findByGroup_Id(10L))
                .thenReturn(List.of());

        when(expenseRepository.findByGroup_Id(10L))
                .thenReturn(List.of());

        when(settlementRepository.findByGroup_Id(10L))
                .thenReturn(List.of());

        List<BalanceResponse> balances = balanceService.getGroupBalances(10L);

        assertNotNull(balances);
        assertTrue(balances.isEmpty());
    }

    @Test
    void shouldReduceBalancesAfterPartialSettlement() {


        ExpenseGroup group = org.mockito.Mockito.mock(ExpenseGroup.class);

        User anmol = org.mockito.Mockito.mock(User.class);
        User rahul = org.mockito.Mockito.mock(User.class);

        when(anmol.getId()).thenReturn(1L);
        when(anmol.getName()).thenReturn("Anmol");

        when(rahul.getId()).thenReturn(2L);
        when(rahul.getName()).thenReturn("Rahul");

        GroupMember anmolMember = org.mockito.Mockito.mock(GroupMember.class);
        GroupMember rahulMember = org.mockito.Mockito.mock(GroupMember.class);

        when(anmolMember.getUser()).thenReturn(anmol);
        when(rahulMember.getUser()).thenReturn(rahul);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(memberRepository.findByGroup_Id(10L))
                .thenReturn(List.of(anmolMember, rahulMember));

        Expense expense = org.mockito.Mockito.mock(Expense.class);

        when(expense.getId()).thenReturn(100L);
        when(expense.getPaidBy()).thenReturn(rahul);
        when(expense.getAmount()).thenReturn(new BigDecimal("200.00"));

        when(expenseRepository.findByGroup_Id(10L))
                .thenReturn(List.of(expense));

        ExpenseSplit anmolSplit = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(anmolSplit.getUser()).thenReturn(anmol);
        when(anmolSplit.getShareamount()).thenReturn(new BigDecimal("200.00"));

        ExpenseSplit rahulSplit = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(rahulSplit.getUser()).thenReturn(rahul);
        when(rahulSplit.getShareamount()).thenReturn(new BigDecimal("0.00"));

        when(splitRepository.findByExpense_IdIn(List.of(100L)))
                .thenReturn(List.of(anmolSplit, rahulSplit));

        Settlement settlement = org.mockito.Mockito.mock(Settlement.class);

        when(settlement.getPaidBy()).thenReturn(anmol);
        when(settlement.getPaidTo()).thenReturn(rahul);
        when(settlement.getAmount()).thenReturn(new BigDecimal("100.00"));

        when(settlementRepository.findByGroup_Id(10L))
                .thenReturn(List.of(settlement));


        List<BalanceResponse> balances = balanceService.getGroupBalances(10L);


        assertEquals(2, balances.size());

        assertEquals(
                new BigDecimal("-100.00"),
                balances.get(0).getNetBalance()
        );

        assertEquals(
                new BigDecimal("100.00"),
                balances.get(1).getNetBalance()
        );

        BigDecimal total = balances.stream()
                .map(BalanceResponse::getNetBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("0.00"), total);
    }

    @Test
    void shouldClearBalancesAfterFullSettlement() {

        ExpenseGroup group = org.mockito.Mockito.mock(ExpenseGroup.class);

        User anmol = org.mockito.Mockito.mock(User.class);
        User rahul = org.mockito.Mockito.mock(User.class);

        when(anmol.getId()).thenReturn(1L);
        when(anmol.getName()).thenReturn("Anmol");

        when(rahul.getId()).thenReturn(2L);
        when(rahul.getName()).thenReturn("Rahul");

        GroupMember anmolMember = org.mockito.Mockito.mock(GroupMember.class);
        GroupMember rahulMember = org.mockito.Mockito.mock(GroupMember.class);

        when(anmolMember.getUser()).thenReturn(anmol);
        when(rahulMember.getUser()).thenReturn(rahul);

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(memberRepository.findByGroup_Id(10L))
                .thenReturn(List.of(anmolMember, rahulMember));

        Expense expense = org.mockito.Mockito.mock(Expense.class);

        when(expense.getId()).thenReturn(100L);
        when(expense.getPaidBy()).thenReturn(rahul);
        when(expense.getAmount()).thenReturn(new BigDecimal("200.00"));

        when(expenseRepository.findByGroup_Id(10L))
                .thenReturn(List.of(expense));

        ExpenseSplit anmolSplit = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(anmolSplit.getUser()).thenReturn(anmol);
        when(anmolSplit.getShareamount()).thenReturn(new BigDecimal("200.00"));

        ExpenseSplit rahulSplit = org.mockito.Mockito.mock(ExpenseSplit.class);
        when(rahulSplit.getUser()).thenReturn(rahul);
        when(rahulSplit.getShareamount()).thenReturn(new BigDecimal("0.00"));

        when(splitRepository.findByExpense_IdIn(List.of(100L)))
                .thenReturn(List.of(anmolSplit, rahulSplit));

        Settlement settlement = org.mockito.Mockito.mock(Settlement.class);

        when(settlement.getPaidBy()).thenReturn(anmol);
        when(settlement.getPaidTo()).thenReturn(rahul);
        when(settlement.getAmount()).thenReturn(new BigDecimal("200.00"));

        when(settlementRepository.findByGroup_Id(10L))
                .thenReturn(List.of(settlement));

        List<BalanceResponse> balances = balanceService.getGroupBalances(10L);

        assertEquals(2, balances.size());

        assertEquals(
                new BigDecimal("0.00"),
                balances.get(0).getNetBalance()
        );

        assertEquals(
                new BigDecimal("0.00"),
                balances.get(1).getNetBalance()
        );
    }
}
