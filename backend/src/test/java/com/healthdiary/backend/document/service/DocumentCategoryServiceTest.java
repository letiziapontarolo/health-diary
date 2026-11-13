package com.healthdiary.backend.document.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.document.dto.CreateDocumentCategoryRequest;
import com.healthdiary.backend.document.entity.DocumentCategory;
import com.healthdiary.backend.document.repository.DocumentCategoryRepository;
import com.healthdiary.backend.document.repository.DocumentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class DocumentCategoryServiceTest {

    private DocumentCategoryRepository categoryRepository;
    private DocumentRepository documentRepository;
    private UserRepository userRepository;
    private DocumentCategoryService categoryService;
    private User user;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(DocumentCategoryRepository.class);
        documentRepository = mock(DocumentRepository.class);
        userRepository = mock(UserRepository.class);

        categoryService = new DocumentCategoryService(
                categoryRepository,
                documentRepository,
                userRepository);

        user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));
    }

    @Test
    void returnsAccessibleCategories() {
        DocumentCategory defaultCategory = mock(DocumentCategory.class);
        DocumentCategory customCategory = mock(DocumentCategory.class);

        when(defaultCategory.getName()).thenReturn("Referti");
        when(defaultCategory.isDefault()).thenReturn(true);

        when(customCategory.getName()).thenReturn("Assicurazioni");
        when(customCategory.isDefault()).thenReturn(false);

        when(categoryRepository.findByUserIsNullOrUserIdOrderByNameAsc(1L))
                .thenReturn(List.of(customCategory, defaultCategory));

        var response = categoryService.getCategories("user@example.com");

        assertEquals(2, response.size());
        assertEquals("Assicurazioni", response.get(0).name());
        assertFalse(response.get(0).isDefault());
        assertEquals("Referti", response.get(1).name());
        assertTrue(response.get(1).isDefault());
    }

    @Test
    void createsCustomCategory() {
        when(categoryRepository.saveAndFlush(any(DocumentCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = categoryService.createCategory(
                "user@example.com",
                new CreateDocumentCategoryRequest("  Assicurazioni  "));

        assertEquals("Assicurazioni", response.name());
        assertFalse(response.isDefault());

        verify(categoryRepository).saveAndFlush(
                argThat(category -> category.getUser() == user
                        && category.getName().equals("Assicurazioni")));
    }

    @Test
    void rejectsDuplicateCategoryName() {
        when(categoryRepository.existsAccessibleCategoryWithName(
                "referti", 1L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.createCategory(
                        "user@example.com",
                        new CreateDocumentCategoryRequest("referti")));

        assertEquals(409, exception.getStatusCode().value());

        verify(categoryRepository, never())
                .saveAndFlush(any(DocumentCategory.class));
    }

    @Test
    void renamesOwnCategory() {
        DocumentCategory category = new DocumentCategory(user, "Assicurazioni");

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));

        when(categoryRepository.saveAndFlush(any(DocumentCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = categoryService.renameCategory(
                10L,
                "user@example.com",
                new CreateDocumentCategoryRequest("Documenti assicurativi"));

        assertEquals("Documenti assicurativi", response.name());
        assertEquals("Documenti assicurativi", category.getName());
    }

    @Test
    void deletesUnusedOwnCategory() {
        DocumentCategory category = new DocumentCategory(user, "Assicurazioni");

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));

        categoryService.deleteCategory(10L, "user@example.com");

        verify(documentRepository).existsByCategoryId(10L);
        verify(categoryRepository).delete(category);
        verify(categoryRepository).flush();
    }

    @Test
    void rejectsDeletingUsedCategory() {
        DocumentCategory category = new DocumentCategory(user, "Assicurazioni");

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));

        when(documentRepository.existsByCategoryId(10L))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.deleteCategory(
                        10L, "user@example.com"));

        assertEquals(409, exception.getStatusCode().value());

        verify(categoryRepository, never())
                .delete(any(DocumentCategory.class));
    }

    @Test
    void rejectsRenamingCategoryNotOwnedByUser() {
        when(categoryRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.renameCategory(
                        99L,
                        "user@example.com",
                        new CreateDocumentCategoryRequest("Nuovo nome")));

        assertEquals(404, exception.getStatusCode().value());

        verify(categoryRepository, never())
                .saveAndFlush(any(DocumentCategory.class));
    }

    @Test
    void rejectsDeletingCategoryNotOwnedByUser() {
        when(categoryRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.deleteCategory(
                        99L, "user@example.com"));

        assertEquals(404, exception.getStatusCode().value());

        verify(categoryRepository, never())
                .delete(any(DocumentCategory.class));
    }

    @Test
    void rejectsBlankCategoryName() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.createCategory(
                        "user@example.com",
                        new CreateDocumentCategoryRequest("   ")));

        assertEquals(400, exception.getStatusCode().value());

        verify(categoryRepository, never())
                .saveAndFlush(any(DocumentCategory.class));
    }

    @Test
    void rejectsCategoryNameLongerThan100Characters() {
        String longName = "A".repeat(101);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.createCategory(
                        "user@example.com",
                        new CreateDocumentCategoryRequest(longName)));

        assertEquals(400, exception.getStatusCode().value());
    }

}
