package com.bayerwestphalian.campaign.campaign;

/** Service command for approving a submitted campaign (optional compliance review notes). */
public record ApproveCampaignCommand(String complianceReviewNotes) {}
