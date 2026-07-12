package com.bayerwestphalian.campaign.segment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "segment_criteria")
public class SegmentCriteria {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_id", nullable = false)
    private Segment segment;

    @NotBlank @Size(max = 100) @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "operator", nullable = false, columnDefinition = "segment_operator")
    private SegmentOperator operator;

    @NotBlank @Column(name = "value", nullable = false, columnDefinition = "text")
    private String value;

    @Size(max = 50) @Column(name = "logical_group", length = 50)
    private String logicalGroup;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "join_operator", nullable = false, columnDefinition = "segment_join_operator")
    private SegmentJoinOperator joinOperator = SegmentJoinOperator.AND;

    protected SegmentCriteria() {}

    private SegmentCriteria(
            Segment segment,
            String fieldName,
            SegmentOperator operator,
            String value,
            String logicalGroup,
            SegmentJoinOperator joinOperator) {
        assignSegment(segment);
        updateFieldName(fieldName);
        this.operator = Objects.requireNonNull(operator, "Segment operator is required");
        updateValue(value);
        updateLogicalGroup(logicalGroup);
        this.joinOperator = joinOperator != null ? joinOperator : SegmentJoinOperator.AND;
    }

    public static SegmentCriteria create(
            Segment segment, String fieldName, SegmentOperator operator, String value) {
        return create(segment, fieldName, operator, value, null, SegmentJoinOperator.AND);
    }

    public static SegmentCriteria create(
            Segment segment,
            String fieldName,
            SegmentOperator operator,
            String value,
            String logicalGroup,
            SegmentJoinOperator joinOperator) {
        return new SegmentCriteria(segment, fieldName, operator, value, logicalGroup, joinOperator);
    }

    public UUID getId() {
        return id;
    }

    public Segment getSegment() {
        return segment;
    }

    public UUID getSegmentId() {
        return segment != null ? segment.getId() : null;
    }

    public String getFieldName() {
        return fieldName;
    }

    public SegmentOperator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public String getLogicalGroup() {
        return logicalGroup;
    }

    public SegmentJoinOperator getJoinOperator() {
        return joinOperator;
    }

    void assignSegment(Segment segment) {
        this.segment = Objects.requireNonNull(segment, "Segment is required");
    }

    void detachSegment() {
        this.segment = null;
    }

    public void updateFieldName(String fieldName) {
        this.fieldName = requireNonBlank(fieldName, "Field name");
    }

    public void updateOperator(SegmentOperator operator) {
        this.operator = Objects.requireNonNull(operator, "Segment operator is required");
    }

    public void updateValue(String value) {
        this.value = requireNonBlank(value, "Criterion value");
    }

    public void updateLogicalGroup(String logicalGroup) {
        this.logicalGroup = normalizeOptionalGroup(logicalGroup);
    }

    public void updateJoinOperator(SegmentJoinOperator joinOperator) {
        this.joinOperator =
                Objects.requireNonNull(joinOperator, "Segment join operator is required");
    }

    public boolean matches(String candidate) {
        return matchesValue(operator, value, candidate);
    }

    public static boolean matchesValue(SegmentOperator operator, String value, String candidate) {
        if (candidate == null || value == null || value.isBlank()) {
            return false;
        }

        String normalizedCandidate = candidate.trim();
        String normalizedValue = value.trim();

        if (operator == SegmentOperator.EQUALS) {
            return normalizedCandidate.equalsIgnoreCase(normalizedValue);
        }
        if (operator == SegmentOperator.NOT_EQUALS) {
            return !normalizedCandidate.equalsIgnoreCase(normalizedValue);
        }
        if (operator == SegmentOperator.CONTAINS) {
            return normalizedCandidate.toLowerCase().contains(normalizedValue.toLowerCase());
        }
        if (operator == SegmentOperator.IN) {
            return Arrays.stream(normalizedValue.split(","))
                    .map(String::trim)
                    .anyMatch(item -> item.equalsIgnoreCase(normalizedCandidate));
        }
        if (operator == SegmentOperator.BETWEEN) {
            return matchesBetween(normalizedCandidate, normalizedValue);
        }
        if (operator == SegmentOperator.BEFORE) {
            return compareComparable(normalizedCandidate, normalizedValue) < 0;
        }
        if (operator == SegmentOperator.AFTER) {
            return compareComparable(normalizedCandidate, normalizedValue) > 0;
        }

        return false;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    private static boolean matchesBetween(String candidate, String rangeValue) {
        String[] parts =
                rangeValue.contains("..") ? rangeValue.split("\\.\\.") : rangeValue.split(",");
        if (parts.length != 2) {
            return false;
        }

        String lowerBound = parts[0].trim();
        String upperBound = parts[1].trim();

        try {
            double candidateNumber = Double.parseDouble(candidate);
            double lowerNumber = Double.parseDouble(lowerBound);
            double upperNumber = Double.parseDouble(upperBound);
            return candidateNumber >= lowerNumber && candidateNumber <= upperNumber;
        } catch (NumberFormatException ignored) {
            return candidate.compareToIgnoreCase(lowerBound) >= 0
                    && candidate.compareToIgnoreCase(upperBound) <= 0;
        }
    }

    private static int compareComparable(String candidate, String boundary) {
        try {
            double candidateNumber = Double.parseDouble(candidate);
            double boundaryNumber = Double.parseDouble(boundary);
            return Double.compare(candidateNumber, boundaryNumber);
        } catch (NumberFormatException ignored) {
            return candidate.compareToIgnoreCase(boundary);
        }
    }

    private static String normalizeOptionalGroup(String logicalGroup) {
        if (logicalGroup == null) {
            return null;
        }

        String trimmed = logicalGroup.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Logical group must not be blank");
        }
        return trimmed;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
