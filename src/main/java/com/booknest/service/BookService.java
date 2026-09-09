package com.booknest.service;

import com.booknest.entity.Book;
import com.booknest.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service class for Book operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final BookRepository bookRepository;

    public List<Book> getAllBooks() {
        return bookRepository.findAllActive();
    }

    public Page<Book> searchBooks(String keyword, Pageable pageable) {
        return bookRepository.searchBooks(keyword, pageable);
    }

    @SuppressWarnings("null")
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    public Optional<Book> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    public List<Book> getFeaturedBooks() {
        return bookRepository.findFeaturedBooks();
    }

    public List<Book> getBestSellers() {
        return bookRepository.findBestSellers();
    }

    public List<Book> getNewArrivals() {
        return bookRepository.findNewArrivals();
    }

    public List<Book> getBooksByCategory(Long categoryId) {
        return bookRepository.findByCategory(categoryId);
    }

    public List<Book> getBooksByAuthor(Long authorId) {
        return bookRepository.findByAuthor(authorId);
    }

    public List<Book> getBooksByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return bookRepository.findByPriceRange(minPrice, maxPrice);
    }

    public List<Book> getBooksByMinRating(BigDecimal rating) {
        return bookRepository.findByMinRating(rating);
    }

    public List<Book> getInStockBooks() {
        return bookRepository.findInStockBooks();
    }

    @SuppressWarnings("null")
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    @SuppressWarnings("null")
    public Book updateBook(Book book) {
        return bookRepository.save(book);
    }

    @SuppressWarnings("null")
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    @SuppressWarnings("null")
    public void incrementViewCount(Long bookId) {
        bookRepository.findById(bookId).ifPresent(book -> {
            book.setViewCount(book.getViewCount() + 1);
            bookRepository.save(book);
        });
    }

    @SuppressWarnings("null")
    public void incrementSoldCount(Long bookId, Integer quantity) {
        bookRepository.findById(bookId).ifPresent(book -> {
            book.setSoldCount(book.getSoldCount() + quantity);
            book.setStock(book.getStock() - quantity);
            bookRepository.save(book);
        });
    }

    public List<Book> getLowStockBooks(Integer threshold) {
        return bookRepository.findLowStockBooks(threshold);
    }

    public Long countByCategory(Long categoryId) {
        return bookRepository.countByCategory(categoryId);
    }
}
