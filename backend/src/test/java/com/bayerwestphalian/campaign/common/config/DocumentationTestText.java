package com.bayerwestphalian.campaign.common.config;

/** Keeps prose contract assertions independent of Markdown line wrapping. */
final class DocumentationTestText {

    private DocumentationTestText() {}

    static String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }
}
