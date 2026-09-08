package com.booknest.config;

import com.booknest.entity.*;
import com.booknest.repository.*;
import com.booknest.util.CategoryCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Data Initializer to populate database with sample data
 * Creates categories, authors, books (100+ titles across English and major
 * Indian languages), and the admin user.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /** Generic, royalty-free cover photography reused across catalog entries. */
    private static final String[] COVERS = {
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
        "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&h=800&fit=crop",
        "https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=600&h=800&fit=crop",
        "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&h=800&fit=crop",
        "https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=600&h=800&fit=crop",
        "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=600&h=800&fit=crop"
    };

    private final Map<String, Category> categoryMap = new HashMap<>();
    private final Map<String, Author> authorMap = new HashMap<>();
    private int seq = 0;

    @Override
    public void run(String... args) {
        initializeRoles();
        initializeAdminUser();
        initializeCategories();
        initializeAuthors();
        initializeBooks();
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

    /**
     * Reconciles the database against {@link CategoryCatalog#CATEGORIES}, the single
     * canonical category list. Unlike a one-shot "insert only if the table is empty"
     * seed, this runs an existence check per category every startup, so any category
     * that is missing (e.g. left over from an older, smaller taxonomy, or never
     * inserted due to a partial prior run) is added automatically instead of silently
     * staying absent - which is what previously caused newly-added categories to have
     * no books: the category row itself didn't exist, or a stale duplicate did, so
     * lookups by name/id fell out of sync with what the book data referenced.
     */
    private void initializeCategories() {
        int inserted = 0;
        for (String[] cat : CategoryCatalog.CATEGORIES) {
            String name = cat[0].trim();
            if (categoryRepository.findByName(name).isEmpty()) {
                Category category = new Category();
                category.setName(name);
                category.setDescription(cat[1]);
                categoryRepository.save(category);
                inserted++;
            }
        }
        if (inserted > 0) {
            System.out.println("✓ Categories reconciled (" + inserted + " added, "
                + CategoryCatalog.CATEGORIES.length + " total canonical categories)");
        }
        for (Category c : categoryRepository.findAll()) {
            categoryMap.put(c.getName(), c);
        }
    }

    /**
     * Reconciles authors the same way {@link #initializeCategories()} reconciles
     * categories: an existence check per author on every startup (not just when the
     * table is empty), so an author referenced by a book newly added to this file is
     * never missing and never inserted twice. Authors have no admin-editable fields, so
     * it is safe to also keep an existing author's biography in sync with this file.
     */
    private void initializeAuthors() {
        {
            String[][] authors = {
                // English
                {"James Clear", "American author and speaker focused on habits and continuous improvement."},
                {"Robert T. Kiyosaki", "Entrepreneur and financial-education author of the Rich Dad series."},
                {"Morgan Housel", "Financial writer and partner at The Collaborative Fund."},
                {"Héctor García & Francesc Miralles", "Spanish authors who popularized the Japanese concept of ikigai."},
                {"Cal Newport", "Computer scientist and author writing on focus and deep work."},
                {"Robert C. Martin", "Veteran software engineer known for advocating clean coding practices."},
                {"Joshua Bloch", "Software engineer and former Java architect at Sun Microsystems and Google."},
                {"David Thomas & Andrew Hunt", "Software consultants and authors of pragmatic programming guides."},
                {"J.K. Rowling", "British author best known for the Harry Potter series."},
                {"J.R.R. Tolkien", "English writer and philologist, author of Middle-earth fantasy classics."},
                {"Paulo Coelho", "Brazilian novelist celebrated for allegorical, spiritual fiction."},
                {"Peter Thiel", "Entrepreneur, investor, and co-founder of PayPal and Palantir."},
                {"Benjamin Graham", "Economist regarded as the father of value investing."},
                {"Eric Ries", "Entrepreneur who pioneered the Lean Startup methodology."},
                {"Simon Sinek", "Author and speaker on leadership and organizational purpose."},
                {"Jim Collins", "Business researcher and author studying enduring great companies."},
                {"Stephen R. Covey", "Educator and author on leadership and personal effectiveness."},
                {"Yuval Noah Harari", "Israeli historian and author of sweeping histories of humankind."},
                {"Anne Frank", "German-Dutch diarist whose wartime writings became a global classic."},
                {"A.P.J. Abdul Kalam", "Indian aerospace scientist and 11th President of India."},
                {"Walter Isaacson", "American journalist and biographer of leading innovators."},
                {"Stephen Hawking", "British theoretical physicist and cosmologist."},
                {"Carl Sagan", "American astronomer and science communicator."},
                {"Dan Brown", "American author of best-selling mystery-thriller novels."},
                {"Agatha Christie", "English writer best known for classic detective novels."},
                {"Gillian Flynn", "American author of psychological thriller fiction."},
                {"Jane Austen", "English novelist known for wit and social commentary."},
                {"John Green", "American author of contemporary young-adult fiction."},
                {"George Orwell", "English novelist and essayist known for political fiction."},
                {"Harper Lee", "American novelist best known for her Pulitzer Prize-winning work."},
                {"Antoine de Saint-Exupéry", "French writer and aviator."},
                {"E.B. White", "American writer and essayist known for children's classics."},
                {"Roald Dahl", "British author celebrated for inventive children's fiction."},
                {"Marcus Aurelius", "Roman emperor and Stoic philosopher."},
                {"Plato", "Classical Greek philosopher and founder of the Academy in Athens."},
                {"Paramahansa Yogananda", "Indian yogi and spiritual teacher."},
                {"Eckhart Tolle", "German-born spiritual teacher and author."},
                {"Thomas H. Cormen & Charles E. Leiserson", "Computer scientists and co-authors of a leading algorithms text."},
                {"Gayle Laakmann McDowell", "Software engineer and author on technical interview preparation."},
                {"R.S. Aggarwal", "Indian author of widely used mathematics and aptitude textbooks."},
                {"Norman Lewis", "American author known for vocabulary-building guides."},
                {"Aravind Adiga", "Indian author and journalist, Booker Prize winner."},
                {"R. K. Narayan", "Indian author known for stories set in fictional Malgudi."},
                {"Ramachandra Guha", "Indian historian and biographer."},
                {"Angela Duckworth", "American psychologist known for research on grit."},
                {"Tara Westover", "American memoirist and historian."},
                {"Nelson Mandela", "South African anti-apartheid leader and former President."},
                {"Bessel van der Kolk", "Psychiatrist and researcher on trauma."},
                {"Dale Carnegie", "American writer and pioneer of self-improvement courses."},
                {"R.J. Palacio", "American author known for contemporary children's fiction."},
                {"Vasant Lad", "Ayurvedic physician and author introducing Ayurveda to Western readers."},

                // Hindi
                {"Munshi Premchand", "Pioneering Hindi-Urdu novelist known for realist social fiction."},
                {"Harivansh Rai Bachchan", "Celebrated Hindi poet of the Chhayavad romantic tradition."},
                {"Mahadevi Verma", "Hindi poet and Jnanpith Award laureate."},
                {"Ramdhari Singh Dinkar", "Hindi poet known as the national poet of vigor and rebellion."},
                {"Jaishankar Prasad", "Hindi poet, playwright, and pillar of the Chhayavad movement."},
                {"Shrilal Shukla", "Hindi novelist known for political satire."},
                {"Devaki Nandan Khatri", "Pioneer of popular Hindi adventure fiction."},
                {"Phanishwar Nath Renu", "Hindi novelist known for regional fiction rooted in rural Bihar."},

                // Marathi
                {"P. L. Deshpande", "Beloved Marathi humorist, playwright, and essayist."},
                {"V. S. Khandekar", "Marathi novelist and Jnanpith Award laureate."},
                {"V. P. Kale", "Marathi author known for humorous social sketches."},
                {"Kusumagraj", "Pen name of V. V. Shirwadkar, Marathi poet and Jnanpith laureate."},
                {"Shivaji Sawant", "Marathi novelist known for epic historical fiction."},
                {"Sane Guruji", "Marathi writer and freedom fighter known for moral children's literature."},
                {"Bhalchandra Nemade", "Marathi novelist and Jnanpith Award laureate."},

                // Bengali
                {"Rabindranath Tagore", "Bengali polymath, Nobel laureate in Literature."},
                {"Sarat Chandra Chattopadhyay", "Bengali novelist known for emotionally rich social fiction."},
                {"Bankim Chandra Chattopadhyay", "Bengali novelist and author of Anandamath."},
                {"Bibhutibhushan Bandyopadhyay", "Bengali novelist known for rural coming-of-age fiction."},
                {"Satyajit Ray", "Bengali filmmaker and author, creator of detective Feluda."},

                // Gujarati
                {"Govardhanram Tripathi", "Gujarati novelist known for the monumental Saraswatichandra."},
                {"K. M. Munshi", "Gujarati writer, lawyer, and founder of the Bharatiya Vidya Bhavan."},
                {"Pannalal Patel", "Gujarati novelist and Jnanpith Award laureate."},
                {"Jhaverchand Meghani", "Gujarati poet and folklorist known as Rashtriya Shayar."},
                {"Ramanbhai Neelkanth", "Gujarati writer known for satirical fiction."},
                {"Chunilal Madia", "Gujarati novelist and short-story writer."},

                // Tamil
                {"Kalki Krishnamurthy", "Tamil journalist and author of historical epics."},
                {"Thiruvalluvar", "Ancient Tamil poet and philosopher, author of the Thirukkural."},
                {"Ilango Adigal", "Ancient Tamil poet-prince, author of the Silappatikaram."},
                {"Sujatha Rangarajan", "Tamil writer known for science fiction and popular fiction."},
                {"Jeyakanthan", "Tamil writer and Jnanpith Award laureate."},
                {"Kambar", "Medieval Tamil poet, author of the Kambaramayanam."},

                // Telugu
                {"Viswanatha Satyanarayana", "Telugu poet and novelist, Jnanpith Award laureate."},
                {"Gurajada Apparao", "Telugu writer and playwright, pioneer of modern Telugu drama."},
                {"Chalam", "Telugu writer known for progressive, boundary-pushing fiction."},
                {"Unnava Lakshminarayana", "Telugu novelist known for social-reform fiction."},
                {"Mokkapati Narasimha Sastry", "Telugu humorist and novelist."},
                {"Nannaya, Tikkana & Yerrapragada", "The trinity of poets who rendered the Mahabharata into Telugu."},
                {"Krishnadevaraya", "Vijayanagara emperor and celebrated Telugu poet."},
                {"Buchi Babu", "Telugu novelist known for introspective, psychological fiction."},

                // Kannada
                {"Kuvempu", "Pen name of K. V. Puttappa, Kannada poet and Jnanpith laureate."},
                {"U. R. Ananthamurthy", "Kannada novelist and a leading voice of the Navya movement."},
                {"S. L. Bhyrappa", "Kannada novelist known for philosophically dense fiction."},
                {"K. Shivarama Karanth", "Kannada writer and Jnanpith Award laureate."},

                // Malayalam
                {"M. T. Vasudevan Nair", "Malayalam novelist and screenwriter, Jnanpith laureate."},
                {"Thakazhi Sivasankara Pillai", "Malayalam novelist and Jnanpith Award laureate."},
                {"O. V. Vijayan", "Malayalam novelist and cartoonist known for modernist fiction."},
                {"Vaikom Muhammad Basheer", "Malayalam writer known for humane, humorous storytelling."},
                {"S. K. Pottekkatt", "Malayalam novelist and travel writer, Jnanpith laureate."},
                {"Benyamin", "Contemporary Malayalam novelist known for migrant-life fiction."},

                // Punjabi
                {"Amrita Pritam", "Punjabi poet and novelist, first woman Sahitya Akademi laureate."},
                {"Nanak Singh", "Pioneering Punjabi novelist of the modern era."},
                {"Shiv Kumar Batalvi", "Punjabi poet celebrated for lyrical, romantic verse."},
                {"Kartar Singh Duggal", "Punjabi short-story writer and novelist."},
                {"Gurdial Singh", "Punjabi novelist and Jnanpith Award laureate."},

                // Urdu
                {"Mirza Ghalib", "Classical Urdu and Persian poet of the Mughal era."},
                {"Saadat Hasan Manto", "Urdu short-story writer known for unflinching Partition fiction."},
                {"Mirza Hadi Ruswa", "Urdu novelist known for Umrao Jaan Ada."},
                {"Ismat Chughtai", "Urdu writer known for bold, feminist short fiction."},
                {"Rajinder Singh Bedi", "Urdu short-story writer and novelist."},
                {"Qurratulain Hyder", "Urdu novelist known for epic, historical fiction."}
            };

            for (Author a : authorRepository.findAll()) {
                authorMap.put(a.getName(), a);
            }

            int inserted = 0;
            for (String[] author : authors) {
                String name = author[0];
                Author auth = authorMap.get(name);
                if (auth == null) {
                    auth = new Author();
                    auth.setName(name);
                    auth.setBiography(author[1]);
                    auth = authorRepository.save(auth);
                    authorMap.put(name, auth);
                    inserted++;
                } else if (!author[1].equals(auth.getBiography())) {
                    auth.setBiography(author[1]);
                    authorRepository.save(auth);
                }
            }
            if (inserted > 0) {
                System.out.println("✓ Authors reconciled (" + inserted + " added, " + authors.length + " total canonical authors)");
            }
        }
    }

    private void initializeBooks() {
        java.util.List<Book> books = new java.util.ArrayList<>();

        // ==================== ENGLISH ====================
        books.add(b("Atomic Habits", "James Clear", "Self Development", "English",
            "A practical guide to building good habits and breaking bad ones through tiny, consistent changes.",
            399, 20, 4.8, 320, "Random House India", true, true));
        books.add(b("Rich Dad Poor Dad", "Robert T. Kiyosaki", "Finance & Investing", "English",
            "Robert Kiyosaki contrasts two father figures to teach lessons on financial literacy and building wealth.",
            349, 20, 4.7, 288, "Plata Publishing", true, true));
        books.add(b("The Psychology of Money", "Morgan Housel", "Finance & Investing", "English",
            "Timeless lessons on how people think about money, risk, and long-term wealth.",
            399, 15, 4.8, 256, "Jaico Publishing House", true, true));
        books.add(b("Ikigai", "Héctor García & Francesc Miralles", "Self Development", "English",
            "A Japanese-inspired exploration of purpose, longevity, and everyday happiness.",
            349, 25, 4.6, 208, "Penguin Random House India", true, true));
        books.add(b("Deep Work", "Cal Newport", "Self Development", "English",
            "A framework for cultivating focused, distraction-free work in a noisy world.",
            449, 15, 4.6, 296, "Piatkus", true, false));
        books.add(b("Clean Code", "Robert C. Martin", "Programming & Technology", "English",
            "A handbook of agile software craftsmanship for writing readable, maintainable code.",
            699, 10, 4.8, 464, "Pearson Education", true, true));
        books.add(b("Effective Java", "Joshua Bloch", "Programming & Technology", "English",
            "Expert best practices and idioms for writing robust, efficient Java code.",
            899, 10, 4.8, 412, "Addison-Wesley", true, false));
        books.add(b("The Pragmatic Programmer", "David Thomas & Andrew Hunt", "Programming & Technology", "English",
            "Timeless, practical advice for becoming a more effective and thoughtful developer.",
            699, 15, 4.9, 352, "Addison-Wesley", true, true));
        books.add(b("Harry Potter and the Sorcerer's Stone", "J.K. Rowling", "Fiction", "English",
            "A young boy discovers he is a wizard and begins his adventures at Hogwarts School.",
            599, 20, 4.9, 320, "Bloomsbury", true, true));
        books.add(b("The Hobbit", "J.R.R. Tolkien", "Fiction", "English",
            "Bilbo Baggins is swept into an epic quest to reclaim a dwarf kingdom from a dragon.",
            449, 15, 4.8, 310, "HarperCollins India", true, false));
        books.add(b("The Alchemist", "Paulo Coelho", "Fiction", "English",
            "A shepherd boy's journey across the desert in search of treasure and his personal legend.",
            299, 30, 4.7, 197, "HarperOne", true, true));
        books.add(b("Zero to One", "Peter Thiel", "Entrepreneurship", "English",
            "Peter Thiel's contrarian insights on building startups that create truly new value.",
            449, 20, 4.6, 224, "Crown Business", true, false));
        books.add(b("The Intelligent Investor", "Benjamin Graham", "Finance & Investing", "English",
            "Benjamin Graham's classic framework for disciplined, value-based investing.",
            799, 15, 4.7, 640, "Harper Business", true, true));
        books.add(b("The Lean Startup", "Eric Ries", "Entrepreneurship", "English",
            "A methodology for building businesses through rapid experimentation and validated learning.",
            499, 15, 4.5, 336, "Portfolio Penguin", false, false));
        books.add(b("Start with Why", "Simon Sinek", "Entrepreneurship", "English",
            "Simon Sinek explains how great leaders and organizations inspire action by starting with purpose.",
            399, 20, 4.6, 256, "Portfolio Penguin", false, true));
        books.add(b("Good to Great", "Jim Collins", "Management", "English",
            "Jim Collins researches why some companies make the leap from good to truly great.",
            549, 10, 4.5, 320, "Harper Business", false, false));
        books.add(b("The 7 Habits of Highly Effective People", "Stephen R. Covey", "Self Development", "English",
            "A principle-centered approach to personal and professional effectiveness.",
            399, 15, 4.7, 372, "Simon & Schuster", true, false));
        books.add(b("Sapiens", "Yuval Noah Harari", "History", "English",
            "A sweeping account of how Homo sapiens came to dominate the world.",
            499, 20, 4.8, 464, "Vintage Books", true, true));
        books.add(b("The Diary of a Young Girl", "Anne Frank", "Biography & Memoir", "English",
            "Anne Frank's poignant diary written while hiding from Nazi persecution.",
            299, 20, 4.7, 283, "Penguin Classics", false, false));
        books.add(b("Wings of Fire", "A.P.J. Abdul Kalam", "Biography & Memoir", "English",
            "The inspiring autobiography of India's Missile Man and former President, A.P.J. Abdul Kalam.",
            349, 25, 4.8, 260, "Universities Press", true, true));
        books.add(b("Steve Jobs", "Walter Isaacson", "Biography & Memoir", "English",
            "Walter Isaacson's definitive biography of Apple's visionary co-founder.",
            599, 15, 4.6, 656, "Simon & Schuster", false, false));
        books.add(b("A Brief History of Time", "Stephen Hawking", "Science", "English",
            "Stephen Hawking's accessible explanation of cosmology, black holes, and the universe.",
            449, 20, 4.7, 256, "Bantam Books", true, false));
        books.add(b("Cosmos", "Carl Sagan", "Science", "English",
            "Carl Sagan's celebrated tour of the universe and humanity's place within it.",
            499, 15, 4.8, 384, "Ballantine Books", false, false));
        books.add(b("The Da Vinci Code", "Dan Brown", "Mystery & Thriller", "English",
            "A murder in the Louvre leads symbologist Robert Langdon into a web of ancient secrets.",
            399, 20, 4.4, 480, "Doubleday", true, true));
        books.add(b("And Then There Were None", "Agatha Christie", "Mystery & Thriller", "English",
            "Ten strangers are trapped on an island and murdered one by one in Agatha Christie's classic.",
            299, 15, 4.7, 264, "HarperCollins India", false, false));
        books.add(b("Gone Girl", "Gillian Flynn", "Mystery & Thriller", "English",
            "A woman's disappearance turns her marriage into the center of a media firestorm and a twisted mystery.",
            349, 20, 4.4, 432, "Crown Publishing", false, false));
        books.add(b("Pride and Prejudice", "Jane Austen", "Romance", "English",
            "Jane Austen's beloved novel of wit, romance, and social manners in Regency England.",
            249, 25, 4.7, 279, "Penguin Classics", true, false));
        books.add(b("The Fault in Our Stars", "John Green", "Romance", "English",
            "Two teenagers with cancer fall in love in John Green's moving story.",
            299, 20, 4.6, 313, "Penguin Books", false, true));
        books.add(b("1984", "George Orwell", "Fiction", "English",
            "George Orwell's dystopian warning about totalitarianism, surveillance, and truth.",
            299, 20, 4.7, 328, "Penguin Classics", true, true));
        books.add(b("To Kill a Mockingbird", "Harper Lee", "Fiction", "English",
            "Harper Lee's landmark novel of racial injustice seen through a child's eyes in the American South.",
            349, 15, 4.8, 336, "Harper Perennial", false, false));
        books.add(b("The Little Prince", "Antoine de Saint-Exupéry", "Kids", "English",
            "A poetic tale of a young prince's travels and timeless lessons on love and loss.",
            249, 20, 4.8, 96, "Harcourt", true, false));
        books.add(b("Charlotte's Web", "E.B. White", "Kids", "English",
            "A pig named Wilbur is saved by his unlikely friend, a clever spider named Charlotte.",
            249, 15, 4.7, 184, "HarperCollins India", false, false));
        books.add(b("Matilda", "Roald Dahl", "Kids", "English",
            "Roald Dahl's story of a brilliant girl with a gift for mischief and telekinesis.",
            279, 20, 4.7, 240, "Puffin Books", false, false));
        books.add(b("Meditations", "Marcus Aurelius", "Philosophy", "English",
            "The private reflections of Roman Emperor Marcus Aurelius on virtue and duty.",
            249, 25, 4.7, 256, "Penguin Classics", true, false));
        books.add(b("The Republic", "Plato", "Philosophy", "English",
            "Plato's foundational dialogue on justice, politics, and the ideal state.",
            299, 15, 4.5, 416, "Penguin Classics", false, false));
        books.add(b("Autobiography of a Yogi", "Paramahansa Yogananda", "Spirituality", "English",
            "Paramahansa Yogananda's spiritual memoir introducing yoga and meditation to the West.",
            349, 20, 4.8, 528, "Self-Realization Fellowship", true, false));
        books.add(b("The Power of Now", "Eckhart Tolle", "Spirituality", "English",
            "Eckhart Tolle's guide to finding peace by living fully in the present moment.",
            349, 10, 4.6, 236, "New World Library", false, true));
        books.add(b("Introduction to Algorithms", "Thomas H. Cormen & Charles E. Leiserson", "Programming & Technology", "English",
            "A comprehensive, rigorous introduction to the design and analysis of algorithms.",
            1299, 15, 4.7, 1312, "MIT Press", false, false));
        books.add(b("Cracking the Coding Interview", "Gayle Laakmann McDowell", "Programming & Technology", "English",
            "189 programming interview questions and solutions to help ace technical interviews.",
            899, 10, 4.8, 687, "CareerCup", true, true));
        books.add(b("Quantitative Aptitude for Competitive Examinations", "R.S. Aggarwal", "Competitive Exams", "English",
            "A comprehensive resource for mastering quantitative aptitude for competitive exams.",
            399, 20, 4.5, 960, "S. Chand Publishing", true, true));
        books.add(b("Word Power Made Easy", "Norman Lewis", "Competitive Exams", "English",
            "Norman Lewis's classic, systematic approach to building a powerful vocabulary.",
            299, 15, 4.6, 432, "Goyal Publishers", false, false));
        books.add(b("The White Tiger", "Aravind Adiga", "Fiction", "English",
            "Aravind Adiga's Booker Prize-winning tale of ambition and class in modern India.",
            349, 20, 4.4, 321, "HarperCollins India", false, false));
        books.add(b("Malgudi Days", "R. K. Narayan", "Fiction", "English",
            "R. K. Narayan's beloved short stories set in the fictional town of Malgudi.",
            299, 15, 4.7, 245, "Indian Thought Publications", false, false));
        books.add(b("India After Gandhi", "Ramachandra Guha", "History", "English",
            "Ramachandra Guha's sweeping history of India since independence.",
            699, 15, 4.6, 928, "Picador India", false, false));
        books.add(b("Grit", "Angela Duckworth", "Self Development", "English",
            "Angela Duckworth explores why passion and perseverance matter more than talent.",
            399, 15, 4.5, 352, "Scribner", false, false));
        books.add(b("Educated", "Tara Westover", "Biography & Memoir", "English",
            "Tara Westover's memoir of growing up off-grid and her journey to a formal education.",
            399, 20, 4.8, 352, "Windmill Books", true, false));
        books.add(b("Long Walk to Freedom", "Nelson Mandela", "Biography & Memoir", "English",
            "Nelson Mandela's autobiography chronicling his fight against apartheid.",
            499, 15, 4.8, 656, "Little, Brown and Company", false, true));
        books.add(b("The Body Keeps the Score", "Bessel van der Kolk", "Health & Wellness", "English",
            "Bessel van der Kolk explains how trauma reshapes the body and mind, and paths to healing.",
            499, 15, 4.7, 464, "Penguin Books", false, false));
        books.add(b("How to Win Friends and Influence People", "Dale Carnegie", "Education & Academics", "English",
            "Dale Carnegie's timeless guide to communication, persuasion, and building relationships.",
            299, 20, 4.7, 291, "Simon & Schuster", true, true));
        books.add(b("Wonder", "R.J. Palacio", "Kids", "English",
            "A boy with facial differences navigates friendship and kindness in his first year at school.",
            299, 15, 4.8, 316, "Corgi Books", false, false));
        books.add(b("Ayurveda: The Science of Self-Healing", "Vasant Lad", "Health & Wellness", "English",
            "An accessible introduction to Ayurvedic principles for health and self-healing.",
            349, 10, 4.5, 158, "Motilal Banarsidass", false, false));

        // ==================== HINDI ====================
        books.add(b("Godaan", "Munshi Premchand", "Hindi Literature", "Hindi",
            "Munshi Premchand's epic portrayal of the struggles of a poor farmer and rural Indian society.",
            249, 15, 4.7, 366, "Rajkamal Prakashan", true, true));
        books.add(b("Gaban", "Munshi Premchand", "Hindi Literature", "Hindi",
            "Premchand's novel exploring greed, deceit, and the moral compromises of middle-class ambition.",
            199, 15, 4.5, 312, "Rajkamal Prakashan", false, false));
        books.add(b("Nirmala", "Munshi Premchand", "Hindi Literature", "Hindi",
            "Premchand's tragic novel on the evils of dowry and child marriage in early 20th-century India.",
            179, 20, 4.5, 176, "Lokbharti Prakashan", false, false));
        books.add(b("Madhushala", "Harivansh Rai Bachchan", "Poetry", "Hindi",
            "Harivansh Rai Bachchan's celebrated poem collection using the metaphor of a tavern for life's philosophy.",
            199, 15, 4.8, 192, "Rajpal & Sons", true, false));
        books.add(b("Yama", "Mahadevi Verma", "Poetry", "Hindi",
            "Mahadevi Verma's Jnanpith Award-winning collection of lyrical Hindi poetry.",
            179, 10, 4.6, 168, "Lokbharti Prakashan", false, false));
        books.add(b("Rashmirathi", "Ramdhari Singh Dinkar", "Poetry", "Hindi",
            "Ramdhari Singh Dinkar's epic poem retelling the Mahabharata from Karna's perspective.",
            199, 15, 4.8, 184, "Lokbharti Prakashan", true, true));
        books.add(b("Kurukshetra", "Ramdhari Singh Dinkar", "Poetry", "Hindi",
            "Dinkar's philosophical epic poem reflecting on war and peace through the Mahabharata.",
            199, 10, 4.6, 160, "Lokbharti Prakashan", false, false));
        books.add(b("Kamayani", "Jaishankar Prasad", "Hindi Literature", "Hindi",
            "Jaishankar Prasad's celebrated epic poem exploring human emotion through the myth of Manu.",
            219, 15, 4.6, 224, "Vani Prakashan", false, false));
        books.add(b("Raag Darbari", "Shrilal Shukla", "Hindi Literature", "Hindi",
            "Shrilal Shukla's satirical masterpiece on politics and corruption in rural India.",
            299, 15, 4.7, 368, "Rajkamal Prakashan", true, false));
        books.add(b("Chandrakanta", "Devaki Nandan Khatri", "Hindi Literature", "Hindi",
            "Devaki Nandan Khatri's classic adventure novel credited with popularizing Hindi prose fiction.",
            249, 20, 4.5, 344, "Diamond Books", false, false));
        books.add(b("Maila Anchal", "Phanishwar Nath Renu", "Hindi Literature", "Hindi",
            "Phanishwar Nath Renu's landmark regional novel set in rural Bihar.",
            249, 15, 4.6, 352, "Rajkamal Prakashan", false, false));

        // ==================== MARATHI ====================
        books.add(b("Vyakti Ani Valli", "P. L. Deshpande", "Marathi Literature", "Marathi",
            "P. L. Deshpande's beloved collection of humorous character sketches from everyday Maharashtrian life.",
            249, 15, 4.8, 224, "Mehta Publishing House", true, true));
        books.add(b("Batatyachi Chal", "P. L. Deshpande", "Marathi Literature", "Marathi",
            "P. L. Deshpande's witty portrayal of life in a traditional Mumbai chawl.",
            229, 15, 4.6, 192, "Mehta Publishing House", false, false));
        books.add(b("Yayati", "V. S. Khandekar", "Marathi Literature", "Marathi",
            "V. S. Khandekar's Jnanpith Award-winning novel reimagining the mythological king Yayati.",
            279, 20, 4.8, 296, "Mehta Publishing House", true, true));
        books.add(b("Kande Pohe", "V. P. Kale", "Marathi Literature", "Marathi",
            "V. P. Kale's humorous classic on the traditional Maharashtrian bride-seeing ceremony.",
            199, 10, 4.5, 176, "Mehta Publishing House", false, false));
        books.add(b("Vishakha", "Kusumagraj", "Poetry", "Marathi",
            "Kusumagraj's acclaimed collection of Marathi poetry.",
            179, 15, 4.7, 144, "Popular Prakashan", false, false));
        books.add(b("Natasamrat", "Kusumagraj", "Marathi Literature", "Marathi",
            "Kusumagraj's celebrated play about an aging actor's tragic later years.",
            229, 15, 4.8, 168, "Popular Prakashan", true, false));
        books.add(b("Mrityunjay", "Shivaji Sawant", "Marathi Literature", "Marathi",
            "Shivaji Sawant's epic novel giving voice to Karna, the tragic hero of the Mahabharata.",
            349, 20, 4.9, 592, "Mehta Publishing House", true, true));
        books.add(b("Chhava", "Shivaji Sawant", "Marathi Literature", "Marathi",
            "Shivaji Sawant's stirring novel on the life of Chhatrapati Sambhaji Maharaj.",
            349, 15, 4.8, 624, "Continental Prakashan", false, true));
        books.add(b("Shyamchi Aai", "Sane Guruji", "Marathi Literature", "Marathi",
            "Sane Guruji's tender, autobiographical tales of a mother's moral teachings.",
            199, 15, 4.7, 208, "Continental Prakashan", false, false));
        books.add(b("Kosala", "Bhalchandra Nemade", "Marathi Literature", "Marathi",
            "Bhalchandra Nemade's groundbreaking modern Marathi novel of youth and alienation.",
            249, 10, 4.6, 256, "Popular Prakashan", false, false));

        // ==================== BENGALI ====================
        books.add(b("Gitanjali", "Rabindranath Tagore", "Poetry", "Bengali",
            "Rabindranath Tagore's Nobel Prize-winning collection of devotional and lyrical poems.",
            249, 20, 4.9, 168, "Visva-Bharati", true, true));
        books.add(b("Gora", "Rabindranath Tagore", "Bengali Literature", "Bengali",
            "Tagore's sweeping novel exploring identity and nationalism in colonial Bengal.",
            349, 15, 4.6, 480, "Visva-Bharati", false, false));
        books.add(b("Kabuliwala", "Rabindranath Tagore", "Bengali Literature", "Bengali",
            "Tagore's touching short story of friendship between a Bengali girl and an Afghan trader.",
            149, 15, 4.7, 96, "Ananda Publishers", true, false));
        books.add(b("Devdas", "Sarat Chandra Chattopadhyay", "Bengali Literature", "Bengali",
            "Sarat Chandra Chattopadhyay's tragic love story of a man's self-destructive devotion.",
            249, 20, 4.6, 176, "Ananda Publishers", true, true));
        books.add(b("Parineeta", "Sarat Chandra Chattopadhyay", "Bengali Literature", "Bengali",
            "Sarat Chandra Chattopadhyay's tender novel of love and social convention.",
            199, 15, 4.6, 128, "Ananda Publishers", false, false));
        books.add(b("Anandamath", "Bankim Chandra Chattopadhyay", "Bengali Literature", "Bengali",
            "Bankim Chandra Chattopadhyay's novel of rebellion, home to the song Vande Mataram.",
            229, 15, 4.5, 240, "Dey's Publishing", false, false));
        books.add(b("Pather Panchali", "Bibhutibhushan Bandyopadhyay", "Bengali Literature", "Bengali",
            "Bibhutibhushan Bandyopadhyay's classic coming-of-age novel of rural Bengal.",
            299, 15, 4.7, 384, "Dey's Publishing", false, false));
        books.add(b("Sonar Kella", "Satyajit Ray", "Mystery & Thriller", "Bengali",
            "Satyajit Ray's beloved Feluda mystery, The Golden Fortress.",
            199, 20, 4.7, 152, "Ananda Publishers", false, false));

        // ==================== GUJARATI ====================
        books.add(b("Saraswatichandra", "Govardhanram Tripathi", "Gujarati Literature", "Gujarati",
            "Govardhanram Tripathi's monumental Gujarati novel of social reform and idealism.",
            349, 15, 4.6, 512, "Gurjar Granth Karyalay", true, false));
        books.add(b("Prithvi Vallabh", "K. M. Munshi", "Gujarati Literature", "Gujarati",
            "K. M. Munshi's historical novel of romance and valor in ancient Malwa.",
            249, 15, 4.6, 288, "Gurjar Granth Karyalay", false, false));
        books.add(b("Patanni Prabhuta", "K. M. Munshi", "Gujarati Literature", "Gujarati",
            "K. M. Munshi's historical novel from his acclaimed Gujarat trilogy.",
            249, 10, 4.5, 296, "Gurjar Granth Karyalay", false, false));
        books.add(b("Manvini Bhavai", "Pannalal Patel", "Gujarati Literature", "Gujarati",
            "Pannalal Patel's Jnanpith Award-winning novel of famine and rural resilience.",
            279, 15, 4.7, 320, "Navbharat Sahitya Mandir", true, true));
        books.add(b("Saurashtra ni Rasdhar", "Jhaverchand Meghani", "Gujarati Literature", "Gujarati",
            "Jhaverchand Meghani's celebrated collection of Saurashtra's folk tales and legends.",
            299, 15, 4.7, 336, "Gurjar Granth Karyalay", false, false));
        books.add(b("Sorath Tara Vaheta Pani", "Jhaverchand Meghani", "Gujarati Literature", "Gujarati",
            "Meghani's evocative novel of life and honor in the Saurashtra region.",
            249, 10, 4.6, 264, "Navbharat Sahitya Mandir", false, false));
        books.add(b("Bhadrambhadra", "Ramanbhai Neelkanth", "Gujarati Literature", "Gujarati",
            "Ramanbhai Neelkanth's satirical classic mocking orthodox resistance to reform.",
            199, 15, 4.4, 224, "Gurjar Granth Karyalay", false, false));
        books.add(b("Kashino Dikro", "Chunilal Madia", "Gujarati Literature", "Gujarati",
            "Chunilal Madia's poignant novel of a mother's devotion and rural Gujarati life.",
            229, 15, 4.5, 248, "Navbharat Sahitya Mandir", false, false));

        // ==================== TAMIL ====================
        books.add(b("Ponniyin Selvan", "Kalki Krishnamurthy", "Tamil Literature", "Tamil",
            "Kalki Krishnamurthy's epic historical novel of Chola-dynasty intrigue and adventure.",
            399, 20, 4.9, 2400, "Vanathi Pathippagam", true, true));
        books.add(b("Sivakamiyin Sabatham", "Kalki Krishnamurthy", "Tamil Literature", "Tamil",
            "Kalki Krishnamurthy's historical romance set in the Pallava dynasty.",
            349, 15, 4.7, 1600, "Vanathi Pathippagam", false, true));
        books.add(b("Parthiban Kanavu", "Kalki Krishnamurthy", "Tamil Literature", "Tamil",
            "Kalki Krishnamurthy's tale of duty and heroism in the Pallava era.",
            249, 15, 4.6, 320, "Vanathi Pathippagam", false, false));
        books.add(b("Thirukkural", "Thiruvalluvar", "Poetry", "Tamil",
            "Thiruvalluvar's timeless collection of couplets on virtue, wealth, and love.",
            199, 10, 4.9, 268, "Kalachuvadu Publications", true, false));
        books.add(b("Silappatikaram", "Ilango Adigal", "Tamil Literature", "Tamil",
            "Ilango Adigal's classical Tamil epic of love, justice, and vengeance.",
            249, 15, 4.6, 296, "Kalachuvadu Publications", false, false));
        books.add(b("En Iniya Iyanthira", "Sujatha Rangarajan", "Tamil Literature", "Tamil",
            "Sujatha Rangarajan's pioneering Tamil science-fiction novel.",
            199, 15, 4.5, 184, "Vanathi Pathippagam", false, false));
        books.add(b("Sila Nerangalil Sila Manithargal", "Jeyakanthan", "Tamil Literature", "Tamil",
            "Jeyakanthan's Jnanpith Award-winning novel of morality and human nature.",
            229, 10, 4.6, 208, "Kalachuvadu Publications", false, false));
        books.add(b("Kambaramayanam", "Kambar", "Poetry", "Tamil",
            "Kambar's classical Tamil retelling of the Ramayana epic.",
            299, 10, 4.7, 456, "Kalachuvadu Publications", false, false));

        // ==================== TELUGU ====================
        books.add(b("Veyi Padagalu", "Viswanatha Satyanarayana", "Telugu Literature", "Telugu",
            "Viswanatha Satyanarayana's Jnanpith Award-winning epic novel of a changing Andhra society.",
            349, 15, 4.7, 520, "Emesco Books", true, true));
        books.add(b("Kanyasulkam", "Gurajada Apparao", "Telugu Literature", "Telugu",
            "Gurajada Apparao's landmark Telugu play satirizing the practice of bride price.",
            199, 15, 4.6, 216, "Visalandhra Publishing House", false, false));
        books.add(b("Maidanam", "Chalam", "Telugu Literature", "Telugu",
            "Chalam's bold novel challenging social norms around love and freedom.",
            229, 10, 4.4, 176, "Emesco Books", false, false));
        books.add(b("Malapalli", "Unnava Lakshminarayana", "Telugu Literature", "Telugu",
            "Unnava Lakshminarayana's pioneering novel of caste and rural struggle.",
            279, 15, 4.5, 384, "Visalandhra Publishing House", false, false));
        books.add(b("Barrister Parvateesam", "Mokkapati Narasimha Sastry", "Telugu Literature", "Telugu",
            "Mokkapati Narasimha Sastry's classic comic novel of a young man's misadventures abroad.",
            249, 15, 4.6, 312, "Emesco Books", false, false));
        books.add(b("Andhra Mahabharatam", "Nannaya, Tikkana & Yerrapragada", "Telugu Literature", "Telugu",
            "The celebrated Telugu rendering of the Mahabharata by Nannaya, Tikkana, and Yerrapragada.",
            399, 10, 4.7, 640, "Visalandhra Publishing House", true, false));
        books.add(b("Amuktamalyada", "Krishnadevaraya", "Poetry", "Telugu",
            "Emperor Krishnadevaraya's classical Telugu epic poem of devotion and romance.",
            299, 10, 4.6, 288, "Emesco Books", false, false));
        books.add(b("Chivaraku Migiledi", "Buchi Babu", "Telugu Literature", "Telugu",
            "Buchi Babu's acclaimed novel exploring the inner life of its protagonist.",
            249, 15, 4.5, 296, "Visalandhra Publishing House", false, false));

        // ==================== KANNADA ====================
        books.add(b("Kanuru Heggadithi", "Kuvempu", "Kannada Literature", "Kannada",
            "Kuvempu's Jnanpith Award-winning novel of family and land in rural Karnataka.",
            299, 15, 4.7, 384, "Sapna Book House", true, true));
        books.add(b("Ramayana Darshanam", "Kuvempu", "Poetry", "Kannada",
            "Kuvempu's Jnanpith-winning epic poem reimagining the Ramayana.",
            349, 10, 4.8, 480, "Udayaravi Prakashana", true, false));
        books.add(b("Samskara", "U. R. Ananthamurthy", "Kannada Literature", "Kannada",
            "U. R. Ananthamurthy's landmark novel questioning ritual and orthodoxy in a Brahmin village.",
            249, 15, 4.7, 176, "Sapna Book House", false, true));
        books.add(b("Parva", "S. L. Bhyrappa", "Kannada Literature", "Kannada",
            "S. L. Bhyrappa's acclaimed reimagining of the Mahabharata in realistic detail.",
            349, 15, 4.8, 656, "Sahitya Bhandara", true, false));
        books.add(b("Vamsha Vriksha", "S. L. Bhyrappa", "Kannada Literature", "Kannada",
            "S. L. Bhyrappa's novel exploring tradition and modernity across generations.",
            279, 10, 4.6, 312, "Sahitya Bhandara", false, false));
        books.add(b("Gruhabhanga", "S. L. Bhyrappa", "Kannada Literature", "Kannada",
            "S. L. Bhyrappa's poignant novel of a woman's struggle against poverty and hardship.",
            249, 15, 4.6, 288, "Sahitya Bhandara", false, false));
        books.add(b("Chomana Dudi", "K. Shivarama Karanth", "Kannada Literature", "Kannada",
            "K. Shivarama Karanth's Jnanpith Award-winning novel on caste oppression.",
            229, 15, 4.6, 224, "Sapna Book House", false, false));
        books.add(b("Mookajjiya Kanasugalu", "K. Shivarama Karanth", "Kannada Literature", "Kannada",
            "Karanth's Jnanpith-winning novel exploring faith through a grandmother's wisdom.",
            249, 10, 4.7, 256, "Sapna Book House", false, false));

        // ==================== MALAYALAM ====================
        books.add(b("Randamoozham", "M. T. Vasudevan Nair", "Malayalam Literature", "Malayalam",
            "M. T. Vasudevan Nair's acclaimed retelling of the Mahabharata from Bhima's perspective.",
            299, 15, 4.8, 320, "DC Books", true, true));
        books.add(b("Chemmeen", "Thakazhi Sivasankara Pillai", "Malayalam Literature", "Malayalam",
            "Thakazhi Sivasankara Pillai's Jnanpith-winning tragic love story set among fisherfolk.",
            249, 20, 4.7, 168, "DC Books", true, true));
        books.add(b("Kayar", "Thakazhi Sivasankara Pillai", "Malayalam Literature", "Malayalam",
            "Thakazhi Sivasankara Pillai's sweeping saga of land and society in Kerala.",
            349, 15, 4.6, 768, "DC Books", false, false));
        books.add(b("Khasakkinte Itihasam", "O. V. Vijayan", "Malayalam Literature", "Malayalam",
            "O. V. Vijayan's landmark novel, The Legends of Khasak, on myth and modernity.",
            279, 15, 4.7, 224, "DC Books", false, false));
        books.add(b("Balyakalasakhi", "Vaikom Muhammad Basheer", "Malayalam Literature", "Malayalam",
            "Vaikom Muhammad Basheer's tender novel of childhood friendship and lost love.",
            199, 15, 4.7, 112, "Mathrubhumi Books", false, false));
        books.add(b("Pathummayude Aadu", "Vaikom Muhammad Basheer", "Malayalam Literature", "Malayalam",
            "Basheer's warm, humorous novella of family life in a Kerala household.",
            179, 10, 4.6, 96, "Mathrubhumi Books", false, false));
        books.add(b("Oru Desathinte Katha", "S. K. Pottekkatt", "Malayalam Literature", "Malayalam",
            "S. K. Pottekkatt's Jnanpith-winning epic of a village's history and people.",
            349, 15, 4.7, 800, "DC Books", false, false));
        books.add(b("Aadujeevitham", "Benyamin", "Malayalam Literature", "Malayalam",
            "Benyamin's bestselling novel of an Indian migrant worker's ordeal in the Gulf, Goat Days.",
            299, 20, 4.8, 258, "DC Books", true, true));

        // ==================== PUNJABI ====================
        books.add(b("Pinjar", "Amrita Pritam", "Punjabi Literature", "Punjabi",
            "Amrita Pritam's powerful novel of a woman caught in the trauma of Partition.",
            249, 15, 4.7, 176, "Lokgeet Prakashan", true, true));
        books.add(b("Sunehade", "Amrita Pritam", "Poetry", "Punjabi",
            "Amrita Pritam's Sahitya Akademi Award-winning collection of Punjabi poetry.",
            199, 10, 4.6, 144, "Chetna Prakashan", false, false));
        books.add(b("Pavitra Paapi", "Nanak Singh", "Punjabi Literature", "Punjabi",
            "Nanak Singh's acclaimed novel of a fallen man's search for redemption.",
            229, 15, 4.6, 264, "Lokgeet Prakashan", false, false));
        books.add(b("Loona", "Shiv Kumar Batalvi", "Poetry", "Punjabi",
            "Shiv Kumar Batalvi's celebrated verse-play retelling a Punjabi folk legend.",
            199, 15, 4.7, 128, "Chetna Prakashan", true, false));
        books.add(b("Adh Chanani Raat", "Kartar Singh Duggal", "Punjabi Literature", "Punjabi",
            "Kartar Singh Duggal's evocative novel of rural Punjabi life.",
            219, 10, 4.5, 208, "Lokgeet Prakashan", false, false));
        books.add(b("Marhi Da Deeva", "Gurdial Singh", "Punjabi Literature", "Punjabi",
            "Gurdial Singh's Jnanpith-winning novel of poverty and dignity in rural Punjab.",
            249, 15, 4.6, 232, "Chetna Prakashan", false, false));

        // ==================== URDU ====================
        books.add(b("Diwan-e-Ghalib", "Mirza Ghalib", "Poetry", "Urdu",
            "Mirza Ghalib's timeless collection of Urdu ghazals on love, loss, and philosophy.",
            249, 15, 4.9, 320, "Maktaba Jamia", true, true));
        books.add(b("Toba Tek Singh", "Saadat Hasan Manto", "Urdu Literature", "Urdu",
            "Saadat Hasan Manto's searing short story on the madness of Partition.",
            199, 20, 4.8, 160, "Educational Publishing House", true, true));
        books.add(b("Umrao Jaan Ada", "Mirza Hadi Ruswa", "Urdu Literature", "Urdu",
            "Mirza Hadi Ruswa's classic novel of a courtesan's life in 19th-century Lucknow.",
            279, 15, 4.6, 296, "Educational Publishing House", false, false));
        books.add(b("Lihaaf", "Ismat Chughtai", "Urdu Literature", "Urdu",
            "Ismat Chughtai's bold, controversial short story challenging social taboos.",
            199, 15, 4.6, 144, "Maktaba Jamia", false, false));
        books.add(b("Ek Chadar Maili Si", "Rajinder Singh Bedi", "Urdu Literature", "Urdu",
            "Rajinder Singh Bedi's Sahitya Akademi-winning novella of duty and social custom.",
            199, 10, 4.5, 128, "Educational Publishing House", false, false));
        books.add(b("Aag Ka Darya", "Qurratulain Hyder", "Urdu Literature", "Urdu",
            "Qurratulain Hyder's monumental novel spanning centuries of Indian history, River of Fire.",
            349, 15, 4.7, 432, "Maktaba Jamia", false, false));

        // Books above are grouped by language for readability while authoring them, but
        // saving them in that grouped order would make the default "Recommended" view
        // (plain insertion order, page 1 of /books) show 12 English titles in a row before
        // any other language ever appears - effectively burying the whole multilingual
        // catalog several pages deep. Interleaving round-robin across languages here means
        // the very first page mixes in every language immediately.
        reconcileBooks(interleaveByLanguage(books));
    }

    /**
     * Reconciles the database against the canonical seed list above the same way
     * {@link #initializeCategories()} reconciles categories - an existence check per
     * book on every startup, not a one-shot "only seed if the table is completely
     * empty" guard. That old guard is exactly what caused this project's persistent
     * seeding problem: once the books table had any row at all, editing prices,
     * categories, or descriptions in this file and restarting the app changed
     * nothing, because {@code initializeBooks()} returned immediately. Now:
     *   - a title that already exists (matched by its unique {@link Book#getTitle()})
     *     has its catalog fields (price, discount, rating, category, language,
     *     description, publisher, pages, author, featured/best-seller flags) synced
     *     to this file, so an edited price or a book moved to a different category
     *     actually takes effect on restart;
     *   - a title that doesn't exist yet is inserted, so a newly added book (e.g. to
     *     fill a category that previously had none) actually appears;
     *   - a title is never inserted twice, no matter how many times the app restarts.
     * Fields the admin panel can change at runtime for an existing book - stock,
     * sold/view counts, rating count, isbn, cover image, published date - are
     * deliberately left untouched here so this reconciliation can never undo real
     * inventory or admin activity; only the catalog-defining fields this file itself
     * declares are kept in sync.
     */
    private void reconcileBooks(java.util.List<Book> seedBooks) {
        Map<String, Book> existingByTitle = new HashMap<>();
        for (Book existing : bookRepository.findAll()) {
            existingByTitle.put(existing.getTitle(), existing);
        }

        java.util.List<Book> toInsert = new java.util.ArrayList<>();
        int updated = 0;
        for (Book seed : seedBooks) {
            Book existing = existingByTitle.get(seed.getTitle());
            if (existing == null) {
                toInsert.add(seed);
                continue;
            }
            existing.setDescription(seed.getDescription());
            existing.setAuthor(seed.getAuthor());
            existing.setCategory(seed.getCategory());
            existing.setLanguage(seed.getLanguage());
            existing.setPrice(seed.getPrice());
            existing.setDiscount(seed.getDiscount());
            existing.setRating(seed.getRating());
            existing.setPages(seed.getPages());
            existing.setPublisher(seed.getPublisher());
            existing.setFeatured(seed.getFeatured());
            existing.setBestSeller(seed.getBestSeller());
            existing.setDeleted(false);
            bookRepository.save(existing);
            updated++;
        }
        if (!toInsert.isEmpty()) {
            bookRepository.saveAll(toInsert);
        }
        if (!toInsert.isEmpty() || updated > 0) {
            System.out.println("✓ Books reconciled (" + toInsert.size() + " added, " + updated
                + " updated, " + seedBooks.size() + " total canonical titles across 11 languages)");
        }
    }

    /**
     * Reorders books round-robin by language (preserving each language's relative
     * order and the order languages first appear) so the default catalog view isn't
     * dominated by whichever language happens to be authored first in the list above.
     */
    private java.util.List<Book> interleaveByLanguage(java.util.List<Book> source) {
        Map<String, java.util.ArrayDeque<Book>> byLanguage = new java.util.LinkedHashMap<>();
        for (Book book : source) {
            byLanguage.computeIfAbsent(book.getLanguage(), k -> new java.util.ArrayDeque<>()).add(book);
        }
        java.util.List<Book> result = new java.util.ArrayList<>(source.size());
        boolean addedAny = true;
        while (addedAny) {
            addedAny = false;
            for (java.util.ArrayDeque<Book> queue : byLanguage.values()) {
                Book next = queue.poll();
                if (next != null) {
                    result.add(next);
                    addedAny = true;
                }
            }
        }
        return result;
    }

    /**
     * Builds and persists-ready populates one Book with realistic, deterministic sample metadata.
     */
    private Book b(String title, String authorName, String categoryName, String language,
                   String description, int price, int discount, double rating, int pages,
                   String publisher, boolean featured, boolean bestSeller) {
        seq++;
        Author author = authorMap.get(authorName);
        Category category = categoryMap.get(categoryName);
        if (author == null) {
            throw new IllegalStateException("Unknown author referenced in seed data: " + authorName);
        }
        if (category == null) {
            throw new IllegalStateException("Unknown category referenced in seed data: " + categoryName);
        }

        Book book = new Book();
        book.setTitle(title);
        book.setDescription(description);
        book.setAuthor(author);
        book.setCategory(category);
        book.setLanguage(language);
        book.setPrice(BigDecimal.valueOf(price));
        book.setDiscount(BigDecimal.valueOf(discount));
        book.setRating(BigDecimal.valueOf(rating));
        book.setRatingCount((int) (Math.random() * 800) + 40);
        book.setStock((int) (Math.random() * 60) + 5);
        book.setIsbn(String.format("978-93-%06d", seq));
        book.setPublisher(publisher);
        book.setPages(pages);
        book.setImageUrl(COVERS[seq % COVERS.length]);
        book.setFeatured(featured);
        book.setBestSeller(bestSeller);
        book.setNewArrival(seq % 9 == 0);
        book.setSoldCount((int) (Math.random() * 400));
        book.setViewCount((int) (Math.random() * 900));
        book.setPublishedDate(LocalDateTime.now().minusDays(30L + ((long) seq * 41) % 3650));
        return book;
    }
}
