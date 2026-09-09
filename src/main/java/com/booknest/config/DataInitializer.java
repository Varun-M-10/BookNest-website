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

import javax.sql.DataSource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Initializer to populate the database with the BookNest catalog.
 * <p>
 * Roles and the admin user are created once. The book catalog (categories,
 * authors, seed reviewer accounts, and books) is loaded from the bundled
 * {@code data/catalog.json} resource and reconciled into the database on
 * every startup - an existence/equality check per category, author, and
 * book (matched by title), not a one-shot "only seed if the table is
 * empty" guard and not a wipe-and-reload. That matters in production: a
 * previous version of this method skipped reseeding once the row counts
 * looked right, which meant editing catalog.json (a price, a description,
 * a cover image, a corrected ISBN) had no effect on an already-seeded
 * database - and the version before that cleared and reinserted the whole
 * catalog on any mismatch, which would have discarded reviews, cart items,
 * and wishlist entries tied to those rows. Reconciling in place means an
 * edit in catalog.json always reaches the running database on the next
 * restart, and a title that already exists is updated rather than
 * duplicated or wiped. Existing reviews are left untouched; new reviews
 * are only seeded for a book the very first time it's inserted, so restarts
 * never duplicate them. Fields the store itself owns at runtime - stock,
 * sold/view counts, rating and rating count, new-arrival flag - are left
 * alone on an update so this reconciliation can never undo real inventory
 * or admin activity; only the catalog-defining fields catalog.json itself
 * declares are kept in sync.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String CATALOG_RESOURCE = "data/catalog.json";
    private static final String SEED_READER_PASSWORD = "Reader@123";

    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        relaxIsbnNotNullConstraint();
        initializeRoles();
        initializeAdminUser();
        seedCatalog();
    }

    /**
     * {@code spring.jpa.hibernate.ddl-auto=update} only ever adds missing
     * tables/columns - it never relaxes a constraint an existing column
     * already has, on either H2 or MySQL. The {@code isbn} column was
     * originally {@code NOT NULL}; books.isbn is now nullable in the entity
     * (a title this catalog has no verified ISBN for is left blank rather
     * than given a fabricated one), but on a database that already has the
     * books table from before this change, the physical column still
     * enforces NOT NULL until something explicitly alters it. This runs
     * that ALTER once at startup, trying MySQL syntax (production) and
     * falling back to H2 syntax (local/dev) - and is a no-op, not an
     * error, once the column is already nullable or the table doesn't
     * exist yet (a brand-new database created fresh from the current
     * entity mapping has no NOT NULL constraint to relax in the first
     * place).
     */
    private void relaxIsbnNotNullConstraint() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("ALTER TABLE books MODIFY COLUMN isbn VARCHAR(255) NULL");
                log.info("✓ isbn column ALTER (MySQL syntax) succeeded");
            } catch (Exception mysqlAttemptFailed) {
                log.warn("isbn ALTER (MySQL syntax) did not apply: {}", mysqlAttemptFailed.getMessage());
                try {
                    stmt.execute("ALTER TABLE books ALTER COLUMN isbn SET NULL");
                    log.info("✓ isbn column ALTER (H2 syntax) succeeded");
                } catch (Exception h2AttemptFailed) {
                    log.warn("isbn ALTER (H2 syntax) did not apply either: {}", h2AttemptFailed.getMessage());
                }
            }

            // Neither ALTER applying isn't necessarily a problem - a database
            // Hibernate created fresh from the current (already-nullable)
            // entity mapping never had a NOT NULL constraint to relax. What
            // matters is the actual current state, so check it directly
            // (information_schema is queryable the same way on MySQL and H2)
            // and say so loudly if it's still NOT NULL - that combination is
            // exactly what crashed startup once already, and per-book error
            // handling in seedCatalog() means it now degrades to "this one
            // book fails to save" instead of "the whole app won't start",
            // but it's still a real, loggable problem worth knowing about.
            try (var rs = stmt.executeQuery(
                    "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                            + "WHERE TABLE_NAME = 'BOOKS' AND COLUMN_NAME = 'ISBN'")) {
                if (rs.next()) {
                    String isNullable = rs.getString(1);
                    if ("NO".equalsIgnoreCase(isNullable)) {
                        log.error("⚠ isbn column is STILL NOT NULL after both ALTER attempts - "
                                + "the database user Railway is using most likely lacks ALTER/DDL "
                                + "privileges. Books with no verified ISBN will fail to save (and be "
                                + "skipped, not crash the app - see seedCatalog()) until this is fixed "
                                + "directly against the database, e.g.: "
                                + "ALTER TABLE books MODIFY COLUMN isbn VARCHAR(255) NULL;");
                    } else {
                        log.info("✓ isbn column confirmed nullable (IS_NULLABLE={})", isNullable);
                    }
                }
            } catch (Exception verifyFailed) {
                log.debug("Could not verify isbn nullability via information_schema: {}", verifyFailed.getMessage());
            }
        } catch (Exception e) {
            log.warn("⚠ Could not verify/relax isbn NOT NULL constraint: {}", e.getMessage());
        }
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

        Map<String, Category> categoryByName = new HashMap<>();
        for (Category c : categoryRepository.findAll()) {
            categoryByName.put(c.getName(), c);
        }
        int categoriesInserted = 0;
        for (JsonNode c : root.get("categories")) {
            String name = c.get("name").asText();
            Category category = categoryByName.get(name);
            if (category == null) {
                category = new Category();
                category.setName(name);
                category.setDescription(c.get("description").asText());
                categoryByName.put(name, categoryRepository.save(category));
                categoriesInserted++;
            } else if (category.getDeleted() != null && category.getDeleted()) {
                category.setDeleted(false);
                categoryRepository.save(category);
            }
        }

        Map<String, Author> authorByName = new HashMap<>();
        for (Author a : authorRepository.findAll()) {
            authorByName.put(a.getName(), a);
        }
        int authorsInserted = 0;
        for (JsonNode a : root.get("authors")) {
            String name = a.get("name").asText();
            String biography = a.get("biography").asText();
            Author author = authorByName.get(name);
            if (author == null) {
                author = new Author();
                author.setName(name);
                author.setBiography(biography);
                authorByName.put(name, authorRepository.save(author));
                authorsInserted++;
            } else if (!biography.equals(author.getBiography())) {
                author.setBiography(biography);
                authorRepository.save(author);
            }
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

        Map<String, Book> existingByTitle = new HashMap<>();
        for (Book b : bookRepository.findAll()) {
            existingByTitle.put(b.getTitle(), b);
        }

        int bookCount = 0;
        int updated = 0;
        int reviewCount = 0;
        int failed = 0;
        Set<String> catalogTitles = new HashSet<>();
        Random rnd = new Random(20260906L);
        for (JsonNode b : root.get("books")) {
            String title = b.get("title").asText();
            catalogTitles.add(title);
            Category category = categoryByName.get(b.get("category").asText());
            Author author = authorByName.get(b.get("author").asText());
            if (category == null || author == null) {
                continue;
            }

            // One bad row (a constraint the live database enforces more
            // strictly than the current entity mapping expects, a value
            // catalog.json didn't anticipate, anything unforeseen) must
            // never be able to crash startup and take the whole site down
            // with it - that happened once already, from an ISBN this
            // reconciliation tried to null out on a database whose isbn
            // column a runtime ALTER couldn't actually make nullable (a
            // managed database's application user commonly lacks DDL
            // privileges, silently short-circuiting relaxIsbnNotNullConstraint
            // above). So: catch per book, log it, keep going - a partially
            // reconciled catalog serving traffic beats no catalog at all.
            boolean isNew = (existingByTitle.get(title) == null);
            try {
                Book book = existingByTitle.get(title);
                if (isNew) {
                    book = new Book();
                }

                book.setTitle(title);
                book.setDescription(b.get("description").asText());
                book.setIsbn(textOrNull(b.get("isbn")));
                book.setPublisher(b.get("publisher").asText());
                book.setLanguage(b.get("language").asText());
                book.setPages(b.get("pages").asInt());
                book.setPrice(BigDecimal.valueOf(b.get("price").asDouble()));
                book.setDiscount(BigDecimal.valueOf(b.get("discount").asDouble()));
                book.setImageUrl(resolveImageUrl(b.get("image").asText()));
                book.setFeatured(b.get("featured").asBoolean());
                book.setBestSeller(b.get("bestSeller").asBoolean());
                book.setCategory(category);
                book.setAuthor(author);
                book.setDeleted(false);
                LocalDate publishedDate = LocalDate.parse(b.get("publishedDate").asText());
                book.setPublishedDate(publishedDate.atStartOfDay());

                // Store-owned fields: seeded only for a brand-new title, then left
                // alone forever after so this reconciliation can never undo real
                // stock changes, sold/view counts, or the rating a book has earned
                // from its (never re-seeded) reviews.
                if (isNew) {
                    book.setStock(b.get("stock").asInt());
                    book.setRating(BigDecimal.valueOf(b.get("rating").asDouble()));
                    book.setRatingCount(b.get("ratingCount").asInt());
                    book.setNewArrival(rnd.nextDouble() < 0.12);
                    book.setSoldCount(rnd.nextInt(300));
                    book.setViewCount(rnd.nextInt(1500));
                }

                book = bookRepository.save(book);
                existingByTitle.put(title, book);
                bookCount++;
                if (isNew) {
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
                } else {
                    updated++;
                }
            } catch (Exception e) {
                failed++;
                log.error("⚠ Skipping '{}' - failed to {} during catalog reconciliation: {}",
                        title, isNew ? "insert" : "update", e.getMessage());
            }
        }

        // A book that exists in the database but is no longer part of
        // catalog.json (a title removed or renamed in a later edit) is
        // soft-deleted rather than left behind - otherwise it keeps
        // showing up in the storefront indefinitely, invisible to every
        // check above because those all key off catalog.json's own title
        // list. Soft-delete (the same mechanism @SQLDelete already uses
        // for an admin-initiated delete) rather than a hard delete, since
        // this row may still be referenced by past orders/reviews.
        int retired = 0;
        for (Map.Entry<String, Book> entry : existingByTitle.entrySet()) {
            Book existing = entry.getValue();
            boolean stillActive = existing.getDeleted() == null || !existing.getDeleted();
            if (stillActive && !catalogTitles.contains(entry.getKey())) {
                existing.setDeleted(true);
                bookRepository.save(existing);
                retired++;
            }
        }

        System.out.println("✓ Catalog reconciled: " + categoriesInserted + " categories added, " + authorsInserted
                + " authors added, " + (bookCount - updated) + " books added, " + updated
                + " books updated, " + retired + " retired (no longer in catalog.json), "
                + failed + " skipped due to errors, "
                + reviewers.size() + " reviewers, " + reviewCount + " new reviews");
    }

    /**
     * catalog.json's "image" field is either a real cover's absolute URL
     * (e.g. an Open Library cover CDN link, {@code https://covers.openlibrary
     * .org/b/id/12539702-L.jpg}) or, for the handful of titles no reliable
     * bibliographic source had a verifiable cover for, a filename served
     * from the bundled {@code /images/books/} static resources.
     */
    private static String resolveImageUrl(String image) {
        return image.startsWith("http://") || image.startsWith("https://")
                ? image
                : "/images/books/" + image;
    }

    /** Jackson's {@code asText()} throws on a missing node and returns "" for a JSON
     *  null, neither of which is what an intentionally blank catalog field (e.g. an
     *  ISBN this catalog couldn't verify) should become on a nullable column. */
    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text.isBlank() ? null : text;
    }

    private JsonNode loadCatalog() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = new ClassPathResource(CATALOG_RESOURCE).getInputStream()) {
            return mapper.readTree(is);
        }
    }
}
