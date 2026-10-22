package com.example.splitwise.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="expense_groups")
public class ExpenseGroup {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch=FetchType.LAZY, optional = false)
    @JoinColumn(name="created_by", nullable = false)
    private User createdBy;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected  ExpenseGroup() {}
    public ExpenseGroup(String name, User createdBy){
        this.name = name;
        this.createdBy = createdBy;
    }

    @PrePersist
    void prePersist(){
        if(createdAt == null){
            createdAt = Instant.now();
        }
    }
    public Long getId() {return id;}
    public String getName() {return name;}
    public User getCreatedBy() {return createdBy;}
    public Instant getCreatedAt() {return createdAt;}
}
