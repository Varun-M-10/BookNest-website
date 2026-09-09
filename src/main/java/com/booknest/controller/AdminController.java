package com.booknest.controller;

import com.booknest.entity.*;
import com.booknest.service.*;
import com.booknest.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for admin dashboard and management
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final AuthorService authorService;
    private final OrderService orderService;
    private final UserService userService;
    private final ReviewService reviewService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        User currentUser = userService.getCurrentUser();
        List<Book> books = bookService.getAllBooks();
        List<Category> categories = categoryService.getAllCategories();
        List<User> users = userService.getAllUsers();
        List<Order> orders = orderService.getAllOrders();
        List<Order> pendingOrders = orderService.getPendingOrders();
        List<Book> lowStockBooks = bookService.getLowStockBooks(10);
        BigDecimal totalRevenue = orderService.getTotalRevenue();

        model.addAttribute("totalBooks", books.size());
        model.addAttribute("totalCategories", categories.size());
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("pendingOrders", pendingOrders.size());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("lowStockBooks", lowStockBooks);
        model.addAttribute("recentOrders", orders.subList(0, Math.min(10, orders.size())));
        model.addAttribute("currentUser", currentUser);

        return "admin/dashboard";
    }

    // ==================== BOOK MANAGEMENT ====================

    @GetMapping("/books")
    public String adminBooks(@RequestParam(value = "search", required = false) String search, Model model) {
        User currentUser = userService.getCurrentUser();
        List<Book> books;
        if (search != null && !search.trim().isEmpty()) {
            books = bookService.searchBooks(search.trim(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        } else {
            books = bookService.getAllBooks();
        }

        model.addAttribute("books", books);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("authors", authorService.getAllAuthors());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("search", search);

        return "admin/books";
    }

    @GetMapping("/books/add")
    public String addBook(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("authors", authorService.getAllAuthors());
        model.addAttribute("currentUser", currentUser);

        return "admin/book-form";
    }

    @PostMapping("/books/save")
    public String saveBook(@ModelAttribute Book book,
                           @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                           RedirectAttributes redirectAttributes) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedUrl = FileUploadUtil.saveFile("books", imageFile);
                if (uploadedUrl != null) {
                    book.setImageUrl(uploadedUrl);
                }
            } else if (book.getId() != null) {
                // Keep existing image if not uploading new file
                bookService.getBookById(book.getId()).ifPresent(existing -> {
                    if (book.getImageUrl() == null || book.getImageUrl().trim().isEmpty()) {
                        book.setImageUrl(existing.getImageUrl());
                    }
                });
            }

            if (book.getImageUrl() == null || book.getImageUrl().trim().isEmpty()) {
                book.setImageUrl("https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=300&h=400&fit=crop");
            }

            // A blank ISBN is a deliberate, allowed state (an admin leaving it
            // unset rather than inventing one) - store it as SQL NULL, not "",
            // since isbn is a unique column and two rows with "" would collide.
            if (book.getIsbn() != null && book.getIsbn().trim().isEmpty()) {
                book.setIsbn(null);
            }

            bookService.saveBook(book);
            redirectAttributes.addFlashAttribute("success", "Book saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save book: " + e.getMessage());
        }

        return "redirect:/admin/books";
    }

    @GetMapping("/books/edit/{id}")
    public String editBook(@PathVariable Long id, Model model) {
        User currentUser = userService.getCurrentUser();
        Book book = bookService.getBookById(id).orElse(null);
        if (book == null) {
            return "redirect:/admin/books";
        }

        model.addAttribute("book", book);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("authors", authorService.getAllAuthors());
        model.addAttribute("currentUser", currentUser);

        return "admin/book-form";
    }

    @GetMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("success", "Book deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting book: " + e.getMessage());
        }
        return "redirect:/admin/books";
    }

    // ==================== CATEGORY MANAGEMENT ====================

    @GetMapping("/categories")
    public String adminCategories(Model model) {
        User currentUser = userService.getCurrentUser();
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("currentUser", currentUser);

        return "admin/categories";
    }

    @GetMapping("/categories/add")
    public String addCategory(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("category", new Category());
        model.addAttribute("currentUser", currentUser);

        return "admin/category-form";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategory(@PathVariable Long id, Model model) {
        User currentUser = userService.getCurrentUser();
        Category category = categoryService.getCategoryById(id).orElse(null);
        if (category == null) {
            return "redirect:/admin/categories";
        }

        model.addAttribute("category", category);
        model.addAttribute("currentUser", currentUser);

        return "admin/category-form";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               RedirectAttributes redirectAttributes) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedUrl = FileUploadUtil.saveFile("categories", imageFile);
                if (uploadedUrl != null) {
                    category.setImageUrl(uploadedUrl);
                }
            } else if (category.getId() != null) {
                categoryService.getCategoryById(category.getId()).ifPresent(existing -> {
                    if (category.getImageUrl() == null || category.getImageUrl().trim().isEmpty()) {
                        category.setImageUrl(existing.getImageUrl());
                    }
                });
            }

            categoryService.saveCategory(category);
            redirectAttributes.addFlashAttribute("success", "Category saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Category deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete category with associated books.");
        }
        return "redirect:/admin/categories";
    }

    // ==================== USER MANAGEMENT ====================

    @GetMapping("/users")
    public String adminUsers(@RequestParam(value = "query", required = false) String query, Model model) {
        User currentUser = userService.getCurrentUser();
        List<User> users = userService.searchUsers(query);

        model.addAttribute("users", users);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("query", query);

        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User updated = userService.toggleUserStatus(id);
        if (updated != null) {
            redirectAttributes.addFlashAttribute("success", 
                "User account " + (updated.isEnabled() ? "enabled" : "disabled") + " successfully.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String toggleUserRole(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User updated = userService.toggleUserAdminRole(id);
        if (updated != null) {
            redirectAttributes.addFlashAttribute("success", "User role updated successfully.");
        }
        return "redirect:/admin/users";
    }

    // ==================== ORDER MANAGEMENT ====================

    @GetMapping("/orders")
    public String adminOrders(@RequestParam(value = "status", required = false) String status, Model model) {
        User currentUser = userService.getCurrentUser();
        List<Order> orders;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            orders = orderService.getOrdersByStatus(status.toUpperCase());
        } else {
            orders = orderService.getAllOrders();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status != null ? status : "ALL");
        model.addAttribute("currentUser", currentUser);

        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String adminOrderDetails(@PathVariable Long id, Model model) {
        User currentUser = userService.getCurrentUser();
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return "redirect:/admin/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("currentUser", currentUser);

        return "admin/order-details";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id, 
                                    @RequestParam String status, 
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Order status updated to " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update order status: " + e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    // ==================== ANALYTICS & REPORTS ====================

    @GetMapping("/analytics")
    public String analytics(Model model) {
        User currentUser = userService.getCurrentUser();
        List<Book> books = bookService.getAllBooks();
        List<Order> orders = orderService.getAllOrders();
        List<Category> categories = categoryService.getAllCategories();

        // Top Selling Books (sorted by soldCount)
        List<Book> topSellingBooks = books.stream()
                .filter(b -> b.getSoldCount() != null && b.getSoldCount() > 0)
                .sorted(Comparator.comparing((Book b) -> b.getSoldCount()).reversed())
                .limit(8)
                .collect(Collectors.toList());

        // Popular Categories
        Map<String, Long> categoryBookCounts = new HashMap<>();
        for (Category cat : categories) {
            categoryBookCounts.put(cat.getName(), bookService.countByCategory(cat.getId()));
        }

        BigDecimal totalRevenue = orderService.getTotalRevenue();
        long completedOrders = orders.stream().filter(o -> "DELIVERED".equals(o.getStatus())).count();
        long pendingOrders = orders.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
        long cancelledOrders = orders.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("cancelledOrders", cancelledOrders);
        model.addAttribute("topSellingBooks", topSellingBooks);
        model.addAttribute("categoryBookCounts", categoryBookCounts);

        return "admin/analytics";
    }

    // ==================== REVIEWS MANAGEMENT ====================

    @GetMapping("/reviews")
    public String adminReviews(Model model) {
        User currentUser = userService.getCurrentUser();
        List<Review> pendingReviews = reviewService.getPendingReviews();
        model.addAttribute("reviews", pendingReviews);
        model.addAttribute("currentUser", currentUser);

        return "admin/reviews";
    }

    @PostMapping("/reviews/{id}/approve")
    public String approveReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reviewService.approveReview(id);
        redirectAttributes.addFlashAttribute("success", "Review approved.");
        return "redirect:/admin/reviews";
    }
}
