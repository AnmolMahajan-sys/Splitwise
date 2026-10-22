package com.example.splitwise.dto;

import com.example.splitwise.entity.ExpenseGroup;

public class GroupResponse {

    private Long id;
    private String name;
    private Long createdByUserId;
    private String createdByName;

    public GroupResponse(Long id, String name, Long createdByUserId, String createdByName) {
        this.id = id;
        this.name = name;
        this.createdByUserId = createdByUserId;
        this.createdByName = createdByName;
    }

    public static GroupResponse from(ExpenseGroup group) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getCreatedBy().getId(),
                group.getCreatedBy().getName()

        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }
    public String getCreatedByName() {
        return createdByName;
    }
}
