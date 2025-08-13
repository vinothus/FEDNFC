package com.company.invoice.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for building consistent URLs across the application.
 * Handles domain configuration for deployment environments.
 * This service is shared across multiple modules (api, email, etc.)
 */
@Service
@Slf4j
public class UrlBuilderService {

    @Value("${invoice.app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${server.servlet.context-path:/invoice-automation}")
    private String contextPath;

    /**
     * Build a complete download URL for a PDF using the download token
     * 
     * @param downloadToken The secure download token
     * @return Complete URL for downloading the PDF
     */
    public String buildDownloadUrl(String downloadToken) {
        if (downloadToken == null || downloadToken.trim().isEmpty()) {
            log.warn("⚠️ Attempted to build download URL with null/empty token");
            return null;
        }

        String url = baseUrl + contextPath + "/pdf/download/" + downloadToken;
        log.debug("🔗 Built download URL: {}", url);
        return url;
    }

    /**
     * Build a complete URL for invoice redirect endpoint
     * 
     * @param invoiceId The invoice ID
     * @return Complete URL for invoice download redirect
     */
    public String buildInvoiceDownloadUrl(Long invoiceId) {
        if (invoiceId == null) {
            log.warn("⚠️ Attempted to build invoice download URL with null ID");
            return null;
        }

        String url = baseUrl + contextPath + "/invoices/" + invoiceId + "/download";
        log.debug("🔗 Built invoice download URL: {}", url);
        return url;
    }

    /**
     * Get the configured base URL (useful for debugging and logging)
     * 
     * @return The configured base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Get the configured context path
     * 
     * @return The configured context path
     */
    public String getContextPath() {
        return contextPath;
    }
}
