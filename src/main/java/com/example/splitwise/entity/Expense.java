package com.example.splitwise.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="paid_by", nullable = false)
    private User paidBy;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 12,scale = 2)
    private BigDecimal amount;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    protected Expense() {}

    public Expense(
            ExpenseGroup group,
            User paidBy,
            String description,
            BigDecimal amount
    ){
        this.group = group;
        this.paidBy = paidBy;
        this.description=description;
        this.amount=amount;
    }

    @PrePersist
    void prePersist(){
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {return id;}
    public ExpenseGroup getGroup() {return group;}
    public User getPaidBy() {return paidBy;}
    public String getDescription() {return description;}
    public BigDecimal getAmount() {return amount;}
}
