package com.example.splitwise.service;

import com.example.splitwise.dto.BalanceResponse;
import com.example.splitwise.entity.*;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.repository.ExpenseGroupRepository;
import com.example.splitwise.repository.ExpenseRepository;
import com.example.splitwise.repository.ExpenseSplitRepository;
import com.example.splitwise.repository.GroupMemberRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BalanceService {

    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;

    public BalanceService(ExpenseGroupRepository groupRepository, GroupMemberRepository memberRepository, ExpenseRepository expenseRepository, ExpenseSplitRepository splitRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
    }

    @Transactional(readOnly=true)
    public List<BalanceResponse> getGroupBalances(Long groupId){
        ExpenseGroup group=groupRepository.findById(groupId).orElseThrow(()-> new ResourceNotFoundException(
                "Group not found with ID: "+groupId
        ));
        List<GroupMember> members=memberRepository.findByGroup_Id(groupId);

        Map<Long, BigDecimal> balances=new HashMap<>();

        for(GroupMember member:members){
            balances.put(member.getUser().getId(),BigDecimal.ZERO.setScale(2));

        }
        List<Expense> expenses=expenseRepository.findByGroup_Id(groupId);
        if(!expenses.isEmpty()){
            List<Long> expenseIds=expenses.stream().map(Expense::getId).toList();

            List<ExpenseSplit> splits=splitRepository.findByExpense_IdIn(expenseIds);

            for(Expense expense:expenses){
                Long payerId=expense.getPaidBy().getId();

                balances.compute(payerId,(id, current)->{
                    if(current==null){
                        throw new IllegalStateException(
                                "Expense payer is not a group member: "+id
                        );

                    }
                   return current.add(expense.getAmount());
                });
            }
            for(ExpenseSplit split:splits){
                Long userId=split.getUser().getId();

                balances.compute(userId,(id,current)->
                {
                    if(current==null){
                        throw new IllegalStateException(
                                "Expense participant is not a group member: "+id
                        );
                    }
                    return current.subtract(split.getShareamount());
                });
            }
        }
        List<BalanceResponse> result=new ArrayList<>();

        for(GroupMember member:members){
            User user=member.getUser();

            result.add(new BalanceResponse(
                    user.getId(),
                    user.getName(),
                    balances.get(user.getId())
            ));
        }
        return result;
    }
}
