package com.company.invoice.email.service;

import lombok.Data;
import java.util.List;

/**
 * Service interface for monitoring emails and extracting invoice attachments.
 */
public interface EmailMonitoringService {

    /**
     * Connect to the email server using IMAP.
     * 
     * @return true if connection successful, false otherwise
     */
    boolean connectToEmailServer();

    /**
     * Fetch unread emails that contain PDF attachments.
     * 
     * @return list of emails with PDF attachments
     */
    List<EmailMessage> fetchUnreadEmailsWithPDFs();

    /**
     * Mark an email as processed.
     * 
     * @param messageId the email message ID
     */
    void markEmailAsProcessed(String messageId);

    /**
     * Test email server connectivity.
     * 
     * @return true if server is reachable and authentication successful
     */
    boolean testConnection();

    /**
     * Close email server connection.
     */
    void disconnect();

    /**
     * Data class representing an email message with PDF attachments.
     */
    @Data
    class EmailMessage {
        private String messageId;
        private String subject;
        private String fromAddress;
        private String receivedDate;
        private List<PdfAttachment> pdfAttachments;
        private boolean processed;
    }

    /**
     * Data class representing a PDF attachment.
     */
    @Data
    class PdfAttachment {
        private String filename;
        private byte[] content;
        private long size;
        private String contentType;
    }
}
