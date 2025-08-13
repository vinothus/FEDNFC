package com.company.invoice.api.controller;

import com.company.invoice.email.entity.Invoice;
import com.company.invoice.email.repository.InvoiceRepository;
import com.company.invoice.api.dto.response.InvoiceResponse;
import com.company.invoice.api.dto.response.UploadResponse;
import com.company.invoice.common.service.UrlBuilderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Invoice Management", description = "Invoice processing and management endpoints")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final UrlBuilderService urlBuilderService;

    @GetMapping("/invoices")
    @Operation(summary = "Get all invoices", description = "Retrieve a paginated list of invoices")
    @PreAuthorize("hasAnyRole('USER', 'APPROVER', 'ADMIN')")
    public ResponseEntity<?> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("📄 Getting all invoices - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        
        try {
            // Create sort object
            Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            
            // Create pageable object
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Get invoices from repository
            Page<Invoice> invoicePage = invoiceRepository.findAll(pageable);
            
            // Convert to DTOs
            List<InvoiceResponse> invoiceResponses = invoicePage.getContent()
                .stream()
                .map(InvoiceResponse::fromInvoice)
                .collect(Collectors.toList());
            
            // Create paginated response
            Map<String, Object> response = new HashMap<>();
            response.put("content", invoiceResponses);
            response.put("totalPages", invoicePage.getTotalPages());
            response.put("totalElements", invoicePage.getTotalElements());
            response.put("size", invoicePage.getSize());
            response.put("number", invoicePage.getNumber());
            response.put("numberOfElements", invoicePage.getNumberOfElements());
            response.put("first", invoicePage.isFirst());
            response.put("last", invoicePage.isLast());
            response.put("empty", invoicePage.isEmpty());
            
            log.info("✅ Successfully retrieved {} invoices (page {} of {}, total: {})", 
                invoiceResponses.size(), page + 1, invoicePage.getTotalPages(), invoicePage.getTotalElements());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error retrieving invoices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/invoices/{id}")
    @Operation(summary = "Get invoice by ID", description = "Retrieve a specific invoice by its ID")
    @PreAuthorize("hasAnyRole('USER', 'APPROVER', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        log.info("Getting invoice by ID: {}", id);
        
        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
            
            if (invoiceOpt.isEmpty()) {
                log.warn("Invoice not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Invoice invoice = invoiceOpt.get();
            InvoiceResponse invoiceResponse = InvoiceResponse.fromInvoice(invoice);
            
            log.info("Successfully retrieved invoice: {}", invoice.getOriginalFilename());
            return ResponseEntity.ok(invoiceResponse);
            
        } catch (Exception e) {
            log.error("Error retrieving invoice with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/invoices/{id}/details")
    @Operation(summary = "Get detailed invoice data", description = "Retrieve invoice with raw OCR text and extraction details")
    @PreAuthorize("hasAnyRole('USER', 'APPROVER', 'ADMIN')")
    public ResponseEntity<?> getInvoiceDetails(@PathVariable Long id) {
        log.info("🔍 Getting detailed invoice data for ID: {}", id);
        
        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
            
            if (invoiceOpt.isEmpty()) {
                log.warn("❌ Invoice not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Invoice invoice = invoiceOpt.get();
            
            // Create enhanced response with raw text and extraction details
            Map<String, Object> response = new HashMap<>();
            
            // Basic invoice data
            response.put("id", invoice.getId());
            response.put("fileName", invoice.getOriginalFilename());
            response.put("status", invoice.getProcessingStatus().name());
            response.put("vendorName", invoice.getVendorName() != null ? invoice.getVendorName() : "");
            response.put("totalAmount", invoice.getTotalAmount() != null ? invoice.getTotalAmount().doubleValue() : null);
            response.put("currency", invoice.getCurrency());
            response.put("uploadDate", invoice.getCreatedAt().toString());
            response.put("invoiceNumber", invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "");
            response.put("invoiceDate", invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().toString() : "");
            response.put("dueDate", invoice.getDueDate() != null ? invoice.getDueDate().toString() : "");
            response.put("ocrStatus", invoice.getOcrStatus().name());
            response.put("ocrConfidence", invoice.getOcrConfidence() != null ? invoice.getOcrConfidence().doubleValue() : null);
            response.put("fileSize", invoice.getFileSize());
            
            // Raw OCR text
            response.put("rawText", invoice.getRawExtractedText() != null && !invoice.getRawExtractedText().trim().isEmpty() 
                ? invoice.getRawExtractedText() 
                : getSampleRawText(invoice));
            
            // Extracted data
            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("vendor", invoice.getVendorName());
            extractedData.put("total", invoice.getTotalAmount() != null ? invoice.getTotalAmount().toString() : null);
            extractedData.put("currency", invoice.getCurrency());
            extractedData.put("invoiceNumber", invoice.getInvoiceNumber());
            extractedData.put("invoiceDate", invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().toString() : null);
            extractedData.put("dueDate", invoice.getDueDate() != null ? invoice.getDueDate().toString() : null);
            extractedData.put("ocrConfidence", invoice.getOcrConfidence() != null ? invoice.getOcrConfidence().doubleValue() : null);
            extractedData.put("ocrMethod", invoice.getOcrMethod() != null ? invoice.getOcrMethod() : "unknown");
            extractedData.put("processingTimeMs", invoice.getOcrProcessingTimeMs());
            extractedData.put("wordCount", invoice.getTextWordCount());
            extractedData.put("characterCount", invoice.getTextCharacterCount());
            extractedData.put("usedPatterns", invoice.getUsedPatternIds() != null ? invoice.getUsedPatternIds() : "");
            extractedData.put("patternSummary", invoice.getPatternMatchSummary() != null ? invoice.getPatternMatchSummary() : "");
            extractedData.put("confidenceDetails", invoice.getExtractionConfidenceDetails() != null ? invoice.getExtractionConfidenceDetails() : "{}");
            
            response.put("extractedData", extractedData);
            
            log.info("✅ Successfully retrieved detailed invoice data: {} (hasRawText: {})", 
                invoice.getOriginalFilename(), 
                invoice.getRawExtractedText() != null && !invoice.getRawExtractedText().trim().isEmpty());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error retrieving detailed invoice data for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve invoice details", "message", e.getMessage()));
        }
    }

    @GetMapping("/invoices/{id}/download")
    @Operation(summary = "Download invoice PDF", description = "Download the PDF file for a specific invoice")
    public ResponseEntity<?> downloadInvoice(@PathVariable Long id) {
        log.info("📥 Download request for invoice ID: {}", id);
        
        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
            
            if (invoiceOpt.isEmpty()) {
                log.warn("❌ Invoice not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Invoice invoice = invoiceOpt.get();
            
            if (invoice.getDownloadToken() == null || invoice.getDownloadToken().trim().isEmpty()) {
                log.error("❌ No download token available for invoice: {}", id);
                return ResponseEntity.status(HttpStatus.GONE)
                        .body("Download link not available for this invoice");
            }
            
            // Redirect to the PDF download endpoint with the token (absolute URL to avoid path issues)
            String downloadUrl = urlBuilderService.buildDownloadUrl(invoice.getDownloadToken());
            
            log.info("🔗 Redirecting to download URL: {}", downloadUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(downloadUrl));
            
            return ResponseEntity.status(HttpStatus.FOUND)
                    .headers(headers)
                    .build();
            
        } catch (Exception e) {
            log.error("❌ Error processing download request for invoice {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Download failed: " + e.getMessage());
        }
    }

    @PostMapping("/invoices/upload")
    @Operation(summary = "Upload invoice", description = "Upload a new invoice PDF for processing")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UploadResponse> uploadInvoice(@RequestParam("file") MultipartFile file) {
        log.info("Uploading invoice file: {}", file.getOriginalFilename());
        
        try {
            // Validate file
            if (file.isEmpty()) {
                log.warn("Empty file uploaded");
                return ResponseEntity.badRequest()
                    .body(UploadResponse.error("File is empty"));
            }
            
            // Check if it's a PDF
            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                log.warn("Invalid file type: {}", contentType);
                return ResponseEntity.badRequest()
                    .body(UploadResponse.error("Only PDF files are allowed"));
            }
            
            // Check file size (50MB limit)
            long maxSize = 50 * 1024 * 1024; // 50MB in bytes
            if (file.getSize() > maxSize) {
                log.warn("File too large: {} bytes", file.getSize());
                return ResponseEntity.badRequest()
                    .body(UploadResponse.error("File size exceeds 50MB limit"));
            }
            
            // Generate unique filename and tokens
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".pdf";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            String downloadToken = UUID.randomUUID().toString();
            
            // Calculate checksum
            String checksum = calculateChecksum(file.getBytes());
            
            // Check for duplicates
            List<Invoice> existingInvoices = invoiceRepository.findByChecksum(checksum);
            if (!existingInvoices.isEmpty()) {
                log.warn("Duplicate file detected: {}", checksum);
                return ResponseEntity.badRequest()
                    .body(UploadResponse.error("This file has already been uploaded"));
            }
            
            // Create storage directory if it doesn't exist
            String storageDir = System.getProperty("user.dir") + "/pdf-storage";
            Path storagePath = Paths.get(storageDir);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            
            // Store file in filesystem
            String filePath = storageDir + "/" + uniqueFilename;
            Path targetPath = Paths.get(filePath);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create download URL
            String downloadUrl = urlBuilderService.buildDownloadUrl(downloadToken);
            
            // Create Invoice entity
            Invoice invoice = Invoice.builder()
                .filename(uniqueFilename)
                .originalFilename(originalFilename)
                .filePath(filePath)
                .fileSize(file.getSize())
                .contentType(contentType)
                .checksum(checksum)
                .pdfBlob(file.getBytes()) // Store in database BLOB as well
                .downloadToken(downloadToken)
                .downloadUrl(downloadUrl)
                .downloadExpiresAt(LocalDateTime.now().plusDays(30)) // 30 days expiry
                .downloadCount(0)
                .maxDownloads(10)
                .processingStatus(Invoice.ProcessingStatus.PENDING)
                .ocrStatus(Invoice.OcrStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .receivedDate(LocalDateTime.now())
                .currency("USD") // Default currency
                .build();
            
            // Save to database
            Invoice savedInvoice = invoiceRepository.save(invoice);
            
            log.info("Successfully uploaded invoice: {} with ID: {}", originalFilename, savedInvoice.getId());
            
            // TODO: Trigger OCR processing here
            // ocrProcessingService.processInvoice(savedInvoice.getId());
            
            return ResponseEntity.ok(UploadResponse.success(
                savedInvoice.getId().toString(),
                originalFilename,
                file.getSize()
            ));
            
        } catch (IOException e) {
            log.error("Error saving file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(UploadResponse.error("Failed to save file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(UploadResponse.error("Upload failed: " + e.getMessage()));
        }
    }
    
    /**
     * Calculate SHA-256 checksum for file content
     */
    private String calculateChecksum(byte[] fileContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileContent);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error calculating checksum", e);
            return UUID.randomUUID().toString(); // Fallback
        }
    }

    /**
     * Generate sample raw text when actual OCR text is not available
     */
    private String getSampleRawText(Invoice invoice) {
        // If this is a real invoice with extracted data, create realistic sample text
        if (invoice.getVendorName() != null || invoice.getTotalAmount() != null || invoice.getInvoiceNumber() != null) {
            StringBuilder sampleText = new StringBuilder();
            
            sampleText.append("=== SAMPLE OCR TEXT ===\n");
            sampleText.append("This is simulated OCR text based on extracted invoice data.\n\n");
            
            if (invoice.getVendorName() != null) {
                sampleText.append("VENDOR: ").append(invoice.getVendorName()).append("\n");
            }
            
            if (invoice.getInvoiceNumber() != null) {
                sampleText.append("INVOICE #: ").append(invoice.getInvoiceNumber()).append("\n");
            }
            
            if (invoice.getInvoiceDate() != null) {
                sampleText.append("DATE: ").append(invoice.getInvoiceDate()).append("\n");
            }
            
            if (invoice.getTotalAmount() != null) {
                sampleText.append("TOTAL: ").append(invoice.getCurrency()).append(" ").append(invoice.getTotalAmount()).append("\n");
            }
            
            if (invoice.getDueDate() != null) {
                sampleText.append("DUE DATE: ").append(invoice.getDueDate()).append("\n");
            }
            
            sampleText.append("\n--- End of extracted fields ---\n");
            sampleText.append("Raw OCR processing is not yet implemented.\n");
            sampleText.append("This text was generated from extracted database fields.\n");
            sampleText.append("File: ").append(invoice.getOriginalFilename()).append("\n");
            sampleText.append("Upload Date: ").append(invoice.getCreatedAt()).append("\n");
            
            return sampleText.toString();
        }
        
        // Use realistic sample invoice text based on user-provided example
        return String.format("""
            === SAMPLE INVOICE TEXT (Based on Real Example) ===
            
            CPB Software (Germany) GmbH - Im Bruch 3 - 63897 Miltenberg/Main
            
            Musterkunde AG
            Mr. John Doe
            Musterstr. 23
            12345 Musterstadt
            
            Name: Stefanie Müller
            Phone: +49 9371 9786-0
            
            Invoice WMACCESS Internet
            VAT No. DE199378386
            Invoice No %s
            Customer No 12345
            Date 1. März 2024
            Invoice Period 01.02.2024 - 29.02.2024
            
            Service Description                                Amount -without VAT-  quantity
            Basic Fee wmView                                   130,00                1
            Basis fee for additional user accounts             10,00                 0
            Basic Fee wmGuide                                  50,00                 0
            Change of user accounts                           1.000,00               0
            Transaction Fee T1                                 10,00                 0
            Transaction Fee T2                                 0,58                 14
            Transaction Fee T3                                 0,70                 0
            Transaction Fee T4                                 1,50                162
            Transaction Fee T5                                 0,50                 0
            Transaction Fee T6                                 0,80                 0
            
            Total Amount                                      381,12
            VAT 19%%                                           72,41
            Gross Amount incl. VAT                           453,53
            
            Terms of Payment: Immediate payment without discount.
            Any bank charges must be paid by the invoice recipient.
            Bank fees at our expense will be charged to the invoice recipient!
            
            Please credit the amount invoiced to IBAN DE29 1234 5678 9012 3456 78 | BIC GENODE51MIC (SEPA Credit Transfer)
            
            This invoice is generated automatically and will not be signed
            
            --- EXTRACTION NOTES ---
            File: %s
            Size: %s bytes
            Upload Date: %s
            Processing Status: %s
            
            NOTE: This is sample OCR text for demonstration purposes.
            Real OCR processing pipeline is under development.
            """, 
            invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "123100401",
            invoice.getOriginalFilename(),
            invoice.getFileSize(),
            invoice.getCreatedAt(),
            invoice.getProcessingStatus().name()
        );
    }

    @DeleteMapping("/invoices/{id}")
    @Operation(summary = "Delete invoice", description = "Delete an invoice and its associated files")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        log.info("Deleting invoice with ID: {}", id);
        
        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
            
            if (invoiceOpt.isEmpty()) {
                log.warn("Invoice not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Invoice invoice = invoiceOpt.get();
            log.info("Deleting invoice: {}", invoice.getOriginalFilename());
            
            // Delete the invoice from database
            invoiceRepository.delete(invoice);
            
            log.info("Successfully deleted invoice with ID: {}", id);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("Error deleting invoice with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/invoices/{id}/approve")
    @Operation(summary = "Approve invoice", description = "Approve an invoice for payment")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<?> approveInvoice(@PathVariable Long id) {
        log.info("Approving invoice ID: {}", id);
        // TODO: Implement invoice approval
        return ResponseEntity.ok("Invoice approval endpoint - implementation pending");
    }

    @PutMapping("/invoices/{id}/reject")
    @Operation(summary = "Reject invoice", description = "Reject an invoice with reason")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<?> rejectInvoice(@PathVariable Long id, @RequestBody String reason) {
        log.info("Rejecting invoice ID: {} with reason: {}", id, reason);
        // TODO: Implement invoice rejection
        return ResponseEntity.ok("Invoice rejection endpoint - implementation pending");
    }

    @PostMapping("/invoices/{id}/reprocess")
    @Operation(summary = "Reprocess failed invoice", description = "Retry processing for a failed invoice")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> reprocessInvoice(@PathVariable Long id) {
        log.info("Reprocessing invoice with ID: {}", id);
        
        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
            
            if (invoiceOpt.isEmpty()) {
                log.warn("Invoice not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Invoice invoice = invoiceOpt.get();
            
            // Reset processing status to trigger reprocessing
            invoice.setProcessingStatus(Invoice.ProcessingStatus.PENDING);
            invoice.setOcrStatus(Invoice.OcrStatus.PENDING);
            invoiceRepository.save(invoice);
            
            log.info("Successfully queued invoice for reprocessing: {}", invoice.getOriginalFilename());
            
            // Return a simple success response for now
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Invoice queued for reprocessing\"}");
            
        } catch (Exception e) {
            log.error("Error reprocessing invoice with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\": false, \"message\": \"Reprocessing failed: " + e.getMessage() + "\"}");
        }
    }
}
