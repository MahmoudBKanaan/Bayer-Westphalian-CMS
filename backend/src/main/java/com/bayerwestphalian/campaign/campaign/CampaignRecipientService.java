package com.bayerwestphalian.campaign.campaign;

import com.bayerwestphalian.campaign.auth.method.CampaignWriteAccess;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.segment.SegmentService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Generates and persists campaign recipient snapshots from selected campaign targeting. */
@Service
public class CampaignRecipientService {

    private final CampaignRepository campaignRepository;
    private final CampaignRecipientRepository campaignRecipientRepository;
    private final CustomerRepository customerRepository;
    private final SegmentService segmentService;

    public CampaignRecipientService(
            CampaignRepository campaignRepository,
            CampaignRecipientRepository campaignRecipientRepository,
            CustomerRepository customerRepository,
            SegmentService segmentService) {
        this.campaignRepository = campaignRepository;
        this.campaignRecipientRepository = campaignRecipientRepository;
        this.customerRepository = customerRepository;
        this.segmentService = segmentService;
    }

    /**
     * Generates the stored recipient snapshot for a campaign (KB item 267).
     *
     * <p>The generated rows mirror campaign-scoped segment evaluation: every criteria match is
     * persisted either as {@link CampaignRecipientStatus#ELIGIBLE} or {@link
     * CampaignRecipientStatus#EXCLUDED} with its row-level exclusion reason and explanation.
     */
    @CampaignWriteAccess
    @Transactional
    public List<CampaignRecipientView> generateRecipients(UUID campaignId) {
        validateCampaignId(campaignId);
        Campaign campaign = findCampaign(campaignId);
        List<CampaignRecipientCandidate> candidates =
                segmentService.evaluateCampaignRecipientCandidates(
                        campaign.getId(), campaign.getSegmentId());

        campaignRecipientRepository.deleteByCampaign_Id(campaign.getId());

        List<CampaignRecipient> recipients = new ArrayList<>();
        Set<UUID> processedCustomerIds = new LinkedHashSet<>();
        for (CampaignRecipientCandidate candidate : candidates) {
            if (candidate == null || candidate.customerId() == null) {
                continue;
            }
            if (!processedCustomerIds.add(candidate.customerId())) {
                continue;
            }
            Customer customer =
                    customerRepository
                            .findById(candidate.customerId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Customer", candidate.customerId()));
            recipients.add(toRecipient(campaign, customer, candidate));
        }

        return campaignRecipientRepository.saveAll(recipients).stream()
                .map(CampaignRecipientView::from)
                .toList();
    }

    /**
     * Lists stored eligible recipients for a campaign (KB item 269).
     *
     * <p>Only recipients persisted with {@link CampaignRecipientStatus#ELIGIBLE} are returned; the
     * excluded snapshot remains available for reporting and compliance analysis through separate
     * recipient workflows.
     */
    @PreAuthorize("@authz.canReadCampaigns()")
    @Transactional(readOnly = true)
    public List<CampaignRecipientView> listEligibleRecipients(UUID campaignId) {
        validateCampaignId(campaignId);
        Campaign campaign = findCampaign(campaignId);
        return campaignRecipientRepository
                .findByCampaignIdAndEligibilityStatus(
                        campaign.getId(), CampaignRecipientStatus.ELIGIBLE)
                .stream()
                .map(CampaignRecipientView::from)
                .toList();
    }

    /**
     * Lists stored excluded recipients for a campaign (KB item 270).
     *
     * <p>Returns recipients persisted with {@link CampaignRecipientStatus#EXCLUDED}, including the
     * stored exclusion reason and eligibility explanation for compliance review and audience
     * troubleshooting.
     */
    @PreAuthorize("@authz.canReadCampaigns()")
    @Transactional(readOnly = true)
    public List<CampaignRecipientView> listExcludedRecipients(UUID campaignId) {
        validateCampaignId(campaignId);
        Campaign campaign = findCampaign(campaignId);
        return campaignRecipientRepository
                .findByCampaignIdAndEligibilityStatus(
                        campaign.getId(), CampaignRecipientStatus.EXCLUDED)
                .stream()
                .map(CampaignRecipientView::from)
                .toList();
    }

    /**
     * Summarizes persisted recipient response counts for a campaign (KB item 284).
     *
     * <p>The summary reflects the stored recipient snapshot and launch outcomes by counting
     * recipients currently marked eligible, excluded, sent, and failed.
     */
    @PreAuthorize("@authz.canReadCampaigns()")
    @Transactional(readOnly = true)
    public CampaignRecipientSummaryView summarizeRecipients(UUID campaignId) {
        validateCampaignId(campaignId);
        Campaign campaign = findCampaign(campaignId);
        UUID resolvedCampaignId = campaign.getId();
        return new CampaignRecipientSummaryView(
                resolvedCampaignId,
                campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        resolvedCampaignId, CampaignRecipientStatus.ELIGIBLE),
                campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        resolvedCampaignId, CampaignRecipientStatus.EXCLUDED),
                campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        resolvedCampaignId, CampaignRecipientStatus.SENT),
                campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        resolvedCampaignId, CampaignRecipientStatus.FAILED));
    }

    private CampaignRecipient toRecipient(
            Campaign campaign, Customer customer, CampaignRecipientCandidate candidate) {
        if (candidate.eligible()) {
            return CampaignRecipient.eligible(
                    campaign, customer, candidate.eligibilityExplanation());
        }
        return CampaignRecipient.excluded(
                campaign,
                customer,
                defaultExclusionReason(candidate.exclusionReason()),
                candidate.eligibilityExplanation());
    }

    private Campaign findCampaign(UUID campaignId) {
        return campaignRepository
                .findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
    }

    private void validateCampaignId(UUID campaignId) {
        if (campaignId == null) {
            throw new ValidationException(
                    "Campaign recipient generation validation failed",
                    List.of("campaignId: is required"));
        }
    }

    private String defaultExclusionReason(String exclusionReason) {
        return exclusionReason == null ? "UNKNOWN" : exclusionReason;
    }
}
