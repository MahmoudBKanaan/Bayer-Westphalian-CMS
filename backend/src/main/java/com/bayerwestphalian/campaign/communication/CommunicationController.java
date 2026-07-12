package com.bayerwestphalian.campaign.communication;

import com.bayerwestphalian.campaign.campaign.ContactEventSearchRequest;
import com.bayerwestphalian.campaign.campaign.ContactEventView;
import com.bayerwestphalian.campaign.campaign.RecordContactEventRequest;
import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API for campaign/customer communication history. */
@RestController
@RequestMapping("/api/contact-events")
public class CommunicationController {

    private final CommunicationService communicationService;

    public CommunicationController(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    /**
     * Contact timeline endpoint (KB item 333).
     *
     * <p>{@code GET /api/contact-events/timeline} returns contact history events ordered by the
     * repository timeline sort. Optional query filters: {@code customerId}, {@code campaignId}, and
     * {@code eventType}.
     */
    @GetMapping("/timeline")
    @PreAuthorize("@authz.canReadCustomers() || @authz.canReadCampaigns()")
    public ResponseEntity<ApiResponse<List<ContactEventView>>> getContactTimeline(
            @Valid @ModelAttribute ContactEventSearchRequest searchRequest) {
        List<ContactEventView> timeline =
                communicationService.searchContactEvents(searchRequest.toCriteria());

        return ResponseEntity.ok(ApiResponse.success("Contact timeline loaded", timeline));
    }

    /** Record a new contact event or outcome manually. */
    @PostMapping
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    public ResponseEntity<ApiResponse<ContactEventView>> recordContactEvent(
            @Valid @RequestBody RecordContactEventRequest request) {
        ContactEventView event = communicationService.recordContactEvent(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Contact event recorded", event));
    }
}
