package com.example.splitwise.service;

import com.example.splitwise.dto.CreateGroupRequest;
import com.example.splitwise.dto.GroupResponse;
import com.example.splitwise.entity.ExpenseGroup;
import com.example.splitwise.entity.User;
import com.example.splitwise.exception.ResourceNotFoundException;
import com.example.splitwise.repository.ExpenseGroupRepository;
import com.example.splitwise.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class GroupService {

    private final ExpenseGroupRepository groupRepository;
    private final UserRepository userRepository;


    public GroupService(ExpenseGroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request){
        String groupName = request.getName().trim();

        User creator=userRepository.findById(request.getCreatedByUserId()).
                orElseThrow(()-> new ResourceNotFoundException(
                        "User not found with id " + request.getCreatedByUserId()
                ));

        ExpenseGroup group=new  ExpenseGroup(groupName,creator);
        ExpenseGroup savedGroup=groupRepository.save(group);

        return GroupResponse.from(savedGroup);
    }
}
