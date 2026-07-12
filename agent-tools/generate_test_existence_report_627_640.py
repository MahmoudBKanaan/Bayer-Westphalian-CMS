"""Generate desktop PDF: test existence report for Sprint 16 items 627-640."""

from datetime import date
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import (
    HRFlowable,
    KeepTogether,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

OUT = Path(r"D:\Documents\IU\AgileApp\docs\evidence\sprint-16-test-existence-report-627-640.pdf")
OUT.parent.mkdir(parents=True, exist_ok=True)

rows = [
    {
        "item": "627",
        "title": "Run role-based access tests",
        "verdict": "EXIST",
        "count": "~36+ primary classes (auth package 25; plus unauthorized/role tests)",
        "locations": [
            "backend/.../auth/* (ProtectedEndpointSecurityTests, MethodAuthorizationAnnotationTests, AuthorizationExpressionsTests, Jwt*, PasswordHashing*, SecurityConfigurationTests, ...)",
            "backend UnauthorizedCreateProductTests, UnauthorizedCreateContactEventTests, UnauthorizedUserCannotExportRestrictedReportsTests, BiAnalystCannotEditSegmentUnlessAllowedTests, CampaignManagerSegmentCreationPermissionTests",
            "frontend: features/auth/permissions.test.ts, roleBasedMenu.test.ts, roleBasedMenu + roleNavigation integration, tests/e2e/role-based-menu.spec.ts",
        ],
        "notes": "Sufficient RBAC surface for item 627. Execution not assessed.",
    },
    {
        "item": "628",
        "title": "Run consent and eligibility tests",
        "verdict": "EXIST",
        "count": "consent package 8; Eligibility* 3; segment eligibility/consent filters 6+; reminder consent suites",
        "locations": [
            "backend/.../consent/* (ConsentServiceTests, ConsentControllerTests, ConsentChangeCreatesAuditLogTests, OptOutChangeCreatesAuditLogTests, ...)",
            "backend/.../campaign/EligibilityServiceTests, EligibilityResponseTests, EligibilityRulesDocumentationTests",
            "backend segment preview eligibility + consent filter tests; DoNotContactChangeCreatesAuditLogTests; ReminderRespectsConsentAndContactLimitsTests",
            "frontend: consentUpdateFlow + integration + e2e/consent-update.spec.ts; exclusionReasons tests",
        ],
        "notes": "Covers consent lifecycle and eligibility gate. Execution not assessed.",
    },
    {
        "item": "629",
        "title": "Run campaign workflow tests",
        "verdict": "EXIST",
        "count": "campaign test package: 67 *Tests.java classes",
        "locations": [
            "backend/.../campaign/* — create/submit/approve/reject/launch, draft update, product/segment selection, recipients, status, audit hooks, integration endpoints",
            "frontend: campaignCreation/Launch/complianceApproval flows + integration + Playwright campaign-creation, campaign-launch, compliance-approval specs",
        ],
        "notes": "Full lifecycle coverage present as test classes. Execution not assessed.",
    },
    {
        "item": "630",
        "title": "Run reminder tests",
        "verdict": "EXIST",
        "count": "schedule package: 27 *Tests.java",
        "locations": [
            "backend/.../schedule/* — Green/Yellow/Red level rules, payment due, payment-completed block, 3/6/12 month product expiration, scheduler, repository IT, service, controller",
            "frontend: RemindersPage.test.tsx, ReminderLevelBadge.test.tsx, api/reminders.test.ts",
        ],
        "notes": "Reminder domain well covered. Execution not assessed.",
    },
    {
        "item": "631",
        "title": "Run analytics tests",
        "verdict": "EXIST",
        "count": "analytics package: 33 *Tests.java",
        "locations": [
            "backend/.../analytics/* — KPI calculate* suites, rates, dashboard/executive/campaign/product performance endpoints, service/controller",
            "frontend: dashboardAnalyticsFlow, dashboardCharts, AnalyticsPage, ExecutiveDashboardPage, analytics API, dashboard-analytics e2e",
        ],
        "notes": "KPI and dashboard analytics tests present. Execution not assessed.",
    },
    {
        "item": "632",
        "title": "Run report export tests",
        "verdict": "EXIST",
        "count": "report package: 16 *Tests.java",
        "locations": [
            "backend/.../report/* — CsvExportWorksTests, PdfExportWorksTests, CampaignCsv/Pdf endpoints, ReportService, unauthorized export, audit on export, history",
            "frontend: reportDownload.test.ts, ReportDownloadPanel.test.tsx, ReportsPage.test.tsx, api/reports.test.ts",
        ],
        "notes": "CSV/PDF export suites present. Execution not assessed.",
    },
    {
        "item": "633",
        "title": "Run audit log tests",
        "verdict": "EXIST",
        "count": "audit package 6 + ~20 domain CreatesAuditLog* tests",
        "locations": [
            "backend/.../audit/* — AuditServiceTests, AuditControllerTests, repository, documentation",
            "CreatesAuditLog* for consent, opt-out, campaign create/submit/approve/launch, product changes, user create/disable/role, report export, customer delete/DNC",
            "frontend: AuditPage.test.tsx, auditLogs API, AuditActionBadge",
        ],
        "notes": "Core audit module + sensitive-action audit hooks exist. Execution not assessed.",
    },
    {
        "item": "634",
        "title": "Run AI rule tests",
        "verdict": "EXIST",
        "count": "ai package: 15 *Tests.java",
        "locations": [
            "backend/.../ai/* — AiRecommendationServiceTests, AiSearchServiceTests, CampaignCopyServiceTests, AiSupportsHumanDecisionMakingOnlyTests, ConfigurableMonthlyContactLimitAiTests, documentation/evidence tests",
            "frontend: api/ai.test.ts, AiExplanationDisplay, AiRecommendationSections",
        ],
        "notes": "AI decision-support and human-approval rules covered. Execution not assessed.",
    },
    {
        "item": "635",
        "title": "Run frontend component tests",
        "verdict": "EXIST",
        "count": "24 component test files under frontend/src/components/",
        "locations": [
            "frontend/src/components/**/*.test.tsx (badges, dialogs, panels, charts, FormValidationMessage, frontendComponentInventory, ...)",
            "docs/testing/frontend-component-tests.md (item 595)",
        ],
        "notes": "Component suite exists (Vitest/RTL). Execution not assessed.",
    },
    {
        "item": "636",
        "title": "Run frontend integration tests",
        "verdict": "EXIST",
        "count": "15 integration test modules under frontend/src/test/integration/",
        "locations": [
            "login, customer, consent, product, segment, campaign create/launch, compliance, dashboard, role menu/navigation, keyboard, a11y, workflow routes, authRouting",
            "docs/testing/frontend-integration-tests.md (item 596); renderApp harness",
        ],
        "notes": "Integration layer present. Execution not assessed.",
    },
    {
        "item": "637",
        "title": "Run E2E happy-path tests",
        "verdict": "EXIST",
        "count": "1 happy-path Playwright spec + supporting flow/mock unit tests; 13 additional e2e specs",
        "locations": [
            "frontend/tests/e2e/happy-path.spec.ts",
            "frontend/src/features/e2e/happyPathFlow.test.ts, happyPathApiMock.test.ts",
            "docs/testing/playwright-e2e.md (item 597)",
            "Related journey specs: login-flow, customer-creation, consent-update, product-creation, segment-creation, campaign-creation, compliance-approval, campaign-launch, dashboard-analytics",
        ],
        "notes": "Happy-path E2E artifact exists. Execution not assessed.",
    },
    {
        "item": "638",
        "title": "Run accessibility checks",
        "verdict": "EXIST",
        "count": "3 feature a11y unit + 2 integration + 2 Playwright specs",
        "locations": [
            "features/a11y/keyboardNavigationFlow.test.ts, mainScreensAccessibility.test.ts, accessibilityNotes.test.ts",
            "integration: keyboardNavigation, mainScreensAccessibility",
            "e2e: keyboard-navigation.spec.ts, main-screens-accessibility.spec.ts",
            "docs: accessibility-notes.md, ui-keyboard-navigation.md, ui-main-screens-accessibility.md",
        ],
        "notes": "Basic a11y automated checks exist (labels/keyboard/main screens). Not a full axe/WCAG audit tool run. Execution not assessed.",
    },
    {
        "item": "639",
        "title": "Run performance smoke checks for search and dashboard",
        "verdict": "EXIST",
        "count": "Dedicated PerformanceSmokeTests + performanceSmoke.ts (NFR-003 < 1000 ms); plus functional search/dashboard suites",
        "locations": [
            "backend/.../performance/PerformanceSmokeTests.java — customer/product search + dashboard aggregation under 1s on project-scale datasets",
            "backend/.../performance/PerformanceSmokeDocumentationTests.java",
            "frontend/src/features/testing/performanceSmoke.ts + performanceSmoke.test.ts",
            "docs/testing/performance-smoke.md (item 639 / NFR-003)",
        ],
        "notes": (
            "Dedicated performance smoke suite exists for NFR-003. Functional search/dashboard tests remain supporting evidence. Execution not assessed."
        ),
    },
    {
        "item": "640",
        "title": "Run security regression tests",
        "verdict": "EXIST",
        "count": "15+ security-focused backend classes (auth hardening + common.api/config)",
        "locations": [
            "ProtectedEndpointSecurityTests, SecurityConfigurationTests, PasswordHashingServiceTests, HttpsEnforcementFilterTests, ApiSecurityHeadersFilterTests, ProductionCorsConfigurationTests",
            "ProductionStackTraceHiddenTests, SecureErrorResponsesTests, SecretPresenceValidatorTests, SensitiveAuditAndProductionSafetyAcceptanceTests, SecurityHardeningDocumentationTests",
            "LoginAttemptTrackerTests / rate-limit docs; overlaps with 627 RBAC suite",
        ],
        "notes": "Security regression set exists. Execution not assessed.",
    },
]

summary = {
    "EXIST": sum(1 for r in rows if r["verdict"] == "EXIST"),
    "PARTIAL": sum(1 for r in rows if r["verdict"] == "PARTIAL"),
    "MISSING": sum(1 for r in rows if r["verdict"] == "MISSING"),
}

styles = getSampleStyleSheet()
styles.add(
    ParagraphStyle(
        name="CoverTitle",
        fontName="Helvetica-Bold",
        fontSize=18,
        leading=22,
        alignment=TA_CENTER,
        spaceAfter=8,
        textColor=colors.HexColor("#0F2942"),
    )
)
styles.add(
    ParagraphStyle(
        name="CoverSub",
        fontName="Helvetica",
        fontSize=11,
        leading=14,
        alignment=TA_CENTER,
        textColor=colors.HexColor("#334155"),
        spaceAfter=4,
    )
)
styles.add(
    ParagraphStyle(
        name="Section",
        fontName="Helvetica-Bold",
        fontSize=13,
        leading=16,
        textColor=colors.HexColor("#0F2942"),
        spaceBefore=14,
        spaceAfter=8,
    )
)
styles.add(
    ParagraphStyle(
        name="Body",
        fontName="Helvetica",
        fontSize=9.5,
        leading=12.5,
        textColor=colors.HexColor("#1e293b"),
        spaceAfter=6,
    )
)
styles.add(
    ParagraphStyle(
        name="Small",
        fontName="Helvetica",
        fontSize=8.5,
        leading=11,
        textColor=colors.HexColor("#334155"),
    )
)
styles.add(
    ParagraphStyle(
        name="ItemTitle",
        fontName="Helvetica-Bold",
        fontSize=11,
        leading=13,
        textColor=colors.HexColor("#0F2942"),
        spaceBefore=4,
        spaceAfter=4,
    )
)
styles.add(
    ParagraphStyle(
        name="Mono",
        fontName="Courier",
        fontSize=7.5,
        leading=9.5,
        textColor=colors.HexColor("#0f172a"),
    )
)
styles.add(
    ParagraphStyle(
        name="Cell",
        fontName="Helvetica",
        fontSize=8,
        leading=10,
        textColor=colors.HexColor("#0f172a"),
    )
)
styles.add(
    ParagraphStyle(
        name="CellBold",
        fontName="Helvetica-Bold",
        fontSize=8,
        leading=10,
        textColor=colors.HexColor("#0f172a"),
    )
)
styles.add(
    ParagraphStyle(
        name="Th",
        fontName="Helvetica-Bold",
        fontSize=8,
        leading=10,
        textColor=colors.white,
    )
)

VERDICT_COLORS = {
    "EXIST": colors.HexColor("#166534"),
    "PARTIAL": colors.HexColor("#a16207"),
    "MISSING": colors.HexColor("#991b1b"),
}
VERDICT_BG = {
    "EXIST": colors.HexColor("#dcfce7"),
    "PARTIAL": colors.HexColor("#fef9c3"),
    "MISSING": colors.HexColor("#fee2e2"),
}


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor("#cbd5e1"))
    canvas.setLineWidth(0.5)
    canvas.line(0.7 * inch, 0.55 * inch, letter[0] - 0.7 * inch, 0.55 * inch)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#64748b"))
    canvas.drawString(
        0.7 * inch,
        0.35 * inch,
        "Bayer-Westphalian CMS — Sprint 16 test existence (627–640)",
    )
    canvas.drawRightString(letter[0] - 0.7 * inch, 0.35 * inch, f"Page {doc.page}")
    canvas.restoreState()


story = []
story.append(Spacer(1, 0.5 * inch))
story.append(Paragraph("Sprint 16 — Test Existence Report", styles["CoverTitle"]))
story.append(Paragraph("Backlog items <b>627–640</b> (suite run categories)", styles["CoverSub"]))
story.append(Paragraph("Bayer-Westphalian Campaign Management Platform", styles["CoverSub"]))
story.append(Spacer(1, 0.12 * inch))
story.append(
    HRFlowable(width="100%", thickness=1.5, color=colors.HexColor("#0F2942"), spaceAfter=10)
)
story.append(Paragraph(f"<b>Report date:</b> {date.today().isoformat()}", styles["Body"]))
story.append(
    Paragraph(
        "<b>Scope:</b> Static repository inventory of whether automated tests "
        "<i>exist</i> for each backlog category.",
        styles["Body"],
    )
)
story.append(
    Paragraph(
        "<b>Out of scope:</b> Running suites, pass/fail results, coverage percentages, CI status.",
        styles["Body"],
    )
)
story.append(
    Paragraph(
        "<b>Method:</b> Filesystem scan of backend/src/test/java, frontend unit/integration/e2e "
        "tests, and related docs (workspace inventory).",
        styles["Body"],
    )
)
story.append(
    Paragraph(
        "<b>Verdict key:</b> <b>EXIST</b> — suite addresses the category; "
        "<b>PARTIAL</b> — related tests only / dedicated suite incomplete; "
        "<b>MISSING</b> — no meaningful tests found.",
        styles["Body"],
    )
)

story.append(Paragraph("1. Executive summary", styles["Section"]))
sum_data = [
    [
        Paragraph("<b>Verdict</b>", styles["Th"]),
        Paragraph("<b>Count (of 14)</b>", styles["Th"]),
        Paragraph("<b>Interpretation</b>", styles["Th"]),
    ],
    [
        Paragraph("<b>EXIST</b>", styles["CellBold"]),
        Paragraph(str(summary["EXIST"]), styles["Cell"]),
        Paragraph("Category has a usable automated suite in-repo", styles["Cell"]),
    ],
    [
        Paragraph("<b>PARTIAL</b>", styles["CellBold"]),
        Paragraph(str(summary["PARTIAL"]), styles["Cell"]),
        Paragraph("Gaps vs a strict reading of the backlog wording", styles["Cell"]),
    ],
    [
        Paragraph("<b>MISSING</b>", styles["CellBold"]),
        Paragraph(str(summary["MISSING"]), styles["Cell"]),
        Paragraph("Nothing found to run for that category", styles["Cell"]),
    ],
]
sum_t = Table(sum_data, colWidths=[1.1 * inch, 1.3 * inch, 4.6 * inch])
sum_t.setStyle(
    TableStyle(
        [
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0F2942")),
            ("BACKGROUND", (0, 1), (-1, 1), VERDICT_BG["EXIST"]),
            ("BACKGROUND", (0, 2), (-1, 2), VERDICT_BG["PARTIAL"]),
            ("BACKGROUND", (0, 3), (-1, 3), VERDICT_BG["MISSING"]),
            ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#94a3b8")),
            ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
            ("LEFTPADDING", (0, 0), (-1, -1), 6),
            ("RIGHTPADDING", (0, 0), (-1, -1), 6),
            ("TOPPADDING", (0, 0), (-1, -1), 5),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ]
    )
)
story.append(sum_t)
story.append(Spacer(1, 0.12 * inch))
story.append(
    Paragraph(
        f"<b>Overall:</b> <b>{summary['EXIST']} of 14</b> items have existing automated test suites. "
        f"<b>{summary['PARTIAL']}</b> item is partial (639 performance smoke). "
        f"<b>{summary['MISSING']}</b> items fully missing. "
        "This report does <b>not</b> claim the tests pass when run.",
        styles["Body"],
    )
)

story.append(Paragraph("2. Overview matrix", styles["Section"]))
matrix = [
    [
        Paragraph("<b>Item</b>", styles["Th"]),
        Paragraph("<b>Backlog title</b>", styles["Th"]),
        Paragraph("<b>Verdict</b>", styles["Th"]),
        Paragraph("<b>Evidence volume (approx.)</b>", styles["Th"]),
    ]
]
for r in rows:
    matrix.append(
        [
            Paragraph(r["item"], styles["CellBold"]),
            Paragraph(r["title"], styles["Cell"]),
            Paragraph(f"<b>{r['verdict']}</b>", styles["CellBold"]),
            Paragraph(r["count"], styles["Cell"]),
        ]
    )
mt = Table(matrix, colWidths=[0.55 * inch, 2.5 * inch, 0.85 * inch, 3.1 * inch])
style_cmds = [
    ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0F2942")),
    ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#94a3b8")),
    ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ("LEFTPADDING", (0, 0), (-1, -1), 4),
    ("RIGHTPADDING", (0, 0), (-1, -1), 4),
    ("TOPPADDING", (0, 0), (-1, -1), 4),
    ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.HexColor("#f8fafc"), colors.white]),
]
for i, r in enumerate(rows, start=1):
    style_cmds.append(("BACKGROUND", (2, i), (2, i), VERDICT_BG[r["verdict"]]))
    style_cmds.append(("TEXTCOLOR", (2, i), (2, i), VERDICT_COLORS[r["verdict"]]))
mt.setStyle(TableStyle(style_cmds))
story.append(mt)

story.append(PageBreak())
story.append(Paragraph("3. Detailed findings by backlog item", styles["Section"]))
story.append(
    Paragraph(
        "Each section lists representative locations. Counts are approximate class/file counts "
        "from the workspace scan; they are existence evidence, not a promise of green CI.",
        styles["Small"],
    )
)

for r in rows:
    block = []
    block.append(Paragraph(f"Item {r['item']} — {r['title']}", styles["ItemTitle"]))
    vtable = Table(
        [
            [
                Paragraph(f"<b>Verdict: {r['verdict']}</b>", styles["CellBold"]),
                Paragraph(f"<b>Evidence:</b> {r['count']}", styles["Cell"]),
            ]
        ],
        colWidths=[1.6 * inch, 5.4 * inch],
    )
    vtable.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (0, 0), VERDICT_BG[r["verdict"]]),
                ("TEXTCOLOR", (0, 0), (0, 0), VERDICT_COLORS[r["verdict"]]),
                ("BACKGROUND", (1, 0), (1, 0), colors.HexColor("#f1f5f9")),
                ("BOX", (0, 0), (-1, -1), 0.4, colors.HexColor("#94a3b8")),
                ("LEFTPADDING", (0, 0), (-1, -1), 6),
                ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
            ]
        )
    )
    block.append(vtable)
    block.append(Spacer(1, 4))
    block.append(Paragraph("<b>Primary locations</b>", styles["Small"]))
    for loc in r["locations"]:
        block.append(Paragraph(f"• {loc}", styles["Mono"]))
    block.append(Spacer(1, 3))
    block.append(Paragraph(f"<b>Notes:</b> {r['notes']}", styles["Body"]))
    block.append(
        HRFlowable(
            width="100%",
            thickness=0.4,
            color=colors.HexColor("#e2e8f0"),
            spaceBefore=2,
            spaceAfter=6,
        )
    )
    story.append(KeepTogether(block))

story.append(PageBreak())
story.append(Paragraph("4. Gaps and recommendations", styles["Section"]))
story.append(
    Paragraph(
        "<b>639 — Performance smoke:</b> Functional tests for product/customer search and "
        "dashboard endpoints/pages are present, which partially support search and dashboard "
        "quality. The backlog asks for <i>performance smoke checks</i> (aligned with NFR-003: "
        "normal searches under ~1 second for the project dataset). No dedicated timed smoke test "
        "class (e.g. PerformanceSmokeTests) was found. <b>Recommendation:</b> add lightweight "
        "smoke tests that call search and dashboard APIs against the local/test dataset and assert "
        "response time under a project threshold, then wire item 639 to those classes.",
        styles["Body"],
    )
)
story.append(
    Paragraph(
        "<b>638 — Accessibility:</b> Marked EXIST for project-level basic checks (keyboard "
        "navigation, main-screen labels/structure, notes). This is not a full automated "
        "axe-core/WCAG AA audit. Optional enhancement: integrate axe in Playwright for main routes.",
        styles["Body"],
    )
)
story.append(
    Paragraph(
        "<b>627 vs 640 overlap:</b> Role-based access and security regression suites share several "
        "classes (e.g. ProtectedEndpointSecurityTests). Both categories EXIST; when running, a "
        "single Maven filter may cover both if desired.",
        styles["Body"],
    )
)
story.append(
    Paragraph(
        "<b>Next steps:</b> This report only answers whether tests exist to be run. Actual "
        "execution belongs to Sprint 16 run/fix items and full-suite gates (e.g. 617 / 642).",
        styles["Body"],
    )
)

story.append(Paragraph("5. Suggested run commands (reference only — not executed)", styles["Section"]))
cmds = [
    ("627 RBAC / auth", "cd backend; mvn test -Dtest=*Authorization*,*ProtectedEndpoint*,*Unauthorized*,*SecurityConfiguration*"),
    ("628 Consent / eligibility", "cd backend; mvn test -Dtest=*Consent*,*Eligibility*,*DoNotContact*,*OptOut*"),
    ("629 Campaign workflow", "cd backend; mvn test -Dtest=com.bayerwestphalian.campaign.campaign.**"),
    ("630 Reminders", "cd backend; mvn test -Dtest=com.bayerwestphalian.campaign.schedule.**"),
    ("631 Analytics", "cd backend; mvn test -Dtest=com.bayerwestphalian.campaign.analytics.**"),
    ("632 Reports", "cd backend; mvn test -Dtest=com.bayerwestphalian.campaign.report.**"),
    ("633 Audit", "cd backend; mvn test -Dtest=*Audit*"),
    ("634 AI", "cd backend; mvn test -Dtest=com.bayerwestphalian.campaign.ai.**"),
    ("635 FE components", "cd frontend; npx vitest run src/components"),
    ("636 FE integration", "cd frontend; npx vitest run src/test/integration"),
    ("637 Happy-path E2E", "cd frontend; npx playwright test tests/e2e/happy-path.spec.ts"),
    ("638 Accessibility", "cd frontend; npx vitest run src/features/a11y; npx playwright test keyboard-navigation main-screens-accessibility"),
    ("639 Perf smoke", "(not dedicated yet) functional: ProductSearch* + Dashboard* backend; productSearch + dashboard* frontend"),
    ("640 Security regression", "cd backend; mvn test -Dtest=*Security*,*Https*,*Secret*,*StackTrace*,*SecureError*,*SensitiveAudit*"),
]
cmd_data = [
    [
        Paragraph("<b>Item</b>", styles["Th"]),
        Paragraph("<b>Illustrative command (not run for this report)</b>", styles["Th"]),
    ]
]
for a, b in cmds:
    cmd_data.append(
        [
            Paragraph(a, styles["Cell"]),
            Paragraph(f"<font face='Courier' size='7'>{b}</font>", styles["Cell"]),
        ]
    )
ct = Table(cmd_data, colWidths=[1.7 * inch, 5.3 * inch])
ct.setStyle(
    TableStyle(
        [
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0F2942")),
            ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#94a3b8")),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.HexColor("#f8fafc"), colors.white]),
            ("LEFTPADDING", (0, 0), (-1, -1), 4),
            ("RIGHTPADDING", (0, 0), (-1, -1), 4),
            ("TOPPADDING", (0, 0), (-1, -1), 3),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
        ]
    )
)
story.append(ct)

story.append(Spacer(1, 0.18 * inch))
story.append(Paragraph("6. Document control", styles["Section"]))
story.append(Paragraph(f"<b>Output file:</b> {OUT}", styles["Small"]))
story.append(
    Paragraph(
        "<b>Classification:</b> Internal project QA evidence / desktop report",
        styles["Small"],
    )
)
story.append(
    Paragraph(
        "<b>Related maps:</b> docs/testing/functional-requirements-test-map.md (620), "
        "business-rules-test-map.md (621), non-functional-requirements-test-map.md (622)",
        styles["Small"],
    )
)

doc = SimpleDocTemplate(
    str(OUT),
    pagesize=letter,
    leftMargin=0.7 * inch,
    rightMargin=0.7 * inch,
    topMargin=0.65 * inch,
    bottomMargin=0.7 * inch,
    title="Sprint 16 Test Existence Report 627-640",
    author="Bayer-Westphalian CMS QA",
)
doc.build(story, onFirstPage=footer, onLaterPages=footer)
print(f"Wrote {OUT}")
print(f"Size bytes: {OUT.stat().st_size}")
print(
    f"Summary EXIST={summary['EXIST']} PARTIAL={summary['PARTIAL']} MISSING={summary['MISSING']}"
)
