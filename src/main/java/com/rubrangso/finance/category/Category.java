package com.rubrangso.finance.category;

import com.rubrangso.finance.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a transaction category. System defaults have {@code user = null} and
 * {@code custom = false}. Per-user custom categories have {@code custom = true}.
 * Name uniqueness (case-insensitive) is enforced at the service layer.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @Column(nullable = false)
    private boolean custom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    protected Category() {}

    public Category(String name, CategoryType type, boolean custom, User user) {
        this.name = name;
        this.type = type;
        this.custom = custom;
        this.user = user;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public CategoryType getType() { return type; }
    public boolean isCustom() { return custom; }
    public User getUser() { return user; }
}
