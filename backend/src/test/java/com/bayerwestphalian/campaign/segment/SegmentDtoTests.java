package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SegmentDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validatesCreateSegmentCriteriaRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(CreateSegmentCriteriaRequest.class, "fieldName")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(
                        field(CreateSegmentCriteriaRequest.class, "fieldName")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(100);
        assertThat(
                        field(CreateSegmentCriteriaRequest.class, "operator")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreateSegmentCriteriaRequest.class, "value")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(
                        field(CreateSegmentCriteriaRequest.class, "logicalGroup")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(50);
    }

    @Test
    void validatesCreateAndUpdateSegmentRequestFieldsFromKb() throws Exception {
        assertThat(field(CreateSegmentRequest.class, "name").isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(CreateSegmentRequest.class, "name").getAnnotation(Size.class).max())
                .isEqualTo(255);
        assertThat(field(UpdateSegmentRequest.class, "name").isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(UpdateSegmentRequest.class, "name").getAnnotation(Size.class).max())
                .isEqualTo(255);
        assertThat(field(SegmentSearchRequest.class, "term").getAnnotation(Size.class).max())
                .isEqualTo(255);
    }

    @Test
    void mapsCreateAndUpdateSegmentRequestsToCommands() {
        CreateSegmentCriteriaRequest criterionRequest =
                new CreateSegmentCriteriaRequest(
                        "customer_type",
                        SegmentOperator.EQUALS,
                        "CUSTOMER",
                        "core-audience",
                        SegmentJoinOperator.OR);

        CreateSegmentCommand createCommand =
                new CreateSegmentRequest(
                                "Expiring homeowner policies",
                                "Customers with policies expiring soon",
                                SegmentVisibility.TEAM,
                                List.of(criterionRequest))
                        .toCommand();
        UpdateSegmentCommand updateCommand =
                new UpdateSegmentRequest(
                                "Renewal audience",
                                "Updated renewal targeting",
                                SegmentVisibility.GLOBAL,
                                List.of(criterionRequest))
                        .toCommand();

        assertThat(createCommand.name()).isEqualTo("Expiring homeowner policies");
        assertThat(createCommand.description()).isEqualTo("Customers with policies expiring soon");
        assertThat(createCommand.visibility()).isEqualTo(SegmentVisibility.TEAM);
        assertThat(createCommand.criteria()).hasSize(1);
        assertThat(createCommand.criteria().getFirst().fieldName()).isEqualTo("customer_type");
        assertThat(createCommand.criteria().getFirst().operator())
                .isEqualTo(SegmentOperator.EQUALS);
        assertThat(createCommand.criteria().getFirst().value()).isEqualTo("CUSTOMER");
        assertThat(createCommand.criteria().getFirst().logicalGroup()).isEqualTo("core-audience");
        assertThat(createCommand.criteria().getFirst().joinOperator())
                .isEqualTo(SegmentJoinOperator.OR);

        assertThat(updateCommand.name()).isEqualTo("Renewal audience");
        assertThat(updateCommand.description()).isEqualTo("Updated renewal targeting");
        assertThat(updateCommand.visibility()).isEqualTo(SegmentVisibility.GLOBAL);
        assertThat(updateCommand.criteria()).hasSize(1);
    }

    @Test
    void defaultsCriteriaJoinOperatorToAndWhenOmitted() {
        CreateSegmentCriteriaCommand command =
                new CreateSegmentCriteriaRequest(
                                "city", SegmentOperator.CONTAINS, "Berlin", null, null)
                        .toCommand();

        assertThat(command.joinOperator()).isEqualTo(SegmentJoinOperator.AND);
    }

    @Test
    void mapsSegmentPreviewRequestToCommand() {
        SegmentPreviewCommand command =
                new SegmentPreviewRequest(
                                List.of(
                                        new CreateSegmentCriteriaRequest(
                                                "age",
                                                SegmentOperator.BETWEEN,
                                                "30..65",
                                                "retirement-readiness",
                                                SegmentJoinOperator.OR)))
                        .toCommand();

        assertThat(command.criteria()).hasSize(1);
        assertThat(command.criteria().getFirst().fieldName()).isEqualTo("age");
        assertThat(command.criteria().getFirst().joinOperator()).isEqualTo(SegmentJoinOperator.OR);
    }

    @Test
    void rejectsCreateSegmentRequestWithoutRequiredKbFields() {
        CreateSegmentRequest request = new CreateSegmentRequest(" ", null, null, List.of());

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("name");
    }

    @Test
    void rejectsCreateSegmentCriteriaRequestWithoutRequiredKbFields() {
        CreateSegmentCriteriaRequest request =
                new CreateSegmentCriteriaRequest(" ", null, " ", null, null);

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("fieldName", "operator", "value");
    }

    @Test
    void rejectsInvalidFieldNameAndLogicalGroupLengths() {
        CreateSegmentCriteriaRequest request =
                new CreateSegmentCriteriaRequest(
                        "X".repeat(101), SegmentOperator.EQUALS, "CUSTOMER", "Y".repeat(51), null);

        assertThat(invalidFields(request)).contains("fieldName", "logicalGroup");
    }

    @Test
    void normalizesSegmentSearchCriteriaForRepositoryFilters() {
        UUID ownerUserId = UUID.randomUUID();
        SegmentSearchCriteria criteria =
                new SegmentSearchRequest(
                                "  renewal audience  ", ownerUserId, SegmentVisibility.TEAM)
                        .toCriteria();
        SegmentSearchCriteria blankCriteria =
                new SegmentSearchRequest("   ", null, null).toCriteria();

        assertThat(criteria.term()).isEqualTo("renewal audience");
        assertThat(criteria.ownerUserId()).isEqualTo(ownerUserId);
        assertThat(criteria.visibility()).isEqualTo(SegmentVisibility.TEAM);
        assertThat(blankCriteria.term()).isNull();
        assertThat(blankCriteria.ownerUserId()).isNull();
        assertThat(blankCriteria.visibility()).isNull();
    }

    @Test
    void mapsSegmentAndCriteriaEntitiesToViews() {
        User owner =
                User.create(
                        "campaign.manager@bayer-westphalian.test",
                        "$2a$10$hashed-password-placeholder",
                        "Campaign Manager");
        ReflectionTestUtils.setField(
                owner, "id", UUID.fromString("10000000-0000-0000-0000-000000000001"));

        Segment segment =
                Segment.create(
                        "Renewal audience",
                        "Customers nearing policy expiration",
                        owner,
                        SegmentVisibility.TEAM);
        ReflectionTestUtils.setField(
                segment, "id", UUID.fromString("20000000-0000-0000-0000-000000000001"));
        SegmentCriteria criterion =
                segment.addCriteria(
                        "customer_type",
                        SegmentOperator.EQUALS,
                        "CUSTOMER",
                        "core-audience",
                        SegmentJoinOperator.AND);
        ReflectionTestUtils.setField(
                criterion, "id", UUID.fromString("30000000-0000-0000-0000-000000000001"));

        SegmentView segmentView = SegmentView.from(segment);
        SegmentCriteriaView criteriaView = SegmentCriteriaView.from(criterion);

        assertThat(segmentView.id()).isEqualTo(segment.getId());
        assertThat(segmentView.name()).isEqualTo("Renewal audience");
        assertThat(segmentView.description()).isEqualTo("Customers nearing policy expiration");
        assertThat(segmentView.ownerUserId()).isEqualTo(owner.getId());
        assertThat(segmentView.ownerFullName()).isEqualTo("Campaign Manager");
        assertThat(segmentView.visibility()).isEqualTo(SegmentVisibility.TEAM);
        assertThat(segmentView.criteria()).containsExactly(criteriaView);

        assertThat(criteriaView.id()).isEqualTo(criterion.getId());
        assertThat(criteriaView.segmentId()).isEqualTo(segment.getId());
        assertThat(criteriaView.fieldName()).isEqualTo("customer_type");
        assertThat(criteriaView.operator()).isEqualTo(SegmentOperator.EQUALS);
        assertThat(criteriaView.value()).isEqualTo("CUSTOMER");
        assertThat(criteriaView.logicalGroup()).isEqualTo("core-audience");
        assertThat(criteriaView.joinOperator()).isEqualTo(SegmentJoinOperator.AND);
    }

    @Test
    void mapsSegmentViewWhenOwnerIsUnassigned() {
        Segment segment = Segment.create("Global baseline", null, null, SegmentVisibility.GLOBAL);

        SegmentView view = SegmentView.from(segment);

        assertThat(view.ownerUserId()).isNull();
        assertThat(view.ownerFullName()).isNull();
        assertThat(view.visibility()).isEqualTo(SegmentVisibility.GLOBAL);
        assertThat(view.criteria()).isEmpty();
    }

    private static Field field(Class<?> type, String fieldName) throws Exception {
        return type.getDeclaredField(fieldName);
    }

    private static Set<String> invalidFields(Object request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
