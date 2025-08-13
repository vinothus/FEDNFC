package com.company.invoice.api.config;

import com.company.invoice.email.entity.Invoice;
import com.company.invoice.email.repository.InvoiceRepository;
import com.company.invoice.data.entity.User;
import com.company.invoice.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Sample data initializer to create sample invoices for testing
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after main data initialization
public class SampleDataInitializer implements CommandLineRunner {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        // Create default admin user
        if (userRepository.count() == 0) {
            log.info("👤 Creating default admin user...");
            createDefaultUser();
            log.info("✅ Default admin user created successfully!");
        }
        
        if (invoiceRepository.count() == 0) {
            log.info("🎯 Creating sample invoice data...");
            createSampleInvoices();
            log.info("✅ Sample invoice data created successfully!");
        } else {
            log.info("📋 Sample invoices already exist, skipping creation");
        }
    }

    private void createDefaultUser() {
        User admin = User.builder()
            .username("admin")
            .email("admin@invoiceautomation.com")
            .passwordHash(passwordEncoder.encode("admin123"))
            .firstName("System")
            .lastName("Administrator")
            .role(User.Role.ADMIN)
            .isActive(true)
            .isLocked(false)
            .failedLoginAttempts(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        userRepository.save(admin);
        log.info("Created admin user: username=admin, password=admin123");
    }

    private void createSampleInvoices() {
        List<String> vendors = Arrays.asList(
            "Acme Corporation", "Tech Solutions Inc", "Office Supplies Ltd", 
            "Global Services", "Digital Systems", "Professional Services Co",
            "Manufacturing Solutions", "Business Partners LLC", "Enterprise Solutions",
            "Innovation Technologies"
        );

        List<String> currencies = Arrays.asList("USD", "EUR", "GBP", "CAD");
        List<Invoice.ProcessingStatus> statuses = Arrays.asList(
            Invoice.ProcessingStatus.COMPLETED,
            Invoice.ProcessingStatus.COMPLETED,
            Invoice.ProcessingStatus.COMPLETED,
            Invoice.ProcessingStatus.PENDING,
            Invoice.ProcessingStatus.FAILED
        );

        for (int i = 1; i <= 25; i++) {
            Invoice invoice = new Invoice();
            
            // Basic info
            invoice.setInvoiceNumber("INV-2024-" + String.format("%04d", i));
            invoice.setVendorName(vendors.get(random.nextInt(vendors.size())));
            invoice.setOriginalFilename("invoice_" + i + ".pdf");
            invoice.setFilename("invoice_" + i + ".pdf");
            invoice.setFilePath("/dev-pdf-storage/invoice_" + i + ".pdf");
            invoice.setChecksum("checksum_" + i);
            invoice.setContentType("application/pdf");
            invoice.setFileSize((long) (1000 + random.nextInt(9000))); // 1-10KB
            
            // Amounts
            BigDecimal amount = BigDecimal.valueOf(100 + random.nextInt(9900) + random.nextDouble());
            invoice.setTotalAmount(amount);
            invoice.setCurrency(currencies.get(random.nextInt(currencies.size())));
            
            // Dates
            LocalDateTime createdDate = LocalDateTime.now().minusDays(random.nextInt(30));
            invoice.setCreatedAt(createdDate);
            invoice.setUpdatedAt(createdDate.plusHours(random.nextInt(24)));
            invoice.setReceivedDate(createdDate);
            invoice.setInvoiceDate(createdDate.minusDays(random.nextInt(10)).toLocalDate());
            
            // Email info
            invoice.setSenderEmail("invoices@" + invoice.getVendorName().toLowerCase().replace(" ", "") + ".com");
            
            // Processing info
            invoice.setProcessingStatus(statuses.get(random.nextInt(statuses.size())));
            invoice.setOcrStatus(invoice.getProcessingStatus() == Invoice.ProcessingStatus.COMPLETED ? Invoice.OcrStatus.COMPLETED : Invoice.OcrStatus.PENDING);
            invoice.setOcrConfidence(invoice.getProcessingStatus() == Invoice.ProcessingStatus.COMPLETED ? BigDecimal.valueOf(85 + random.nextInt(15)) : null);
            invoice.setOcrProcessingTimeMs(invoice.getProcessingStatus() == Invoice.ProcessingStatus.COMPLETED ? 1000 + random.nextInt(4000) : null);
            
            // Text extraction
            if (invoice.getProcessingStatus() == Invoice.ProcessingStatus.COMPLETED) {
                String extractedText = String.format(
                    "INVOICE\n\nFrom: %s\nInvoice Number: %s\nDate: %s\n\nAmount Due: %s %s\n\nThank you for your business!",
                    invoice.getVendorName(),
                    invoice.getInvoiceNumber(),
                    invoice.getInvoiceDate().toString(),
                    invoice.getCurrency(),
                    invoice.getTotalAmount()
                );
                invoice.setRawExtractedText(extractedText);
                invoice.setTextCharacterCount(extractedText.length());
                invoice.setTextWordCount(extractedText.split("\\s+").length);
            }
            
            // Download info
            invoice.setDownloadToken("token_" + i + "_" + System.currentTimeMillis());
            invoice.setMaxDownloads(10);
            invoice.setDownloadCount(random.nextInt(3));
            
            invoiceRepository.save(invoice);
        }
        
        log.info("📊 Created {} sample invoices", 25);
    }
}
