package com.booknest.controller;

import com.booknest.entity.Author;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for category-related pages
 */
@Controller
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final BookService bookService;
    private final UserService userService;

    @GetMapping("/categories")
    public String categories(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        List<Category> categories = categoryService.getAllCategoriesWithBooks();
        if (categories.isEmpty()) {
            categories = getSampleCategories();
        }
        model.addAttribute("categories", categories);

        return "categories";
    }

    @GetMapping("/category/{id}")
    public String categoryBooks(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid category ID specified.");
            return "redirect:/categories";
        }

        Category category = categoryService.getCategoryById(id).orElse(null);
        if (category == null) {
            List<Category> sampleCategories = getSampleCategories();
            for (Category c : sampleCategories) {
                if (c.getId().equals(id)) {
                    category = c;
                    break;
                }
            }
        }

        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Category with ID " + id + " does not exist.");
            return "redirect:/categories";
        }

        List<Book> books = bookService.getBooksByCategory(id);
        if (books == null) {
            books = new ArrayList<>();
        }

        if (books.isEmpty()) {
            List<Book> allBooks = bookService.getAllBooks();
            for (Book b : allBooks) {
                if (b != null && b.getCategory() != null && 
                   (id.equals(b.getCategory().getId()) || 
                    category.getName().equalsIgnoreCase(b.getCategory().getName()))) {
                    books.add(b);
                }
            }
        }

        List<Category> allCategories = categoryService.getAllCategories();
        if (allCategories.isEmpty()) {
            allCategories = getSampleCategories();
        }

        Map<String, Author> uniqueAuthors = new LinkedHashMap<>();
        for (Book book : books) {
            if (book != null && book.getAuthor() != null && book.getAuthor().getName() != null) {
                uniqueAuthors.put(book.getAuthor().getName(), book.getAuthor());
            }
        }
        List<Author> authors = new ArrayList<>(uniqueAuthors.values());

        model.addAttribute("category", category);
        model.addAttribute("books", books);
        model.addAttribute("categories", allCategories);
        model.addAttribute("authors", authors);

        return "books";
    }

    private List<Category> getSampleCategories() {
        List<Category> sampleCategories = new ArrayList<>();
        String[] categoryNames = {"Fiction", "Non-Fiction", "Self Help", "Business", "Technology", "Programming", "History", "Romance", "Children", "Science"};
        for (String name : categoryNames) {
            Category category = new Category();
            category.setId((long) (sampleCategories.size() + 1));
            category.setName(name);
            category.setDescription(name + " books for curious readers.");
            sampleCategories.add(category);
        }
        return sampleCategories;
    }
}
