package com.booknest.service;

import com.booknest.entity.Book;
import com.booknest.entity.User;
import com.booknest.entity.Wishlist;
import com.booknest.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class for Wishlist operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final BookService bookService;

    public List<Wishlist> getWishlistByUser(User user) {
        return wishlistRepository.findByUserId(user.getId());
    }

    public Wishlist addToWishlist(User user, Long bookId) {
        if (wishlistRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            throw new RuntimeException("Book already in wishlist");
        }

        Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setBook(book);

        return wishlistRepository.save(wishlist);
    }

    public void removeFromWishlist(User user, Long bookId) {
        wishlistRepository.deleteByUserIdAndBookId(user.getId(), bookId);
    }

    public boolean isInWishlist(User user, Long bookId) {
        return wishlistRepository.existsByUserIdAndBookId(user.getId(), bookId);
    }

    @SuppressWarnings("null")
    public void clearWishlist(User user) {
        List<Wishlist> wishlistItems = wishlistRepository.findByUserId(user.getId());
        wishlistRepository.deleteAll(wishlistItems);
    }
}
