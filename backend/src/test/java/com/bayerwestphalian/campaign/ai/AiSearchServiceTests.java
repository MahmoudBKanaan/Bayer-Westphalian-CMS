package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.CommunicationChannel;
import com.bayerwestphalian.campaign.campaign.ContactEvent;
import com.bayerwestphalian.campaign.campaign.ContactEventRepository;
import com.bayerwestphalian.campaign.campaign.ContactEventType;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/** KB item 472: Implement AiSearchService for FR-015 fuzzy customer search. */
@DisplayName("472 Implement AiSearchService")
class AiSearchServiceTests {

    private final CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
    private final ProductOwnershipRepository productOwnershipRepository =
            Mockito.mock(ProductOwnershipRepository.class);
    private final ContactEventRepository contactEventRepository =
            Mockito.mock(ContactEventRepository.class);
    private final AiSearchService service =
            new AiSearchService(
                    customerRepository, productOwnershipRepository, contactEventRepository);

    @Test
    void declaresKbServiceContractAndAuthorization() throws Exception {
        assertThat(AiSearchService.class.getAnnotation(Service.class)).isNotNull();
        Transactional transactional = AiSearchService.class.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();

        assertMethodAuthorization(
                "fuzzyCustomerSearch",
                new Class<?>[] {String.class},
                "@authz.canReadCustomers()");
        assertMethodAuthorization(
                "weightedSearch",
                new Class<?>[] {String.class, int.class},
                "@authz.canReadCustomers()");
        assertMethodAuthorization(
                "explainScore",
                new Class<?>[] {Customer.class, String.class},
                "@authz.canReadCustomers()");
    }

    @Test
    void fuzzyCustomerSearchFindsTypoMatchesAndExplainsScore() {
        Customer ada =
                customer(
                        "20000000-0000-0000-0000-000000000472",
                        "Ada",
                        "Lovelace",
                        "ada.lovelace@example.test",
                        "+49 30 111111",
                        "Berlin",
                        "referral");
        Customer alan =
                customer(
                        "20000000-0000-0000-0000-000000000473",
                        "Alan",
                        "Turing",
                        "alan.turing@example.test",
                        "+49 30 222222",
                        "Munich",
                        "import");
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(alan, ada));

        AiCustomerSearchView view = service.fuzzyCustomerSearch("Ada Lovelce");

        assertThat(view.query()).isEqualTo("Ada Lovelce");
        assertThat(view.totalHits()).isEqualTo(1);
        AiCustomerSearchHitView hit = view.results().get(0);
        assertThat(hit.fullName()).isEqualTo("Ada Lovelace");
        assertThat(hit.score()).isGreaterThanOrEqualTo(BigDecimal.valueOf(30));
        assertThat(hit.explainScore())
                .extracting(ScoreExplanationView::factor)
                .contains("full name");
        assertThat(hit.explainScore())
                .extracting(ScoreExplanationView::detail)
                .anySatisfy(
                        detail -> {
                            assertThat(detail).contains("fuzzy match");
                            assertThat(detail).contains("full name: Ada Lovelace");
                        });
        verify(customerRepository).findActiveProfiles();
    }

    @Test
    void weightedSearchRanksByWeightedFieldsAndHonorsLimit() {
        Customer exactName =
                customer(
                        "20000000-0000-0000-0000-000000000474",
                        "Mila",
                        "Brandt",
                        "mila.brandt@example.test",
                        "+49 89 111111",
                        "Cologne",
                        "branch");
        Customer emailMatch =
                customer(
                        "20000000-0000-0000-0000-000000000475",
                        "Nora",
                        "Klein",
                        "mila.renewal@example.test",
                        "+49 89 222222",
                        "Munich",
                        "web");
        Customer cityMatch =
                customer(
                        "20000000-0000-0000-0000-000000000476",
                        "Paul",
                        "Weber",
                        "paul.weber@example.test",
                        "+49 89 333333",
                        "Mila",
                        "event");
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(cityMatch, emailMatch, exactName));

        AiCustomerSearchView view = service.weightedSearch("mila", 2);

        assertThat(view.totalHits()).isEqualTo(2);
        assertThat(view.results())
                .extracting(AiCustomerSearchHitView::fullName)
                .containsExactly("Mila Brandt", "Nora Klein");
        assertThat(view.results().get(0).score()).isGreaterThan(view.results().get(1).score());
        assertThat(view.results().get(0).explainScore())
                .extracting(ScoreExplanationView::factor)
                .contains("first name", "full name");
        assertThat(view.results().get(0).explainScore())
                .anySatisfy(
                        explanation -> {
                            assertThat(explanation.factor()).isEqualTo("full name");
                            assertThat(explanation.weight()).isEqualByComparingTo("45");
                            assertThat(explanation.contribution()).isPositive();
                            assertThat(explanation.detail()).contains("Mila Brandt");
                        });
        assertThat(view.results().get(1).explainScore())
                .extracting(ScoreExplanationView::factor)
                .contains("email");
    }

    @Test
    void weightedSearchSupportsLocationAndSourceMatches() {
        Customer munich =
                customer(
                        "20000000-0000-0000-0000-000000000477",
                        "Greta",
                        "Meyer",
                        "greta.meyer@example.test",
                        "+49 89 444444",
                        "Munich",
                        "beneficiary expo");
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(munich));

        AiCustomerSearchView cityResults = service.weightedSearch("munich", 10);
        AiCustomerSearchView sourceResults = service.weightedSearch("beneficiary expo", 10);

        assertThat(cityResults.results())
                .singleElement()
                .satisfies(
                        hit -> {
                            assertThat(hit.fullName()).isEqualTo("Greta Meyer");
                            assertThat(hit.explainScore())
                                    .extracting(ScoreExplanationView::factor)
                                    .contains("city");
                        });
        assertThat(sourceResults.results())
                .singleElement()
                .satisfies(
                        hit -> {
                            assertThat(hit.fullName()).isEqualTo("Greta Meyer");
                            assertThat(hit.explainScore())
                                    .extracting(ScoreExplanationView::factor)
                                    .contains("source");
                        });
    }

    @Test
    @DisplayName("494 Fuzzy search returns relevant customers")
    void fuzzySearchReturnsRelevantCustomersAcrossKbSignals() {
        Customer productRelevant =
                customer(
                        "20000000-0000-0000-0000-000000000494",
                        "Eva",
                        "Schmidt",
                        "eva.schmidt@example.test",
                        "+49 30 494494",
                        "Bonn",
                        "branch");
        Customer noteRelevant =
                customer(
                        "20000000-0000-0000-0000-000000000495",
                        "Jonas",
                        "Becker",
                        "jonas.becker@example.test",
                        "+49 30 495495",
                        "Hamburg",
                        "web");
        Customer unrelated =
                customer(
                        "20000000-0000-0000-0000-000000000496",
                        "Clara",
                        "Vogel",
                        "clara.vogel@example.test",
                        "+49 30 496496",
                        "Munich",
                        "referral");
        Product retirementPlan =
                product(
                        "Family Retirement Income Plan",
                        ProductType.INVESTMENT_FUND,
                        "Long-term retirement income protection");
        doReturn(List.of(ProductOwnership.create(
                        productRelevant, retirementPlan, LocalDate.of(2026, 1, 1), null)))
                .when(productOwnershipRepository)
                .findByCustomerId(productRelevant.getId());
        doReturn(List.of(ContactEvent.record(
                        noteRelevant,
                        null,
                        CommunicationChannel.PHONE,
                        ContactEventType.NOTE,
                        Instant.parse("2026-07-01T10:00:00Z"),
                        null,
                        null,
                        "Asked about retirement income after beneficiary payout")))
                .when(contactEventRepository)
                .findByCustomerId(noteRelevant.getId());
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(unrelated, noteRelevant, productRelevant));

        AiCustomerSearchView view = service.fuzzyCustomerSearch("retirement income");

        assertThat(view.query()).isEqualTo("retirement income");
        assertThat(view.results())
                .extracting(AiCustomerSearchHitView::fullName)
                .containsExactly("Eva Schmidt", "Jonas Becker");
        assertThat(view.results().get(0).explainScore())
                .extracting(ScoreExplanationView::factor)
                .contains("product");
        assertThat(view.results().get(1).explainScore())
                .extracting(ScoreExplanationView::factor)
                .contains("notes");
        assertThat(view.results())
                .extracting(AiCustomerSearchHitView::fullName)
                .doesNotContain("Clara Vogel");
    }

    @Test
    void explainScoreOutputIncludesWeightedContributionAndEvidence() {
        Customer customer =
                customer(
                        "20000000-0000-0000-0000-000000000479",
                        "Lea",
                        "Sommer",
                        "lea.sommer@example.test",
                        "+49 30 777777",
                        "Berlin",
                        "advisor note");

        List<ScoreExplanationView> explanation = service.explainScore(customer, "lea");

        assertThat(explanation)
                .anySatisfy(
                        score -> {
                            assertThat(score.factor()).isEqualTo("first name");
                            assertThat(score.weight()).isEqualByComparingTo("28");
                            assertThat(score.contribution()).isEqualByComparingTo("28");
                            assertThat(score.detail()).isEqualTo("exact match (first name: Lea)");
                        });
        assertThat(explanation)
                .anySatisfy(
                        score -> {
                            assertThat(score.factor()).isEqualTo("email");
                            assertThat(score.contribution()).isPositive();
                            assertThat(score.detail()).contains("email: lea.sommer@example.test");
                        });
    }

    @Test
    void explainScoreReturnsNoMatchReasonWhenCustomerDoesNotMatchQuery() {
        Customer customer =
                customer(
                        "20000000-0000-0000-0000-000000000478",
                        "Helena",
                        "Fischer",
                        "helena.fischer@example.test",
                        "+49 40 555555",
                        "Hamburg",
                        "phone");

        List<ScoreExplanationView> explanation = service.explainScore(customer, "zurich");

        assertThat(explanation)
                .singleElement()
                .satisfies(
                        score -> {
                            assertThat(score.factor()).isEqualTo("overall");
                            assertThat(score.contribution()).isEqualByComparingTo(BigDecimal.ZERO);
                            assertThat(score.detail())
                                    .isEqualTo("No meaningful fuzzy customer match");
                        });
    }

    @Test
    void validatesQueryAndLimit() {
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> service.fuzzyCustomerSearch("   "))
                .withMessage("AI customer search validation failed");

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> service.weightedSearch("ada", 0))
                .withMessage("AI customer search validation failed");

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(
                        () -> service.weightedSearch("ada", AiCustomerSearchRequest.MAX_LIMIT + 1))
                .withMessage("AI customer search validation failed");
    }

    private static void assertMethodAuthorization(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws Exception {
        Method method = AiSearchService.class.getMethod(methodName, parameterTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expectedExpression);
    }

    private static Customer customer(
            String id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String city,
            String source) {
        Customer customer = Customer.create(CustomerType.PROSPECT, firstName, lastName);
        ReflectionTestUtils.setField(customer, "id", UUID.fromString(id));
        customer.updateContactDetails(email, phone);
        customer.updateAddress("Main Street 1", city, "Germany");
        customer.recordSource(source);
        return customer;
    }

    private static Product product(String name, ProductType productType, String description) {
        Product product = Product.create(name, productType, BigDecimal.TEN, 12);
        product.updateDetails(name, productType, description, 12, "standard");
        return product;
    }
}
