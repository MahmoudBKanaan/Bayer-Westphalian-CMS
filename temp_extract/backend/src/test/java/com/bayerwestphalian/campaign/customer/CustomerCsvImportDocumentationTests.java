package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerCsvImportDocumentationTests {

    private static final Path CUSTOMER_CSV_IMPORT_GUIDE =
            Path.of("../docs/admin/customer-csv-import-guide.md");

    @Test
    void documentsCsvImportInvalidRowRejectionAndRowLevelErrors() throws Exception {
        String guide = Files.readString(CUSTOMER_CSV_IMPORT_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Customer CSV Import Guide")
                .contains("POST /api/customers/import")
                .contains("multipart form data")
                .contains("Valid rows are imported")
                .contains("Invalid rows are rejected")
                .contains("do not create customer records")
                .contains("partially succeed")
                .contains("importedCount")
                .contains("failedCount")
                .contains("customers")
                .contains("errors")
                .contains("lineNumber")
                .contains("field")
                .contains("message")
                .contains("value");
    }

    @Test
    void documentsCsvHeadersAndValidationFields() throws Exception {
        String guide = Files.readString(CUSTOMER_CSV_IMPORT_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("UTF-8 encoded comma-separated values")
                .contains("first non-empty line is treated as the")
                .contains("Header names are matched")
                .contains("case-insensitively")
                .contains("Fields may be quoted")
                .contains("customer_type")
                .contains("first_name")
                .contains("last_name")
                .contains("email")
                .contains("phone")
                .contains("address_line")
                .contains("city")
                .contains("country")
                .contains("date_of_birth")
                .contains("age_group")
                .contains("status")
                .contains("do_not_contact")
                .contains("source")
                .contains("required header set")
                .contains("must be a valid email");
    }

    @Test
    void documentsCsvAcceptedValuesDefaultsAndFormats() throws Exception {
        String guide = Files.readString(CUSTOMER_CSV_IMPORT_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("CUSTOMER")
                .contains("PROSPECT")
                .contains("BENEFICIARY")
                .contains("ACTIVE")
                .contains("INACTIVE")
                .contains("INTERESTED")
                .contains("UNINTERESTED")
                .contains("CONVERTED")
                .contains("MINOR")
                .contains("AGE_18_25")
                .contains("AGE_26_40")
                .contains("AGE_41_60")
                .contains("AGE_60_PLUS")
                .contains("18_25")
                .contains("26_40")
                .contains("41_60")
                .contains("60_PLUS")
                .contains("yyyy-MM-dd")
                .contains("future dates are rejected")
                .contains("blank defaults to `ACTIVE`")
                .contains("`true` or `false`; blank defaults to `false`")
                .contains("maximum 100 characters")
                .contains("maximum 255 characters")
                .contains("7 to 50 characters");
    }

    @Test
    void documentationIndexLinksCustomerCsvImportGuide() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("admin/customer-csv-import-guide.md");
    }
}
