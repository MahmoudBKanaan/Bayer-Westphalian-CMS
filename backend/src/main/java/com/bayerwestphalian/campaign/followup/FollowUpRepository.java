package com.bayerwestphalian.campaign.followup;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FollowUpRepository
        extends JpaRepository<FollowUpTask, UUID>, JpaSpecificationExecutor<FollowUpTask> {

    List<FollowUpTask> findByAssignedTo_Id(UUID userId);

    List<FollowUpTask> findByStatusIn(List<FollowUpTaskStatus> statuses);

    List<FollowUpTask> findByCustomer_Id(UUID customerId);

    default List<FollowUpTask> findByAssignedTo(UUID userId) {
        return findByAssignedTo_Id(userId);
    }

    default List<FollowUpTask> findOpenTasks() {
        return findByStatusIn(List.of(FollowUpTaskStatus.OPEN, FollowUpTaskStatus.IN_PROGRESS));
    }

    default List<FollowUpTask> findByCustomerId(UUID customerId) {
        return findByCustomer_Id(customerId);
    }

    default List<FollowUpTask> search(FollowUpTaskSearchCriteria criteria) {
        // Do not use Sort.Order.nullsLast() here: Spring Data JPA Criteria does not support
        // null precedence (UnsupportedOperationException: Applying Null Precedence...).
        // Null due dates are ordered last in-memory after fetch.
        List<FollowUpTask> tasks =
                findAll(
                        matchesCriteria(criteria),
                        Sort.by(
                                Sort.Order.asc("dueDate"),
                                Sort.Order.desc("priority"),
                                Sort.Order.asc("createdAt")));
        tasks.sort(
                java.util.Comparator.comparing(
                                FollowUpTask::getDueDate,
                                java.util.Comparator.nullsLast(
                                        java.util.Comparator.naturalOrder()))
                        .thenComparing(
                                FollowUpTask::getPriority,
                                java.util.Comparator.nullsLast(
                                        java.util.Comparator.reverseOrder()))
                        .thenComparing(
                                FollowUpTask::getCreatedAt,
                                java.util.Comparator.nullsLast(
                                        java.util.Comparator.naturalOrder())));
        return tasks;
    }

    private static Specification<FollowUpTask> matchesCriteria(
            FollowUpTaskSearchCriteria criteria) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (criteria == null) {
                return predicate;
            }

            if (criteria.customerId() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.equal(root.get("customer").get("id"), criteria.customerId()));
            }
            if (criteria.assignedTo() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.equal(
                                        root.get("assignedTo").get("id"), criteria.assignedTo()));
            }
            if (criteria.priority() != null) {
                predicate =
                        builder.and(
                                predicate, builder.equal(root.get("priority"), criteria.priority()));
            }
            if (criteria.status() != null) {
                predicate =
                        builder.and(predicate, builder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.dueDateFrom() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.greaterThanOrEqualTo(
                                        root.get("dueDate"), criteria.dueDateFrom()));
            }
            if (criteria.dueDateTo() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.lessThanOrEqualTo(root.get("dueDate"), criteria.dueDateTo()));
            }

            return predicate;
        };
    }
}
