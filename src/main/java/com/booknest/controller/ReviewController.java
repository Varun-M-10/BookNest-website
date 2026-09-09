package com.booknest.controller;

import com.booknest.entity.Book;
import com.booknest.entity.Review;
import com.booknest.entity.User;
import com.booknest.service.BookService;
import com.booknest.service.ReviewService;
import com.booknest.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for review operations
 */
@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final BookService bookService;
    private final UserService userService;

    @PostMapping("/review/add")
    public String addReview(@RequestParam("bookId") Long bookId,
                           @RequestParam("comment") String comment,
                           @RequestParam(value = "rating", defaultValue = "5") Integer rating,
                           RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to submit a review");
            return "redirect:/login";
        }

        Book book = bookService.getBookById(bookId).orElse(null);
        if (book == null) {
            redirectAttributes.addFlashAttribute("error", "Book not found");
            return "redirect:/books";
        }

        try {
            Review review = new Review();
            review.setBook(book);
            review.setUser(currentUser);
            review.setComment(comment);
            review.setRating(rating);
            review.setApproved(false); // Reviews need admin approval

            reviewService.saveReview(review);
            redirectAttributes.addFlashAttribute("success", "Review submitted successfully! It will be visible after approval.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/book/" + bookId;
    }
}
