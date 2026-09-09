package com.booknest.service;

import com.booknest.entity.Author;
import com.booknest.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for Author operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthorService {

    private final AuthorRepository authorRepository;

    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    @SuppressWarnings("null")
    public Optional<Author> getAuthorById(Long id) {
        return authorRepository.findById(id);
    }

    public Optional<Author> getAuthorByIdWithBooks(Long id) {
        return authorRepository.findByIdWithBooks(id);
    }

    public Optional<Author> getAuthorByName(String name) {
        return authorRepository.findByName(name);
    }

    public List<Author> searchAuthors(String name) {
        return authorRepository.findByNameContainingIgnoreCase(name);
    }

    @SuppressWarnings("null")
    public Author saveAuthor(Author author) {
        return authorRepository.save(author);
    }

    @SuppressWarnings("null")
    public Author updateAuthor(Author author) {
        return authorRepository.save(author);
    }

    @SuppressWarnings("null")
    public void deleteAuthor(Long id) {
        authorRepository.deleteById(id);
    }

    public boolean existsByName(String name) {
        return authorRepository.existsByName(name);
    }
}
