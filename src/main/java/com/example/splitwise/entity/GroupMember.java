package com.example.splitwise.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name="group_members",
        uniqueConstraints = @UniqueConstraint(
                name="uq_group_member",
                columnNames = {"group_id", "user_id"}
        )
)
public class GroupMember {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch=FetchType.LAZY,optional = false)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(name="joined_at", nullable = false)
    private Instant joinedAt;

    protected  GroupMember() {}
    public GroupMember(ExpenseGroup group, User user) {
        this.group = group;
        this.user = user;
    }

    @PrePersist
    void prePersist() {
        if(joinedAt == null) {
            joinedAt = Instant.now();
        }
    }
    public Long getId() {return id;}
    public ExpenseGroup getGroup() {return group;}
    public User getUser() {return user;}
}
