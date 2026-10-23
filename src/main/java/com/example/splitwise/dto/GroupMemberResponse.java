package com.example.splitwise.dto;

import com.example.splitwise.entity.GroupMember;

public class GroupMemberResponse {

    private Long membershipId;
    private Long groupId;
    private Long userId;
    private String name;
    private String email;

    public  GroupMemberResponse(
            Long membershipId,
            Long groupId,
            Long userId,
            String name,
            String email
    ) {
        this.membershipId = membershipId;
        this.groupId = groupId;
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public static GroupMemberResponse from(
            GroupMember member){
   return new GroupMemberResponse(member.getId(),
        member.getGroup().getId(),
        member.getUser().getId(),
        member.getUser().getName(),
        member.getUser().getEmail()
        );
    }

    public Long getMembershipId() {
        return membershipId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
