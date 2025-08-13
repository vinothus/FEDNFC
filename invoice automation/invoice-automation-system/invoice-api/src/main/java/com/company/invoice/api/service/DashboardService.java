package com.company.invoice.api.service;

import com.company.invoice.api.dto.response.DashboardSummaryResponse;
import com.company.invoice.api.dto.response.RecentInvoiceResponse;
import com.company.invoice.data.repository.UserRepository;
import com.company.invoice.email.entity.Invoice;
import com.company.invoice.email.repository.InvoiceRepository;
import com.company.invoice.email.scheduler.EmailPollingScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for dashboard data aggregation and analytics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final EmailPollingScheduler emailPollingScheduler;

    /**
     * Get complete dashboard summary
     */
    public DashboardSummaryResponse getDashboardSummary() {
        log.info("📊 Generating dashboard summary");

        return DashboardSummaryResponse.builder()
            .overallStats(getOverallStats())
            .processingStats(getProcessingStats())
            .recentInvoices(getRecentInvoices(10))
            .statusBreakdown(getStatusBreakdown())
            .systemHealth(getSystemHealth())
            .build();
    }

    /**
     * Get overall system statistics
     */
    public DashboardSummaryResponse.OverallStats getOverallStats() {
        log.debug("📈 Calculating overall statistics");

        long totalInvoices = invoiceRepository.count();
        BigDecimal totalAmount = Optional.ofNullable(invoiceRepository.getTotalAmountSum())
            .orElse(BigDecimal.ZERO);
        
        long successfulInvoices = invoiceRepository.countByProcessingStatus(Invoice.ProcessingStatus.COMPLETED);
        double successRate = totalInvoices > 0 ? (successfulInvoices * 100.0) / totalInvoices : 0.0;
        
        // Calculate average processing time (mock implementation)
        double avgProcessingTime = calculateAverageProcessingTime();
        
        long totalVendors = invoiceRepository.countDistinctVendors();

        return DashboardSummaryResponse.OverallStats.builder()
            .totalInvoices(totalInvoices)
            .totalAmount(totalAmount)
            .successRate(BigDecimal.valueOf(successRate).setScale(1, RoundingMode.HALF_UP).doubleValue())
            .avgProcessingTime(avgProcessingTime)
            .totalVendors(totalVendors)
            .build();
    }

    /**
     * Get processing statistics for different time periods
     */
    public DashboardSummaryResponse.ProcessingStats getProcessingStats() {
        log.debug("📅 Calculating processing statistics by time period");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);

        // Today stats
        long todayCount = invoiceRepository.countByReceivedDateAfter(startOfDay);
        BigDecimal todayAmount = Optional.ofNullable(invoiceRepository.getTotalAmountByDateRange(startOfDay, now))
            .orElse(BigDecimal.ZERO);

        // Week stats
        long weekCount = invoiceRepository.countByReceivedDateAfter(startOfWeek);
        BigDecimal weekAmount = Optional.ofNullable(invoiceRepository.getTotalAmountByDateRange(startOfWeek, now))
            .orElse(BigDecimal.ZERO);

        // Month stats
        long monthCount = invoiceRepository.countByReceivedDateAfter(startOfMonth);
        BigDecimal monthAmount = Optional.ofNullable(invoiceRepository.getTotalAmountByDateRange(startOfMonth, now))
            .orElse(BigDecimal.ZERO);

        return DashboardSummaryResponse.ProcessingStats.builder()
            .todayCount(todayCount)
            .weekCount(weekCount)
            .monthCount(monthCount)
            .todayAmount(todayAmount)
            .weekAmount(weekAmount)
            .monthAmount(monthAmount)
            .build();
    }

    /**
     * Get recent invoices
     */
    public List<RecentInvoiceResponse> getRecentInvoices(int limit) {
        log.debug("📋 Fetching {} recent invoices", limit);

        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "receivedDate"));
        List<Invoice> recentInvoices = invoiceRepository.findAll(pageRequest).getContent();

        return recentInvoices.stream()
            .map(RecentInvoiceResponse::fromInvoice)
            .collect(Collectors.toList());
    }

    /**
     * Get processing status breakdown
     */
    public DashboardSummaryResponse.StatusBreakdown getStatusBreakdown() {
        log.debug("🔍 Calculating status breakdown");

        long completed = invoiceRepository.countByProcessingStatus(Invoice.ProcessingStatus.COMPLETED);
        long inProgress = invoiceRepository.countByProcessingStatus(Invoice.ProcessingStatus.PENDING);
        long failed = invoiceRepository.countByProcessingStatus(Invoice.ProcessingStatus.FAILED);
        long pending = invoiceRepository.countByProcessingStatus(Invoice.ProcessingStatus.PENDING);

        return DashboardSummaryResponse.StatusBreakdown.builder()
            .completed(completed)
            .inProgress(inProgress)
            .failed(failed)
            .pendingReview(pending)
            .build();
    }

    /**
     * Get system health information
     */
    public DashboardSummaryResponse.SystemHealth getSystemHealth() {
        log.debug("🏥 Checking system health");

        // Email scheduler status
        String emailSchedulerStatus = "UNKNOWN";
        LocalDateTime lastEmailCheck = null;
        
        try {
            if (emailPollingScheduler != null) {
                emailSchedulerStatus = emailPollingScheduler.isHealthy() ? "RUNNING" : "ERROR";
                lastEmailCheck = emailPollingScheduler.getLastSuccessfulRun();
            }
        } catch (Exception e) {
            log.warn("⚠️ Error checking email scheduler status: {}", e.getMessage());
            emailSchedulerStatus = "ERROR";
        }

        // OCR queue size (mock implementation)
        Integer ocrQueueSize = 0; // Could be implemented with actual queue monitoring

        // Database status
        String databaseStatus = "HEALTHY";
        try {
            invoiceRepository.count(); // Simple health check
        } catch (Exception e) {
            log.error("🚨 Database health check failed: {}", e.getMessage());
            databaseStatus = "ERROR";
        }

        // Average OCR confidence
        Double avgOcrConfidence = Optional.ofNullable(invoiceRepository.getAverageOcrConfidence())
            .map(conf -> conf.doubleValue())
            .orElse(0.0);

        return DashboardSummaryResponse.SystemHealth.builder()
            .emailSchedulerStatus(emailSchedulerStatus)
            .lastEmailCheck(lastEmailCheck)
            .ocrQueueSize(ocrQueueSize)
            .databaseStatus(databaseStatus)
            .avgOcrConfidence(BigDecimal.valueOf(avgOcrConfidence).setScale(1, RoundingMode.HALF_UP).doubleValue())
            .build();
    }

    /**
     * Get invoices by processing status
     */
    public List<RecentInvoiceResponse> getInvoicesByStatus(Invoice.ProcessingStatus status, int limit) {
        log.debug("📊 Fetching {} invoices with status: {}", limit, status);

        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "receivedDate"));
        Page<Invoice> invoicesPage = invoiceRepository.findByProcessingStatus(status, pageRequest);

        return invoicesPage.getContent().stream()
            .map(RecentInvoiceResponse::fromInvoice)
            .collect(Collectors.toList());
    }

    /**
     * Get processing statistics for a date range
     */
    public DashboardSummaryResponse.ProcessingStats getProcessingStatsForRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("📊 Calculating processing statistics for range: {} to {}", startDate, endDate);

        long count = invoiceRepository.countByReceivedDateBetween(startDate, endDate);
        BigDecimal amount = Optional.ofNullable(invoiceRepository.getTotalAmountByDateRange(startDate, endDate))
            .orElse(BigDecimal.ZERO);

        return DashboardSummaryResponse.ProcessingStats.builder()
            .todayCount(count)
            .todayAmount(amount)
            .build();
    }

    /**
     * Calculate average processing time
     */
    private double calculateAverageProcessingTime() {
        // This is a simplified implementation
        // In a real system, you would track processing start/end times
        try {
            Double avgTime = invoiceRepository.getAverageProcessingTime();
            return avgTime != null ? avgTime : 0.0;
        } catch (Exception e) {
            log.warn("⚠️ Could not calculate average processing time: {}", e.getMessage());
            return 0.0;
        }
    }
}
