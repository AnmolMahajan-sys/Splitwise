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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SettlementService {

    private final ExpenseGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository memberRepository;
    private final SettlementRepository settlementRepository;


    public SettlementService(ExpenseGroupRepository groupRepository, UserRepository userRepository, GroupMemberRepository memberRepository, SettlementRepository settlementRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public SettlementResponse createSettlement(
            Long groupId,
            CreateSettlementRequest request
    ){
        ExpenseGroup  group = groupRepository.findById(groupId).orElseThrow(()->new ResourceNotFoundException("Group not found with ID: "+groupId));

        User paidBy=userRepository.findById(
                request.getPaidByUserId()
        ).orElseThrow(()->new ResourceNotFoundException("Payer not found with ID: "+request.getPaidByUserId()));

        User paidTo=userRepository.findById(
                request.getPaidToUserId()
        ).orElseThrow(()->new ResourceNotFoundException("Reciever not found with ID: "+request.getPaidToUserId()));

        if(paidBy.getId().equals(paidTo.getId())){
            throw new InvalidExpenseException("Payer and reciever must be different users");
        }

        if (!memberRepository.existsByGroup_IdAndUser_Id(groupId, paidBy.getId())){
            throw new InvalidExpenseException("Payer is not member of this group");
        }

        if(!memberRepository.existsByGroup_IdAndUser_Id(groupId, paidTo.getId())){
            throw new InvalidExpenseException("Reciever is not member of this group");
        }

        BigDecimal amount;
        try{
            amount = request.getAmount().setScale(2, RoundingMode.UNNECESSARY);
        }catch (ArithmeticException ex){
            throw new InvalidExpenseException("Settlement amount cannot have more than 2 decimal places.");
        }

        if(amount.signum()<=0){
            throw new InvalidExpenseException("Settlement amount cannot be negative.");
        }

        Settlement settlement=new Settlement(group,paidBy,paidTo,amount);
        Settlement saved=settlementRepository.save(settlement);

        return SettlementResponse.from(saved);
    }

    @Transactional(readOnly=true)
    public List<SettlementResponse> getGroupSettlements(Long groupId){
        if(!groupRepository.existsById(groupId)){
            throw new ResourceNotFoundException("Group not found with ID: "+groupId);
        }
        return settlementRepository.findByGroup_Id(groupId).stream().map(SettlementResponse::from).toList();
    }

}
