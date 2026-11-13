package com.healthdiary.backend.document.controller;

import com.healthdiary.backend.document.dto.CreateDocumentCategoryRequest;
import com.healthdiary.backend.document.dto.DocumentCategoryResponse;
import com.healthdiary.backend.document.service.DocumentCategoryService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/document-categories")
public class DocumentCategoryController {

    private final DocumentCategoryService categoryService;

    public DocumentCategoryController(DocumentCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<DocumentCategoryResponse> getCategories(
            Authentication authentication) {

        return categoryService.getCategories(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<DocumentCategoryResponse> createCategory(
            @RequestBody CreateDocumentCategoryRequest request,
            Authentication authentication) {

        DocumentCategoryResponse response = categoryService.createCategory(
                authentication.getName(),
                request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{categoryId}")
    public DocumentCategoryResponse renameCategory(
            @PathVariable Long categoryId,
            @RequestBody CreateDocumentCategoryRequest request,
            Authentication authentication) {

        return categoryService.renameCategory(
                categoryId,
                authentication.getName(),
                request);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            @PathVariable Long categoryId,
            Authentication authentication) {

        categoryService.deleteCategory(
                categoryId,
                authentication.getName());
    }
}
