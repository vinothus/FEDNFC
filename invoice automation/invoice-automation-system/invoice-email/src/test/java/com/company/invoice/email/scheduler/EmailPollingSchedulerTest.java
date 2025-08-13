package com.company.invoice.email.scheduler;

import com.company.invoice.email.service.EmailMonitoringService;
import com.company.invoice.email.service.PdfStorageService;
import com.company.invoice.email.service.database.PdfDatabaseService;
import com.company.invoice.ocr.service.InvoiceOcrProcessingService;
import com.company.invoice.email.service.InvoiceDataUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailPollingScheduler.
 */
@ExtendWith(MockitoExtension.class)
public class EmailPollingSchedulerTest {

            @Mock
    private EmailMonitoringService emailMonitoringService;

    @Mock
    private PdfStorageService pdfStorageService;
    
    @Mock
    private PdfDatabaseService pdfDatabaseService;
    
    @Mock
    private InvoiceOcrProcessingService ocrProcessingService;
    
    @Mock
    private InvoiceDataUpdateService invoiceDataUpdateService;
    
    private EmailPollingScheduler emailPollingScheduler;

    @BeforeEach
    void setUp() {
        emailPollingScheduler = new EmailPollingScheduler(emailMonitoringService, pdfStorageService, pdfDatabaseService, ocrProcessingService, invoiceDataUpdateService);
    }

    @Test
    void testPollEmailsForInvoices_NoEmailsFound() {
        // Given
        when(emailMonitoringService.connectToEmailServer()).thenReturn(true);
        when(emailMonitoringService.fetchUnreadEmailsWithPDFs()).thenReturn(new ArrayList<>());

        // When
        emailPollingScheduler.pollEmailsForInvoices();

        // Then
        verify(emailMonitoringService).connectToEmailServer();
        verify(emailMonitoringService).fetchUnreadEmailsWithPDFs();
        verify(emailMonitoringService).disconnect();
        verifyNoMoreInteractions(emailMonitoringService);
    }

    @Test
    void testPollEmailsForInvoices_ConnectionFailed() {
        // Given
        when(emailMonitoringService.connectToEmailServer()).thenReturn(false);

        // When
        emailPollingScheduler.pollEmailsForInvoices();

        // Then
        verify(emailMonitoringService).connectToEmailServer();
        verify(emailMonitoringService).disconnect();
        verifyNoMoreInteractions(emailMonitoringService);
    }

    @Test
    void testPollEmailsForInvoices_WithEmailsFound() {
        // Given
        EmailMonitoringService.EmailMessage email = new EmailMonitoringService.EmailMessage();
        email.setMessageId("test-message-id");
        email.setSubject("Test Invoice");

        EmailMonitoringService.PdfAttachment pdfAttachment = new EmailMonitoringService.PdfAttachment();
        pdfAttachment.setFilename("invoice.pdf");
        pdfAttachment.setContent(new byte[2048]); // 2KB test PDF
        pdfAttachment.setSize(2048L);
        pdfAttachment.setContentType("application/pdf");

        email.setPdfAttachments(List.of(pdfAttachment));

        when(emailMonitoringService.connectToEmailServer()).thenReturn(true);
        when(emailMonitoringService.fetchUnreadEmailsWithPDFs()).thenReturn(List.of(email));

        // When
        emailPollingScheduler.pollEmailsForInvoices();

        // Then
        verify(emailMonitoringService).connectToEmailServer();
        verify(emailMonitoringService).fetchUnreadEmailsWithPDFs();
        verify(emailMonitoringService).markEmailAsProcessed("test-message-id");
        verify(emailMonitoringService).disconnect();
    }

    @Test
    void testEmailServiceHealthCheck() {
        // Given
        when(emailMonitoringService.testConnection()).thenReturn(true);

        // When
        emailPollingScheduler.emailServiceHealthCheck();

        // Then
        verify(emailMonitoringService).testConnection();
    }
}
