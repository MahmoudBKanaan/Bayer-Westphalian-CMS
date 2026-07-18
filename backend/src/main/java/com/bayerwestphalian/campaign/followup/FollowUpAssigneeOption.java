package com.bayerwestphalian.campaign.followup;

import com.bayerwestphalian.campaign.user.User;
import java.util.UUID;

/** Selectable assignee for follow-up assignment (Customer Service Agent accounts). */
public record FollowUpAssigneeOption(UUID id, String fullName, String email) {

    public static FollowUpAssigneeOption from(User user) {
        return new FollowUpAssigneeOption(user.getId(), user.getFullName(), user.getEmail());
    }
}
