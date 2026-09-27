package com.booknest.service;

import com.booknest.entity.*;
import com.booknest.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for Order operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final BookService bookService;

    /**
     * Turns the user's cart into an order. Runs in one transaction: the order,
     * its items, its payment, the stock updates and the cart clear either all
     * commit or all roll back, so a failed order never empties the cart.
     */
    public Order createOrder(User user, Address shippingAddress, String paymentMethod, String deliveryOption, String notes) {
        Cart cart = cartService.getCartForCheckout(user);

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw OrderPlacementException.emptyCart();
        }

        // Validate every item before touching anything
        for (CartItem item : cart.getCartItems()) {
            Book book = item.getBook();
            if (book == null) {
                throw new OrderPlacementException("A book in your cart is no longer available. Please review your cart.");
            }
            if (book.getStock() == null || book.getStock() < item.getQuantity()) {
                int available = book.getStock() != null ? Math.max(book.getStock(), 0) : 0;
                throw new OrderPlacementException("Only " + available + " left in stock for '" + book.getTitle()
                        + "'. Please update the quantity in your cart.");
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setNotes(notes);

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItems = 0;
        for (CartItem cartItem : cart.getCartItems()) {
            Book book = cartItem.getBook();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setBook(book);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(book.getDiscountedPrice());
            orderItem.setDiscount(book.getDiscount() != null ? book.getDiscount() : BigDecimal.ZERO);
            order.getOrderItems().add(orderItem);

            subtotal = subtotal.add(book.getDiscountedPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            totalItems += cartItem.getQuantity();

            // Decrement stock & increment sold count
            bookService.incrementSoldCount(book.getId(), cartItem.getQuantity());
        }

        BigDecimal shippingAmount = "EXPRESS".equalsIgnoreCase(deliveryOption) ? new BigDecimal("99.00") : BigDecimal.ZERO;

        order.setTotalAmount(subtotal);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingAmount(shippingAmount);
        order.setFinalAmount(subtotal.add(shippingAmount));
        order.setTotalItems(totalItems);

        boolean isOnlinePayment = "CARD".equalsIgnoreCase(paymentMethod) || "UPI".equalsIgnoreCase(paymentMethod);
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "COD");
        order.setStatus(isOnlinePayment ? "PROCESSING" : "PENDING");
        order.setPaymentStatus(isOnlinePayment ? "PAID" : "PENDING");

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getFinalAmount());
        payment.setStatus(isOnlinePayment ? "SUCCESS" : "PENDING");
        payment.setPaymentMethod(order.getPaymentMethod());
        payment.setPaymentGateway(isOnlinePayment ? "BookNest Payment Gateway" : "Cash on Delivery");
        order.setPayment(payment);

        Order savedOrder = orderRepository.saveAndFlush(order);

        cartService.clearCart(user);

        return savedOrder;
    }

    /**
     * The user's latest order if it was placed within the last {@code seconds}
     * seconds; used to recognise a repeated Place Order submission.
     */
    public Optional<Order> findRecentOrder(User user, long seconds) {
        return orderRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .filter(o -> o.getCreatedAt() != null
                        && o.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(seconds)));
    }

    @SuppressWarnings("null")
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public Order getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).orElse(null);
    }

    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUserId(user.getId());
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    public Order updateOrderStatus(Long orderId, String status) {
        Order order = getOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }

        order.setStatus(status);

        if ("SHIPPED".equals(status)) {
            order.setShippedAt(LocalDateTime.now());
        } else if ("DELIVERED".equals(status)) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }

    public Order updatePaymentStatus(Long orderId, String paymentStatus) {
        Order order = getOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }

        order.setPaymentStatus(paymentStatus);

        if (order.getPayment() != null) {
            order.getPayment().setStatus(paymentStatus);
        }

        return orderRepository.save(order);
    }

    public void cancelOrder(Long orderId, User user) {
        Order order = getOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to order");
        }

        if (!"PENDING".equalsIgnoreCase(order.getStatus()) && !"PROCESSING".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Cannot cancel order that has already been shipped or processed.");
        }

        order.setStatus("CANCELLED");
        order.setPaymentStatus("CANCELLED");

        if (order.getPayment() != null) {
            order.getPayment().setStatus("CANCELLED");
        }

        // Restore stock
        for (OrderItem orderItem : order.getOrderItems()) {
            bookService.incrementSoldCount(orderItem.getBook().getId(), -orderItem.getQuantity());
        }

        orderRepository.save(order);
    }

    public List<Order> getPendingOrders() {
        return orderRepository.findPendingOrders();
    }

    public List<Order> getShippedOrders() {
        return orderRepository.findShippedOrders();
    }

    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = orderRepository.calculateTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
}
