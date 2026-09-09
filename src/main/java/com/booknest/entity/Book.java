package com.booknest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Book Entity representing books in the store
 */
@Entity
@Table(name = "books")
@Data
@EqualsAndHashCode(exclude = {"category", "author", "orderItems", "wishlist", "reviews", "cartItems"})
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE books SET deleted = true WHERE id = ?")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(nullable = false)
    private String publisher;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private Integer pages;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer ratingCount = 0;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(nullable = false)
    private Boolean bestSeller = false;

    @Column(nullable = false)
    private Boolean newArrival = false;

    @Column(nullable = false)
    private Integer soldCount = 0;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderItem> orderItems = new HashSet<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Wishlist> wishlist = new HashSet<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Review> reviews = new HashSet<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CartItem> cartItems = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public BigDecimal getDiscountedPrice() {
        BigDecimal result = price;
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountAmount = price.multiply(discount)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            result = price.subtract(discountAmount);
        }
        // Money is always 2 decimal places; the intermediate discount math above
        // can otherwise leave a larger scale (e.g. 296.6500) that renders with
        // stray trailing zeros wherever the value is displayed or serialized.
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public Integer getDiscountPercentage() {
        return discount != null ? discount.intValue() : 0;
    }

    @Transient
    public Boolean isInStock() {
        return stock != null && stock > 0;
    }

    /**
     * Display-formatted list price, e.g. "1,299" or "799.50". Whole rupee
     * amounts are shown with no decimals; amounts with paise show exactly two.
     * Formatting only — the underlying {@link #price} stays a numeric BigDecimal.
     */
    @Transient
    public String getFormattedPrice() {
        return formatMoney(price);
    }

    /** Display-formatted {@link #getDiscountedPrice()}; see {@link #getFormattedPrice()}. */
    @Transient
    public String getFormattedDiscountedPrice() {
        return formatMoney(getDiscountedPrice());
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        boolean hasPaise = normalized.stripTrailingZeros().scale() > 0;

        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(hasPaise ? 2 : 0);
        format.setMaximumFractionDigits(2);
        return format.format(normalized);
    }
}
