package com.example.splitwise.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name="expense_splits",
        uniqueConstraints = @UniqueConstraint(
                name="uq_expense_split_user",
                columnNames={"expense_id","user_id"}
        )
)
public class ExpenseSplit {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(
            name="share_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal shareamount;

    protected  ExpenseSplit() {}

    public ExpenseSplit(Expense expense, User user, BigDecimal shareamount) {
        this.expense = expense;
        this.user = user;
        this.shareamount = shareamount;
    }

    public Long getId() {return id;}
    public Expense getExpense() {return expense;}
    public User getUser() {return user;}
    public BigDecimal getShareamount() {return shareamount;}
}

