package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 214: campaign DTOs map request/command/view types for create, update, search, and reject.
 */
class CampaignDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private static final UUID SEGMENT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000201");
    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");

    @Test
    void validatesCreateAndUpdateCampaignRequestFieldsFromKb() throws Exception {
        assertThat(field(CreateCampaignRequest.class, "name").isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(CreateCampaignRequest.class, "name").getAnnotation(NotBlank.class).message())
                .isEqualTo("Campaign name is required.");
        assertThat(field(CreateCampaignRequest.class, "name").getAnnotation(Size.class).max())
                .isEqualTo(255);
        assertThat(field(CreateCampaignRequest.class, "name").getAnnotation(Size.class).message())
                .isEqualTo("Campaign name must be 255 characters or fewer.");
        assertThat(
                        field(CreateCampaignRequest.class, "objective")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(
                        field(CreateCampaignRequest.class, "objective")
                                .getAnnotation(NotBlank.class)
                                .message())
                .isEqualTo("Campaign objective is required.");
        assertThat(
                        field(CreateCampaignRequest.class, "channel")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreateCampaignRequest.class, "channel")
                                .getAnnotation(NotNull.class)
                                .message())
                .isEqualTo("Campaign channel is required.");
        assertThat(
                        field(CreateCampaignRequest.class, "messageSubject")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(255);
        assertThat(
                        field(CreateCampaignRequest.class, "messageSubject")
                                .getAnnotation(Size.class)
                                .message())
                .isEqualTo("Message subject must be 255 characters or fewer.");

        assertThat(field(UpdateCampaignRequest.class, "name").isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(UpdateCampaignRequest.class, "name").getAnnotation(NotBlank.class).message())
                .isEqualTo("Campaign name is required.");
        assertThat(
                        field(UpdateCampaignRequest.class, "objective")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(
                        field(UpdateCampaignRequest.class, "channel")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(UpdateCampaignRequest.class, "messageSubject")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(255);
        assertThat(field(CampaignSearchRequest.class, "term").getAnnotation(Size.class).max())
                .isEqualTo(255);
        assertThat(
                        field(RejectCampaignRequest.class, "rejectionReason")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(
                        field(RejectCampaignRequest.class, "rejectionReason")
                                .getAnnotation(NotBlank.class)
                                .message())
                .isEqualTo("Rejection reason is required.");
    }

    @Test
    void exposesUserFacingCampaignFormValidationMessages() {
        assertThat(invalidMessages(new CreateCampaignRequest(" ", " ", null, null, "X".repeat(256), null, null, null, List.of())))
                .contains(
                        "Campaign name is required.",
                        "Campaign objective is required.",
                        "Campaign channel is required.",
                        "Message subject must be 255 characters or fewer.");
        assertThat(invalidMessages(new UpdateCampaignRequest("", null, null, null, null, null, null, null, null)))
                .contains(
                        "Campaign name is required.",
                        "Campaign objective is required.",
                        "Campaign channel is required.");
        assertThat(invalidMessages(new RejectCampaignRequest("  ")))
                .contains("Rejection reason is required.");
    }

    @Test
    void mapsCreateCampaignRequestToCommandWithProductIds() {
        CreateCampaignCommand command =
                new CreateCampaignRequest(
                                "Life renewal outreach",
                                "Promote life insurance renewals",
                                SEGMENT_ID,
                                CampaignChannel.EMAIL,
                                "Renew your cover",
                                "Dear customer, ...",
                                LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 30),
                                List.of(PRODUCT_ID))
                        .toCommand();

        assertThat(command.name()).isEqualTo("Life renewal outreach");
        assertThat(command.objective()).isEqualTo("Promote life insurance renewals");
        assertThat(command.segmentId()).isEqualTo(SEGMENT_ID);
        assertThat(command.channel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(command.messageSubject()).isEqualTo("Renew your cover");
        assertThat(command.messageBody()).isEqualTo("Dear customer, ...");
        assertThat(command.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(command.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(command.productIds()).containsExactly(PRODUCT_ID);
    }

    @Test
    void createCampaignRequestDefaultsNullProductIdsToEmptyList() {
        CreateCampaignCommand command =
                new CreateCampaignRequest(
                                "Name",
                                "Objective",
                                null,
                                CampaignChannel.SMS,
                                null,
                                null,
                                null,
                                null,
                                null)
                        .toCommand();

        assertThat(command.productIds()).isEmpty();
        assertThat(command.segmentId()).isNull();
        assertThat(command.channel()).isEqualTo(CampaignChannel.SMS);
    }

    @Test
    void mapsUpdateCampaignRequestToCommandPreservingNullProductIds() {
        UpdateCampaignCommand withProducts =
                new UpdateCampaignRequest(
                                "Updated name",
                                "Updated objective",
                                SEGMENT_ID,
                                CampaignChannel.MIXED,
                                "Subject",
                                "Body",
                                LocalDate.of(2026, 10, 1),
                                LocalDate.of(2026, 10, 15),
                                List.of(PRODUCT_ID))
                        .toCommand();
        UpdateCampaignCommand withoutProductChange =
                new UpdateCampaignRequest(
                                "Updated name",
                                "Updated objective",
                                SEGMENT_ID,
                                CampaignChannel.PHONE,
                                null,
                                null,
                                null,
                                null,
                                null)
                        .toCommand();

        assertThat(withProducts.productIds()).containsExactly(PRODUCT_ID);
        assertThat(withProducts.channel()).isEqualTo(CampaignChannel.MIXED);
        assertThat(withoutProductChange.productIds()).isNull();
        assertThat(withoutProductChange.channel()).isEqualTo(CampaignChannel.PHONE);
    }

    @Test
    void mapsRejectCampaignRequestToCommand() {
        RejectCampaignCommand command =
                new RejectCampaignRequest("Missing consent language in message body").toCommand();

        assertThat(command.rejectionReason())
                .isEqualTo("Missing consent language in message body");
        assertThat(command.complianceReviewNotes()).isNull();
    }

    @Test
    void mapsRejectCampaignRequestWithOptionalComplianceReviewNotes() {
        RejectCampaignCommand command =
                new RejectCampaignRequest(
                                "Missing consent language in message body",
                                "Attach guardian consent proof before resubmit.")
                        .toCommand();

        assertThat(command.rejectionReason())
                .isEqualTo("Missing consent language in message body");
        assertThat(command.complianceReviewNotes())
                .isEqualTo("Attach guardian consent proof before resubmit.");
    }

    @Test
    void mapsApproveCampaignRequestToCommandWithOptionalComplianceReviewNotes() {
        ApproveCampaignCommand withNotes =
                new ApproveCampaignRequest("Audience eligibility verified.").toCommand();
        ApproveCampaignCommand withoutNotes = new ApproveCampaignRequest(null).toCommand();

        assertThat(withNotes.complianceReviewNotes()).isEqualTo("Audience eligibility verified.");
        assertThat(withoutNotes.complianceReviewNotes()).isNull();
    }

    @Test
    void rejectsCreateCampaignRequestWithoutRequiredKbFields() {
        CreateCampaignRequest request =
                new CreateCampaignRequest(
                        " ",
                        " ",
                        null,
                        null,
                        "X".repeat(256),
                        null,
                        null,
                        null,
                        List.of());

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("name", "objective", "channel", "messageSubject");
    }

    @Test
    void rejectsUpdateCampaignRequestWithoutRequiredKbFields() {
        UpdateCampaignRequest request =
                new UpdateCampaignRequest(
                        "",
                        null,
                        null,
                        null,
                        "X".repeat(256),
                        null,
                        null,
                        null,
                        null);

        assertThat(invalidFields(request))
                .contains("name", "objective", "channel", "messageSubject");
    }

    @Test
    void rejectsBlankRejectionReason() {
        assertThat(invalidFields(new RejectCampaignRequest("  "))).contains("rejectionReason");
        assertThat(invalidFields(new RejectCampaignRequest("Valid reason"))).isEmpty();
    }

    @Test
    void normalizesCampaignSearchCriteriaForRepositoryFilters() {
        UUID ownerUserId = UUID.randomUUID();
        CampaignSearchCriteria criteria =
                new CampaignSearchRequest(
                                "  life renewal  ",
                                ownerUserId,
                                CampaignStatus.SUBMITTED,
                                SEGMENT_ID)
                        .toCriteria();
        CampaignSearchCriteria blankCriteria =
                new CampaignSearchRequest("   ", null, null, null).toCriteria();

        assertThat(criteria.term()).isEqualTo("life renewal");
        assertThat(criteria.ownerUserId()).isEqualTo(ownerUserId);
        assertThat(criteria.status()).isEqualTo(CampaignStatus.SUBMITTED);
        assertThat(criteria.segmentId()).isEqualTo(SEGMENT_ID);
        assertThat(blankCriteria.term()).isNull();
        assertThat(blankCriteria.ownerUserId()).isNull();
        assertThat(blankCriteria.status()).isNull();
        assertThat(blankCriteria.segmentId()).isNull();
    }

    @Test
    void mapsCampaignEntityToViewWithProductIds() {
        User owner =
                User.create(
                        "campaign.manager@bayer-westphalian.test",
                        "$2a$10$hashed-password-placeholder",
                        "Campaign Manager");
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);

        Segment segment =
                Segment.create(
                        "Munich prospects",
                        "Location audience",
                        owner,
                        SegmentVisibility.TEAM);
        ReflectionTestUtils.setField(segment, "id", SEGMENT_ID);

        Campaign campaign =
                Campaign.create(
                        "Life renewal outreach",
                        "Promote life insurance renewals",
                        owner,
                        segment,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        ReflectionTestUtils.setField(campaign, "createdAt", Instant.parse("2026-07-09T10:00:00Z"));
        ReflectionTestUtils.setField(campaign, "updatedAt", Instant.parse("2026-07-09T11:00:00Z"));
        campaign.updateMessage("Renew your cover", "Dear customer, ...");
        campaign.updateSchedule(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        CampaignView view = CampaignView.from(campaign, List.of(PRODUCT_ID));

        assertThat(view.id()).isEqualTo(CAMPAIGN_ID);
        assertThat(view.name()).isEqualTo("Life renewal outreach");
        assertThat(view.objective()).isEqualTo("Promote life insurance renewals");
        assertThat(view.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(view.ownerUserId()).isEqualTo(OWNER_ID);
        assertThat(view.ownerFullName()).isEqualTo("Campaign Manager");
        assertThat(view.segmentId()).isEqualTo(SEGMENT_ID);
        assertThat(view.segmentName()).isEqualTo("Munich prospects");
        assertThat(view.channel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(view.messageSubject()).isEqualTo("Renew your cover");
        assertThat(view.messageBody()).isEqualTo("Dear customer, ...");
        assertThat(view.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(view.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(view.productIds()).containsExactly(PRODUCT_ID);
        assertThat(view.approvedByUserId()).isNull();
        assertThat(view.rejectionReason()).isNull();
        assertThat(view.complianceReviewNotes()).isNull();
        assertThat(view.createdAt()).isEqualTo(Instant.parse("2026-07-09T10:00:00Z"));
        assertThat(view.updatedAt()).isEqualTo(Instant.parse("2026-07-09T11:00:00Z"));
    }

    @Test
    void mapsCampaignComplianceReviewNotesIntoViewAfterApprove() {
        User owner =
                User.create(
                        "campaign.manager@bayer-westphalian.test",
                        "$2a$10$hashed-password-placeholder",
                        "Campaign Manager");
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        User compliance =
                User.create(
                        "compliance.officer@bayer-westphalian.test",
                        "$2a$10$hashed-password-placeholder",
                        "Compliance Officer");
        ReflectionTestUtils.setField(
                compliance, "id", UUID.fromString("10000000-0000-0000-0000-000000000106"));

        Campaign campaign =
                Campaign.create(
                        "Life renewal outreach",
                        "Promote life insurance renewals",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        campaign.submit();
        campaign.approve(compliance, "Consent and segment eligibility verified.");

        CampaignView view = CampaignView.from(campaign);

        assertThat(view.status()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(view.complianceReviewNotes())
                .isEqualTo("Consent and segment eligibility verified.");
        assertThat(view.approvedByUserId())
                .isEqualTo(UUID.fromString("10000000-0000-0000-0000-000000000106"));
        assertThat(view.approvedByFullName()).isEqualTo("Compliance Officer");
        assertThat(view.rejectionReason()).isNull();
    }

    @Test
    void mapsCampaignViewWithoutProductIdsAsEmptyList() {
        User owner =
                User.create(
                        "cm@test",
                        "$2a$10$hash",
                        "CM");
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        Campaign campaign =
                Campaign.create(
                        "Simple",
                        "Objective",
                        owner,
                        null,
                        CampaignChannel.PHONE);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);

        CampaignView view = CampaignView.from(campaign);

        assertThat(view.productIds()).isEmpty();
        assertThat(view.segmentId()).isNull();
        assertThat(view.segmentName()).isNull();
        assertThat(view.channel()).isEqualTo(CampaignChannel.PHONE);
    }

    @Test
    void mapsCampaignRejectionReasonIntoViewAfterReject() {
        User owner =
                User.create(
                        "campaign.manager@bayer-westphalian.test",
                        "$2a$10$hashed-password-placeholder",
                        "Campaign Manager");
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);

        Campaign campaign =
                Campaign.create(
                        "Life renewal outreach",
                        "Promote life insurance renewals",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        campaign.submit();
        campaign.reject("Missing consent language in message body");

        CampaignView view = CampaignView.from(campaign);

        assertThat(view.status()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(view.rejectionReason()).isEqualTo("Missing consent language in message body");
        assertThat(view.approvedByUserId()).isNull();
        assertThat(view.approvedAt()).isNull();
    }

    @Test
    void mapsApprovedCampaignViewWithApproverFields() {
        User owner =
                User.create("cm@test", "$2a$10$hash", "Campaign Manager");
        User compliance =
                User.create("co@test", "$2a$10$hash", "Compliance Officer");
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        ReflectionTestUtils.setField(
                compliance, "id", UUID.fromString("10000000-0000-0000-0000-000000000106"));

        Campaign campaign =
                Campaign.create(
                        "Approved campaign",
                        "Ready to launch",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        campaign.submit();
        campaign.approve(compliance);

        CampaignView view = CampaignView.from(campaign);

        assertThat(view.status()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(view.approvedByUserId()).isEqualTo(compliance.getId());
        assertThat(view.approvedByFullName()).isEqualTo("Compliance Officer");
        assertThat(view.approvedAt()).isNotNull();
        assertThat(view.rejectionReason()).isNull();
    }

    private static Set<String> invalidFields(Object target) {
        return VALIDATOR.validate(target).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static Set<String> invalidMessages(Object target) {
        return VALIDATOR.validate(target).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
