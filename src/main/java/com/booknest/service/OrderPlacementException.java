package com.booknest.service;

/**
 * An order could not be placed for a reason the customer can act on (empty
 * cart, book no longer available, not enough stock). The message is safe to
 * show on the checkout page as-is; any other exception during checkout is
 * unexpected and must not be shown to the customer.
 */
public class OrderPlacementException extends RuntimeException {

    private final boolean emptyCart;

    public OrderPlacementException(String message) {
        this(message, false);
    }

    private OrderPlacementException(String message, boolean emptyCart) {
        super(message);
        this.emptyCart = emptyCart;
    }

    public static OrderPlacementException emptyCart() {
        return new OrderPlacementException("Your cart is empty", true);
    }

    public boolean isEmptyCart() {
        return emptyCart;
    }
}
