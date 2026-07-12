package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "segments")
public class Segment extends BaseEntity {

    @NotBlank @Size(max = 255) @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "visibility", nullable = false, columnDefinition = "segment_visibility")
    private SegmentVisibility visibility = SegmentVisibility.PRIVATE;

    @OneToMany(mappedBy = "segment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SegmentCriteria> criteria = new ArrayList<>();

    protected Segment() {}

    private Segment(String name, String description, User owner, SegmentVisibility visibility) {
        updateName(name);
        this.description = description;
        this.owner = owner;
        this.visibility = visibility != null ? visibility : SegmentVisibility.PRIVATE;
    }

    public static Segment create(
            String name, String description, User owner, SegmentVisibility visibility) {
        return new Segment(name, description, owner, visibility);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public User getOwner() {
        return owner;
    }

    public UUID getOwnerUserId() {
        return owner != null ? owner.getId() : null;
    }

    public SegmentVisibility getVisibility() {
        return visibility;
    }

    public List<SegmentCriteria> getCriteria() {
        return List.copyOf(criteria);
    }

    public SegmentCriteria addCriteria(String fieldName, SegmentOperator operator, String value) {
        return addCriteria(fieldName, operator, value, null, SegmentJoinOperator.AND);
    }

    public SegmentCriteria addCriteria(
            String fieldName,
            SegmentOperator operator,
            String value,
            String logicalGroup,
            SegmentJoinOperator joinOperator) {
        SegmentCriteria criterion =
                SegmentCriteria.create(
                        this, fieldName, operator, value, logicalGroup, joinOperator);
        criteria.add(criterion);
        return criterion;
    }

    public void addCriteria(SegmentCriteria criterion) {
        Objects.requireNonNull(criterion, "Segment criterion is required");
        criterion.assignSegment(this);
        criteria.add(criterion);
    }

    public void removeCriteria(SegmentCriteria criterion) {
        if (criterion != null && criteria.remove(criterion)) {
            criterion.detachSegment();
        }
    }

    public void updateName(String name) {
        this.name = requireNonBlank(name, "Segment name");
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void assignOwner(User owner) {
        this.owner = owner;
    }

    public void changeVisibility(SegmentVisibility visibility) {
        this.visibility = Objects.requireNonNull(visibility, "Segment visibility is required");
    }

    public boolean isGlobal() {
        return visibility == SegmentVisibility.GLOBAL;
    }

    public boolean isOwnedBy(UUID userId) {
        return owner != null && userId != null && userId.equals(owner.getId());
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
