package com.booknest.service;

import com.booknest.entity.*;
import com.booknest.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    public Order createOrder(User user, Address shippingAddress, String paymentMethod, String deliveryOption, String notes) {
        Cart cart = cartService.getCartByUser(user);

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Your cart is empty");
        }

        // Validate stock for all items
        for (CartItem item : cart.getCartItems()) {
            if (item.getBook().getStock() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for '" + item.getBook().getTitle() + "'. Available: " + item.getBook().getStock());
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setNotes(notes);

        BigDecimal subtotal = cart.getTotalAmount();
        BigDecimal shippingAmount = "EXPRESS".equalsIgnoreCase(deliveryOption) ? new BigDecimal("99.00") : BigDecimal.ZERO;
        BigDecimal finalAmount = subtotal.add(shippingAmount);

        order.setTotalAmount(subtotal);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingAmount(shippingAmount);
        order.setFinalAmount(finalAmount);
        order.setTotalItems(cart.getTotalItems());

        boolean isOnlinePayment = "CARD".equalsIgnoreCase(paymentMethod) || "UPI".equalsIgnoreCase(paymentMethod);
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "COD");
        order.setStatus(isOnlinePayment ? "PROCESSING" : "PENDING");
        order.setPaymentStatus(isOnlinePayment ? "PAID" : "PENDING");

        for (CartItem cartItem : cart.getCartItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setBook(cartItem.getBook());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getBook().getDiscountedPrice());
            orderItem.setDiscount(cartItem.getBook().getDiscount());
            order.getOrderItems().add(orderItem);

            // Decrement stock & increment sold count
            bookService.incrementSoldCount(cartItem.getBook().getId(), cartItem.getQuantity());
        }

        Order savedOrder = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setAmount(savedOrder.getFinalAmount());
        payment.setStatus(isOnlinePayment ? "SUCCESS" : "PENDING");
        payment.setPaymentMethod(order.getPaymentMethod());
        payment.setPaymentGateway(isOnlinePayment ? "BookNest Payment Gateway" : "Cash on Delivery");
        savedOrder.setPayment(payment);

        cartService.clearCart(user);

        return orderRepository.save(savedOrder);
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
