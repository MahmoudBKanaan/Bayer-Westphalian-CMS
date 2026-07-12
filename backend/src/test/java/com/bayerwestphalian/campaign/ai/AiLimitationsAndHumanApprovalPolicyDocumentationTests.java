package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KB item 507: AI limitations and human approval policy.
 *
 * <p>Asserts that {@code docs/modules/ai-limitations-and-human-approval.md} states COMP-005 /
 * non-bypass limitations, mandatory human approval for campaign copy, decision authority, and
 * operator/engineering obligations, and is linked from the docs index and related AI feature doc.
 */
@DisplayName("507 AI limitations and human approval policy")
class AiLimitationsAndHumanApprovalPolicyDocumentationTests {

    private static final Path POLICY_DOC =
            Path.of("../docs/modules/ai-limitations-and-human-approval.md");
    private static final Path AI_FEATURES_DOC = Path.of("../docs/modules/ai-features.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path AI_PACKAGE_INFO =
            Path.of("src/main/java/com/bayerwestphalian/campaign/ai/package-info.java");

    @Test
    void documentsPolicyTitleStatementAndKbTraceability() throws Exception {
        String documentation = Files.readString(POLICY_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("# AI Limitations and Human Approval Policy")
                .contains("## Policy Statement (KB)")
                .contains("decision-support")
                .contains("COMP-005")
                .contains("Item **468**")
                .contains("Item **507**")
                .contains("Item **512**")
                .contains("AI-005")
                .contains("never")
                .contains("override");
    }

    @Test
    void documentsWhatAiMayAndMustNotDo() throws Exception {
        String documentation = Files.readString(POLICY_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## What AI May Do")
                .contains("Search and rank")
                .contains("Suggest")
                .contains("Recommend")
                .contains("Score")
                .contains("Draft")
                .contains("Warn")
                .contains("## What AI Must Not Do (Hard Limitations)")
                .contains("Cannot approve, reject, submit, launch")
                .contains("Cannot override or invent consent")
                .contains("Cannot ignore marketing opt-out")
                .contains("Cannot bypass do-not-contact")
                .contains("Cannot bypass `EligibilityService`")
                .contains("Cannot auto-apply campaign copy")
                .contains("Cannot self-approve")
                .contains("Cannot send marketing messages")
                .contains("EligibilityService");
    }

    @Test
    void documentsHumanApprovalAndCampaignCopyRules() throws Exception {
        String documentation = Files.readString(POLICY_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Human Approval Policy")
                .contains("### Scope of mandatory human review")
                .contains("### Campaign copy approval rules (AI-005)")
                .contains("requiresHumanApproval")
                .contains("true")
                .contains("AiRecommendationType.COPY")
                .contains("unapproved")
                .contains("approveCampaignCopy")
                .contains("authenticated human")
                .contains("### Stored recommendation integrity")
                .contains("Explanation required");
    }

    @Test
    void documentsDecisionAuthorityAndObligations() throws Exception {
        String documentation = Files.readString(POLICY_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Decision Authority Matrix")
                .contains("Compliance approval")
                .contains("## Operator Obligations")
                .contains("explanations")
                .contains("## Engineering Obligations")
                .contains("campaign lifecycle")
                .contains("## Explicit Non-Goals")
                .contains("Fully automated campaign creation and launch");
    }

    @Test
    void documentsRelatedModulesAndEvidence() throws Exception {
        String documentation = Files.readString(POLICY_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Related Documentation")
                .contains("ai-features.md")
                .contains("ai-decision-support-explanation.md")
                .contains("ai-test-evidence.md")
                .contains("eligibility-rules.md")
                .contains("consent-module.md")
                .contains("campaign-lifecycle.md")
                .contains("## Implementation Evidence")
                .contains("CampaignCopySuggestionView")
                .contains("AiLimitationsAndHumanApprovalPolicyDocumentationTests")
                .contains("ai-limitations-and-human-approval.md");
    }

    @Test
    void documentationIndexLinksPolicyDocument() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("modules/ai-limitations-and-human-approval.md")
                .contains("AI Limitations and Human Approval Policy");
    }

    @Test
    void aiFeatureDocLinksPolicyDocument() throws Exception {
        String features = Files.readString(AI_FEATURES_DOC, StandardCharsets.UTF_8);

        assertThat(features)
                .contains("ai-limitations-and-human-approval.md")
                .contains("AI Limitations and Human Approval Policy");
    }

    @Test
    void aiPackageInfoReferencesPolicyDocument() throws Exception {
        String packageInfo = Files.readString(AI_PACKAGE_INFO, StandardCharsets.UTF_8);

        assertThat(packageInfo)
                .contains("docs/modules/ai-limitations-and-human-approval.md")
                .contains("item 507");
    }
}
