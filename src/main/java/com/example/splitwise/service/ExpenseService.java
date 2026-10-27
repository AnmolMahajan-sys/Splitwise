package com.example.splitwise.service;

import com.example.splitwise.dto.CreateExpenseRequest;
import com.example.splitwise.dto.ExpenseResponse;
import com.example.splitwise.entity.Expense;
import com.example.splitwise.entity.ExpenseGroup;
import com.example.splitwise.entity.ExpenseSplit;
import com.example.splitwise.entity.User;
import com.example.splitwise.exception.InvalidExpenseException;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class ExpenseService {

    private final ExpenseGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository memberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;

    public ExpenseService(ExpenseGroupRepository groupRepository, UserRepository userRepository, GroupMemberRepository memberRepository, ExpenseRepository expenseRepository, ExpenseSplitRepository splitRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
    }

    @Transactional
    public ExpenseResponse createExpense(
            Long groupId, CreateExpenseRequest request){

        if (request == null) {
            throw new InvalidExpenseException(
                    "Expense request cannot be null"
            );
        }

        if (request.getPaidByUserId() <= 0) {
            throw new InvalidExpenseException(
                    "Payer ID must be greater than zero"
            );
        }

        ExpenseGroup group = groupRepository.findById(groupId).orElseThrow(()->new ResourceNotFoundException(
                "Group not found with id " + groupId
        ));



        BigDecimal requestedAmount = request.getAmount();

        if (requestedAmount == null ||
                requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidExpenseException(
                    "Expense amount must be greater than zero"
            );
        }

        if (requestedAmount.scale() > 2) {
            throw new InvalidExpenseException(
                    "Expense amount must have at most two decimal places"
            );
        }

        String description = request.getDescription();

        if (description == null || description.isBlank()) {
            throw new InvalidExpenseException(
                    "Expense description cannot be blank"
            );
        }

        if (description.length() > 255) {
            throw new InvalidExpenseException(
                    "Expense description cannot exceed 255 characters"
            );
        }

        Long payerId=request.getPaidByUserId();
        User payer =userRepository.findById(payerId).orElseThrow(()->new ResourceNotFoundException(
                "User not found with id " + payerId
        ));

        if(!memberRepository.existsByGroup_IdAndUser_Id(groupId, payerId)){
            throw new InvalidExpenseException("User is not a member of this group");

        }
        List<Long> participantIds = request.getParticipantUserIds();

        if (participantIds == null || participantIds.isEmpty()) {
            throw new InvalidExpenseException("At least one participant is required");
        }

        Set<Long> uniqueParticipants = new HashSet<>(participantIds);

        if (uniqueParticipants.size() != participantIds.size()) {
            throw new InvalidExpenseException("Duplicate participants are not allowed");
        }

        List<User> partipants=new ArrayList<>();

        for(Long userId:participantIds){
            User partipant=userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException(
                    "User not found with id " + userId
            ));

            if(!memberRepository.existsByGroup_IdAndUser_Id(groupId, userId)){
                throw new InvalidExpenseException("User "+userId+" is not a member of group "+groupId);
            }
            partipants.add(partipant);
        }
        BigDecimal amount=requestedAmount.setScale(2, RoundingMode.UNNECESSARY);

        Expense expense=new Expense(group,payer,request.getDescription().trim(),amount);
        Expense savedExpense=expenseRepository.save(expense);

        List<BigDecimal> shares=calculateEqualShares(amount,partipants.size());

        List<ExpenseSplit> splits=new ArrayList<>();
        for(int i=0;i<partipants.size();i++){
            splits.add(new ExpenseSplit(savedExpense,partipants.get(i),shares.get(i)));
        }

        List<ExpenseSplit> savedSplits=splitRepository.saveAll(splits);
        return ExpenseResponse.from(savedExpense,savedSplits);
    }

    private List<BigDecimal> calculateEqualShares(BigDecimal amount,int participantsCount){
        long totalCents=amount.movePointRight(2).longValueExact();

        long baseShareCents=totalCents/participantsCount;
        long remainderCents=totalCents%participantsCount;

        List<BigDecimal> shares=new ArrayList<>();
        for(int i=0;i<participantsCount;i++){
            long cents=baseShareCents;

            if(i<remainderCents){
                cents++;
            }
            shares.add(BigDecimal.valueOf(cents,2));
        }
        return shares;
    }

    @Transactional(readOnly=true)
    public List<ExpenseResponse> getGroupExpenses(Long groupId){

        if(!groupRepository.existsById(groupId)){
            throw new ResourceNotFoundException("Group not found with id " + groupId);
        }

        List<Expense> expenses=expenseRepository.findByGroup_Id(groupId);
        if(expenses.isEmpty()){
            return List.of();
        }

        List<Long> expenseIds=expenses.stream().map(Expense::getId).toList();

        Map<Long, List<ExpenseSplit>> splitsByExpenseId=splitRepository.findByExpense_IdIn(expenseIds).stream().collect(Collectors.groupingBy(
                split -> split.getExpense().getId()
        ));

        return expenses.stream().map(expense-> ExpenseResponse.from(
                expense,
                splitsByExpenseId.getOrDefault(expense.getId(),List.of())
        )).toList();
    }

}
