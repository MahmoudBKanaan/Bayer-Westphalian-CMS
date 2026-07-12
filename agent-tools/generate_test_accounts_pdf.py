"""Generate a desktop PDF listing Bayer-Westphalian role test accounts."""

from datetime import datetime
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch, mm
from reportlab.platypus import HRFlowable, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle


def main() -> None:
    desktop = Path.home() / "Desktop"
    desktop.mkdir(parents=True, exist_ok=True)
    out = desktop / "Bayer-Westphalian_Test_Accounts.pdf"

    accounts = [
        ("ADMIN", "admin@bayer-westphalian.test", "Test Admin", "MVP"),
        ("CAMPAIGN_MANAGER", "campaign.manager@bayer-westphalian.test", "Test Campaign Manager", "MVP"),
        ("BI_ANALYST", "bi.analyst@bayer-westphalian.test", "Test BI Analyst", "MVP"),
        ("PRODUCT_MANAGER", "product.manager@bayer-westphalian.test", "Test Product Manager", "MVP"),
        ("COMPLIANCE_OFFICER", "compliance.officer@bayer-westphalian.test", "Test Compliance Officer", "MVP"),
        (
            "CUSTOMER_SERVICE_AGENT",
            "customer.service@bayer-westphalian.test",
            "Test Customer Service Agent",
            "MVP",
        ),
        ("SALES_AGENT", "sales.agent@bayer-westphalian.test", "Test Sales Agent", "Extended"),
        (
            "MARKETING_ANALYST",
            "marketing.analyst@bayer-westphalian.test",
            "Test Marketing Analyst",
            "Extended",
        ),
        ("EXECUTIVE_VIEWER", "executive.viewer@bayer-westphalian.test", "Test Executive Viewer", "Extended"),
        ("SYSTEM_AUDITOR", "system.auditor@bayer-westphalian.test", "Test System Auditor", "Extended"),
    ]

    password = "Neoarel@7368"
    generated = datetime.now().astimezone().strftime("%Y-%m-%d %H:%M %Z")

    doc = SimpleDocTemplate(
        str(out),
        pagesize=A4,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=16 * mm,
        bottomMargin=16 * mm,
        title="Bayer-Westphalian Test Accounts",
        author="Bayer-Westphalian Campaign Platform",
    )

    styles = getSampleStyleSheet()
    title_style = ParagraphStyle(
        "TitleCustom",
        parent=styles["Heading1"],
        fontSize=16,
        spaceAfter=6,
        textColor=colors.HexColor("#0f2744"),
    )
    subtitle = ParagraphStyle(
        "Subtitle",
        parent=styles["Normal"],
        fontSize=10,
        textColor=colors.HexColor("#334155"),
        spaceAfter=10,
    )
    body = ParagraphStyle(
        "Body",
        parent=styles["Normal"],
        fontSize=10,
        leading=14,
        textColor=colors.HexColor("#1e293b"),
    )
    note = ParagraphStyle(
        "Note",
        parent=styles["Normal"],
        fontSize=9,
        leading=12,
        textColor=colors.HexColor("#64748b"),
        spaceBefore=8,
    )

    story = []
    story.append(Paragraph("Bayer-Westphalian Campaign Platform", title_style))
    story.append(Paragraph("Test Accounts by System Role", styles["Heading2"]))
    story.append(Paragraph(f"Generated: {generated}", subtitle))
    story.append(
        HRFlowable(width="100%", thickness=1, color=colors.HexColor("#94a3b8"), spaceAfter=10)
    )
    story.append(
        Paragraph(
            "One ACTIVE test user exists for each system role. "
            "All accounts below share the same password.",
            body,
        )
    )
    story.append(Spacer(1, 6))
    story.append(
        Paragraph(
            f"<b>Shared password:</b> <font face='Courier'>{password}</font>",
            body,
        )
    )
    story.append(Spacer(1, 12))

    rows = [["#", "Role", "Email", "Display name", "Tier"]]
    for i, (role, email, name, tier) in enumerate(accounts, start=1):
        rows.append([str(i), role, email, name, tier])

    table = Table(
        rows,
        colWidths=[0.4 * inch, 1.7 * inch, 2.55 * inch, 1.7 * inch, 0.75 * inch],
    )
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0f2744")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("FONTSIZE", (0, 0), (-1, 0), 9),
                ("FONTSIZE", (0, 1), (-1, -1), 8),
                ("FONTNAME", (0, 1), (-1, -1), "Helvetica"),
                ("ALIGN", (0, 0), (0, -1), "CENTER"),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#cbd5e1")),
                (
                    "ROWBACKGROUNDS",
                    (0, 1),
                    (-1, -1),
                    [colors.white, colors.HexColor("#f1f5f9")],
                ),
                ("LEFTPADDING", (0, 0), (-1, -1), 5),
                ("RIGHTPADDING", (0, 0), (-1, -1), 5),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]
        )
    )
    story.append(table)
    story.append(
        Paragraph(
            "Internal test use only. Do not use these credentials in production. "
            "Accounts are seeded via Flyway migration V21 and stored in PostgreSQL (bwc_campaign).",
            note,
        )
    )
    story.append(
        Paragraph(
            "Login URL (local): http://localhost:5173/login &nbsp;|&nbsp; "
            "API: http://localhost:8080/api/auth/login",
            note,
        )
    )

    doc.build(story)
    print(out)
    print(f"exists={out.exists()} size={out.stat().st_size}")


if __name__ == "__main__":
    main()
