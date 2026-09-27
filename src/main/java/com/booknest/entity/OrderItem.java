package com.booknest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * OrderItem Entity for items in an order
 *
 * Identity equals/hashCode (see Order): value-based equality would also make
 * two new items with the same quantity and price collapse into one entry in
 * Order.orderItems.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@ToString(exclude = {"order", "book"})
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    /** Unit price actually charged, i.e. the book's discounted price at order time. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Discount percentage that was already applied to {@link #price}; kept for reference. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @PrePersist
    @PreUpdate
    protected void calculateTotalPrice() {
        // price is already discounted; applying discount again here used to
        // charge the discount twice on every line total.
        if (price != null && quantity != null) {
            totalPrice = price.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
