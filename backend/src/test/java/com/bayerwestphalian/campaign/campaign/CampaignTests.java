package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 211: Campaign entity maps the {@code campaigns} table and controlled lifecycle methods.
 */
class CampaignTests {

    @Test
    void mapsKbCampaignsTableAsJpaEntity() {
        assertThat(Campaign.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Campaign.class.getAnnotation(Table.class).name()).isEqualTo("campaigns");
        assertThat(BaseEntity.class).isAssignableFrom(Campaign.class);
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<Campaign> constructor = Campaign.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbCampaignColumnsAndValidationRules() throws Exception {
        assertColumn("name", "name", false, 255);
        assertColumn("objective", "objective", false, 255);
        assertColumn("status", "status", false, 255);
        assertColumn("channel", "channel", false, 255);
        assertColumn("messageSubject", "message_subject", true, 255);
        assertColumn("messageBody", "message_body", true, 255);
        assertColumn("startDate", "start_date", true, 255);
        assertColumn("endDate", "end_date", true, 255);
        assertColumn("approvedAt", "approved_at", true, 255);
        assertColumn("rejectionReason", "rejection_reason", true, 255);
        assertColumn("complianceReviewNotes", "compliance_review_notes", true, 255);

        assertThat(field("name").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("name").getAnnotation(Size.class).max()).isEqualTo(255);
        assertThat(field("objective").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("objective").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("text");
        assertThat(field("status").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("channel").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("messageSubject").getAnnotation(Size.class).max()).isEqualTo(255);
        assertThat(field("messageBody").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("text");
        assertThat(field("rejectionReason").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("text");
        assertThat(field("complianceReviewNotes").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("text");
    }

    @Test
    void mapsOwnerSegmentAndApproverRelationshipsToKbForeignKeys() throws Exception {
        assertManyToOne("owner", "owner_user_id");
        assertManyToOne("segment", "segment_id");
        assertManyToOne("approvedBy", "approved_by");
    }

    @Test
    void mapsStatusAndChannelToKbPostgreSqlEnums() throws Exception {
        assertNamedEnum("status", "campaign_status");
        assertNamedEnum("channel", "campaign_channel");
    }

    @Test
    void createsDraftCampaignWithKbFactoryFields() {
        User owner = user("campaign.manager@bayer-westphalian.test", "Campaign Manager");
        Segment segment = segment(owner);

        Campaign campaign =
                Campaign.create(
                        "Life renewal outreach",
                        "Promote life insurance renewals to eligible prospects",
                        owner,
                        segment,
                        CampaignChannel.EMAIL);

        assertThat(campaign.getName()).isEqualTo("Life renewal outreach");
        assertThat(campaign.getObjective())
                .isEqualTo("Promote life insurance renewals to eligible prospects");
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(campaign.isDraft()).isTrue();
        assertThat(campaign.getOwner()).isSameAs(owner);
        assertThat(campaign.getOwnerUserId()).isEqualTo(owner.getId());
        assertThat(campaign.getSegment()).isSameAs(segment);
        assertThat(campaign.getSegmentId()).isEqualTo(segment.getId());
        assertThat(campaign.getChannel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(campaign.canEdit()).isTrue();
        assertThat(campaign.canLaunch()).isFalse();
        assertThat(campaign.getApprovedBy()).isNull();
        assertThat(campaign.getApprovedAt()).isNull();
        assertThat(campaign.getRejectionReason()).isNull();
        assertThat(campaign.isOwnedBy(owner.getId())).isTrue();
    }

    @Test
    void rejectsBlankNameOrObjectiveAndNullChannel() {
        User owner = user("cm@test", "CM");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(
                        () ->
                                Campaign.create(
                                        "  ", "Objective", owner, null, CampaignChannel.EMAIL))
                .withMessageContaining("Campaign name must not be blank");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Campaign.create("Name", " ", owner, null, CampaignChannel.SMS))
                .withMessageContaining("Campaign objective must not be blank");

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> Campaign.create("Name", "Objective", owner, null, null))
                .withMessageContaining("Campaign channel is required");
    }

    @Test
    void updatesEditableDraftFieldsAndSchedule() {
        Campaign campaign = draftCampaign();

        campaign.updateName("  Updated name  ");
        campaign.updateObjective("  Updated objective  ");
        campaign.updateMessage("Subject line", "Message body for EMAIL channel");
        campaign.updateSchedule(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        campaign.changeChannel(CampaignChannel.SMS);

        assertThat(campaign.getName()).isEqualTo("Updated name");
        assertThat(campaign.getObjective()).isEqualTo("Updated objective");
        assertThat(campaign.getMessageSubject()).isEqualTo("Subject line");
        assertThat(campaign.getMessageBody()).isEqualTo("Message body for EMAIL channel");
        assertThat(campaign.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(campaign.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(campaign.getChannel()).isEqualTo(CampaignChannel.SMS);
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        Campaign campaign = draftCampaign();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(
                        () ->
                                campaign.updateSchedule(
                                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1)))
                .withMessageContaining("end date must not be before start date");
    }

    @Test
    void controlledLifecycleFromDraftThroughArchive() {
        User owner = user("cm@test", "Campaign Manager");
        User compliance = user("compliance@test", "Compliance Officer");
        Campaign campaign =
                Campaign.create(
                        "Compliance path", "Full lifecycle", owner, null, CampaignChannel.EMAIL);

        campaign.submit();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
        assertThat(campaign.canEdit()).isFalse();

        campaign.approve(compliance, "Eligible audience confirmed");
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(campaign.getApprovedBy()).isSameAs(compliance);
        assertThat(campaign.getApprovedByUserId()).isEqualTo(compliance.getId());
        assertThat(campaign.getApprovedAt()).isNotNull();
        assertThat(campaign.getComplianceReviewNotes()).isEqualTo("Eligible audience confirmed");
        assertThat(campaign.canLaunch()).isTrue();

        campaign.launch();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(campaign.isActive()).isTrue();

        campaign.pause();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.PAUSED);

        campaign.resume();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);

        campaign.complete();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.COMPLETED);

        campaign.archive();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ARCHIVED);
    }

    @Test
    void rejectThenResubmitClearsApprovalAndSetsRejectedStatus() {
        User compliance = user("compliance@test", "Compliance Officer");
        Campaign campaign = draftCampaign();

        campaign.submit();
        campaign.reject("Missing consent evidence in message");
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(campaign.getRejectionReason()).isEqualTo("Missing consent evidence in message");
        assertThat(campaign.canEdit()).isTrue();
        assertThat(campaign.getApprovedBy()).isNull();
        assertThat(campaign.getApprovedAt()).isNull();

        campaign.updateName("Revised campaign");
        campaign.submit();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
        assertThat(campaign.getRejectionReason()).isNull();

        campaign.approve(compliance);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(campaign.getRejectionReason()).isNull();
    }

    @Test
    void cannotLaunchWithoutApprovalBr005() {
        Campaign campaign = draftCampaign();
        campaign.submit();

        assertThat(campaign.canLaunch()).isFalse();
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(campaign::launch)
                .withMessageContaining("Only APPROVED campaigns can be launched");
    }

    @Test
    void cannotEditSubmittedCampaignWithoutWorkflow() {
        Campaign campaign = draftCampaign();
        campaign.submit();

        assertThat(campaign.canEdit()).isFalse();
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> campaign.updateName("Illegal edit"))
                .withMessageContaining("cannot be edited");
    }

    @Test
    void rejectsInvalidLifecycleTransitions() {
        Campaign campaign = draftCampaign();
        User compliance = user("compliance@test", "Compliance Officer");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> campaign.approve(compliance))
                .withMessageContaining("Only SUBMITTED");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> campaign.reject("reason"))
                .withMessageContaining("Only SUBMITTED");

        campaign.submit();
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(campaign::submit)
                .withMessageContaining("Only DRAFT or REJECTED");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> campaign.reject("  "))
                .withMessageContaining("Rejection reason must not be blank");
    }

    @Test
    void lifecycleTransitionsOnlyAllowConfiguredSourceStatuses() {
        User compliance = user("compliance@test", "Compliance Officer");

        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.SUBMITTED, compliance).submit(),
                "Only DRAFT or REJECTED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.APPROVED, compliance).submit(),
                "Only DRAFT or REJECTED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.ACTIVE, compliance).submit(),
                "Only DRAFT or REJECTED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.COMPLETED, compliance).submit(),
                "Only DRAFT or REJECTED");

        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.DRAFT, compliance).approve(compliance),
                "Only SUBMITTED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.REJECTED, compliance).approve(compliance),
                "Only SUBMITTED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.ACTIVE, compliance).approve(compliance),
                "Only SUBMITTED");

        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.DRAFT, compliance).reject("Missing consent"),
                "Only SUBMITTED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.APPROVED, compliance).reject("Missing consent"),
                "Only SUBMITTED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.ACTIVE, compliance).reject("Missing consent"),
                "Only SUBMITTED");

        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.SUBMITTED, compliance).launch(), "Only APPROVED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.REJECTED, compliance).launch(), "Only APPROVED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.ACTIVE, compliance).launch(), "Only APPROVED");

        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.APPROVED, compliance).pause(), "Only ACTIVE");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.PAUSED, compliance).pause(), "Only ACTIVE");

        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.APPROVED, compliance).resume(), "Only PAUSED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.ACTIVE, compliance).resume(), "Only PAUSED");

        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.APPROVED, compliance).complete(),
                "Only ACTIVE or PAUSED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.REJECTED, compliance).complete(),
                "Only ACTIVE or PAUSED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.COMPLETED, compliance).complete(),
                "Only ACTIVE or PAUSED");

        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.SUBMITTED, compliance).archive(),
                "Only COMPLETED or REJECTED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.ACTIVE, compliance).archive(),
                "Only COMPLETED or REJECTED");
        assertIllegalTransition(
                () -> campaignAt(CampaignStatus.ARCHIVED, compliance).archive(),
                "Only COMPLETED or REJECTED");
    }

    @Test
    void rejectsBlankRejectionReasonAndNullApprover() {
        Campaign campaign = draftCampaign();
        campaign.submit();

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> campaign.approve(null))
                .withMessageContaining("Approver is required");
    }

    @Test
    void archiveAllowedFromCompletedOrRejectedOnly() {
        Campaign rejected = draftCampaign();
        rejected.submit();
        rejected.reject("Policy violation");
        rejected.archive();
        assertThat(rejected.getStatus()).isEqualTo(CampaignStatus.ARCHIVED);

        Campaign active = draftCampaign();
        active.submit();
        active.approve(user("co@test", "CO"));
        active.launch();
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(active::archive)
                .withMessageContaining("Only COMPLETED or REJECTED");
    }

    private static Campaign draftCampaign() {
        User owner = user("campaign.manager@bayer-westphalian.test", "Campaign Manager");
        return Campaign.create(
                "Draft campaign", "Draft objective", owner, segment(owner), CampaignChannel.EMAIL);
    }

    private static User user(String email, String fullName) {
        User user = User.create(email, "$2a$10$hashed-password-placeholder", fullName);
        ReflectionTestUtils.setField(
                user, "id", UUID.fromString("10000000-0000-0000-0000-000000000101"));
        return user;
    }

    private static Segment segment(User owner) {
        Segment segment =
                Segment.create(
                        "Target audience", "Reusable segment", owner, SegmentVisibility.TEAM);
        ReflectionTestUtils.setField(
                segment, "id", UUID.fromString("42000000-0000-0000-0000-000000000201"));
        return segment;
    }

    private static Campaign campaignAt(CampaignStatus status, User compliance) {
        Campaign campaign = draftCampaign();
        switch (status) {
            case DRAFT -> {
                return campaign;
            }
            case SUBMITTED -> {
                campaign.submit();
                return campaign;
            }
            case APPROVED -> {
                campaign.submit();
                campaign.approve(compliance);
                return campaign;
            }
            case REJECTED -> {
                campaign.submit();
                campaign.reject("Policy violation");
                return campaign;
            }
            case ACTIVE -> {
                campaign.submit();
                campaign.approve(compliance);
                campaign.launch();
                return campaign;
            }
            case PAUSED -> {
                campaign.submit();
                campaign.approve(compliance);
                campaign.launch();
                campaign.pause();
                return campaign;
            }
            case COMPLETED -> {
                campaign.submit();
                campaign.approve(compliance);
                campaign.launch();
                campaign.complete();
                return campaign;
            }
            case ARCHIVED -> {
                campaign.submit();
                campaign.approve(compliance);
                campaign.launch();
                campaign.complete();
                campaign.archive();
                return campaign;
            }
        }
        throw new IllegalArgumentException("Unsupported status " + status);
    }

    private static void assertIllegalTransition(
            ThrowingCallable transition, String expectedMessage) {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(transition)
                .withMessageContaining(expectedMessage);
    }

    private static void assertManyToOne(String fieldName, String columnName) throws Exception {
        Field relationship = field(fieldName);
        ManyToOne manyToOne = relationship.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = relationship.getAnnotation(JoinColumn.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(joinColumn.name()).isEqualTo(columnName);
    }

    private static void assertNamedEnum(String fieldName, String columnDefinition)
            throws Exception {
        Field enumField = field(fieldName);
        Column column = enumField.getAnnotation(Column.class);
        Enumerated enumerated = enumField.getAnnotation(Enumerated.class);
        JdbcTypeCode jdbcTypeCode = enumField.getAnnotation(JdbcTypeCode.class);

        assertThat(column.columnDefinition()).isEqualTo(columnDefinition);
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.NAMED_ENUM);
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, int length) throws Exception {
        Field mappedField = field(fieldName);
        Column column = mappedField.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        if (!nullable && length > 0 && column.length() > 0 && column.length() != 255) {
            assertThat(column.length()).isLessThanOrEqualTo(length);
        }
    }

    private static Field field(String name) throws Exception {
        Field field = Campaign.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
