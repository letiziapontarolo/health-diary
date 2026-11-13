package com.healthdiary.backend.document.repository;

import com.healthdiary.backend.document.dto.DocumentFilter;
import com.healthdiary.backend.document.entity.Document;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;

public final class DocumentSpecifications {

    private DocumentSpecifications() {
    }

    public static Specification<Document> withFilters(
            Long userId,
            DocumentFilter filter,
            LocalDate today) {

        return (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            predicates.add(builder.equal(
                    root.get("user").get("id"),
                    userId));

            if (filter.name() != null && !filter.name().isBlank()) {
                String name = "%" + filter.name()
                        .trim()
                        .toLowerCase(Locale.ROOT) + "%";

                predicates.add(builder.like(
                        builder.lower(root.get("displayName")),
                        name));
            }

            if (filter.categoryId() != null) {
                predicates.add(builder.equal(
                        root.get("category").get("id"),
                        filter.categoryId()));
            }

            if (filter.fromDate() != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        root.get("documentDate"),
                        filter.fromDate()));
            }

            if (filter.toDate() != null) {
                predicates.add(builder.lessThanOrEqualTo(
                        root.get("documentDate"),
                        filter.toDate()));
            }

            if (filter.notes() != null && !filter.notes().isBlank()) {
                String notes = "%" + filter.notes()
                        .trim()
                        .toLowerCase(Locale.ROOT) + "%";

                predicates.add(builder.like(
                        builder.lower(root.get("notes")),
                        notes));
            }

            if (Boolean.TRUE.equals(filter.expiredOnly())) {
                predicates.add(builder.lessThan(
                        root.get("expiryDate"),
                        today));
            }

            return builder.and(predicates.toArray(
                    new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
