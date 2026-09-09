package com.booknest.service;

import com.booknest.entity.Book;
import com.booknest.entity.Cart;
import com.booknest.entity.CartItem;
import com.booknest.entity.User;
import com.booknest.repository.CartItemRepository;
import com.booknest.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service class for Cart operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookService bookService;

    public Cart getCartByUser(User user) {
        Optional<Cart> cart = cartRepository.findByUserIdWithItems(user.getId());
        if (cart.isEmpty()) {
            Cart newCart = new Cart();
            newCart.setUser(user);
            newCart.setTotalAmount(BigDecimal.ZERO);
            newCart.setTotalItems(0);
            return cartRepository.save(newCart);
        }
        return cart.get();
    }

    public Cart addToCart(User user, Long bookId, Integer quantity) {
        Cart cart = getCartByUser(user);
        Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        if (!book.isInStock()) {
            throw new RuntimeException("Book is out of stock");
        }

        int addQty = quantity != null && quantity > 0 ? quantity : 1;

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartIdAndBookId(cart.getId(), bookId);

        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            int newQty = cartItem.getQuantity() + addQty;
            if (newQty > book.getStock()) {
                throw new RuntimeException("Requested quantity exceeds available stock (" + book.getStock() + ")");
            }
            cartItem.setQuantity(newQty);
            cartItem.setPrice(book.getDiscountedPrice());
            cartItem.setTotalPrice(book.getDiscountedPrice().multiply(BigDecimal.valueOf(newQty)));
            cartItemRepository.save(cartItem);
        } else {
            if (addQty > book.getStock()) {
                throw new RuntimeException("Requested quantity exceeds available stock (" + book.getStock() + ")");
            }
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setBook(book);
            cartItem.setQuantity(addQty);
            cartItem.setPrice(book.getDiscountedPrice());
            cartItem.setTotalPrice(book.getDiscountedPrice().multiply(BigDecimal.valueOf(addQty)));
            CartItem savedItem = cartItemRepository.save(cartItem);
            cart.getCartItems().add(savedItem);
        }

        updateCartTotals(cart);
        return cartRepository.save(cart);
    }

    @SuppressWarnings("null")
    public Cart updateCartItem(User user, Long bookId, Integer quantity) {
        Cart cart = getCartByUser(user);
        CartItem cartItem = cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        if (quantity == null || quantity <= 0) {
            cart.getCartItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            Book book = cartItem.getBook();
            if (quantity > book.getStock()) {
                throw new RuntimeException("Requested quantity exceeds available stock (" + book.getStock() + ")");
            }
            cartItem.setQuantity(quantity);
            cartItem.setPrice(book.getDiscountedPrice());
            cartItem.setTotalPrice(book.getDiscountedPrice().multiply(BigDecimal.valueOf(quantity)));
            cartItemRepository.save(cartItem);
        }

        updateCartTotals(cart);
        return cartRepository.save(cart);
    }

    @SuppressWarnings("null")
    public Cart removeFromCart(User user, Long bookId) {
        Cart cart = getCartByUser(user);
        CartItem cartItem = cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        updateCartTotals(cart);
        return cartRepository.save(cart);
    }

    @SuppressWarnings("null")
    public void clearCart(User user) {
        Cart cart = getCartByUser(user);
        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setTotalItems(0);
        cartRepository.save(cart);
    }

    private void updateCartTotals(Cart cart) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        Integer totalItems = 0;

        if (cart.getCartItems() != null) {
            for (CartItem item : cart.getCartItems()) {
                if (item.getPrice() != null && item.getQuantity() != null) {
                    BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    item.setTotalPrice(itemTotal);
                    totalAmount = totalAmount.add(itemTotal);
                    totalItems += item.getQuantity();
                }
            }
        }

        cart.setTotalAmount(totalAmount);
        cart.setTotalItems(totalItems);
    }

    public Integer getCartItemCount(User user) {
        Optional<Cart> cart = cartRepository.findByUserId(user.getId());
        @SuppressWarnings("null")
        Integer count = cart.map(Cart::getTotalItems).orElse(0);
        return count;
    }
}
