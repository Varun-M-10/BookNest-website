package com.booknest.config;

import com.booknest.entity.*;
import com.booknest.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>
 * Separately, {@link #reconcileBookCovers} always runs, even when the block
 * above decides seeding is up to date. A cover-image edit in catalog.json
 * (the common case - swapping a generated placeholder for a real, verified
 * cover URL) is not a taxonomy or count change, so the skip check above
 * would otherwise mean the edit never reaches an already-seeded database,
 * including production. This deliberately does NOT do the broader
 * "reconcile every field, retire missing titles" upsert a prior version of
 * this class attempted: that version also made {@code isbn} nullable and
 * ran a runtime {@code ALTER TABLE} to relax the column's existing NOT NULL
 * constraint on databases created before this class existed - which
 * silently failed against a production database whose user lacks DDL
 * privileges, so the very next save with a null isbn crashed the whole
 * CommandLineRunner and took app startup down with it. Every book in
 * catalog.json already carries a non-null, unique isbn, so none of that is
 * needed here: this method only ever writes imageUrl, a column that has
 * carried a value since the table was first created, to an existing row
 * matched by title - no schema change, and each book is isolated in its
 * own try/catch so one unexpected row can never abort the rest or crash
 * startup.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
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
        JsonNode catalog = loadCatalog();
        seedCatalog(catalog);
        reconcileBookCovers(catalog);
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

    private void seedCatalog(JsonNode root) {
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
            book.setImageUrl(resolveImageUrl(b.get("image").asText()));
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

    /**
     * Updates {@code imageUrl} on existing book rows, matched by title, to
     * whatever catalog.json currently declares - the one field this method
     * touches, and the only column it needs to touch for a cover-image
     * update to reach an already-seeded database (production included) on
     * the next restart. See the class-level Javadoc for why this exists as
     * a narrow always-runs pass rather than folded into the broader
     * insert-only {@link #seedCatalog}, and why it never touches isbn or
     * any schema.
     * <p>
     * Each row is saved in its own try/catch: a single unexpected failure
     * (a stale in-memory reference, a constraint this method didn't
     * anticipate) is logged and skipped rather than allowed to abort the
     * remaining books - or, worse, the rest of {@link #run}.
     */
    private void reconcileBookCovers(JsonNode root) {
        Map<String, String> imageByTitle = new HashMap<>();
        for (JsonNode b : root.get("books")) {
            imageByTitle.put(b.get("title").asText(), resolveImageUrl(b.get("image").asText()));
        }

        int updated = 0;
        int failed = 0;
        for (Book book : bookRepository.findAllActive()) {
            String expectedUrl = imageByTitle.get(book.getTitle());
            if (expectedUrl == null || expectedUrl.equals(book.getImageUrl())) {
                continue;
            }
            try {
                book.setImageUrl(expectedUrl);
                bookRepository.save(book);
                updated++;
            } catch (Exception e) {
                failed++;
                log.warn("⚠ Could not update cover for \"{}\": {}", book.getTitle(), e.getMessage());
            }
        }

        if (updated > 0 || failed > 0) {
            System.out.println("✓ Cover reconciliation: " + updated + " updated, " + failed + " failed"
                    + " (of " + imageByTitle.size() + " catalog entries)");
        }
    }

    /**
     * catalog.json's "image" field is either a real cover's absolute URL
     * (e.g. an Open Library or Wikimedia cover link) or, for the titles no
     * reliable source had a verifiable real cover for, a filename served
     * from the bundled {@code /images/books/} static resources.
     */
    private static String resolveImageUrl(String image) {
        return image.startsWith("http://") || image.startsWith("https://")
                ? image
                : "/images/books/" + image;
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
