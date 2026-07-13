/**
 * Official Bayer-Westphalian Campaign Management Platform logo assets
 * (web-optimized from Files/Logo.png → public/brand/).
 */

export const BRAND_LOGO_FULL_SRC = "/brand/logo.png";
export const BRAND_LOGO_MARK_SRC = "/brand/logo-mark.png";
export const BRAND_LOGO_ALT = "Bayer-Westphalian Campaign Management Platform";

export type BrandLogoVariant = "full" | "mark";

type BrandLogoProps = {
  /** full = login hero with wordmark; mark = compact nav/topbar emblem */
  variant?: BrandLogoVariant;
  className?: string;
  /** Optional decorative size hint for CSS (does not affect intrinsic file). */
  size?: "sm" | "md" | "lg";
};

/**
 * Default product logo for sign-in and application chrome.
 */
export function BrandLogo({
  variant = "mark",
  className,
  size = "md",
}: BrandLogoProps) {
  const src = variant === "full" ? BRAND_LOGO_FULL_SRC : BRAND_LOGO_MARK_SRC;
  const sizeClass =
    size === "lg" ? "brand-logo--lg" : size === "sm" ? "brand-logo--sm" : "brand-logo--md";
  const classes = ["brand-logo", `brand-logo--${variant}`, sizeClass, className]
    .filter(Boolean)
    .join(" ");

  return (
    <img
      src={src}
      alt={BRAND_LOGO_ALT}
      className={classes}
      decoding="async"
      // Full logo is LCP-relevant on login; marks can load lazily in the shell.
      loading={variant === "full" ? "eager" : "lazy"}
    />
  );
}
