import type { CampaignFormPayload, RejectCampaignPayload } from "@/api/campaigns";

/** Field-level validation messages for the campaign create/edit form (item 242). */
export type CampaignFormErrors = Partial<Record<keyof CampaignFormPayload, string>>;

export type RejectCampaignFormErrors = Partial<Record<keyof RejectCampaignPayload, string>>;

export const campaignFormValidationMessages = {
  nameRequired: "Campaign name is required.",
  nameMaxLength: "Campaign name must be 255 characters or fewer.",
  objectiveRequired: "Campaign objective is required.",
  channelRequired: "Campaign channel is required.",
  segmentRequired: "Audience segment is required.",
  messageSubjectRequired: "Message subject is required.",
  messageSubjectMaxLength: "Message subject must be 255 characters or fewer.",
  messageBodyRequired: "Message body is required.",
  startDateRequired: "Schedule date is required.",
  endDateRequired: "End date is required.",
  endDateBeforeStart: "End date must not be before start date.",
  segmentIdInvalid: "Segment id must be a valid UUID.",
  rejectionReasonRequired: "Rejection reason is required.",
} as const;

/**
 * Validates draft campaign create/edit form fields against KB form rules (name, objective, segment,
 * channel, message content, schedule dates). Aligns with backend create/update validation messages.
 */
export function validateCampaignForm(value: CampaignFormPayload): CampaignFormErrors {
  const errors: CampaignFormErrors = {};

  if (value.name.trim().length === 0) {
    errors.name = campaignFormValidationMessages.nameRequired;
  } else if (value.name.length > 255) {
    errors.name = campaignFormValidationMessages.nameMaxLength;
  }

  if (value.objective.trim().length === 0) {
    errors.objective = campaignFormValidationMessages.objectiveRequired;
  }

  if (value.channel == null || String(value.channel).trim().length === 0) {
    errors.channel = campaignFormValidationMessages.channelRequired;
  }

  if (value.messageSubject.trim().length === 0) {
    errors.messageSubject = campaignFormValidationMessages.messageSubjectRequired;
  } else if (value.messageSubject.length > 255) {
    errors.messageSubject = campaignFormValidationMessages.messageSubjectMaxLength;
  }

  const segmentId = value.segmentId.trim();
  if (segmentId.length === 0) {
    errors.segmentId = campaignFormValidationMessages.segmentRequired;
  } else if (!isUuid(segmentId)) {
    errors.segmentId = campaignFormValidationMessages.segmentIdInvalid;
  }

  if (value.messageBody.trim().length === 0) {
    errors.messageBody = campaignFormValidationMessages.messageBodyRequired;
  }

  if (value.startDate.trim().length === 0) {
    errors.startDate = campaignFormValidationMessages.startDateRequired;
  }

  if (value.endDate.trim().length === 0) {
    errors.endDate = campaignFormValidationMessages.endDateRequired;
  }

  if (
    value.startDate.trim().length > 0 &&
    value.endDate.trim().length > 0 &&
    value.endDate < value.startDate
  ) {
    errors.endDate = campaignFormValidationMessages.endDateBeforeStart;
  }

  return errors;
}

/** Validates reject form: formal rejection reason is required (item 232 / 242). */
export function validateRejectCampaignForm(value: RejectCampaignPayload): RejectCampaignFormErrors {
  const errors: RejectCampaignFormErrors = {};
  if (value.rejectionReason.trim().length === 0) {
    errors.rejectionReason = campaignFormValidationMessages.rejectionReasonRequired;
  }
  return errors;
}

export function hasCampaignFormErrors(errors: CampaignFormErrors | RejectCampaignFormErrors) {
  return Object.keys(errors).length > 0;
}

function isUuid(value: string) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
}
