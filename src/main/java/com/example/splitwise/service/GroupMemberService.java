package com.example.splitwise.service;

import com.example.splitwise.dto.AddGroupMemberRequest;
import com.example.splitwise.dto.GroupMemberResponse;
import com.example.splitwise.entity.ExpenseGroup;
import com.example.splitwise.entity.GroupMember;
import com.example.splitwise.entity.User;
import com.example.splitwise.exception.DuplicateGroupMembershipException;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.repository.ExpenseGroupRepository;
import com.example.splitwise.repository.GroupMemberRepository;
import com.example.splitwise.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupMemberService {

    private final ExpenseGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository memberRepository;

    public GroupMemberService(ExpenseGroupRepository groupRepository, UserRepository userRepository, GroupMemberRepository memberRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public GroupMemberResponse addMember(
            Long groupId,
            AddGroupMemberRequest request){
        ExpenseGroup group = groupRepository.findById(groupId).orElseThrow(()-> new ResourceNotFoundException("Group not found with ID: "+groupId));

        Long userId = request.getUserId();

        User user=userRepository.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User not found with ID: "+userId));

        if(memberRepository.existsByGroup_IdAndUser_Id(groupId,userId)){
            throw new DuplicateGroupMembershipException(
                    "User "+userId+" is already member of group "+groupId
            );
        }
        GroupMember member = new GroupMember(group,user);
        GroupMember savedMember = memberRepository.save(member);

        return GroupMemberResponse.from(savedMember);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getMembers(Long groupId){
        if(!groupRepository.existsById(groupId)){
            throw new ResourceNotFoundException("Group not found with ID: "+groupId);
        }

        return memberRepository.findByGroup_Id(groupId).stream().map(GroupMemberResponse::from).toList();
    }
}
