package com.bayerwestphalian.campaign.segment;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Combines multi-criteria segment matches per KB FR-078 (AND/OR join logic).
 *
 * <p><strong>Join semantics (left-to-right):</strong> the {@code joinOperator} on criterion {@code
 * i} (for {@code i > 0}) combines that criterion with the accumulated result of criteria {@code 0
 * .. i-1}. The join operator on the first criterion is ignored because there is nothing prior to
 * join.
 *
 * <p><strong>AND (KB default / item 196):</strong> missing or null {@code join_operator} values are
 * treated as {@link SegmentJoinOperator#AND}. A pure AND chain requires <em>every</em> criterion to
 * match (conjunctive audience intersection). Partial matches are excluded. Example: {@code
 * city=Munich AND customer_type=PROSPECT AND} keeps only Munich prospects — a Munich CUSTOMER or a
 * Berlin PROSPECT does not match.
 *
 * <p><strong>OR (item 197):</strong> example chain {@code city=Munich AND, city=Berlin OR} matches
 * customers in Munich <em>or</em> Berlin (union). Mixed example: {@code type=PROSPECT AND,
 * city=Munich AND, city=Berlin OR} evaluates left-to-right as {@code (type=PROSPECT AND
 * city=Munich) OR city=Berlin}. Pure multi-criterion OR chains use OR on every join after the
 * first.
 */
final class SegmentCriteriaLogicSupport {

    private SegmentCriteriaLogicSupport() {}

    /**
     * Returns {@link SegmentJoinOperator#AND} when the join operator is null (KB FR-078 default for
     * combined segment criteria — AND is the standard conjunctive combination).
     */
    static SegmentJoinOperator defaultJoinOperator(SegmentJoinOperator joinOperator) {
        return joinOperator == null ? SegmentJoinOperator.AND : joinOperator;
    }

    /** True when the join operator is AND (null counts as AND per KB default). */
    static boolean isAnd(SegmentJoinOperator joinOperator) {
        return defaultJoinOperator(joinOperator) == SegmentJoinOperator.AND;
    }

    static boolean isOr(SegmentJoinOperator joinOperator) {
        return defaultJoinOperator(joinOperator) == SegmentJoinOperator.OR;
    }

    /**
     * Combines an accumulated match result with the next criterion using the next criterion's join
     * operator. Null join operators are treated as AND ({@code accumulated && nextMatch}).
     */
    static boolean combine(
            boolean accumulated, boolean nextMatch, SegmentJoinOperator joinOperator) {
        return switch (defaultJoinOperator(joinOperator)) {
            case AND -> accumulated && nextMatch;
            case OR -> accumulated || nextMatch;
        };
    }

    /**
     * Pure AND over match results (KB FR-078 conjunctive logic): empty list matches (no
     * constraints); otherwise every result must be true.
     */
    static boolean matchesAllAnd(List<Boolean> matchResults) {
        if (matchResults == null || matchResults.isEmpty()) {
            return true;
        }
        return matchResults.stream().allMatch(Boolean.TRUE::equals);
    }

    /**
     * Pure OR over match results: empty list matches (no constraints); otherwise at least one
     * result must be true (KB FR-078 OR criteria logic).
     */
    static boolean matchesAnyOr(List<Boolean> matchResults) {
        if (matchResults == null || matchResults.isEmpty()) {
            return true;
        }
        return matchResults.stream().anyMatch(Boolean.TRUE::equals);
    }

    /**
     * Left-to-right evaluation of criterion match flags with per-criterion join operators.
     *
     * <p>{@code joinOperators[i]} links {@code matchResults[i]} to the accumulated result for
     * {@code i > 0}. Sizes must match; empty input matches.
     */
    static boolean evaluate(List<Boolean> matchResults, List<SegmentJoinOperator> joinOperators) {
        if (matchResults == null || matchResults.isEmpty()) {
            return true;
        }
        if (joinOperators == null || joinOperators.size() != matchResults.size()) {
            throw new IllegalArgumentException(
                    "joinOperators size must match matchResults size for segment criteria evaluation");
        }

        boolean combined = Boolean.TRUE.equals(matchResults.getFirst());
        for (int index = 1; index < matchResults.size(); index++) {
            combined =
                    combine(
                            combined,
                            Boolean.TRUE.equals(matchResults.get(index)),
                            joinOperators.get(index));
        }
        return combined;
    }

    /**
     * Evaluates criteria against a matcher predicate using each criterion's join operator (default
     * AND). Supports mixed AND/OR chains via {@link #combine}.
     */
    static <T> boolean matchesAllCriteria(List<T> criteria, Predicate<T> matcher) {
        if (criteria == null || criteria.isEmpty()) {
            return true;
        }

        Boolean combined = null;
        for (T criterion : criteria) {
            boolean matches = matcher.test(criterion);
            if (combined == null) {
                combined = matches;
                continue;
            }
            SegmentJoinOperator joinOperator = resolveJoinOperator(criterion);
            combined = combine(combined, matches, joinOperator);
        }
        return Boolean.TRUE.equals(combined);
    }

    /**
     * AND-only evaluation: every criterion must match, ignoring any OR join operators. Useful for
     * logical groups that are always conjunctive.
     */
    static <T> boolean matchesAllWithAnd(List<T> criteria, Predicate<T> matcher) {
        if (criteria == null || criteria.isEmpty()) {
            return true;
        }
        return criteria.stream().allMatch(matcher);
    }

    /**
     * OR-only evaluation: at least one criterion must match, ignoring AND join operators. Useful
     * when a logical group is disjunctive (any-of).
     */
    static <T> boolean matchesAnyWithOr(List<T> criteria, Predicate<T> matcher) {
        if (criteria == null || criteria.isEmpty()) {
            return true;
        }
        return criteria.stream().anyMatch(matcher);
    }

    /**
     * True when every criterion after the first uses {@link SegmentJoinOperator#OR} (pure OR
     * chain). The first criterion's join operator is ignored under left-to-right join semantics.
     */
    static boolean isPureOrChain(List<SegmentJoinOperator> joinOperators) {
        if (joinOperators == null || joinOperators.size() <= 1) {
            return false;
        }
        for (int index = 1; index < joinOperators.size(); index++) {
            if (!isOr(joinOperators.get(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * True when every criterion after the first uses {@link SegmentJoinOperator#AND} (pure AND
     * chain / KB default conjunctive audience). Null join operators count as AND. Single-criterion
     * or empty chains are not pure multi-criterion AND chains.
     */
    static boolean isPureAndChain(List<SegmentJoinOperator> joinOperators) {
        if (joinOperators == null || joinOperators.size() <= 1) {
            return false;
        }
        for (int index = 1; index < joinOperators.size(); index++) {
            if (!isAnd(joinOperators.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static SegmentJoinOperator resolveJoinOperator(Object criterion) {
        if (criterion instanceof CreateSegmentCriteriaCommand command) {
            return defaultJoinOperator(command.joinOperator());
        }
        if (criterion instanceof SegmentCriteria entity) {
            return defaultJoinOperator(entity.getJoinOperator());
        }
        return SegmentJoinOperator.AND;
    }

    static void validateJoinOperatorPresent(SegmentJoinOperator joinOperator) {
        Objects.requireNonNull(joinOperator, "Segment join operator is required");
    }
}
