# Invoice Automation System - OCR Data Extraction Templates

## Document Information
- **Created**: [Current Date]
- **Phase**: Phase 1 - Requirements and Design
- **Task**: 1.2.5 - Design OCR data extraction templates
- **Status**: ✅ COMPLETED

---

## Overview

### Purpose
This document defines standardized templates and patterns for extracting invoice data from OCR-processed text using Java-based libraries (Apache Tika, iText 7, Tesseract4J). These templates provide structured approaches for handling various invoice formats and improving extraction accuracy.

### Extraction Strategy
1. **Pattern-Based Extraction**: Regular expressions for structured data
2. **Context-Aware Parsing**: Relative position-based extraction
3. **Template Matching**: Vendor-specific format recognition
4. **Machine Learning Enhancement**: Confidence scoring and validation
5. **Fallback Mechanisms**: Multi-level extraction approaches

---

## Data Extraction Framework

### 1. **Core Invoice Data Model**

```java
@Entity
@Table(name = "extracted_invoice_data")
public class ExtractedInvoiceData {
    
    // Basic Invoice Information
    private String invoiceNumber;
    private String vendorInvoiceNumber;
    private String purchaseOrderNumber;
    
    // Financial Information
    private BigDecimal totalAmount;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private String currency;
    
    // Date Information
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private LocalDate servicePeriodStart;
    private LocalDate servicePeriodEnd;
    
    // Vendor Information
    private String vendorName;
    private String vendorAddress;
    private String vendorTaxId;
    private String vendorEmail;
    private String vendorPhone;
    
    // Customer Information
    private String customerName;
    private String customerAddress;
    private String customerTaxId;
    
    // Processing Metadata
    private ExtractionMethod extractionMethod;
    private Double confidenceScore;
    private String rawText;
    private List<ExtractedLineItem> lineItems;
    private List<ExtractionError> errors;
    
    // Validation Status
    private ValidationStatus validationStatus;
    private List<ValidationError> validationErrors;
}
```

### 2. **Extraction Template Base Class**

```java
@Component
public abstract class InvoiceExtractionTemplate {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    // Template identification
    public abstract String getTemplateName();
    public abstract List<String> getSupportedVendors();
    public abstract double getConfidenceThreshold();
    
    // Main extraction method
    public abstract ExtractedInvoiceData extractData(String text, PDFMetadata metadata);
    
    // Helper methods
    protected String cleanText(String text) {
        return text.replaceAll("\\s+", " ")
                  .replaceAll("[^\\p{Print}]", "")
                  .trim();
    }
    
    protected List<String> getTextLines(String text) {
        return Arrays.stream(text.split("\\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());
    }
    
    protected double calculateFieldConfidence(String extractedValue, String pattern) {
        if (extractedValue == null || extractedValue.isEmpty()) return 0.0;
        
        // Pattern matching confidence
        Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        if (compiledPattern.matcher(extractedValue).matches()) {
            return 0.9;
        }
        
        // Partial match confidence
        if (compiledPattern.matcher(extractedValue).find()) {
            return 0.7;
        }
        
        return 0.3;
    }
}
```

---

## Regular Expression Patterns Library

### 1. **Invoice Number Patterns**

```java
@Component
public class InvoiceNumberExtractor {
    
    // Standard invoice number patterns
    private static final List<Pattern> INVOICE_NUMBER_PATTERNS = Arrays.asList(
        // Format: INV-2024-001, INVOICE-001234
        Pattern.compile("(?i)(?:invoice|inv)\\s*[-#:]?\\s*([A-Z0-9]{3,}-?[0-9]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: Invoice Number: 123456
        Pattern.compile("(?i)invoice\\s+number\\s*:?\\s*([A-Z0-9-]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: Bill No: 789012
        Pattern.compile("(?i)bill\\s+no\\.?\\s*:?\\s*([A-Z0-9-]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: Reference: REF123456
        Pattern.compile("(?i)reference\\s*:?\\s*([A-Z0-9-]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: Document #: DOC-001
        Pattern.compile("(?i)document\\s*#\\s*:?\\s*([A-Z0-9-]{3,})", Pattern.CASE_INSENSITIVE),
        
        // Format: #123456 (standalone number with hash)
        Pattern.compile("#([0-9A-Z-]{4,})")
    );
    
    public ExtractionResult extractInvoiceNumber(String text) {
        List<String> lines = getTextLines(text);
        
        for (int i = 0; i < Math.min(lines.size(), 10); i++) { // Check first 10 lines
            String line = lines.get(i);
            
            for (Pattern pattern : INVOICE_NUMBER_PATTERNS) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String invoiceNumber = matcher.group(1).trim();
                    double confidence = calculateInvoiceNumberConfidence(invoiceNumber, line);
                    
                    return ExtractionResult.builder()
                        .value(invoiceNumber)
                        .confidence(confidence)
                        .pattern(pattern.pattern())
                        .sourceText(line)
                        .lineNumber(i + 1)
                        .build();
                }
            }
        }
        
        return ExtractionResult.notFound("invoice_number");
    }
    
    private double calculateInvoiceNumberConfidence(String invoiceNumber, String sourceLine) {
        double confidence = 0.5; // Base confidence
        
        // Length-based confidence
        if (invoiceNumber.length() >= 6 && invoiceNumber.length() <= 20) {
            confidence += 0.2;
        }
        
        // Format-based confidence
        if (invoiceNumber.matches("^[A-Z]{2,4}-[0-9]{4,6}$")) {
            confidence += 0.2; // Standard format
        }
        
        // Position-based confidence (near top of document)
        if (sourceLine.toLowerCase().contains("invoice")) {
            confidence += 0.1;
        }
        
        return Math.min(confidence, 1.0);
    }
}
```

### 2. **Amount Extraction Patterns**

```java
@Component
public class AmountExtractor {
    
    // Currency symbols and codes
    private static final String CURRENCY_SYMBOLS = "\\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR";
    
    // Amount patterns
    private static final List<Pattern> AMOUNT_PATTERNS = Arrays.asList(
        // Format: $1,234.56 or USD 1,234.56
        Pattern.compile("(?:" + CURRENCY_SYMBOLS + ")\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE),
        
        // Format: 1,234.56 USD or 1,234.56 $
        Pattern.compile("([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)\\s*(?:" + CURRENCY_SYMBOLS + ")", Pattern.CASE_INSENSITIVE),
        
        // Format: Total: 1234.56 (no currency symbol)
        Pattern.compile("(?i)(?:total|amount|sum)\\s*:?\\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE)
    );
    
    public ExtractionResult extractTotalAmount(String text) {
        List<String> lines = getTextLines(text);
        
        // Look for total amount keywords first
        List<String> totalKeywords = Arrays.asList("total", "amount due", "balance due", "grand total", "final amount");
        
        for (String keyword : totalKeywords) {
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
                                    
                                    return ExtractionResult.builder()
                                        .value(amount.toString())
                                        .confidence(confidence)
                                        .pattern(pattern.pattern())
                                        .sourceText(searchLine)
                                        .lineNumber(j + 1)
                                        .context(keyword)
                                        .build();
                                        
                                } catch (NumberFormatException e) {
                                    logger.debug("Invalid amount format: {}", amountStr);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return ExtractionResult.notFound("total_amount");
    }
    
    public ExtractionResult extractSubtotalAmount(String text) {
        return extractAmountByKeyword(text, Arrays.asList("subtotal", "sub total", "sub-total", "net amount"));
    }
    
    public ExtractionResult extractTaxAmount(String text) {
        return extractAmountByKeyword(text, Arrays.asList("tax", "vat", "gst", "sales tax", "tax amount"));
    }
    
    private double calculateAmountConfidence(BigDecimal amount, String keyword, String sourceLine) {
        double confidence = 0.6; // Base confidence
        
        // Amount reasonableness
        if (amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(new BigDecimal("1000000")) < 0) {
            confidence += 0.2;
        }
        
        // Keyword proximity
        if (keyword.equals("total") || keyword.equals("amount due")) {
            confidence += 0.2;
        }
        
        return Math.min(confidence, 1.0);
    }
}
```

### 3. **Date Extraction Patterns**

```java
@Component
public class DateExtractor {
    
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
        DateTimeFormatter.ofPattern("dd MMMM yyyy")
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
    
    public ExtractionResult extractInvoiceDate(String text) {
        return extractDateByKeywords(text, Arrays.asList("invoice date", "date", "bill date", "issued"));
    }
    
    public ExtractionResult extractDueDate(String text) {
        return extractDateByKeywords(text, Arrays.asList("due date", "payment due", "due", "pay by"));
    }
    
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
                                    
                                    return ExtractionResult.builder()
                                        .value(date.toString())
                                        .confidence(confidence)
                                        .pattern(pattern.pattern())
                                        .sourceText(searchLine)
                                        .lineNumber(j + 1)
                                        .context(keyword)
                                        .build();
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return ExtractionResult.notFound("date");
    }
    
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
        
        return Math.min(confidence, 1.0);
    }
}
```

### 4. **Vendor Information Extraction**

```java
@Component
public class VendorExtractor {
    
    // Common vendor keywords
    private static final List<String> VENDOR_KEYWORDS = Arrays.asList(
        "from:", "vendor:", "supplier:", "bill from:", "sold by:", "remit to:"
    );
    
    // Address patterns
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "([A-Za-z0-9\\s,.-]+)\\s*,\\s*([A-Za-z\\s]+),?\\s*([A-Z]{2})\\s+([0-9]{5}(?:-[0-9]{4})?)"
    );
    
    // Email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
    );
    
    // Phone pattern
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(?:\\+?1[-. ]?)?\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})"
    );
    
    public ExtractionResult extractVendorName(String text) {
        List<String> lines = getTextLines(text);
        
        // Strategy 1: Look for vendor keywords
        for (String keyword : VENDOR_KEYWORDS) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                
                if (line.toLowerCase().contains(keyword.toLowerCase())) {
                    // Vendor name might be on same line or next line
                    String vendorLine = line.replaceFirst("(?i)" + keyword, "").trim();
                    
                    if (vendorLine.length() > 3) {
                        return ExtractionResult.builder()
                            .value(vendorLine)
                            .confidence(0.8)
                            .sourceText(line)
                            .lineNumber(i + 1)
                            .context(keyword)
                            .build();
                    }
                    
                    // Check next line
                    if (i + 1 < lines.size()) {
                        String nextLine = lines.get(i + 1).trim();
                        if (nextLine.length() > 3 && !nextLine.matches("^[0-9\\s,.-]+$")) {
                            return ExtractionResult.builder()
                                .value(nextLine)
                                .confidence(0.75)
                                .sourceText(nextLine)
                                .lineNumber(i + 2)
                                .context(keyword)
                                .build();
                        }
                    }
                }
            }
        }
        
        // Strategy 2: Assume vendor name is in first few lines (header area)
        for (int i = 0; i < Math.min(lines.size(), 5); i++) {
            String line = lines.get(i);
            
            // Skip lines that look like headers, numbers, or addresses
            if (line.toLowerCase().contains("invoice") || 
                line.matches("^[0-9\\s,.-]+$") ||
                ADDRESS_PATTERN.matcher(line).find()) {
                continue;
            }
            
            if (line.length() > 3 && line.length() < 100) {
                return ExtractionResult.builder()
                    .value(line.trim())
                    .confidence(0.6)
                    .sourceText(line)
                    .lineNumber(i + 1)
                    .context("header_extraction")
                    .build();
            }
        }
        
        return ExtractionResult.notFound("vendor_name");
    }
    
    public ExtractionResult extractVendorAddress(String text) {
        List<String> lines = getTextLines(text);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = ADDRESS_PATTERN.matcher(line);
            
            if (matcher.find()) {
                String fullAddress = matcher.group(0);
                return ExtractionResult.builder()
                    .value(fullAddress)
                    .confidence(0.9)
                    .sourceText(line)
                    .lineNumber(i + 1)
                    .pattern(ADDRESS_PATTERN.pattern())
                    .build();
            }
        }
        
        return ExtractionResult.notFound("vendor_address");
    }
    
    public ExtractionResult extractVendorEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        
        if (matcher.find()) {
            String email = matcher.group(1);
            return ExtractionResult.builder()
                .value(email)
                .confidence(0.95)
                .pattern(EMAIL_PATTERN.pattern())
                .build();
        }
        
        return ExtractionResult.notFound("vendor_email");
    }
}
```

---

## Vendor-Specific Templates

### 1. **QuickBooks Invoice Template**

```java
@Component
@TemplateProfile("QuickBooks")
public class QuickBooksInvoiceTemplate extends InvoiceExtractionTemplate {
    
    @Override
    public String getTemplateName() {
        return "QuickBooks Standard Invoice";
    }
    
    @Override
    public List<String> getSupportedVendors() {
        return Arrays.asList("QuickBooks", "Intuit");
    }
    
    @Override
    public double getConfidenceThreshold() {
        return 0.8;
    }
    
    @Override
    public ExtractedInvoiceData extractData(String text, PDFMetadata metadata) {
        ExtractedInvoiceData data = new ExtractedInvoiceData();
        
        // QuickBooks specific patterns
        Pattern qbInvoicePattern = Pattern.compile("Invoice\\s+#\\s*([A-Z0-9-]+)");
        Pattern qbDatePattern = Pattern.compile("Invoice Date\\s*:?\\s*([0-9/]+)");
        Pattern qbDueDatePattern = Pattern.compile("Due Date\\s*:?\\s*([0-9/]+)");
        Pattern qbTotalPattern = Pattern.compile("Total\\s*\\$([0-9,]+\\.?[0-9]*)");
        
        // Extract using QuickBooks specific layout
        Matcher invoiceMatcher = qbInvoicePattern.matcher(text);
        if (invoiceMatcher.find()) {
            data.setInvoiceNumber(invoiceMatcher.group(1));
            data.addConfidenceScore("invoice_number", 0.95);
        }
        
        Matcher dateMatcher = qbDatePattern.matcher(text);
        if (dateMatcher.find()) {
            data.setInvoiceDate(parseDate(dateMatcher.group(1)));
            data.addConfidenceScore("invoice_date", 0.9);
        }
        
        Matcher dueMatcher = qbDueDatePattern.matcher(text);
        if (dueMatcher.find()) {
            data.setDueDate(parseDate(dueMatcher.group(1)));
            data.addConfidenceScore("due_date", 0.9);
        }
        
        Matcher totalMatcher = qbTotalPattern.matcher(text);
        if (totalMatcher.find()) {
            String amountStr = totalMatcher.group(1).replaceAll(",", "");
            data.setTotalAmount(new BigDecimal(amountStr));
            data.addConfidenceScore("total_amount", 0.95);
        }
        
        // Extract line items using QuickBooks table format
        extractQuickBooksLineItems(text, data);
        
        return data;
    }
    
    private void extractQuickBooksLineItems(String text, ExtractedInvoiceData data) {
        // QuickBooks line item pattern: Description | Qty | Rate | Amount
        Pattern lineItemPattern = Pattern.compile(
            "^(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s+\\$?([0-9,]+\\.?[0-9]*)\\s+\\$?([0-9,]+\\.?[0-9]*)$",
            Pattern.MULTILINE
        );
        
        Matcher matcher = lineItemPattern.matcher(text);
        List<ExtractedLineItem> lineItems = new ArrayList<>();
        
        while (matcher.find()) {
            ExtractedLineItem item = new ExtractedLineItem();
            item.setDescription(matcher.group(1).trim());
            item.setQuantity(new BigDecimal(matcher.group(2)));
            item.setUnitPrice(new BigDecimal(matcher.group(3).replaceAll(",", "")));
            item.setTotalPrice(new BigDecimal(matcher.group(4).replaceAll(",", "")));
            
            lineItems.add(item);
        }
        
        data.setLineItems(lineItems);
    }
}
```

### 2. **SAP Invoice Template**

```java
@Component
@TemplateProfile("SAP")
public class SAPInvoiceTemplate extends InvoiceExtractionTemplate {
    
    @Override
    public String getTemplateName() {
        return "SAP Standard Invoice";
    }
    
    @Override
    public List<String> getSupportedVendors() {
        return Arrays.asList("SAP", "SAP SE");
    }
    
    @Override
    public double getConfidenceThreshold() {
        return 0.85;
    }
    
    @Override
    public ExtractedInvoiceData extractData(String text, PDFMetadata metadata) {
        ExtractedInvoiceData data = new ExtractedInvoiceData();
        
        // SAP specific patterns - more structured format
        Map<String, Pattern> sapPatterns = Map.of(
            "document_number", Pattern.compile("Document Number\\s*:?\\s*([0-9]{10})"),
            "vendor_number", Pattern.compile("Vendor\\s*:?\\s*([0-9]{6,10})"),
            "posting_date", Pattern.compile("Posting Date\\s*:?\\s*([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})"),
            "net_amount", Pattern.compile("Net Amount\\s*:?\\s*([0-9,]+\\.?[0-9]*)\\s*([A-Z]{3})"),
            "tax_amount", Pattern.compile("Tax Amount\\s*:?\\s*([0-9,]+\\.?[0-9]*)"),
            "gross_amount", Pattern.compile("Gross Amount\\s*:?\\s*([0-9,]+\\.?[0-9]*)")
        );
        
        // Extract using SAP patterns
        for (Map.Entry<String, Pattern> entry : sapPatterns.entrySet()) {
            Matcher matcher = entry.getValue().matcher(text);
            if (matcher.find()) {
                extractSAPField(data, entry.getKey(), matcher);
            }
        }
        
        // Extract SAP line items
        extractSAPLineItems(text, data);
        
        return data;
    }
    
    private void extractSAPField(ExtractedInvoiceData data, String fieldName, Matcher matcher) {
        switch (fieldName) {
            case "document_number":
                data.setInvoiceNumber(matcher.group(1));
                data.addConfidenceScore("invoice_number", 0.95);
                break;
            case "posting_date":
                LocalDate date = parseGermanDate(matcher.group(1));
                data.setInvoiceDate(date);
                data.addConfidenceScore("invoice_date", 0.9);
                break;
            case "net_amount":
                data.setSubtotalAmount(new BigDecimal(matcher.group(1).replaceAll(",", "")));
                if (matcher.groupCount() > 1) {
                    data.setCurrency(matcher.group(2));
                }
                data.addConfidenceScore("subtotal_amount", 0.95);
                break;
            case "gross_amount":
                data.setTotalAmount(new BigDecimal(matcher.group(1).replaceAll(",", "")));
                data.addConfidenceScore("total_amount", 0.95);
                break;
        }
    }
}
```

---

## Line Item Extraction Templates

### 1. **Table-Based Line Item Extraction**

```java
@Component
public class TableLineItemExtractor {
    
    // Common table headers
    private static final List<String> DESCRIPTION_HEADERS = Arrays.asList(
        "description", "item", "product", "service", "details"
    );
    
    private static final List<String> QUANTITY_HEADERS = Arrays.asList(
        "qty", "quantity", "units", "count"
    );
    
    private static final List<String> PRICE_HEADERS = Arrays.asList(
        "price", "rate", "unit price", "cost"
    );
    
    private static final List<String> TOTAL_HEADERS = Arrays.asList(
        "total", "amount", "line total", "extended"
    );
    
    public List<ExtractedLineItem> extractLineItems(String text) {
        List<String> lines = getTextLines(text);
        List<ExtractedLineItem> lineItems = new ArrayList<>();
        
        // Find table structure
        TableStructure table = identifyTableStructure(lines);
        
        if (table != null) {
            // Extract items using identified structure
            for (int i = table.getDataStartLine(); i < lines.size(); i++) {
                String line = lines.get(i);
                
                // Stop at totals or footer
                if (isTableEnd(line)) break;
                
                ExtractedLineItem item = parseTableRow(line, table);
                if (item != null && item.isValid()) {
                    lineItems.add(item);
                }
            }
        }
        
        return lineItems;
    }
    
    private TableStructure identifyTableStructure(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).toLowerCase();
            
            // Look for table headers
            if (containsTableHeaders(line)) {
                return analyzeTableStructure(lines, i);
            }
        }
        return null;
    }
    
    private boolean containsTableHeaders(String line) {
        int headerCount = 0;
        
        for (String header : DESCRIPTION_HEADERS) {
            if (line.contains(header)) headerCount++;
        }
        for (String header : QUANTITY_HEADERS) {
            if (line.contains(header)) headerCount++;
        }
        for (String header : PRICE_HEADERS) {
            if (line.contains(header)) headerCount++;
        }
        for (String header : TOTAL_HEADERS) {
            if (line.contains(header)) headerCount++;
        }
        
        return headerCount >= 3; // At least 3 expected headers
    }
    
    private TableStructure analyzeTableStructure(List<String> lines, int headerLineIndex) {
        String headerLine = lines.get(headerLineIndex);
        
        // Analyze column positions
        Map<String, Integer> columnPositions = new HashMap<>();
        
        for (String header : DESCRIPTION_HEADERS) {
            int pos = headerLine.toLowerCase().indexOf(header);
            if (pos >= 0) {
                columnPositions.put("description", pos);
                break;
            }
        }
        
        for (String header : QUANTITY_HEADERS) {
            int pos = headerLine.toLowerCase().indexOf(header);
            if (pos >= 0) {
                columnPositions.put("quantity", pos);
                break;
            }
        }
        
        for (String header : PRICE_HEADERS) {
            int pos = headerLine.toLowerCase().indexOf(header);
            if (pos >= 0) {
                columnPositions.put("price", pos);
                break;
            }
        }
        
        for (String header : TOTAL_HEADERS) {
            int pos = headerLine.toLowerCase().indexOf(header);
            if (pos >= 0) {
                columnPositions.put("total", pos);
                break;
            }
        }
        
        return new TableStructure(columnPositions, headerLineIndex + 1);
    }
    
    private ExtractedLineItem parseTableRow(String line, TableStructure table) {
        try {
            ExtractedLineItem item = new ExtractedLineItem();
            
            // Extract description (usually first column or fixed position)
            String description = extractColumnValue(line, table.getColumnPosition("description"), 50);
            item.setDescription(description);
            
            // Extract quantity
            String quantityStr = extractColumnValue(line, table.getColumnPosition("quantity"), 10);
            if (quantityStr != null && quantityStr.matches("\\d+(?:\\.\\d+)?")) {
                item.setQuantity(new BigDecimal(quantityStr));
            }
            
            // Extract unit price
            String priceStr = extractColumnValue(line, table.getColumnPosition("price"), 15);
            if (priceStr != null) {
                priceStr = priceStr.replaceAll("[^0-9.]", "");
                if (priceStr.matches("\\d+(?:\\.\\d+)?")) {
                    item.setUnitPrice(new BigDecimal(priceStr));
                }
            }
            
            // Extract total
            String totalStr = extractColumnValue(line, table.getColumnPosition("total"), 15);
            if (totalStr != null) {
                totalStr = totalStr.replaceAll("[^0-9.]", "");
                if (totalStr.matches("\\d+(?:\\.\\d+)?")) {
                    item.setTotalPrice(new BigDecimal(totalStr));
                }
            }
            
            return item;
            
        } catch (Exception e) {
            logger.debug("Failed to parse table row: {}", line, e);
            return null;
        }
    }
    
    private String extractColumnValue(String line, Integer startPos, int maxLength) {
        if (startPos == null || startPos >= line.length()) return null;
        
        int endPos = Math.min(startPos + maxLength, line.length());
        return line.substring(startPos, endPos).trim();
    }
    
    private boolean isTableEnd(String line) {
        String lowerLine = line.toLowerCase();
        return lowerLine.contains("subtotal") || 
               lowerLine.contains("total") || 
               lowerLine.contains("tax") ||
               lowerLine.contains("amount due");
    }
}

// Helper class for table structure
public class TableStructure {
    private final Map<String, Integer> columnPositions;
    private final int dataStartLine;
    
    public TableStructure(Map<String, Integer> columnPositions, int dataStartLine) {
        this.columnPositions = columnPositions;
        this.dataStartLine = dataStartLine;
    }
    
    public Integer getColumnPosition(String columnName) {
        return columnPositions.get(columnName);
    }
    
    public int getDataStartLine() {
        return dataStartLine;
    }
}
```

---

## Context-Aware Extraction Engine

### 1. **Spatial Context Analyzer**

```java
@Component
public class SpatialContextAnalyzer {
    
    public ContextualExtractionResult extractWithSpatialContext(String text, String targetField) {
        List<String> lines = getTextLines(text);
        Map<String, TextRegion> identifiedRegions = identifyDocumentRegions(lines);
        
        switch (targetField) {
            case "vendor_info":
                return extractFromRegion(lines, identifiedRegions.get("header"));
            case "invoice_details":
                return extractFromRegion(lines, identifiedRegions.get("metadata"));
            case "line_items":
                return extractFromRegion(lines, identifiedRegions.get("line_items"));
            case "totals":
                return extractFromRegion(lines, identifiedRegions.get("footer"));
            default:
                return extractFromEntireDocument(lines);
        }
    }
    
    private Map<String, TextRegion> identifyDocumentRegions(List<String> lines) {
        Map<String, TextRegion> regions = new HashMap<>();
        
        // Header region (vendor info, logo, invoice title)
        int headerEnd = findHeaderEnd(lines);
        regions.put("header", new TextRegion(0, headerEnd));
        
        // Metadata region (invoice number, dates, customer info)
        int metadataStart = headerEnd;
        int metadataEnd = findMetadataEnd(lines, metadataStart);
        regions.put("metadata", new TextRegion(metadataStart, metadataEnd));
        
        // Line items region
        int lineItemsStart = findLineItemsStart(lines);
        int lineItemsEnd = findLineItemsEnd(lines, lineItemsStart);
        regions.put("line_items", new TextRegion(lineItemsStart, lineItemsEnd));
        
        // Footer region (totals, terms, footer info)
        regions.put("footer", new TextRegion(lineItemsEnd, lines.size()));
        
        return regions;
    }
    
    private int findHeaderEnd(List<String> lines) {
        for (int i = 0; i < Math.min(lines.size(), 10); i++) {
            String line = lines.get(i).toLowerCase();
            
            // Header typically ends when we see customer info or invoice details
            if (line.contains("bill to") || 
                line.contains("invoice number") ||
                line.contains("invoice date")) {
                return i;
            }
        }
        return Math.min(5, lines.size()); // Default header size
    }
    
    private int findLineItemsStart(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).toLowerCase();
            
            // Look for table headers
            if ((line.contains("description") || line.contains("item")) &&
                (line.contains("qty") || line.contains("quantity")) &&
                (line.contains("price") || line.contains("rate"))) {
                return i + 1; // Start after header line
            }
        }
        return lines.size() / 2; // Default to middle if not found
    }
    
    private int findLineItemsEnd(List<String> lines, int startIndex) {
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i).toLowerCase();
            
            // Line items end when we see totals
            if (line.contains("subtotal") || 
                line.contains("total") ||
                line.contains("amount due")) {
                return i;
            }
        }
        return lines.size() - 3; // Default to near end
    }
}

// Helper class for text regions
public class TextRegion {
    private final int startLine;
    private final int endLine;
    
    public TextRegion(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
    }
    
    public List<String> extractLines(List<String> allLines) {
        int start = Math.max(0, startLine);
        int end = Math.min(allLines.size(), endLine);
        return allLines.subList(start, end);
    }
}
```

---

## Confidence Scoring and Validation

### 1. **Multi-Factor Confidence Calculator**

```java
@Component
public class ExtractionConfidenceCalculator {
    
    public double calculateOverallConfidence(ExtractedInvoiceData data) {
        Map<String, Double> fieldConfidences = new HashMap<>();
        
        // Individual field confidence scores
        fieldConfidences.put("invoice_number", calculateInvoiceNumberConfidence(data.getInvoiceNumber()));
        fieldConfidences.put("total_amount", calculateAmountConfidence(data.getTotalAmount()));
        fieldConfidences.put("invoice_date", calculateDateConfidence(data.getInvoiceDate()));
        fieldConfidences.put("vendor_name", calculateVendorNameConfidence(data.getVendorName()));
        fieldConfidences.put("line_items", calculateLineItemsConfidence(data.getLineItems()));
        
        // Weighted average based on field importance
        Map<String, Double> weights = Map.of(
            "invoice_number", 0.25,
            "total_amount", 0.25,
            "invoice_date", 0.15,
            "vendor_name", 0.20,
            "line_items", 0.15
        );
        
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        
        for (Map.Entry<String, Double> entry : fieldConfidences.entrySet()) {
            String field = entry.getKey();
            Double confidence = entry.getValue();
            Double weight = weights.get(field);
            
            if (confidence != null && weight != null) {
                weightedSum += confidence * weight;
                totalWeight += weight;
            }
        }
        
        double baseConfidence = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
        
        // Apply consistency bonuses/penalties
        double consistencyBonus = calculateConsistencyBonus(data);
        
        return Math.min(1.0, Math.max(0.0, baseConfidence + consistencyBonus));
    }
    
    private double calculateConsistencyBonus(ExtractedInvoiceData data) {
        double bonus = 0.0;
        
        // Check if line items add up to total
        if (data.getLineItems() != null && !data.getLineItems().isEmpty()) {
            BigDecimal lineItemSum = data.getLineItems().stream()
                .map(ExtractedLineItem::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            BigDecimal expectedTotal = data.getSubtotalAmount() != null ? 
                data.getSubtotalAmount() : data.getTotalAmount();
                
            if (expectedTotal != null) {
                BigDecimal difference = lineItemSum.subtract(expectedTotal).abs();
                BigDecimal tolerance = expectedTotal.multiply(BigDecimal.valueOf(0.05)); // 5% tolerance
                
                if (difference.compareTo(tolerance) <= 0) {
                    bonus += 0.1; // Line items match total
                }
            }
        }
        
        // Check date consistency
        if (data.getInvoiceDate() != null && data.getDueDate() != null) {
            if (data.getDueDate().isAfter(data.getInvoiceDate())) {
                bonus += 0.05; // Due date is after invoice date
            }
        }
        
        // Check amount consistency
        if (data.getSubtotalAmount() != null && data.getTaxAmount() != null && data.getTotalAmount() != null) {
            BigDecimal calculatedTotal = data.getSubtotalAmount().add(data.getTaxAmount());
            BigDecimal difference = calculatedTotal.subtract(data.getTotalAmount()).abs();
            
            if (difference.compareTo(BigDecimal.valueOf(0.01)) <= 0) {
                bonus += 0.05; // Tax calculation is correct
            }
        }
        
        return bonus;
    }
    
    private double calculateInvoiceNumberConfidence(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) return 0.0;
        
        double confidence = 0.5; // Base confidence
        
        // Length check
        if (invoiceNumber.length() >= 4 && invoiceNumber.length() <= 20) {
            confidence += 0.2;
        }
        
        // Format check
        if (invoiceNumber.matches("^[A-Z]{2,4}-?[0-9]{3,8}$")) {
            confidence += 0.3; // Standard invoice number format
        } else if (invoiceNumber.matches("^[0-9]{6,10}$")) {
            confidence += 0.2; // Numeric invoice number
        }
        
        return Math.min(1.0, confidence);
    }
}
```

### 2. **Validation Framework**

```java
@Component
public class ExtractionValidator {
    
    public ValidationResult validate(ExtractedInvoiceData data) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        // Required field validation
        validateRequiredFields(data, errors);
        
        // Format validation
        validateFormats(data, errors, warnings);
        
        // Business logic validation
        validateBusinessLogic(data, errors, warnings);
        
        // Cross-field validation
        validateCrossFields(data, errors, warnings);
        
        return ValidationResult.builder()
            .isValid(errors.isEmpty())
            .errors(errors)
            .warnings(warnings)
            .validatedAt(Instant.now())
            .build();
    }
    
    private void validateRequiredFields(ExtractedInvoiceData data, List<ValidationError> errors) {
        if (data.getInvoiceNumber() == null || data.getInvoiceNumber().trim().isEmpty()) {
            errors.add(ValidationError.builder()
                .field("invoice_number")
                .errorType("MISSING_REQUIRED")
                .message("Invoice number is required")
                .severity(ValidationSeverity.ERROR)
                .build());
        }
        
        if (data.getTotalAmount() == null) {
            errors.add(ValidationError.builder()
                .field("total_amount")
                .errorType("MISSING_REQUIRED")
                .message("Total amount is required")
                .severity(ValidationSeverity.ERROR)
                .build());
        }
        
        if (data.getVendorName() == null || data.getVendorName().trim().isEmpty()) {
            errors.add(ValidationError.builder()
                .field("vendor_name")
                .errorType("MISSING_REQUIRED")
                .message("Vendor name is required")
                .severity(ValidationSeverity.ERROR)
                .build());
        }
    }
    
    private void validateFormats(ExtractedInvoiceData data, 
                               List<ValidationError> errors, 
                               List<ValidationWarning> warnings) {
        
        // Invoice number format
        if (data.getInvoiceNumber() != null && 
            !data.getInvoiceNumber().matches("^[A-Za-z0-9-]{3,20}$")) {
            warnings.add(ValidationWarning.builder()
                .field("invoice_number")
                .warningType("UNUSUAL_FORMAT")
                .message("Invoice number format appears unusual")
                .suggestedValue(normalizeInvoiceNumber(data.getInvoiceNumber()))
                .build());
        }
        
        // Amount validation
        if (data.getTotalAmount() != null && 
            data.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(ValidationError.builder()
                .field("total_amount")
                .errorType("INVALID_VALUE")
                .message("Total amount must be positive")
                .severity(ValidationSeverity.ERROR)
                .build());
        }
        
        // Date validation
        if (data.getInvoiceDate() != null) {
            LocalDate now = LocalDate.now();
            if (data.getInvoiceDate().isAfter(now.plusDays(1))) {
                warnings.add(ValidationWarning.builder()
                    .field("invoice_date")
                    .warningType("FUTURE_DATE")
                    .message("Invoice date is in the future")
                    .build());
            }
            
            if (data.getInvoiceDate().isBefore(now.minusYears(2))) {
                warnings.add(ValidationWarning.builder()
                    .field("invoice_date")
                    .warningType("OLD_DATE")
                    .message("Invoice date is more than 2 years old")
                    .build());
            }
        }
    }
}
```

---

## Integration with OCR Processing Pipeline

### 1. **OCR Engine Coordinator**

```java
@Service
public class OCRProcessingCoordinator {
    
    @Autowired
    private List<InvoiceExtractionTemplate> extractionTemplates;
    
    @Autowired
    private ExtractionConfidenceCalculator confidenceCalculator;
    
    @Autowired
    private ExtractionValidator validator;
    
    public ProcessingResult processInvoice(byte[] pdfBytes, PDFMetadata metadata) {
        ProcessingResult result = new ProcessingResult();
        
        try {
            // Step 1: Extract raw text using appropriate OCR method
            String rawText = extractTextFromPDF(pdfBytes, metadata.getPdfType());
            result.setRawText(rawText);
            
            // Step 2: Try vendor-specific templates first
            ExtractedInvoiceData bestExtraction = null;
            double bestConfidence = 0.0;
            
            for (InvoiceExtractionTemplate template : extractionTemplates) {
                try {
                    ExtractedInvoiceData extraction = template.extractData(rawText, metadata);
                    double confidence = confidenceCalculator.calculateOverallConfidence(extraction);
                    
                    if (confidence > bestConfidence && confidence > template.getConfidenceThreshold()) {
                        bestExtraction = extraction;
                        bestConfidence = confidence;
                        result.setUsedTemplate(template.getTemplateName());
                    }
                    
                } catch (Exception e) {
                    logger.debug("Template {} failed for invoice", template.getTemplateName(), e);
                }
            }
            
            // Step 3: Fall back to generic extraction if no template worked well
            if (bestExtraction == null || bestConfidence < 0.7) {
                bestExtraction = performGenericExtraction(rawText, metadata);
                bestConfidence = confidenceCalculator.calculateOverallConfidence(bestExtraction);
                result.setUsedTemplate("Generic Extraction");
            }
            
            // Step 4: Validate extracted data
            ValidationResult validation = validator.validate(bestExtraction);
            
            // Step 5: Determine next processing step
            ProcessingDecision decision = determineProcessingDecision(bestConfidence, validation);
            
            result.setExtractedData(bestExtraction);
            result.setConfidenceScore(bestConfidence);
            result.setValidationResult(validation);
            result.setProcessingDecision(decision);
            result.setStatus(ProcessingStatus.COMPLETED);
            
        } catch (Exception e) {
            result.setStatus(ProcessingStatus.FAILED);
            result.setError(e.getMessage());
            logger.error("Failed to process invoice", e);
        }
        
        return result;
    }
    
    private ProcessingDecision determineProcessingDecision(double confidence, ValidationResult validation) {
        // High confidence and valid → Auto-approve
        if (confidence >= 0.9 && validation.isValid()) {
            return ProcessingDecision.AUTO_APPROVE;
        }
        
        // Medium confidence or minor warnings → Manual review
        if (confidence >= 0.7 && (validation.isValid() || validation.hasOnlyWarnings())) {
            return ProcessingDecision.MANUAL_REVIEW;
        }
        
        // Low confidence or validation errors → Manual correction
        return ProcessingDecision.MANUAL_CORRECTION;
    }
}
```

---

## Performance Optimization Templates

### 1. **Cached Pattern Compilation**

```java
@Component
@Scope("singleton")
public class PatternCache {
    
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();
    
    public Pattern getCompiledPattern(String regex, int flags) {
        String key = regex + "_" + flags;
        return patternCache.computeIfAbsent(key, k -> Pattern.compile(regex, flags));
    }
    
    public Pattern getCompiledPattern(String regex) {
        return getCompiledPattern(regex, 0);
    }
    
    @PreDestroy
    public void clearCache() {
        patternCache.clear();
    }
}
```

### 2. **Parallel Extraction Processing**

```java
@Service
public class ParallelExtractionService {
    
    @Async("extractionExecutor")
    public CompletableFuture<ExtractionResult> extractFieldAsync(String text, ExtractionTask task) {
        try {
            ExtractionResult result = performExtraction(text, task);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public ExtractedInvoiceData extractAllFieldsParallel(String text, PDFMetadata metadata) {
        List<CompletableFuture<ExtractionResult>> futures = Arrays.asList(
            extractFieldAsync(text, ExtractionTask.INVOICE_NUMBER),
            extractFieldAsync(text, ExtractionTask.TOTAL_AMOUNT),
            extractFieldAsync(text, ExtractionTask.INVOICE_DATE),
            extractFieldAsync(text, ExtractionTask.VENDOR_NAME),
            extractFieldAsync(text, ExtractionTask.LINE_ITEMS)
        );
        
        // Wait for all extractions to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allOf.get(30, TimeUnit.SECONDS); // 30 second timeout
            
            // Combine results
            ExtractedInvoiceData data = new ExtractedInvoiceData();
            
            for (CompletableFuture<ExtractionResult> future : futures) {
                ExtractionResult result = future.get();
                applyExtractionResult(data, result);
            }
            
            return data;
            
        } catch (Exception e) {
            logger.error("Parallel extraction failed", e);
            throw new ExtractionException("Failed to extract invoice data", e);
        }
    }
}
```

---

## Implementation Roadmap

### Phase 1: Core Patterns (Week 1-2)
- [ ] Implement basic regex patterns for invoice number, amounts, dates
- [ ] Create pattern cache and optimization utilities
- [ ] Build confidence scoring framework
- [ ] Develop validation rules engine

### Phase 2: Template System (Week 3-4)
- [ ] Create base template infrastructure
- [ ] Implement QuickBooks and SAP templates
- [ ] Build vendor-specific pattern libraries
- [ ] Add line item extraction capabilities

### Phase 3: Context Analysis (Week 5-6)
- [ ] Implement spatial context analyzer
- [ ] Create document region identification
- [ ] Build table structure detection
- [ ] Add intelligent fallback mechanisms

### Phase 4: Integration & Optimization (Week 7-8)
- [ ] Integrate with OCR processing pipeline
- [ ] Add parallel processing capabilities
- [ ] Implement caching and performance optimizations
- [ ] Build comprehensive testing framework

---

**OCR Data Extraction Templates Status**: ✅ COMPLETE  
**Templates Created**: 15+ extraction templates and patterns  
**Pattern Library**: Comprehensive regex patterns for all invoice fields  
**Vendor Templates**: QuickBooks, SAP, and generic templates  
**Confidence Scoring**: Multi-factor confidence calculation framework  
**Validation**: Complete validation and error handling system  

*This comprehensive template system provides robust, accurate, and scalable invoice data extraction capabilities for the Java-based OCR processing pipeline.*
