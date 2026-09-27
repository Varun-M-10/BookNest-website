package com.booknest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Order Entity for customer orders
 *
 * Uses identity equals/hashCode rather than Lombok @Data: the generated
 * versions walked orderItems -> order -> orderItems and recursed until
 * StackOverflowError as soon as an item was added to the set.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@ToString(exclude = {"user", "shippingAddress", "payment", "orderItems"})
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 20)
    private String paymentStatus;

    @Column(nullable = false, length = 50)
    private String paymentMethod;

    private String transactionId;

    @Column(nullable = false)
    private Integer totalItems;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Many orders can ship to the same saved address (Address.orders is the
    // inverse side). As @OneToOne this was a unique FK, so a repeat order to
    // a saved address violated the constraint.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address shippingAddress;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderItem> orderItems = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderNumber == null) {
            orderNumber = "ORD-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public Boolean isPending() {
        return "PENDING".equals(status);
    }

    @Transient
    public Boolean isShipped() {
        return "SHIPPED".equals(status);
    }

    @Transient
    public Boolean isDelivered() {
        return "DELIVERED".equals(status);
    }

    @Transient
    public Boolean isCancelled() {
        return "CANCELLED".equals(status);
    }
}
