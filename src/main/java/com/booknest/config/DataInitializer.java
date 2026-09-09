package com.booknest.config;

import com.booknest.entity.*;
import com.booknest.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Initializer to populate the database with the BookNest catalog.
 * <p>
 * Roles and the admin user are created once. The book catalog (categories,
 * authors, seed reviewer accounts, books, and reviews) is loaded from the
 * bundled {@code data/catalog.json} resource. If the catalog already stored
 * in the database matches the expected category taxonomy and book count,
 * seeding is skipped so restarts never insert duplicates. Otherwise the old
 * catalog data is cleared first and the full catalog is reloaded from JSON,
 * so a stale database never lingers alongside the new taxonomy.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String CATALOG_RESOURCE = "data/catalog.json";
    private static final int MIN_EXPECTED_BOOKS = 140;
    private static final String SEED_READER_PASSWORD = "Reader@123";

    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ReviewRepository reviewRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistRepository wishlistRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeAdminUser();
        seedCatalog();
    }

    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            Role adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            adminRole.setDescription("Administrator role with full access");
            roleRepository.save(adminRole);

            Role userRole = new Role();
            userRole.setName("ROLE_USER");
            userRole.setDescription("Standard user role");
            roleRepository.save(userRole);

            System.out.println("✓ Roles initialized");
        }
    }

    private void initializeAdminUser() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setEmail("admin@booknest.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setPhone("9876543210");
            admin.setEnabled(true);
            admin.setAccountNonExpired(true);
            admin.setAccountNonLocked(true);
            admin.setCredentialsNonExpired(true);

            Set<Role> roles = new HashSet<>();
            roles.add(roleRepository.findByName("ROLE_ADMIN").orElseThrow());
            roles.add(roleRepository.findByName("ROLE_USER").orElseThrow());
            admin.setRoles(roles);

            userRepository.save(admin);
            System.out.println("✓ Admin user created (email: admin@booknest.com, password: admin123)");
        }
    }

    private void seedCatalog() throws Exception {
        JsonNode root = loadCatalog();

        Set<String> expectedNames = new LinkedHashSet<>();
        root.get("categories").forEach(c -> expectedNames.add(c.get("name").asText()));

        // Soft-deleted rows (from an admin deleting a category/book) still exist in the
        // table, so this must look only at active rows - otherwise a normal delete
        // permanently desyncs this check and triggers a full reseed on every restart.
        Set<String> existingNames = new HashSet<>();
        categoryRepository.findAllActive().forEach(c -> existingNames.add(c.getName()));
        int activeBookCount = bookRepository.findAllActive().size();

        boolean upToDate = existingNames.equals(expectedNames) && activeBookCount >= MIN_EXPECTED_BOOKS;
        if (upToDate) {
            System.out.println("✓ Catalog already up to date (" + existingNames.size()
                    + " categories, " + activeBookCount + " books) — skipping reseed");
            return;
        }

        System.out.println("⚠ Catalog missing or outdated (found " + existingNames.size() + " categories, "
                + activeBookCount + " books) — clearing and reseeding from " + CATALOG_RESOURCE);
        clearCatalogData();

        Map<String, Category> categoryByName = new HashMap<>();
        for (JsonNode c : root.get("categories")) {
            Category category = new Category();
            category.setName(c.get("name").asText());
            category.setDescription(c.get("description").asText());
            categoryByName.put(category.getName(), categoryRepository.save(category));
        }

        Map<String, Author> authorByName = new HashMap<>();
        for (JsonNode a : root.get("authors")) {
            Author author = new Author();
            author.setName(a.get("name").asText());
            author.setBiography(a.get("biography").asText());
            authorByName.put(author.getName(), authorRepository.save(author));
        }

        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
        List<User> reviewers = new ArrayList<>();
        for (JsonNode r : root.get("reviewers")) {
            String email = r.get("email").asText();
            User reviewer = userRepository.findByEmail(email).orElse(null);
            if (reviewer == null) {
                reviewer = new User();
                reviewer.setFirstName(r.get("firstName").asText());
                reviewer.setLastName(r.get("lastName").asText());
                reviewer.setEmail(email);
                reviewer.setPassword(passwordEncoder.encode(SEED_READER_PASSWORD));
                reviewer.setPhone(r.get("phone").asText());
                reviewer.setEnabled(true);
                reviewer.setAccountNonExpired(true);
                reviewer.setAccountNonLocked(true);
                reviewer.setCredentialsNonExpired(true);
                reviewer.setRoles(new HashSet<>(Set.of(userRole)));
                reviewer = userRepository.save(reviewer);
            }
            reviewers.add(reviewer);
        }

        int bookCount = 0;
        int reviewCount = 0;
        Random rnd = new Random(20260906L);
        for (JsonNode b : root.get("books")) {
            Category category = categoryByName.get(b.get("category").asText());
            Author author = authorByName.get(b.get("author").asText());
            if (category == null || author == null) {
                continue;
            }

            Book book = new Book();
            book.setTitle(b.get("title").asText());
            book.setDescription(b.get("description").asText());
            book.setIsbn(b.get("isbn").asText());
            book.setPublisher(b.get("publisher").asText());
            book.setLanguage(b.get("language").asText());
            book.setPages(b.get("pages").asInt());
            book.setPrice(BigDecimal.valueOf(b.get("price").asDouble()));
            book.setDiscount(BigDecimal.valueOf(b.get("discount").asDouble()));
            book.setStock(b.get("stock").asInt());
            book.setRating(BigDecimal.valueOf(b.get("rating").asDouble()));
            book.setRatingCount(b.get("ratingCount").asInt());
            book.setImageUrl("/images/books/" + b.get("image").asText());
            book.setFeatured(b.get("featured").asBoolean());
            book.setBestSeller(b.get("bestSeller").asBoolean());
            book.setNewArrival(rnd.nextDouble() < 0.12);
            book.setSoldCount(rnd.nextInt(300));
            book.setViewCount(rnd.nextInt(1500));
            book.setCategory(category);
            book.setAuthor(author);
            LocalDate publishedDate = LocalDate.parse(b.get("publishedDate").asText());
            book.setPublishedDate(publishedDate.atStartOfDay());
            book = bookRepository.save(book);
            bookCount++;

            for (JsonNode rv : b.get("reviews")) {
                User reviewer = reviewers.get(rv.get("reviewerIndex").asInt());
                Review review = new Review();
                review.setUser(reviewer);
                review.setBook(book);
                review.setComment(rv.get("comment").asText());
                review.setRating(rv.get("rating").asInt());
                review.setApproved(true);
                review = reviewRepository.save(review);

                // createdAt is force-set to "now" by @PrePersist; re-save to back-date it
                // realistically now that the row already has an id (an update, not an insert).
                LocalDateTime backdated = LocalDateTime.now().minusDays(rv.get("daysAgo").asInt());
                review.setCreatedAt(backdated);
                review.setUpdatedAt(backdated);
                reviewRepository.save(review);
                reviewCount++;
            }
        }

        System.out.println("✓ Catalog seeded: " + categoryByName.size() + " categories, " + authorByName.size()
                + " authors, " + bookCount + " books, " + reviewers.size() + " reviewers, " + reviewCount + " reviews");
    }

    private JsonNode loadCatalog() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = new ClassPathResource(CATALOG_RESOURCE).getInputStream()) {
            return mapper.readTree(is);
        }
    }

    /**
     * Removes the previous catalog (and anything referencing it) so the new
     * taxonomy can be inserted cleanly, without leaving stale rows behind or
     * violating foreign keys. User accounts, roles, orders, and payments are
     * left untouched.
     */
    private void clearCatalogData() {
        orderItemRepository.deleteAllInBatch();
        cartItemRepository.deleteAllInBatch();
        wishlistRepository.deleteAllInBatch();
        reviewRepository.deleteAllInBatch();
        bookRepository.deleteAllInBatch();
        authorRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }
}
