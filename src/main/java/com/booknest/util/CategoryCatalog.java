package com.booknest.util;

/**
 * Single source of truth for the canonical BookNest category taxonomy
 * (name + description pairs, in display order).
 *
 * {@link com.booknest.config.DataInitializer} seeds the database from this
 * list, and any in-memory fallback (used only if the database is ever
 * completely empty) reads from the same list, so the category set can never
 * drift out of sync between the seed data and a fallback.
 */
public final class CategoryCatalog {

    public static final String[][] CATEGORIES = {
        {"Fiction", "Immersive stories and imaginative worlds from celebrated novelists"},
        {"Programming & Technology", "Master programming languages, software craft, and emerging technology"},
        {"Entrepreneurship", "Insights on startups, innovation, and building the future"},
        {"Self Development", "Practical wisdom to build habits, focus, and a purposeful life"},
        {"Romance", "Heartfelt love stories that stay with you"},
        {"Kids", "Delightful stories and lessons for young readers"},
        {"History", "Journeys through the past that shape our present"},
        {"Science", "Explorations of the universe, life, and human knowledge"},
        {"Marathi Literature", "Celebrated novels, plays, and poetry from Maharashtra"},
        {"Hindi Literature", "Timeless prose and poetry from Hindi's finest writers"},
        {"Bengali Literature", "Rich storytelling and verse from Bengal's literary tradition"},
        {"Gujarati Literature", "Classic and modern voices from Gujarati literature"},
        {"Tamil Literature", "Epics, verse, and fiction from Tamil literary heritage"},
        {"Telugu Literature", "Landmark works of Telugu poetry, drama, and prose"},
        {"Kannada Literature", "Award-winning novels and epics from Kannada literature"},
        {"Malayalam Literature", "Acclaimed fiction and memoirs from Kerala's writers"},
        {"Punjabi Literature", "Powerful stories and poetry from Punjab"},
        {"Urdu Literature", "Evocative poetry and prose from the Urdu tradition"},
        {"Spirituality", "Guidance for inner peace and a mindful life"},
        {"Education & Academics", "Reference and skill-building titles for lifelong learners"},
        {"Competitive Exams", "Preparation guides for entrance and competitive examinations"},
        {"Management", "Leadership and organizational wisdom for the modern workplace"},
        {"Biography & Memoir", "Real lives, told in their own extraordinary words"},
        {"Mystery & Thriller", "Gripping suspense and page-turning whodunits"},
        {"Poetry", "Verses that capture emotion, beauty, and thought"},
        {"Health & Wellness", "Guides to a healthier body and calmer mind"},
        {"Finance & Investing", "Practical lessons on money, markets, and wealth"},
        {"Philosophy", "Enduring ideas from history's great thinkers"}
    };

    private CategoryCatalog() {
    }
}
