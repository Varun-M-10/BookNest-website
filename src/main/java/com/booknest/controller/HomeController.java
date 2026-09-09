package com.booknest.controller;

import com.booknest.entity.Book;
import com.booknest.entity.Category;
import com.booknest.entity.User;
import com.booknest.service.BookService;
import com.booknest.service.CategoryService;
import com.booknest.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for home page and general pages
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final UserService userService;

    // Recommended categories to feature on the homepage, in display order.
    // Resolved against the real Category records so IDs are never hardcoded.
    private static final List<String> HOMEPAGE_CATEGORY_NAMES = List.of(
            "Fiction", "Programming & Technology", "Entrepreneurship", "Self Development",
            "Romance", "Kids", "Marathi Literature", "Hindi Literature",
            "Competitive Exams", "Biography & Memoir", "Mystery & Thriller", "Finance & Investing"
    );

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        List<Book> featuredBooks = bookService.getFeaturedBooks();
        List<Book> bestSellers = bookService.getBestSellers();
        List<Category> categories = categoryService.getAllCategories();

        // Limit to 6-8 featured books
        if (featuredBooks.size() > 8) {
            featuredBooks = featuredBooks.subList(0, 8);
        }

        // Limit to 6 best sellers for carousel
        if (bestSellers.size() > 6) {
            bestSellers = bestSellers.subList(0, 6);
        }

        Map<String, Category> categoryByName = new LinkedHashMap<>();
        for (Category category : categories) {
            categoryByName.put(category.getName(), category);
        }
        List<Category> homeCategories = new ArrayList<>();
        for (String name : HOMEPAGE_CATEGORY_NAMES) {
            Category category = categoryByName.get(name);
            if (category != null) {
                homeCategories.add(category);
            }
        }

        // Remove new arrivals section
        model.addAttribute("featuredBooks", featuredBooks);
        model.addAttribute("bestSellers", bestSellers);
        model.addAttribute("categories", categories);
        model.addAttribute("homeCategories", homeCategories);

        return "home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }
        return "about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }
        return "contact";
    }

    @PostMapping("/contact")
    public String contactSubmit(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String subject,
                               @RequestParam String message,
                               RedirectAttributes redirectAttributes) {
        // For now, just show success message
        // In production, this would send an email
        redirectAttributes.addFlashAttribute("success", "Thank you for contacting us! We'll get back to you soon.");
        return "redirect:/contact";
    }
}
