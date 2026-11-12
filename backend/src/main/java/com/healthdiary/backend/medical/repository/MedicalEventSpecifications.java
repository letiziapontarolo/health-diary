package com.healthdiary.backend.medical.repository;

import com.healthdiary.backend.medical.dto.MedicalEventFilter;
import com.healthdiary.backend.medical.entity.MedicalEvent;
import org.springframework.data.jpa.domain.Specification;

public final class MedicalEventSpecifications {

    private MedicalEventSpecifications() {
    }

    public static Specification<MedicalEvent> withFilters(
            Long userId,
            MedicalEventFilter filter
    ) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<
                    jakarta.persistence.criteria.Predicate>();

            predicates.add(
                    cb.equal(root.get("user").get("id"), userId)
            );

            if (filter.category() != null) {
                predicates.add(
                        cb.equal(
                                root.get("service").get("category"),
                                filter.category()
                        )
                );
            }

            if (filter.status() != null) {
                predicates.add(
                        cb.equal(root.get("status"), filter.status())
                );
            }

            if (filter.doctorId() != null) {
                predicates.add(
                        cb.equal(
                                root.get("doctor").get("id"),
                                filter.doctorId()
                        )
                );
            }

            if (filter.facilityId() != null) {
                predicates.add(
                        cb.equal(
                                root.get("facility").get("id"),
                                filter.facilityId()
                        )
                );
            }

            if (filter.fromDate() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("eventDate"),
                                filter.fromDate()
                        )
                );
            }

            if (filter.toDate() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("eventDate"),
                                filter.toDate()
                        )
                );
            }

            return cb.and(predicates.toArray(
                    new jakarta.persistence.criteria.Predicate[0]
            ));
        };
    }
}