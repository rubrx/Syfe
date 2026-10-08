package com.rubrangso.finance.transaction;

import com.rubrangso.finance.category.Category;
import com.rubrangso.finance.category.CategoryType;
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
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Records a single financial transaction for a user.
 * {@code type} is stored denormalized (derived from the category) to allow efficient
 * filtering without a join and to preserve the type even if a category is reassigned.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected Transaction() {}

    public Transaction(BigDecimal amount, LocalDate date, Category category,
                       String description, User user) {
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.description = description;
        this.type = category.getType();
        this.user = user;
    }

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public Category getCategory() { return category; }
    public String getDescription() { return description; }
    public CategoryType getType() { return type; }
    public User getUser() { return user; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCategory(Category category) {
        this.category = category;
        this.type = category.getType();
    }
    public void setDescription(String description) { this.description = description; }
}
