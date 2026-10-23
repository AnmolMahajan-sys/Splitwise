package com.example.splitwise.controller;

import com.example.splitwise.dto.AddGroupMemberRequest;
import com.example.splitwise.dto.GroupMemberResponse;
import com.example.splitwise.service.GroupMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/members")
public class GroupMemberController {

    private final GroupMemberService memberService;

    public GroupMemberController(GroupMemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberResponse addMember(
            @PathVariable Long groupId,
            @Valid @RequestBody AddGroupMemberRequest request){
        return memberService.addMember(groupId, request);
    }

    @GetMapping
    public List<GroupMemberResponse> getMembers(@PathVariable Long groupId){
        return memberService.getMembers(groupId);
    }
}
