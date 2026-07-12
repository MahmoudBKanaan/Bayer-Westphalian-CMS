package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for KB FR-078 OR criteria logic (disjunctive combination of segment filters). Item 197
 * acceptance builds on these helpers.
 */
class SegmentOrCriteriaFilteringTests {

    @Test
    void recognizesOrJoinOperatorAndDefaultsNullToAnd() {
        assertThat(SegmentCriteriaLogicSupport.isOr(SegmentJoinOperator.OR)).isTrue();
        assertThat(SegmentCriteriaLogicSupport.isOr(SegmentJoinOperator.AND)).isFalse();
        assertThat(SegmentCriteriaLogicSupport.isOr(null)).isFalse();
        assertThat(SegmentCriteriaLogicSupport.defaultJoinOperator(SegmentJoinOperator.OR))
                .isEqualTo(SegmentJoinOperator.OR);
        assertThat(SegmentCriteriaLogicSupport.defaultJoinOperator(null))
                .isEqualTo(SegmentJoinOperator.AND);
    }

    @Test
    void combineUsesInclusiveOrWhenJoinOperatorIsOr() {
        assertThat(SegmentCriteriaLogicSupport.combine(false, true, SegmentJoinOperator.OR))
                .isTrue();
        assertThat(SegmentCriteriaLogicSupport.combine(true, false, SegmentJoinOperator.OR))
                .isTrue();
        assertThat(SegmentCriteriaLogicSupport.combine(true, true, SegmentJoinOperator.OR))
                .isTrue();
        assertThat(SegmentCriteriaLogicSupport.combine(false, false, SegmentJoinOperator.OR))
                .isFalse();
    }

    @Test
    void pureOrRequiresAtLeastOneTrueMatch() {
        assertThat(SegmentCriteriaLogicSupport.matchesAnyOr(List.of(false, false, true))).isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAnyOr(List.of(false, false))).isFalse();
        assertThat(SegmentCriteriaLogicSupport.matchesAnyOr(List.of())).isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAnyOr(null)).isTrue();
    }

    @Test
    void evaluateLeftToRightOrChainMatchesAnyBranch() {
        // city=Munich (first) OR city=Berlin OR city=Hamburg
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(false, true, false),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.OR)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(false, false, false),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.OR)))
                .isFalse();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, false, false),
                                List.of(
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.OR)))
                .isTrue();
    }

    @Test
    void evaluateMixedAndOrUsesLeftAssociativity() {
        // (true AND false) OR true  => true
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, false, true),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.OR)))
                .isTrue();
        // (true AND false) OR false => false
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, false, false),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.OR)))
                .isFalse();
        // (false OR true) AND false => false
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(false, true, false),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.AND)))
                .isFalse();
        // (false OR true) AND true => true
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(false, true, true),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.AND)))
                .isTrue();
    }

    @Test
    void firstCriterionJoinOperatorIsIgnoredDuringEvaluation() {
        // First join OR must not force a true result when the first match is false and the second
        // join is AND.
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(false, true),
                                List.of(SegmentJoinOperator.OR, SegmentJoinOperator.AND)))
                .isFalse();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(false, true),
                                List.of(SegmentJoinOperator.AND, SegmentJoinOperator.OR)))
                .isTrue();
    }

    @Test
    void matchesAllCriteriaSupportsOrJoinOnCreateSegmentCriteriaCommand() {
        List<CreateSegmentCriteriaCommand> criteria =
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                null,
                                SegmentJoinOperator.AND),
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Berlin",
                                null,
                                SegmentJoinOperator.OR));

        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria, command -> "Munich".equals(command.value())))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria, command -> "Berlin".equals(command.value())))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria, command -> "Hamburg".equals(command.value())))
                .isFalse();
    }

    @Test
    void matchesAnyWithOrIgnoresJoinOperatorsAndRequiresAnyMatch() {
        List<CreateSegmentCriteriaCommand> criteria =
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                "location",
                                SegmentJoinOperator.AND),
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Berlin",
                                "location",
                                SegmentJoinOperator.AND));

        assertThat(
                        SegmentCriteriaLogicSupport.matchesAnyWithOr(
                                criteria, command -> "Berlin".equals(command.value())))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAnyWithOr(
                                criteria, command -> "Hamburg".equals(command.value())))
                .isFalse();
    }

    @Test
    void detectsPureOrChains() {
        assertThat(
                        SegmentCriteriaLogicSupport.isPureOrChain(
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.OR)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.isPureOrChain(
                                List.of(SegmentJoinOperator.OR, SegmentJoinOperator.AND)))
                .isFalse();
        assertThat(SegmentCriteriaLogicSupport.isPureOrChain(List.of(SegmentJoinOperator.OR)))
                .isFalse();
    }

    @Test
    void evaluateRejectsMismatchedJoinOperatorListSize() {
        assertThatThrownBy(
                        () ->
                                SegmentCriteriaLogicSupport.evaluate(
                                        List.of(true, false), List.of(SegmentJoinOperator.OR)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joinOperators size must match");
    }

    @Test
    void threeWayOrMatchesAllCriteriaMatchesAnyFieldValue() {
        List<CreateSegmentCriteriaCommand> criteria =
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                "location",
                                SegmentJoinOperator.AND),
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Berlin",
                                "location",
                                SegmentJoinOperator.OR),
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Hamburg",
                                "location",
                                SegmentJoinOperator.OR));

        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria, command -> "Berlin".equals(command.value())))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria, command -> "Hamburg".equals(command.value())))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria, command -> "Cologne".equals(command.value())))
                .isFalse();
    }

    @Test
    void matchesAnyOrTruthTableForPartialResults() {
        assertThat(SegmentCriteriaLogicSupport.matchesAnyOr(List.of(true))).isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAnyOr(List.of(false))).isFalse();
        assertThat(SegmentCriteriaLogicSupport.matchesAnyOr(List.of(false, false, true, false)))
                .isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAnyOr(List.of(false, false, false, false)))
                .isFalse();
    }

    @Test
    void mixedAndOrWithCreateCommandsUsesLeftAssociativity() {
        List<CreateSegmentCriteriaCommand> criteria =
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "customer_type",
                                SegmentOperator.EQUALS,
                                "PROSPECT",
                                null,
                                SegmentJoinOperator.AND),
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                null,
                                SegmentJoinOperator.AND),
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Berlin",
                                null,
                                SegmentJoinOperator.OR));

        // Profile is PROSPECT in Munich: (true AND true) OR false => true
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria,
                                command ->
                                        switch (command.fieldName()) {
                                            case "customer_type" ->
                                                    "PROSPECT".equals(command.value());
                                            case "city" -> "Munich".equals(command.value());
                                            default -> false;
                                        }))
                .isTrue();
        // Profile is CUSTOMER in Berlin: (false AND false) OR true => true
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria,
                                command ->
                                        switch (command.fieldName()) {
                                            case "customer_type" -> false;
                                            case "city" -> "Berlin".equals(command.value());
                                            default -> false;
                                        }))
                .isTrue();
        // Profile is CUSTOMER in Hamburg: (false AND false) OR false => false
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria,
                                command ->
                                        switch (command.fieldName()) {
                                            case "customer_type" -> false;
                                            case "city" -> "Hamburg".equals(command.value());
                                            default -> false;
                                        }))
                .isFalse();
    }
}
