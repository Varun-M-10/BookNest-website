package com.booknest.controller;

import com.booknest.entity.Book;
import com.booknest.entity.Category;
import com.booknest.entity.Review;
import com.booknest.entity.User;
import com.booknest.service.BookService;
import com.booknest.service.ReviewService;
import com.booknest.service.UserService;
import com.booknest.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Controller for book-related pages
 */
@Controller
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final UserService userService;
    private final WishlistService wishlistService;
    private final ReviewService reviewService;

    @GetMapping("/books")
    public String books(@RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "category", required = false) Long categoryId,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "24") int size,
                        Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        List<Book> books = bookService.getAllBooks();
        if (books.isEmpty()) {
            books = sampleBooks();
        }

        Map<String, Category> uniqueCategories = new LinkedHashMap<>();
        for (Book book : books) {
            if (book.getCategory() != null) {
                uniqueCategories.put(book.getCategory().getName(), book.getCategory());
            }
        }
        List<Category> categories = new ArrayList<>(uniqueCategories.values());

        model.addAttribute("books", books);
        model.addAttribute("categories", categories);
        Map<String, com.booknest.entity.Author> uniqueAuthors = new LinkedHashMap<>();
        for (Book book : books) {
            if (book.getAuthor() != null) {
                uniqueAuthors.put(book.getAuthor().getName(), book.getAuthor());
            }
        }
        List<com.booknest.entity.Author> authors = new ArrayList<>(uniqueAuthors.values());
        model.addAttribute("authors", authors);

        return "books";
    }

    private List<Book> sampleBooks() {
        String[] covers = {
            "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600&h=800&fit=crop",
            "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=600&h=800&fit=crop",
            "https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=600&h=800&fit=crop",
            "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=600&h=800&fit=crop",
            "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=600&h=800&fit=crop",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600&h=800&fit=crop",
            "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=600&h=800&fit=crop",
            "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&h=800&fit=crop",
            "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600&h=800&fit=crop",
            "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=600&h=800&fit=crop",
            "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&h=800&fit=crop",
            "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&h=800&fit=crop"
        };
        List<Book> books = new ArrayList<>();
        books.add(sampleBook(1001L, "Atomic Habits", "James Clear", "Self Help", "Small changes that create remarkable results and lasting habits.", 449, 15, 4.8, covers[0], 248));
        books.add(sampleBook(1002L, "Rich Dad Poor Dad", "Robert T. Kiyosaki", "Business", "A clear guide to financial literacy, investing, and building wealth.", 399, 20, 4.7, covers[1], 231));
        books.add(sampleBook(1003L, "The Psychology of Money", "Morgan Housel", "Business", "Timeless lessons about wealth, greed, happiness, and human behavior.", 349, 10, 4.8, covers[2], 219));
        books.add(sampleBook(1004L, "Ikigai", "Hector Garcia", "Self Help", "Japanese wisdom for finding purpose, balance, and a longer happier life.", 399, 25, 4.5, covers[3], 188));
        books.add(sampleBook(1005L, "Deep Work", "Cal Newport", "Self Help", "Rules for focused success in a distracted world of constant notifications.", 499, 10, 4.6, covers[4], 177));
        books.add(sampleBook(1006L, "Think Like a Monk", "Jay Shetty", "Self Help", "Practical lessons for training your mind and living with greater purpose.", 449, 15, 4.5, covers[5], 164));
        books.add(sampleBook(1007L, "Clean Code", "Robert C. Martin", "Programming", "A handbook of agile craftsmanship for writing readable, maintainable code.", 699, 10, 4.9, covers[6], 302));
        books.add(sampleBook(1008L, "Effective Java", "Joshua Bloch", "Programming", "Expert techniques and best practices for modern Java development.", 899, 10, 4.9, covers[7], 294));
        books.add(sampleBook(1009L, "Spring in Action", "Craig Walls", "Technology", "Build robust, production-ready applications with the Spring framework.", 899, 10, 4.7, covers[8], 256));
        books.add(sampleBook(1010L, "Head First Java", "Kathy Sierra", "Programming", "A visual, brain-friendly introduction to object-oriented Java programming.", 649, 20, 4.6, covers[9], 241));
        books.add(sampleBook(1011L, "Java: The Complete Reference", "Herbert Schildt", "Programming", "A comprehensive reference for learning Java from fundamentals to advanced APIs.", 749, 15, 4.5, covers[10], 226));
        books.add(sampleBook(1012L, "The Pragmatic Programmer", "David Thomas", "Technology", "Timeless advice for becoming a thoughtful and effective software developer.", 699, 15, 4.9, covers[11], 287));
        books.add(sampleBook(1013L, "Harry Potter", "J.K. Rowling", "Children", "An imaginative magical adventure about friendship, courage, and belonging.", 599, 20, 4.9, covers[0], 318));
        books.add(sampleBook(1014L, "The Hobbit", "J.R.R. Tolkien", "Romance", "A charming fantasy adventure that leads one unlikely hero beyond home.", 449, 15, 4.8, covers[1], 275));
        books.add(sampleBook(1015L, "The Alchemist", "Paulo Coelho", "Romance", "A lyrical journey about dreams, omens, and listening to your heart.", 299, 30, 4.8, covers[2], 311));
        books.add(sampleBook(1016L, "The Lean Startup", "Eric Ries", "Business", "A practical method for building products through learning and iteration.", 499, 15, 4.5, covers[3], 205));
        books.add(sampleBook(1017L, "Wings of Fire", "A.P.J. Abdul Kalam", "History", "An inspiring autobiography of vision, persistence, and scientific achievement.", 349, 25, 4.8, covers[4], 267));
        books.add(sampleBook(1018L, "The Power of Now", "Eckhart Tolle", "Self Help", "A direct guide to finding peace by returning attention to the present moment.", 349, 10, 4.6, covers[5], 198));
        books.add(sampleBook(1019L, "Zero to One", "Peter Thiel", "Business", "Bold ideas about innovation, startups, and creating the future.", 449, 20, 4.5, covers[6], 214));
        books.add(sampleBook(1020L, "The Intelligent Investor", "Benjamin Graham", "Business", "A classic framework for disciplined, value-focused long-term investing.", 799, 15, 4.7, covers[7], 252));
        books.add(sampleBook(1021L, "Design Patterns", "Erich Gamma", "Programming", "Elements of reusable object-oriented software for better design.", 799, 10, 4.8, covers[8], 289));
        books.add(sampleBook(1022L, "Refactoring", "Martin Fowler", "Technology", "Improving the design of existing code for maintainability.", 849, 15, 4.7, covers[9], 267));
        books.add(sampleBook(1023L, "The Art of War", "Sun Tzu", "History", "Ancient Chinese military treatise on strategy and tactics.", 299, 20, 4.6, covers[10], 345));
        books.add(sampleBook(1024L, "Sapiens", "Yuval Noah Harari", "History", "A brief history of humankind from ancient times to the present.", 449, 25, 4.8, covers[11], 412));
        books.add(sampleBook(1025L, "Thinking, Fast and Slow", "Daniel Kahneman", "Self Help", "Understanding the two systems that drive the way we think.", 399, 15, 4.7, covers[0], 278));
        books.add(sampleBook(1026L, "Start with Why", "Simon Sinek", "Business", "How great leaders inspire everyone to take action.", 349, 20, 4.6, covers[1], 234));
        books.add(sampleBook(1027L, "The 4-Hour Workweek", "Tim Ferriss", "Business", "Escape 9-5, live anywhere, and join the new rich.", 399, 25, 4.5, covers[2], 289));
        books.add(sampleBook(1028L, "Cracking the Coding Interview", "Gayle Laakmann", "Programming", "189 programming questions and solutions for technical interviews.", 899, 10, 4.9, covers[3], 456));
        books.add(sampleBook(1029L, "Introduction to Algorithms", "Thomas Cormen", "Technology", "Comprehensive introduction to modern algorithm design and analysis.", 1299, 15, 4.8, covers[4], 512));
        books.add(sampleBook(1030L, "The Great Gatsby", "F. Scott Fitzgerald", "Romance", "A story of the decadence and excess of the Jazz Age.", 299, 20, 4.4, covers[5], 198));
        return books;
    }

    private Book sampleBook(Long id, String title, String authorName, String categoryName, String description,
                            int price, int discount, double rating, String imageUrl, int soldCount) {
        com.booknest.entity.Author author = new com.booknest.entity.Author();
        author.setName(authorName);
        Category category = new Category();
        category.setName(categoryName);
        category.setDescription(categoryName + " books for curious readers.");

        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setDescription(description);
        book.setAuthor(author);
        book.setCategory(category);
        book.setPrice(BigDecimal.valueOf(price));
        book.setDiscount(BigDecimal.valueOf(discount));
        book.setRating(BigDecimal.valueOf(rating));
        book.setRatingCount(100 + soldCount);
        book.setSoldCount(soldCount);
        book.setStock(18);
        book.setImageUrl(imageUrl);
        book.setLanguage("English");
        book.setPublisher("BookNest Editions");
        book.setIsbn("SAMPLE-" + id);
        book.setPages(280);
        book.setFeatured(true);
        book.setBestSeller(soldCount > 250);
        book.setNewArrival(id > 1015);
        book.setDeleted(false);
        book.setViewCount(0);
        book.setCreatedAt(LocalDateTime.now().minusDays(20 + id % 30));
        return book;
    }

    @GetMapping("/book/{id}")
    public String bookDetails(@PathVariable Long id, Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
            boolean inWishlist = wishlistService.isInWishlist(currentUser, id);
            model.addAttribute("inWishlist", inWishlist);
        }

        Book book = bookService.getBookById(id).orElse(null);
        
        // If book not found in database, check sample books
        if (book == null) {
            List<Book> sampleBooks = sampleBooks();
            for (Book sampleBook : sampleBooks) {
                if (sampleBook.getId().equals(id)) {
                    book = sampleBook;
                    break;
                }
            }
        }
        
        if (book == null) {
            return "redirect:/books";
        }

        // Only increment view count if book is in database
        if (bookService.getBookById(id).isPresent()) {
            bookService.incrementViewCount(id);
        }

        model.addAttribute("book", book);

        // Get reviews for this book only if it's from database
        List<Review> reviews = null;
        if (bookService.getBookById(id).isPresent()) {
            reviews = reviewService.getReviewsByBookId(id);
        }
        model.addAttribute("reviews", reviews);

        // Get related books from same category
        List<Book> allBooks = bookService.getAllBooks();
        if (allBooks.isEmpty()) {
            allBooks = sampleBooks();
        }
        
        List<Book> relatedBooks = new ArrayList<>();
        for (Book b : allBooks) {
            if (!b.getId().equals(id) && b.getCategory() != null && 
                b.getCategory().equals(book.getCategory())) {
                relatedBooks.add(b);
                if (relatedBooks.size() >= 4) break;
            }
        }
        // If not enough from same category, add any other books
        if (relatedBooks.size() < 4) {
            for (Book b : allBooks) {
                if (!b.getId().equals(id) && !relatedBooks.contains(b)) {
                    relatedBooks.add(b);
                    if (relatedBooks.size() >= 4) break;
                }
            }
        }
        model.addAttribute("relatedBooks", relatedBooks);

        return "book-details";
    }

    @GetMapping("/search")
    public String search(@RequestParam("keyword") String keyword,
                         @RequestParam(value = "page", defaultValue = "0") int page,
                         Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        List<Book> allBooks = bookService.getAllBooks();
        if (allBooks.isEmpty()) {
            allBooks = sampleBooks();
        }
        
        // Filter books by keyword
        List<Book> filteredBooks = new ArrayList<>();
        for (Book book : allBooks) {
            if (book.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                book.getAuthor().getName().toLowerCase().contains(keyword.toLowerCase()) ||
                book.getDescription().toLowerCase().contains(keyword.toLowerCase())) {
                filteredBooks.add(book);
            }
        }

        Map<String, Category> uniqueCategories = new LinkedHashMap<>();
        for (Book book : filteredBooks) {
            if (book.getCategory() != null) {
                uniqueCategories.put(book.getCategory().getName(), book.getCategory());
            }
        }
        List<Category> categories = new ArrayList<>(uniqueCategories.values());

        Map<String, com.booknest.entity.Author> uniqueAuthors = new LinkedHashMap<>();
        for (Book book : filteredBooks) {
            if (book.getAuthor() != null) {
                uniqueAuthors.put(book.getAuthor().getName(), book.getAuthor());
            }
        }
        List<com.booknest.entity.Author> authors = new ArrayList<>(uniqueAuthors.values());

        model.addAttribute("books", filteredBooks);
        model.addAttribute("categories", categories);
        model.addAttribute("authors", authors);
        model.addAttribute("keyword", keyword);

        return "books";
    }
}
