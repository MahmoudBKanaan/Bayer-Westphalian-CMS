package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for KB FR-078 AND criteria logic (conjunctive combination of segment filters; default
 * join when {@code join_operator} is omitted). Item 196 acceptance builds on these helpers.
 */
class SegmentAndCriteriaFilteringTests {

    @Test
    void recognizesAndJoinOperatorAndDefaultsNullToAnd() {
        assertThat(SegmentCriteriaLogicSupport.isAnd(SegmentJoinOperator.AND)).isTrue();
        assertThat(SegmentCriteriaLogicSupport.isAnd(null)).isTrue();
        assertThat(SegmentCriteriaLogicSupport.isAnd(SegmentJoinOperator.OR)).isFalse();
        assertThat(SegmentCriteriaLogicSupport.defaultJoinOperator(SegmentJoinOperator.AND))
                .isEqualTo(SegmentJoinOperator.AND);
        assertThat(SegmentCriteriaLogicSupport.defaultJoinOperator(null))
                .isEqualTo(SegmentJoinOperator.AND);
    }

    @Test
    void combineUsesConjunctionWhenJoinOperatorIsAnd() {
        assertThat(SegmentCriteriaLogicSupport.combine(true, true, SegmentJoinOperator.AND))
                .isTrue();
        assertThat(SegmentCriteriaLogicSupport.combine(true, false, SegmentJoinOperator.AND))
                .isFalse();
        assertThat(SegmentCriteriaLogicSupport.combine(false, true, SegmentJoinOperator.AND))
                .isFalse();
        assertThat(SegmentCriteriaLogicSupport.combine(false, false, SegmentJoinOperator.AND))
                .isFalse();
        // Null join is AND per KB default.
        assertThat(SegmentCriteriaLogicSupport.combine(true, true, null)).isTrue();
        assertThat(SegmentCriteriaLogicSupport.combine(true, false, null)).isFalse();
    }

    @Test
    void pureAndRequiresEveryMatchTrue() {
        assertThat(SegmentCriteriaLogicSupport.matchesAllAnd(List.of(true, true, true))).isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAllAnd(List.of(true, false, true))).isFalse();
        assertThat(SegmentCriteriaLogicSupport.matchesAllAnd(List.of(false))).isFalse();
        assertThat(SegmentCriteriaLogicSupport.matchesAllAnd(List.of())).isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAllAnd(null)).isTrue();
    }

    @Test
    void evaluateLeftToRightAndChainRequiresAllBranches() {
        // city=Munich AND customer_type=PROSPECT AND status=ACTIVE
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, true, true),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, true, false),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND)))
                .isFalse();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, false, true),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND)))
                .isFalse();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(false, true, true),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND)))
                .isFalse();
    }

    @Test
    void evaluateTreatsNullJoinOperatorsAsAnd() {
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, true), Arrays.asList(null, null)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, false), Arrays.asList(null, null)))
                .isFalse();
    }

    @Test
    void firstCriterionJoinOperatorIsIgnoredDuringAndEvaluation() {
        // First join OR must not affect pure AND evaluation when second join is AND.
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, false),
                                List.of(SegmentJoinOperator.OR, SegmentJoinOperator.AND)))
                .isFalse();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, true),
                                List.of(SegmentJoinOperator.OR, SegmentJoinOperator.AND)))
                .isTrue();
    }

    @Test
    void matchesAllCriteriaSupportsAndJoinOnCreateSegmentCriteriaCommand() {
        List<CreateSegmentCriteriaCommand> criteria =
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                "location",
                                SegmentJoinOperator.AND),
                        new CreateSegmentCriteriaCommand(
                                "customer_type",
                                SegmentOperator.EQUALS,
                                "PROSPECT",
                                "audience",
                                SegmentJoinOperator.AND));

        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria,
                                command ->
                                        ("city".equals(command.fieldName())
                                                        && "Munich".equals(command.value()))
                                                || ("customer_type".equals(command.fieldName())
                                                        && "PROSPECT".equals(command.value()))))
                .isTrue();

        // Fails when only the city criterion matches (AND requires both).
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria, command -> "Munich".equals(command.value())))
                .isFalse();

        // Fails when only the type criterion matches.
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria, command -> "PROSPECT".equals(command.value())))
                .isFalse();
    }

    @Test
    void matchesAllCriteriaDefaultsNullJoinToAnd() {
        List<CreateSegmentCriteriaCommand> criteria =
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city", SegmentOperator.EQUALS, "Munich", null, null),
                        new CreateSegmentCriteriaCommand(
                                "country", SegmentOperator.EQUALS, "Germany", null, null));

        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria,
                                command ->
                                        "Munich".equals(command.value())
                                                || "Germany".equals(command.value())))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria, command -> "Munich".equals(command.value())))
                .isFalse();
    }

    @Test
    void matchesAllWithAndIgnoresOrJoinOperatorsAndRequiresEveryMatch() {
        List<CreateSegmentCriteriaCommand> criteria =
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                null,
                                SegmentJoinOperator.OR),
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Berlin",
                                null,
                                SegmentJoinOperator.OR));

        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllWithAnd(
                                criteria,
                                command ->
                                        "Munich".equals(command.value())
                                                || "Berlin".equals(command.value())))
                .isTrue();
        // Pure AND helper still requires every predicate call to succeed independently.
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllWithAnd(
                                criteria, command -> "Munich".equals(command.value())))
                .isFalse();
    }

    @Test
    void detectsPureAndChains() {
        assertThat(
                        SegmentCriteriaLogicSupport.isPureAndChain(
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.isPureAndChain(
                                List.of(SegmentJoinOperator.OR, SegmentJoinOperator.AND)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.isPureAndChain(
                                Arrays.asList(null, null, SegmentJoinOperator.AND)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.isPureAndChain(
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.OR)))
                .isFalse();
        assertThat(SegmentCriteriaLogicSupport.isPureAndChain(List.of(SegmentJoinOperator.AND)))
                .isFalse();
        assertThat(SegmentCriteriaLogicSupport.isPureAndChain(List.of())).isFalse();
        assertThat(SegmentCriteriaLogicSupport.isPureAndChain(null)).isFalse();
    }

    @Test
    void emptyCriteriaListMatchesAllProfiles() {
        assertThat(SegmentCriteriaLogicSupport.matchesAllCriteria(List.of(), command -> false))
                .isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAllWithAnd(List.of(), command -> false))
                .isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAllCriteria(null, command -> false)).isTrue();
    }

    @Test
    void threeWayAndMatchesAllCriteriaRequiresEveryField() {
        List<CreateSegmentCriteriaCommand> criteria =
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                "location",
                                SegmentJoinOperator.AND),
                        new CreateSegmentCriteriaCommand(
                                "customer_type",
                                SegmentOperator.EQUALS,
                                "PROSPECT",
                                "audience",
                                SegmentJoinOperator.AND),
                        new CreateSegmentCriteriaCommand(
                                "country",
                                SegmentOperator.EQUALS,
                                "Germany",
                                "location",
                                SegmentJoinOperator.AND));

        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria,
                                command ->
                                        switch (command.fieldName()) {
                                            case "city" -> "Munich".equals(command.value());
                                            case "customer_type" ->
                                                    "PROSPECT".equals(command.value());
                                            case "country" -> "Germany".equals(command.value());
                                            default -> false;
                                        }))
                .isTrue();

        // Missing country match fails the AND chain.
        assertThat(
                        SegmentCriteriaLogicSupport.matchesAllCriteria(
                                criteria,
                                command ->
                                        switch (command.fieldName()) {
                                            case "city" -> "Munich".equals(command.value());
                                            case "customer_type" ->
                                                    "PROSPECT".equals(command.value());
                                            case "country" -> false;
                                            default -> false;
                                        }))
                .isFalse();
    }

    @Test
    void matchesAllAndTruthTableForPartialResults() {
        assertThat(SegmentCriteriaLogicSupport.matchesAllAnd(List.of(true))).isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAllAnd(List.of(false))).isFalse();
        assertThat(SegmentCriteriaLogicSupport.matchesAllAnd(List.of(true, true, true, true)))
                .isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAllAnd(List.of(true, true, true, false)))
                .isFalse();
    }
}
