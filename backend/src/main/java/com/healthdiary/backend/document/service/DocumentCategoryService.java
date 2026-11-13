package com.healthdiary.backend.document.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.document.dto.DocumentCategoryResponse;
import com.healthdiary.backend.document.entity.DocumentCategory;
import com.healthdiary.backend.document.repository.DocumentCategoryRepository;
import com.healthdiary.backend.document.dto.CreateDocumentCategoryRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.healthdiary.backend.document.repository.DocumentRepository;
import java.util.List;

@Service
public class DocumentCategoryService {

    private final DocumentCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public DocumentCategoryService(
            DocumentCategoryRepository categoryRepository,
            DocumentRepository documentRepository,
            UserRepository userRepository) {

        this.categoryRepository = categoryRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));
    }

    private DocumentCategoryResponse toResponse(DocumentCategory category) {
        return new DocumentCategoryResponse(
                category.getId(),
                category.getName(),
                category.isDefault());
    }

    @Transactional(readOnly = true)
    public List<DocumentCategoryResponse> getCategories(String email) {
        User user = getCurrentUser(email);

        return categoryRepository
                .findByUserIsNullOrUserIdOrderByNameAsc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Category name is required");
        }

        String normalizedName = name.trim();

        if (normalizedName.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Category name must not exceed 100 characters");
        }

        return normalizedName;
    }

    @Transactional
    public DocumentCategoryResponse createCategory(
            String email,
            CreateDocumentCategoryRequest request) {

        User user = getCurrentUser(email);

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Category metadata is required");
        }

        String name = validateName(request.name());

        if (categoryRepository.existsAccessibleCategoryWithName(
                name, user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Category name already exists");
        }

        DocumentCategory category = new DocumentCategory(user, name);

        return toResponse(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    public DocumentCategoryResponse renameCategory(
            Long categoryId,
            String email,
            CreateDocumentCategoryRequest request) {

        User user = getCurrentUser(email);

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Category metadata is required");
        }

        DocumentCategory category = categoryRepository
                .findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Category not found"));

        String name = validateName(request.name());

        if (categoryRepository.existsAccessibleCategoryWithNameExcludingId(
                name, user.getId(), categoryId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Category name already exists");
        }

        category.rename(name);

        return toResponse(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    public void deleteCategory(Long categoryId, String email) {
        User user = getCurrentUser(email);

        DocumentCategory category = categoryRepository
                .findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Category not found"));

        if (documentRepository.existsByCategoryId(categoryId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Category is currently used by documents");
        }

        categoryRepository.delete(category);
        categoryRepository.flush();
    }

}
