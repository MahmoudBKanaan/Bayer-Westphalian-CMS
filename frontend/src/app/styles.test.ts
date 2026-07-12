import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const stylesPath = path.join(path.dirname(fileURLToPath(import.meta.url)), "styles.css");
const styles = readFileSync(stylesPath, "utf8");
const WCAG_AA_NORMAL_TEXT_RATIO = 4.5;
const WCAG_NON_TEXT_CONTRAST_RATIO = 3;

describe("responsive layout CSS", () => {
  // Item 610: professional UI style tokens documented in docs/development/ui-style-notes.md
  it("keeps a desktop shell with a sticky sidebar", () => {
    expect(styles).toContain("@media (min-width: 1281px)");
    expect(styles).toContain("position: sticky");
    expect(styles).toContain("grid-template-columns: 280px minmax(0, 1fr)");
  });

  it("defines a tablet shell and two-column content layout", () => {
    expect(styles).toContain("@media (min-width: 961px) and (max-width: 1280px)");
    expect(styles).toContain("grid-template-columns: 240px minmax(0, 1fr)");
    expect(styles).toContain("@media (max-width: 960px)");
    expect(styles).toContain("grid-template-columns: repeat(4, minmax(10rem, 1fr))");
    expect(styles).toContain(".user-management-grid");
    expect(styles).toContain(".segment-insight-grid");
  });

  it("keeps mobile internal-use screens usable", () => {
    expect(styles).toContain("@media (max-width: 640px)");
    expect(styles).toContain("min-height: 44px");
    expect(styles).toContain("-webkit-overflow-scrolling: touch");
    expect(styles).toContain("max-height: calc(100vh - 2rem)");
    expect(styles).toContain("max-width: calc(100vw - 2rem)");
    expect(styles).toContain(".table-action-group button");
  });

  it("provides visible keyboard focus indicators", () => {
    // Item 608 / NFR-011: core form controls must show focus-visible rings.
    expect(styles).toContain("a:focus-visible");
    expect(styles).toContain("button:focus-visible");
    expect(styles).toContain("input:focus-visible");
    expect(styles).toContain("select:focus-visible");
    expect(styles).toContain("textarea:focus-visible");
    expect(styles).toContain('[role="tab"]:focus-visible');
    expect(styles).toContain('[role="menuitem"]:focus-visible');
    expect(styles).toContain("outline: 3px solid #2563eb");
    expect(styles).toContain(".skip-link:focus");
  });

  it("supports associated labels for compact form controls", () => {
    expect(styles).toContain(".sr-only");
    expect(styles).toContain("clip-path: inset(50%)");
    expect(styles).toContain("white-space: nowrap");
  });

  it("keeps shared UI color pairs at accessible contrast", () => {
    // Item 609 / NFR-011: main screens rely on these shared tokens for WCAG AA.
    const textPairs = [
      ["Primary button", "#ffffff", "#1d4ed8"],
      ["Disabled button", "#ffffff", "#64748b"],
      ["Secondary button", "#075985", "#e0f2fe"],
      ["Table headers", "#64748b", "#ffffff"],
      ["Breadcrumb separators", "#64748b", "#ffffff"],
      ["Success badge", "#166534", "#dcfce7"],
      ["Warning badge", "#92400e", "#fef3c7"],
      ["Danger badge", "#991b1b", "#fee2e2"],
      ["Info badge", "#1e40af", "#dbeafe"],
    ] as const;

    for (const [label, foreground, background] of textPairs) {
      expect(
        contrastRatio(foreground, background),
        `${label} contrast should meet WCAG AA normal text contrast`,
      ).toBeGreaterThanOrEqual(WCAG_AA_NORMAL_TEXT_RATIO);
    }

    expect(
      contrastRatio("#2563eb", "#ffffff"),
      "Focus ring contrast on light surfaces",
    ).toBeGreaterThanOrEqual(WCAG_NON_TEXT_CONTRAST_RATIO);
  });
});

function contrastRatio(foreground: string, background: string) {
  const foregroundLuminance = relativeLuminance(foreground);
  const backgroundLuminance = relativeLuminance(background);
  const lighter = Math.max(foregroundLuminance, backgroundLuminance);
  const darker = Math.min(foregroundLuminance, backgroundLuminance);

  return (lighter + 0.05) / (darker + 0.05);
}

function relativeLuminance(hexColor: string) {
  const channels = hexColor
    .replace("#", "")
    .match(/.{2}/g)
    ?.map((channel) => Number.parseInt(channel, 16) / 255);

  if (channels == null || channels.length !== 3) {
    throw new Error(`Unsupported color format: ${hexColor}`);
  }

  const [red, green, blue] = channels.map((channel) =>
    channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4,
  ) as [number, number, number];

  return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}
