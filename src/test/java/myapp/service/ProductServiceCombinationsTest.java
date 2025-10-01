package myapp.service;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import myapp.domain.Product;
import myapp.domain.enumeration.ProductStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

public class ProductServiceCombinationsTest {

    private static Validator validator;
    private static int validCases;
    private static int invalidCases;

    // Lists for reporting
    private static final List<String> passed = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> failed = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> tableRows = Collections.synchronizedList(new ArrayList<>());

    @BeforeAll
    public static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private static String clean(String token) {
        if (token == null) return null;
        String t = token.trim();
        while (t.endsWith(";")) t = t.substring(0, t.length() - 1).trim();
        if ("null".equals(t)) return null;
        if ("\"\"".equals(t)) return "";
        return t;
    }

    private static Integer parseIntOrNull(String token) {
        String t = clean(token);
        if (t == null || t.isEmpty()) return null;
        try {
            return Integer.valueOf(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseBigDecimalOrNull(String token) {
        String t = clean(token);
        if (t == null || t.isEmpty()) return null;
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDoubleOrNull(String token) {
        String t = clean(token);
        if (t == null || t.isEmpty()) return null;
        try {
            return Double.valueOf(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Instant parseInstantOrNull(String token) {
        String t = clean(token);
        if (t == null || t.isEmpty()) return null;
        try {
            return Instant.parse(t);
        } catch (Exception e) {
            return null;
        }
    }

    private static ProductStatus parseStatusOrNull(String token) {
        String t = clean(token);
        if (t == null || t.isEmpty()) return null;
        try {
            return ProductStatus.valueOf(t);
        } catch (Exception e) {
            return null;
        }
    }

    // Helper to sanitize cell values for Markdown table
    private static String cell(String v) {
        String s = v == null ? "null" : v.trim();
        s = s.replace("|", "/").replace("\n", " ");
        return s;
    }

    // Helper to build a Product from string tokens shared by both tests
    private static Product buildProduct(
        String title,
        String keywords,
        String description,
        String ratingStr,
        String priceStr,
        String quantityStr,
        String statusStr,
        String weightStr,
        String dimensions,
        String dateAddedStr,
        String dateModifiedStr
    ) {
        // rating: empty allowed, non-integer is invalid -> use sentinel -1 to violate @Min(1)
        Integer rating;
        String rTok = clean(ratingStr);
        if (rTok == null || rTok.isEmpty()) {
            rating = null;
        } else {
            try {
                rating = Integer.valueOf(rTok);
            } catch (NumberFormatException e) {
                rating = -1;
            }
        }

        // price: null/invalid numeric will be null and violate @NotNull
        BigDecimal price = parseBigDecimalOrNull(priceStr);

        // quantity: null/invalid numeric will be null and violate @NotNull
        Integer quantity = parseIntOrNull(quantityStr);

        ProductStatus status = parseStatusOrNull(statusStr);

        // weight: empty allowed, non-numeric invalid -> sentinel -1 to violate @DecimalMin(0)
        Double weight;
        String wTok = clean(weightStr);
        if (wTok == null || wTok.isEmpty()) {
            weight = null;
        } else {
            try {
                weight = Double.valueOf(wTok);
            } catch (NumberFormatException e) {
                weight = -1d;
            }
        }

        Instant dateAdded = parseInstantOrNull(dateAddedStr);

        // dateModified: empty allowed, invalid format -> force < dateAdded to violate rule
        Instant dateModified;
        String dmTok = clean(dateModifiedStr);
        if (dmTok == null || dmTok.isEmpty()) {
            dateModified = null;
        } else {
            try {
                dateModified = Instant.parse(dmTok);
            } catch (Exception e) {
                dateModified = (dateAdded != null) ? dateAdded.minusSeconds(1) : Instant.EPOCH;
            }
        }

        return new Product()
            .id(1L)
            .title(clean(title))
            .keywords(clean(keywords))
            .description(clean(description))
            .rating(rating)
            .quantityInStock(quantity)
            .dimensions(clean(dimensions))
            .price(price)
            .status(status)
            .weight(weight)
            .dateAdded(dateAdded)
            .dateModified(dateModified);
    }

    @ParameterizedTest(name = "Valid combination #{index} - title='{0}'")
    @CsvFileSource(resources = "/pict/valid_test_cases.txt", delimiter = '\t', numLinesToSkip = 1)
    void validCombinationsFromPict(
        String title,
        String keywords,
        String description,
        String ratingStr,
        String priceStr,
        String quantityStr,
        String statusStr,
        String weightStr,
        String dimensions,
        String dateAddedStr,
        String dateModifiedStr,
        TestInfo testInfo
    ) {
        validCases++;
        Product product = buildProduct(
            title,
            keywords,
            description,
            ratingStr,
            priceStr,
            quantityStr,
            statusStr,
            weightStr,
            dimensions,
            dateAddedStr,
            dateModifiedStr
        );
        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        boolean ok = violations.isEmpty();
        if (ok) {
            passed.add(testInfo.getDisplayName());
        } else {
            failed.add(testInfo.getDisplayName());
        }
        // Record compact row for report (inputs + result)
        tableRows.add(
            "| " +
            cell(title) +
            " | " +
            cell(keywords) +
            " | " +
            cell(description) +
            " | " +
            cell(ratingStr) +
            " | " +
            cell(priceStr) +
            " | " +
            cell(quantityStr) +
            " | " +
            cell(statusStr) +
            " | " +
            cell(weightStr) +
            " | " +
            cell(dimensions) +
            " | " +
            cell(dateAddedStr) +
            " | " +
            cell(dateModifiedStr) +
            " | " +
            (ok ? "PASS" : "FAIL") +
            " |"
        );
        assertTrue(ok, "violations: " + violations);
    }

    @ParameterizedTest(name = "Invalid combination #{index} - title='{0}'")
    @CsvFileSource(resources = "/pict/invalid_test_cases.txt", delimiter = '\t', numLinesToSkip = 1)
    void invalidCombinationsFromPict(
        String title,
        String keywords,
        String description,
        String ratingStr,
        String priceStr,
        String quantityStr,
        String statusStr,
        String weightStr,
        String dimensions,
        String dateAddedStr,
        String dateModifiedStr,
        TestInfo testInfo
    ) {
        invalidCases++;
        Product product = buildProduct(
            title,
            keywords,
            description,
            ratingStr,
            priceStr,
            quantityStr,
            statusStr,
            weightStr,
            dimensions,
            dateAddedStr,
            dateModifiedStr
        );
        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        boolean ok = violations.isEmpty();

        if (ok) {
            passed.add(testInfo.getDisplayName());
        } else {
            failed.add(testInfo.getDisplayName());
        }
        // Record compact row for report (inputs + result)
        tableRows.add(
            "| " +
            cell(title) +
            " | " +
            cell(keywords) +
            " | " +
            cell(description) +
            " | " +
            cell(ratingStr) +
            " | " +
            cell(priceStr) +
            " | " +
            cell(quantityStr) +
            " | " +
            cell(statusStr) +
            " | " +
            cell(weightStr) +
            " | " +
            cell(dimensions) +
            " | " +
            cell(dateAddedStr) +
            " | " +
            cell(dateModifiedStr) +
            " | " +
            (ok ? "PASS" : "FAIL") +
            " |"
        );
        assertTrue(ok, "violations: " + violations);
    }

    @AfterAll
    public static void ensureDataLoaded() {
        assertTrue(validCases > 0, "No rows loaded from /pict/valid_test_cases.txt; parameterized test did not run.");
        assertTrue(invalidCases > 0, "No rows loaded from /pict/invalid_test_cases.txt; parameterized test did not run.");

        // Generate report document: compact Markdown table only
        try {
            Path outDir = Paths.get("target", "test-reports");
            Files.createDirectories(outDir);
            Path report = outDir.resolve("product-combinations-report.md");

            StringBuilder sb = new StringBuilder();
            sb.append(
                "| title | keywords | description | rating | price | quantity | status | weight | dimensions | dateAdded | dateModified | result |\n"
            );
            sb.append("|-|-|-|-|-|-|-|-|-|-|-|-|\n");
            for (String row : tableRows) {
                sb.append(row).append('\n');
            }

            Files.write(report, sb.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Relatório gerado em: " + report.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Falha ao gerar relatório: " + e.getMessage());
        }
    }
}
