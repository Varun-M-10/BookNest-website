package com.booknest.service;

import com.booknest.entity.Category;
import com.booknest.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for Category operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAllActive();
    }

    public List<Category> getAllCategoriesWithBooks() {
        return categoryRepository.findAllActiveWithBooks();
    }

    @SuppressWarnings("null")
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Optional<Category> getCategoryByIdWithBooks(Long id) {
        return categoryRepository.findByIdWithBooks(id);
    }

    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    @SuppressWarnings("null")
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    @SuppressWarnings("null")
    public Category updateCategory(Category category) {
        return categoryRepository.save(category);
    }

    @SuppressWarnings("null")
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }
}
