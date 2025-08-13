package com.company.invoice.ocr.service.parsing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive pattern library for extracting invoice data using regex patterns.
 * Contains optimized patterns for invoice numbers, amounts, dates, and vendor information.
 */
@Component
@Slf4j
public class InvoicePatternLibrary {

    // Invoice Number Patterns
    private static final List<Pattern> INVOICE_NUMBER_PATTERNS = Arrays.asList(
        // Standard format: INV-2024-001, INVOICE-001234
        Pattern.compile("(?i)(?:invoice|inv)\\s*[-#:]?\\s*([A-Z0-9]{3,}-?[0-9]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: Invoice Number: 123456 (with colon)
        Pattern.compile("(?i)invoice\\s+number\\s*:?\\s*([A-Z0-9-]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: Invoice Number INV-3337 (WITHOUT colon - for tabular PDFs)
        Pattern.compile("(?i)invoice\\s+number\\s+([A-Z0-9-]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: Order Number 12345 (for order numbers)
        Pattern.compile("(?i)order\\s+number\\s+([A-Z0-9-]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: Bill No: 789012
        Pattern.compile("(?i)bill\\s+no\\.?\\s*:?\\s*([A-Z0-9-]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: Reference: REF123456
        Pattern.compile("(?i)reference\\s*:?\\s*([A-Z0-9-]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: Document #: DOC-001
        Pattern.compile("(?i)document\\s*#\\s*:?\\s*([A-Z0-9-]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: #123456 (standalone number with hash)
        Pattern.compile("#([0-9A-Z-]{4,})")
    );

    // Currency symbols and codes
    private static final String CURRENCY_SYMBOLS = "\\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR";
    
    // Amount patterns
    private static final List<Pattern> AMOUNT_PATTERNS = Arrays.asList(
        // Format: $1,234.56 or USD 1,234.56
        Pattern.compile("(?:" + CURRENCY_SYMBOLS + ")\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE),
        
        // Format: 1,234.56 USD or 1,234.56 $
        Pattern.compile("([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)\\s*(?:" + CURRENCY_SYMBOLS + ")", Pattern.CASE_INSENSITIVE),
        
        // Format: Total Due $93.50 (highest priority - most specific match first)
        Pattern.compile("(?i)total\\s+due\\s+(?:" + CURRENCY_SYMBOLS + ")\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE),
        
        // Format: Total $93.50 (second priority - matches "Total $93.50" at end of invoice)
        Pattern.compile("(?i)^total\\s+(?:" + CURRENCY_SYMBOLS + ")\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)\\s*$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
        
        // Format: Total: 1234.56 (with colon, no currency symbol)
        Pattern.compile("(?i)(?:total|amount|sum)\\s*:?\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE),
        
        // Format: Total/Amount/Sum + space + currency (general case - lower priority)
        Pattern.compile("(?i)(?:total|amount|sum)\\s+(?:" + CURRENCY_SYMBOLS + ")\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE),
        
        // Format: Sub Total $85.00 (for subtotals)
        Pattern.compile("(?i)sub\\s+total\\s+(?:" + CURRENCY_SYMBOLS + ")\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE),
        
        // Format: Tax $8.50 (for tax amounts)
        Pattern.compile("(?i)tax\\s+(?:" + CURRENCY_SYMBOLS + ")\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE)
    );

    // Date format patterns
    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("MMM dd, yyyy"),
        DateTimeFormatter.ofPattern("MMMM dd, yyyy"),
        DateTimeFormatter.ofPattern("dd MMM yyyy"),
        DateTimeFormatter.ofPattern("dd MMMM yyyy"),
        DateTimeFormatter.ofPattern("MMM d, yyyy"),
        DateTimeFormatter.ofPattern("d MMM yyyy")
    );
    
    private static final List<Pattern> DATE_PATTERNS = Arrays.asList(
        // MM/DD/YYYY or DD/MM/YYYY
        Pattern.compile("(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{4})"),
        
        // YYYY-MM-DD
        Pattern.compile("(\\d{4}[/\\-]\\d{1,2}[/\\-]\\d{1,2})"),
        
        // Month DD, YYYY
        Pattern.compile("([A-Za-z]{3,9}\\s+\\d{1,2},?\\s+\\d{4})"),
        
        // DD Month YYYY
        Pattern.compile("(\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{4})")
    );

    // Vendor keywords
    private static final List<String> VENDOR_KEYWORDS = Arrays.asList(
        "from:", "vendor:", "supplier:", "bill from:", "sold by:", "remit to:", "company:", "organization:",
        "from" // Also match "From" without colon as it appears in your invoice
    );

    // Address pattern
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "([A-Za-z0-9\\s,.-]+)\\s*,\\s*([A-Za-z\\s]+),?\\s*([A-Z]{2})\\s+([0-9]{5}(?:-[0-9]{4})?)"
    );

    // Email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
    );

    // Phone pattern (for future use)
    @SuppressWarnings("unused")
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(?:\\+?1[-. ]?)?\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})"
    );

    /**
     * Extract invoice number from text
     */
    public ExtractionResult extractInvoiceNumber(String text) {
        List<String> lines = getTextLines(text);
        
        for (int i = 0; i < Math.min(lines.size(), 15); i++) { // Check first 15 lines
            String line = lines.get(i);
            
            for (Pattern pattern : INVOICE_NUMBER_PATTERNS) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String invoiceNumber = matcher.group(1).trim();
                    double confidence = calculateInvoiceNumberConfidence(invoiceNumber, line, i);
                    
                    if (confidence >= 0.5) { // Minimum confidence threshold
                        return ExtractionResult.builder()
                            .fieldName("invoice_number")
                            .value(invoiceNumber)
                            .confidence(confidence)
                            .pattern(pattern.pattern())
                            .sourceText(line)
                            .lineNumber(i + 1)
                            .extractionMethod("regex_pattern")
                            .build();
                    }
                }
            }
        }
        
        return ExtractionResult.notFound("invoice_number");
    }

    /**
     * Extract total amount from text
     */
    public ExtractionResult extractTotalAmount(String text) {
        return extractAmountByKeywords(text, Arrays.asList("total", "amount due", "balance due", "grand total", "final amount"));
    }

    /**
     * Extract subtotal amount from text
     */
    public ExtractionResult extractSubtotalAmount(String text) {
        return extractAmountByKeywords(text, Arrays.asList("subtotal", "sub total", "sub-total", "net amount"));
    }

    /**
     * Extract tax amount from text
     */
    public ExtractionResult extractTaxAmount(String text) {
        return extractAmountByKeywords(text, Arrays.asList("tax", "vat", "gst", "sales tax", "tax amount"));
    }

    /**
     * Extract invoice date
     */
    public ExtractionResult extractInvoiceDate(String text) {
        return extractDateByKeywords(text, Arrays.asList("invoice date", "date", "bill date", "issued", "date issued"));
    }

    /**
     * Extract due date
     */
    public ExtractionResult extractDueDate(String text) {
        return extractDateByKeywords(text, Arrays.asList("due date", "payment due", "due", "pay by", "payment date"));
    }

    /**
     * Extract vendor name
     */
    public ExtractionResult extractVendorName(String text) {
        List<String> lines = getTextLines(text);
        
        // Strategy 1: Look for vendor keywords
        for (String keyword : VENDOR_KEYWORDS) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                
                if (line.toLowerCase().contains(keyword.toLowerCase())) {
                    // Vendor name might be on same line or next line
                    String vendorLine = line.replaceFirst("(?i)" + keyword, "").trim();
                    
                    if (vendorLine.length() > 3 && isValidVendorName(vendorLine)) {
                        return ExtractionResult.builder()
                            .fieldName("vendor_name")
                            .value(vendorLine)
                            .confidence(0.8)
                            .sourceText(line)
                            .lineNumber(i + 1)
                            .context(keyword)
                            .extractionMethod("keyword_context")
                            .build();
                    }
                    
                    // Check next line
                    if (i + 1 < lines.size()) {
                        String nextLine = lines.get(i + 1).trim();
                        if (nextLine.length() > 3 && isValidVendorName(nextLine)) {
                            return ExtractionResult.builder()
                                .fieldName("vendor_name")
                                .value(nextLine)
                                .confidence(0.75)
                                .sourceText(nextLine)
                                .lineNumber(i + 2)
                                .context(keyword)
                                .extractionMethod("keyword_context")
                                .build();
                        }
                    }
                }
            }
        }
        
        // Strategy 2: Assume vendor name is in first few lines (header area)
        for (int i = 0; i < Math.min(lines.size(), 8); i++) {
            String line = lines.get(i).trim();
            
            // Skip lines that look like headers, numbers, or addresses
            if (line.toLowerCase().contains("invoice") || 
                line.matches("^[0-9\\s,.-]+$") ||
                ADDRESS_PATTERN.matcher(line).find() ||
                line.length() < 3 || line.length() > 100) {
                continue;
            }
            
            if (isValidVendorName(line)) {
                return ExtractionResult.builder()
                    .fieldName("vendor_name")
                    .value(line)
                    .confidence(0.6)
                    .sourceText(line)
                    .lineNumber(i + 1)
                    .context("header_extraction")
                    .extractionMethod("position_based")
                    .build();
            }
        }
        
        return ExtractionResult.notFound("vendor_name");
    }

    /**
     * Extract vendor address
     */
    public ExtractionResult extractVendorAddress(String text) {
        List<String> lines = getTextLines(text);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = ADDRESS_PATTERN.matcher(line);
            
            if (matcher.find()) {
                String fullAddress = matcher.group(0);
                return ExtractionResult.builder()
                    .fieldName("vendor_address")
                    .value(fullAddress)
                    .confidence(0.9)
                    .sourceText(line)
                    .lineNumber(i + 1)
                    .pattern(ADDRESS_PATTERN.pattern())
                    .extractionMethod("regex_pattern")
                    .build();
            }
        }
        
        return ExtractionResult.notFound("vendor_address");
    }

    /**
     * Extract vendor email
     */
    public ExtractionResult extractVendorEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        
        if (matcher.find()) {
            String email = matcher.group(1);
            return ExtractionResult.builder()
                .fieldName("vendor_email")
                .value(email)
                .confidence(0.95)
                .pattern(EMAIL_PATTERN.pattern())
                .extractionMethod("regex_pattern")
                .build();
        }
        
        return ExtractionResult.notFound("vendor_email");
    }

    // Helper methods

    /**
     * Extract amount by specific keywords
     */
    private ExtractionResult extractAmountByKeywords(String text, List<String> keywords) {
        List<String> lines = getTextLines(text);
        
        for (String keyword : keywords) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                
                if (line.toLowerCase().contains(keyword.toLowerCase())) {
                    // Search current line and next 2 lines
                    for (int j = i; j < Math.min(i + 3, lines.size()); j++) {
                        String searchLine = lines.get(j);
                        
                        for (Pattern pattern : AMOUNT_PATTERNS) {
                            Matcher matcher = pattern.matcher(searchLine);
                            while (matcher.find()) {
                                String amountStr = matcher.group(1).replaceAll(",", "");
                                try {
                                    BigDecimal amount = new BigDecimal(amountStr);
                                    double confidence = calculateAmountConfidence(amount, keyword, searchLine);
                                    
                                    if (confidence >= 0.5) {
                                        return ExtractionResult.builder()
                                            .fieldName("amount")
                                            .value(amount.toString())
                                            .confidence(confidence)
                                            .pattern(pattern.pattern())
                                            .sourceText(searchLine)
                                            .lineNumber(j + 1)
                                            .context(keyword)
                                            .extractionMethod("keyword_amount")
                                            .build();
                                    }
                                } catch (NumberFormatException e) {
                                    log.debug("Invalid amount format: {}", amountStr);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return ExtractionResult.notFound("amount");
    }

    /**
     * Extract date by specific keywords
     */
    private ExtractionResult extractDateByKeywords(String text, List<String> keywords) {
        List<String> lines = getTextLines(text);
        
        for (String keyword : keywords) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                
                if (line.toLowerCase().contains(keyword.toLowerCase())) {
                    // Search current line and next line
                    for (int j = i; j < Math.min(i + 2, lines.size()); j++) {
                        String searchLine = lines.get(j);
                        
                        for (Pattern pattern : DATE_PATTERNS) {
                            Matcher matcher = pattern.matcher(searchLine);
                            if (matcher.find()) {
                                String dateStr = matcher.group(1);
                                LocalDate date = parseDate(dateStr);
                                
                                if (date != null) {
                                    double confidence = calculateDateConfidence(date, keyword, searchLine);
                                    
                                    if (confidence >= 0.5) {
                                        return ExtractionResult.builder()
                                            .fieldName("date")
                                            .value(date.toString())
                                            .confidence(confidence)
                                            .pattern(pattern.pattern())
                                            .sourceText(searchLine)
                                            .lineNumber(j + 1)
                                            .context(keyword)
                                            .extractionMethod("keyword_date")
                                            .build();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return ExtractionResult.notFound("date");
    }

    /**
     * Parse date string using multiple formats
     */
    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        return null;
    }

    /**
     * Calculate confidence for invoice number
     */
    private double calculateInvoiceNumberConfidence(String invoiceNumber, String sourceLine, int lineIndex) {
        double confidence = 0.5; // Base confidence
        
        // Length-based confidence
        if (invoiceNumber.length() >= 6 && invoiceNumber.length() <= 20) {
            confidence += 0.2;
        }
        
        // Format-based confidence
        if (invoiceNumber.matches("^[A-Z]{2,4}-[0-9]{4,6}$")) {
            confidence += 0.2; // Standard format
        } else if (invoiceNumber.matches("^[0-9]{6,10}$")) {
            confidence += 0.15; // Numeric format
        }
        
        // Position-based confidence (near top of document)
        if (lineIndex < 5) {
            confidence += 0.1;
        }
        
        // Context-based confidence
        if (sourceLine.toLowerCase().contains("invoice")) {
            confidence += 0.1;
        }
        
        return Math.min(1.0, confidence);
    }

    /**
     * Calculate confidence for amounts
     */
    private double calculateAmountConfidence(BigDecimal amount, String keyword, String sourceLine) {
        double confidence = 0.6; // Base confidence
        
        // Amount reasonableness
        if (amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(new BigDecimal("1000000")) < 0) {
            confidence += 0.2;
        }
        
        // Keyword specificity
        if (keyword.equals("total") || keyword.equals("amount due")) {
            confidence += 0.2;
        } else if (keyword.equals("subtotal") || keyword.equals("tax")) {
            confidence += 0.1;
        }
        
        return Math.min(1.0, confidence);
    }

    /**
     * Calculate confidence for dates
     */
    private double calculateDateConfidence(LocalDate date, String keyword, String sourceLine) {
        double confidence = 0.7; // Base confidence
        
        // Date reasonableness (within last 2 years to next 1 year)
        LocalDate now = LocalDate.now();
        if (date.isAfter(now.minusYears(2)) && date.isBefore(now.plusYears(1))) {
            confidence += 0.2;
        }
        
        // Keyword specificity
        if (keyword.contains("invoice") || keyword.contains("due")) {
            confidence += 0.1;
        }
        
        return Math.min(1.0, confidence);
    }

    /**
     * Validate vendor name
     */
    private boolean isValidVendorName(String name) {
        if (name == null || name.trim().length() < 3) {
            return false;
        }
        
        // Should not be just numbers or common headers
        if (name.matches("^[0-9\\s,.-]+$") ||
            name.toLowerCase().matches(".*(?:invoice|bill|total|amount|page|date).*")) {
            return false;
        }
        
        return true;
    }

    /**
     * Split text into lines and filter empty ones
     */
    private List<String> getTextLines(String text) {
        if (text == null) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(text.split("\\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
    }

    // Result classes

    @lombok.Data
    @lombok.Builder
    public static class ExtractionResult {
        private String fieldName;
        private String value;
        private double confidence;
        private String pattern;
        private String sourceText;
        private int lineNumber;
        private String context;
        private String extractionMethod;
        private boolean found;

        public static ExtractionResult notFound(String fieldName) {
            return ExtractionResult.builder()
                    .fieldName(fieldName)
                    .found(false)
                    .confidence(0.0)
                    .build();
        }

        public boolean isReliable() {
            return found && confidence >= 0.7;
        }

        public boolean isUsable() {
            return found && confidence >= 0.5;
        }
    }
}
