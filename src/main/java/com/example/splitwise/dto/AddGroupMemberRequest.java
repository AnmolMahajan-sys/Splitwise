package com.example.splitwise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AddGroupMemberRequest {

    @NotNull(message = "User ID is required")
    @Positive(message="User ID>0")
    private Long userId;

    public AddGroupMemberRequest() {}

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;

    }
}
