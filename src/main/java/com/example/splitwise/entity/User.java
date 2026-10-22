package com.example.splitwise.entity;

import jakarta.persistence.*;

import java.time.Instant;


@Entity
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length=100)
    private String name;

    @Column(nullable = false, unique = true, length=100)
    private String email;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    protected User() {}
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    @PrePersist
    void prePersist() {
        if(createdAt == null) {
            createdAt = Instant.now();
        }
    }
    public Long getId() {return id;}
    public String getName() {return name;}
    public String getEmail() {return email;}
}
