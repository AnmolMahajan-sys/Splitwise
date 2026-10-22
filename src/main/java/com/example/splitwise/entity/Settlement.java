package com.example.splitwise.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch=FetchType.LAZY, optional = false)
    @JoinColumn(name="paid_by", nullable = false)
    private User paidBy;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="paid_to", nullable = false)
    private User paidTo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    protected  Settlement() {}

    public Settlement(
            ExpenseGroup group,
            User paidBy,
            User paidTo,
            BigDecimal amount
    ){
        this.group = group;
        this.paidBy = paidBy;
        this.paidTo = paidTo;
        this.amount = amount;
    }

    @PrePersist
    void prePersist(){
        if(createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public int getId() {return id;}
    public ExpenseGroup getGroup() {return group;}
    public User getPaidBy() {return paidBy;}
    public User getPaidTo() {return paidTo;}
    public BigDecimal getAmount() {return amount;}


}
