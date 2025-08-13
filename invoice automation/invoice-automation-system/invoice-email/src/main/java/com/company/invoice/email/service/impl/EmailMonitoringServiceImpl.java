package com.company.invoice.email.service.impl;

import com.company.invoice.email.service.EmailMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.HeaderTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.AndTerm;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implementation of EmailMonitoringService for IMAP email monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailMonitoringServiceImpl implements EmailMonitoringService {

    @Value("${spring.mail.host}")
    private String emailHost;

    @Value("${spring.mail.port:993}")
    private int emailPort;

    @Value("${spring.mail.username}")
    private String emailUsername;

    @Value("${spring.mail.password}")
    private String emailPassword;

    @Value("${invoice.email.monitoring.enabled:true}")
    private boolean monitoringEnabled;

    @Value("${invoice.email.monitoring.days-to-check:2}")
    private int daysToCheck;

    private Store store;
    private Folder inbox;

    @Override
    public boolean connectToEmailServer() {
        if (!monitoringEnabled) {
            log.info("Email monitoring is disabled");
            return false;
        }

        try {
            log.debug("Connecting to email server: {}:{}", emailHost, emailPort);

            // Configure email session properties
            Properties props = new Properties();
            props.setProperty("mail.store.protocol", "imaps");
            props.setProperty("mail.imaps.host", emailHost);
            props.setProperty("mail.imaps.port", String.valueOf(emailPort));
            props.setProperty("mail.imaps.ssl.enable", "true");

            // Create session and connect
            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect(emailHost, emailUsername, emailPassword);

            // Open inbox folder
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            log.info("Successfully connected to email server. Inbox contains {} messages. Will check for unread messages from the last {} days", 
                    inbox.getMessageCount(), daysToCheck);
            return true;

        } catch (MessagingException e) {
            log.error("Failed to connect to email server", e);
            return false;
        }
    }

    @Override
    public List<EmailMessage> fetchUnreadEmailsWithPDFs() {
        List<EmailMessage> emailsWithPDFs = new ArrayList<>();

        try {
            if (inbox == null || !inbox.isOpen()) {
                log.warn("Email inbox is not connected. Attempting to reconnect...");
                if (!connectToEmailServer()) {
                    return emailsWithPDFs;
                }
            }

            // Create search criteria: unread messages from the last N days
            FlagTerm unreadFlagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            
            // Calculate the date threshold (N days ago)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -daysToCheck);
            Date fromDate = calendar.getTime();
            
            ReceivedDateTerm dateTerm = new ReceivedDateTerm(ReceivedDateTerm.GE, fromDate);
            SearchTerm combinedTerm = new AndTerm(unreadFlagTerm, dateTerm);
            
            Message[] unreadMessages = inbox.search(combinedTerm);

            log.info("Found {} unread messages from the last {} days (since {})", 
                    unreadMessages.length, daysToCheck, formatDate(fromDate));

            for (Message message : unreadMessages) {
                try {
                    List<PdfAttachment> pdfAttachments = extractPdfAttachments(message);
                    
                    if (!pdfAttachments.isEmpty()) {
                        EmailMessage emailMessage = new EmailMessage();
                        emailMessage.setMessageId(getMessageId(message));
                        emailMessage.setSubject(message.getSubject());
                        emailMessage.setFromAddress(Arrays.toString(message.getFrom()));
                        emailMessage.setReceivedDate(formatDate(message.getReceivedDate()));
                        emailMessage.setPdfAttachments(pdfAttachments);
                        emailMessage.setProcessed(false);

                        emailsWithPDFs.add(emailMessage);
                        log.info("Found email with {} PDF attachments: {}", pdfAttachments.size(), emailMessage.getSubject());
                    }

                } catch (Exception e) {
                    log.error("Error processing message: {}", message.getSubject(), e);
                }
            }

        } catch (MessagingException e) {
            log.error("Error fetching unread emails", e);
        }

        return emailsWithPDFs;
    }

    @Override
    public void markEmailAsProcessed(String messageId) {
        try {
            if (inbox == null || !inbox.isOpen()) {
                log.warn("Email inbox is not connected");
                return;
            }

            // Use IMAP search to find the specific message by Message-ID header
            SearchTerm searchTerm = new HeaderTerm("Message-ID", messageId);
            Message[] messages = inbox.search(searchTerm);
            
            if (messages.length > 0) {
                // Mark the first (and should be only) matching message as read
                Message message = messages[0];
                message.setFlag(Flags.Flag.SEEN, true);
                log.debug("Marked email as processed using IMAP search: {} (found {} matching messages)", 
                         messageId, messages.length);
                
                if (messages.length > 1) {
                    log.warn("Found {} messages with same Message-ID: {}", messages.length, messageId);
                }
            } else {
                log.warn("No message found with Message-ID: {}", messageId);
                
                // Fallback to the old method if search fails (some IMAP servers might not support header search)
                log.debug("Falling back to full message scan for: {}", messageId);
                Message[] allMessages = inbox.getMessages();
                for (Message message : allMessages) {
                    if (messageId.equals(getMessageId(message))) {
                        message.setFlag(Flags.Flag.SEEN, true);
                        log.debug("Marked email as processed using fallback method: {}", messageId);
                        break;
                    }
                }
            }

        } catch (MessagingException e) {
            log.error("Error marking email as processed: {}", messageId, e);
        }
    }

    @Override
    public boolean testConnection() {
        try {
            if (store != null && store.isConnected()) {
                log.debug("Email connection test successful");
                return true;
            } else {
                log.warn("Email store is not connected");
                return connectToEmailServer();
            }
        } catch (Exception e) {
            log.error("Email connection test failed", e);
            return false;
        }
    }

    @Override
    public void disconnect() {
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
            log.debug("Disconnected from email server");
        } catch (MessagingException e) {
            log.error("Error disconnecting from email server", e);
        }
    }

    /**
     * Extract PDF attachments from an email message.
     */
    private List<PdfAttachment> extractPdfAttachments(Message message) throws MessagingException, IOException {
        List<PdfAttachment> pdfAttachments = new ArrayList<>();

        if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) || 
                    bodyPart.getFileName() != null) {
                    
                    String filename = bodyPart.getFileName();
                    String contentType = bodyPart.getContentType().toLowerCase();
                    
                    // Check if it's a PDF file
                    if (filename != null && 
                        (filename.toLowerCase().endsWith(".pdf") || contentType.contains("pdf"))) {
                        
                        PdfAttachment pdfAttachment = new PdfAttachment();
                        pdfAttachment.setFilename(filename);
                        pdfAttachment.setContentType(contentType);
                        pdfAttachment.setSize(bodyPart.getSize());
                        
                        // Read attachment content
                        if (bodyPart instanceof MimeBodyPart) {
                            byte[] content = bodyPart.getInputStream().readAllBytes();
                            pdfAttachment.setContent(content);
                            pdfAttachments.add(pdfAttachment);
                            
                            log.debug("Extracted PDF attachment: {} ({} bytes)", filename, content.length);
                        }
                    }
                }
            }
        }

        return pdfAttachments;
    }

    /**
     * Get unique message ID.
     */
    private String getMessageId(Message message) throws MessagingException {
        String[] headers = message.getHeader("Message-ID");
        if (headers != null && headers.length > 0) {
            return headers[0];
        }
        // Fallback to combination of subject and date
        return message.getSubject() + "_" + message.getReceivedDate().getTime();
    }

    /**
     * Format date for display.
     */
    private String formatDate(Date date) {
        if (date == null) return "Unknown";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }
}
