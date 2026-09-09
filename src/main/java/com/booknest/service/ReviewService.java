package com.booknest.service;

import com.booknest.dto.ReviewDto;
import com.booknest.entity.Book;
import com.booknest.entity.Review;
import com.booknest.entity.User;
import com.booknest.repository.BookRepository;
import com.booknest.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service class for Review operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;

    @SuppressWarnings("null")
    public Review createReview(User user, ReviewDto reviewDto) {
        if (reviewRepository.findByUserIdAndBookId(user.getId(), reviewDto.getBookId()).isPresent()) {
            throw new RuntimeException("You have already reviewed this book");
        }

        Book book = bookRepository.findById(reviewDto.getBookId())
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Review review = new Review();
        review.setUser(user);
        review.setBook(book);
        review.setComment(reviewDto.getComment());
        review.setRating(reviewDto.getRating());
        review.setApproved(false);

        Review savedReview = reviewRepository.save(review);

        updateBookRating(book);

        return savedReview;
    }

    @SuppressWarnings("null")
    public Review updateReview(Long reviewId, ReviewDto reviewDto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setComment(reviewDto.getComment());
        review.setRating(reviewDto.getRating());
        review.setApproved(false);

        Review savedReview = reviewRepository.save(review);
        updateBookRating(review.getBook());

        return savedReview;
    }

    @SuppressWarnings("null")
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        Book book = review.getBook();
        reviewRepository.delete(review);
        updateBookRating(book);
    }

    public List<Review> getReviewsByBookId(Long bookId) {
        return reviewRepository.findApprovedReviewsByBookId(bookId);
    }

    public Page<Review> getReviewsByBookId(Long bookId, Pageable pageable) {
        return reviewRepository.findApprovedReviewsByBookId(bookId, pageable);
    }

    public List<Review> getReviewsByUser(User user) {
        return reviewRepository.findByUserId(user.getId());
    }

    @SuppressWarnings("null")
    public Review getReviewById(Long id) {
        return reviewRepository.findById(id).orElse(null);
    }

    public List<Review> getPendingReviews() {
        return reviewRepository.findPendingReviews();
    }

    @SuppressWarnings("null")
    public Review approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setApproved(true);
        Review savedReview = reviewRepository.save(review);
        updateBookRating(review.getBook());

        return savedReview;
    }

    private void updateBookRating(Book book) {
        Double avgRating = reviewRepository.getAverageRatingByBookId(book.getId());
        Long count = reviewRepository.countApprovedReviewsByBookId(book.getId());

        if (avgRating != null) {
            book.setRating(BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP));
        } else {
            book.setRating(BigDecimal.ZERO);
        }

        book.setRatingCount(count.intValue());
        bookRepository.save(book);
    }

    public Review saveReview(Review review) {
        // Check if user already reviewed this book
        if (reviewRepository.findByUserIdAndBookId(review.getUser().getId(), review.getBook().getId()).isPresent()) {
            throw new RuntimeException("You have already reviewed this book");
        }

        Review savedReview = reviewRepository.save(review);
        updateBookRating(review.getBook());
        return savedReview;
    }
}
