package com.company.invoice.api.service;

import com.company.invoice.api.dto.response.AnalyticsFilterResponse;
import com.company.invoice.api.dto.response.MetricsTrendResponse;
import com.company.invoice.api.dto.response.TrendDataResponse;
import com.company.invoice.email.entity.Invoice;
import com.company.invoice.email.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analytics and trend data processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final InvoiceRepository invoiceRepository;

    public TrendDataResponse getProcessingTrends(int days, LocalDate startDate, LocalDate endDate, 
                                               TrendDataResponse.Granularity granularity, 
                                               Invoice.ProcessingStatus status) {
        log.info("📈 Generating processing trends for {} days with granularity {}", days, granularity);
        
        // Calculate date range
        LocalDate endDateFinal = endDate != null ? endDate : LocalDate.now();
        LocalDate startDateFinal = startDate != null ? startDate : endDateFinal.minusDays(days - 1);
        
        // Fetch invoices for the date range
        List<Invoice> invoices = getInvoicesForDateRange(startDateFinal, endDateFinal, status);
        
        // Group invoices by date based on granularity
        Map<LocalDate, List<Invoice>> groupedInvoices = groupInvoicesByDate(invoices, granularity);
        
        // Generate trend data points
        List<TrendDataResponse.TrendDataPoint> dataPoints = generateTrendDataPoints(
            groupedInvoices, startDateFinal, endDateFinal, granularity);
        
        // Calculate summary
        TrendDataResponse.TrendSummary summary = calculateTrendSummary(dataPoints, startDateFinal, endDateFinal);
        
        return TrendDataResponse.builder()
            .title("Invoice Processing Trends")
            .description(String.format("Processing trends from %s to %s", startDateFinal, endDateFinal))
            .granularity(granularity)
            .startDate(startDateFinal)
            .endDate(endDateFinal)
            .data(dataPoints)
            .summary(summary)
            .build();
    }

    public MetricsTrendResponse getAmountTrends(int days, LocalDate startDate, LocalDate endDate, 
                                              TrendDataResponse.Granularity granularity, String currency) {
        log.info("💰 Generating amount trends for {} days", days);
        
        // Calculate date range
        LocalDate endDateFinal = endDate != null ? endDate : LocalDate.now();
        LocalDate startDateFinal = startDate != null ? startDate : endDateFinal.minusDays(days - 1);
        
        // Fetch invoices for the date range
        List<Invoice> invoices = getInvoicesForDateRange(startDateFinal, endDateFinal, null);
        
        // Filter by currency if specified
        if (currency != null && !currency.isEmpty()) {
            invoices = invoices.stream()
                .filter(invoice -> currency.equals(invoice.getCurrency()))
                .collect(Collectors.toList());
        }
        
        // Group invoices by date
        Map<LocalDate, List<Invoice>> groupedInvoices = groupInvoicesByDate(invoices, granularity);
        
        // Generate amount data points
        List<MetricsTrendResponse.MetricDataPoint> dataPoints = generateAmountDataPoints(
            groupedInvoices, startDateFinal, endDateFinal, granularity);
        
        // Calculate metrics summary
        MetricsTrendResponse.MetricsSummary summary = calculateAmountSummary(dataPoints);
        
        return MetricsTrendResponse.builder()
            .metricType("AMOUNT")
            .title("Invoice Amount Trends")
            .description(String.format("Amount trends from %s to %s", startDateFinal, endDateFinal))
            .unit(currency != null ? currency : "USD")
            .granularity(granularity)
            .startDate(startDateFinal)
            .endDate(endDateFinal)
            .data(dataPoints)
            .summary(summary)
            .build();
    }

    public MetricsTrendResponse getProcessingTimeTrends(int days, LocalDate startDate, LocalDate endDate, 
                                                       TrendDataResponse.Granularity granularity) {
        log.info("⏱️ Generating processing time trends for {} days", days);
        
        // Calculate date range
        LocalDate endDateFinal = endDate != null ? endDate : LocalDate.now();
        LocalDate startDateFinal = startDate != null ? startDate : endDateFinal.minusDays(days - 1);
        
        // Fetch completed invoices for the date range (only completed invoices have processing times)
        List<Invoice> invoices = getInvoicesForDateRange(startDateFinal, endDateFinal, Invoice.ProcessingStatus.COMPLETED);
        
        // Group invoices by date
        Map<LocalDate, List<Invoice>> groupedInvoices = groupInvoicesByDate(invoices, granularity);
        
        // Generate processing time data points
        List<MetricsTrendResponse.MetricDataPoint> dataPoints = generateProcessingTimeDataPoints(
            groupedInvoices, startDateFinal, endDateFinal, granularity);
        
        // Calculate metrics summary
        MetricsTrendResponse.MetricsSummary summary = calculateProcessingTimeSummary(dataPoints);
        
        return MetricsTrendResponse.builder()
            .metricType("PROCESSING_TIME")
            .title("Processing Time Trends")
            .description(String.format("Processing time trends from %s to %s", startDateFinal, endDateFinal))
            .unit("minutes")
            .granularity(granularity)
            .startDate(startDateFinal)
            .endDate(endDateFinal)
            .data(dataPoints)
            .summary(summary)
            .build();
    }

    public AnalyticsFilterResponse getAvailableFilters() {
        log.info("🔍 Getting available analytics filters");
        
        // Get date range
        LocalDate earliestDate = invoiceRepository.findEarliestInvoiceDate()
            .orElse(LocalDate.now().minusDays(365));
        LocalDate latestDate = LocalDate.now();
        
        // Build quick ranges
        List<AnalyticsFilterResponse.QuickRange> quickRanges = Arrays.asList(
            AnalyticsFilterResponse.QuickRange.builder()
                .label("Last 7 days").value("7d").days(7).description("Past week").build(),
            AnalyticsFilterResponse.QuickRange.builder()
                .label("Last 30 days").value("30d").days(30).description("Past month").build(),
            AnalyticsFilterResponse.QuickRange.builder()
                .label("Last 90 days").value("90d").days(90).description("Past quarter").build(),
            AnalyticsFilterResponse.QuickRange.builder()
                .label("Last 365 days").value("365d").days(365).description("Past year").build()
        );
        
        // Get available statuses
        List<AnalyticsFilterResponse.FilterOption> statuses = Arrays.stream(Invoice.ProcessingStatus.values())
            .map(status -> AnalyticsFilterResponse.FilterOption.builder()
                .value(status.name())
                .label(formatStatusLabel(status))
                .description(getStatusDescription(status))
                .count(invoiceRepository.countByProcessingStatus(status))
                .recommended(status == Invoice.ProcessingStatus.COMPLETED)
                .build())
            .collect(Collectors.toList());
        
        // Get available currencies
        List<AnalyticsFilterResponse.FilterOption> currencies = invoiceRepository.findDistinctCurrencies()
            .stream()
            .map(currency -> AnalyticsFilterResponse.FilterOption.builder()
                .value(currency)
                .label(currency)
                .description(String.format("Invoices in %s", currency))
                .count(invoiceRepository.countByCurrency(currency))
                .recommended("USD".equals(currency))
                .build())
            .collect(Collectors.toList());
        
        // Get granularities
        List<AnalyticsFilterResponse.FilterOption> granularities = Arrays.stream(TrendDataResponse.Granularity.values())
            .map(gran -> AnalyticsFilterResponse.FilterOption.builder()
                .value(gran.name())
                .label(gran.getDisplayName())
                .description(String.format("Group data by %s", gran.getDisplayName().toLowerCase()))
                .recommended(gran == TrendDataResponse.Granularity.DAILY)
                .build())
            .collect(Collectors.toList());
        
        return AnalyticsFilterResponse.builder()
            .dateRange(AnalyticsFilterResponse.DateRangeInfo.builder()
                .earliestDate(earliestDate)
                .latestDate(latestDate)
                .suggestedStartDate(latestDate.minusDays(30))
                .suggestedEndDate(latestDate)
                .quickRanges(quickRanges)
                .build())
            .statuses(statuses)
            .currencies(currencies)
            .granularities(granularities)
            .suggestions(Map.of(
                "recommendedDays", 30,
                "recommendedGranularity", "DAILY",
                "recommendedMetrics", Arrays.asList("PROCESSING", "AMOUNTS")
            ))
            .build();
    }

    public Map<String, Object> getAdvancedSummary(int days, boolean includeTrends) {
        log.info("📊 Generating advanced analytics summary for {} days", days);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        // Get basic statistics
        List<Invoice> invoices = getInvoicesForDateRange(startDate, endDate, null);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("period", String.format("%s to %s", startDate, endDate));
        summary.put("totalInvoices", invoices.size());
        summary.put("completedInvoices", invoices.stream().mapToLong(i -> i.getProcessingStatus() == Invoice.ProcessingStatus.COMPLETED ? 1 : 0).sum());
        summary.put("totalAmount", invoices.stream().filter(i -> i.getTotalAmount() != null).mapToDouble(i -> i.getTotalAmount().doubleValue()).sum());
        summary.put("averageAmount", invoices.stream().filter(i -> i.getTotalAmount() != null).mapToDouble(i -> i.getTotalAmount().doubleValue()).average().orElse(0.0));
        
        if (includeTrends) {
            // Add trend data
            TrendDataResponse processingTrends = getProcessingTrends(days, startDate, endDate, 
                TrendDataResponse.Granularity.DAILY, null);
            MetricsTrendResponse amountTrends = getAmountTrends(days, startDate, endDate, 
                TrendDataResponse.Granularity.DAILY, null);
            
            summary.put("processingTrends", processingTrends);
            summary.put("amountTrends", amountTrends);
        }
        
        return summary;
    }

    public String exportTrendData(String format, int days, String dataType) {
        log.info("📤 Exporting trend data - format: {}, days: {}, dataType: {}", format, days, dataType);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        if ("JSON".equals(format)) {
            // Return JSON export
            getAdvancedSummary(days, true);
            // In a real implementation, you'd use a JSON library like Jackson
            return "JSON export data would be here";
        } else if ("CSV".equals(format)) {
            // Return CSV export
            return generateCsvExport(startDate, endDate, dataType);
        }
        
        throw new IllegalArgumentException("Unsupported export format: " + format);
    }

    // Private helper methods

    private List<Invoice> getInvoicesForDateRange(LocalDate startDate, LocalDate endDate, Invoice.ProcessingStatus status) {
        if (status != null) {
            return invoiceRepository.findByCreatedAtBetweenAndProcessingStatus(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), status);
        } else {
            return invoiceRepository.findByCreatedAtBetween(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        }
    }

    private Map<LocalDate, List<Invoice>> groupInvoicesByDate(List<Invoice> invoices, TrendDataResponse.Granularity granularity) {
        return invoices.stream().collect(Collectors.groupingBy(invoice -> {
            LocalDate date = invoice.getCreatedAt().toLocalDate();
            switch (granularity) {
                case WEEKLY:
                    return date.minusDays(date.getDayOfWeek().getValue() - 1); // Start of week
                case MONTHLY:
                    return date.withDayOfMonth(1); // Start of month
                default:
                    return date; // Daily
            }
        }));
    }

    private List<TrendDataResponse.TrendDataPoint> generateTrendDataPoints(
            Map<LocalDate, List<Invoice>> groupedInvoices, LocalDate startDate, LocalDate endDate, 
            TrendDataResponse.Granularity granularity) {
        
        List<TrendDataResponse.TrendDataPoint> dataPoints = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<Invoice> dayInvoices = groupedInvoices.getOrDefault(currentDate, Collections.emptyList());
            
            long total = dayInvoices.size();
            long completed = dayInvoices.stream().mapToLong(i -> i.getProcessingStatus() == Invoice.ProcessingStatus.COMPLETED ? 1 : 0).sum();
            long pending = dayInvoices.stream().mapToLong(i -> i.getProcessingStatus() == Invoice.ProcessingStatus.PENDING ? 1 : 0).sum();
            long processing = dayInvoices.stream().mapToLong(i -> i.getProcessingStatus() == Invoice.ProcessingStatus.PROCESSING ? 1 : 0).sum();
            long failed = dayInvoices.stream().mapToLong(i -> i.getProcessingStatus() == Invoice.ProcessingStatus.FAILED ? 1 : 0).sum();
            
            double amount = dayInvoices.stream()
                .filter(i -> i.getTotalAmount() != null)
                .mapToDouble(i -> i.getTotalAmount().doubleValue())
                .sum();
            
            double successRate = total > 0 ? (completed * 100.0 / total) : 0.0;
            
            dataPoints.add(TrendDataResponse.TrendDataPoint.builder()
                .date(currentDate.format(formatter))
                .rawDate(currentDate)
                .totalInvoices(total)
                .completed(completed)
                .pending(pending)
                .processing(processing)
                .failed(failed)
                .amount(amount)
                .successRate(successRate)
                .build());
            
            // Move to next period based on granularity
            switch (granularity) {
                case WEEKLY:
                    currentDate = currentDate.plusWeeks(1);
                    break;
                case MONTHLY:
                    currentDate = currentDate.plusMonths(1);
                    break;
                default:
                    currentDate = currentDate.plusDays(1);
            }
        }
        
        return dataPoints;
    }

    private TrendDataResponse.TrendSummary calculateTrendSummary(
            List<TrendDataResponse.TrendDataPoint> dataPoints, LocalDate startDate, LocalDate endDate) {
        
        long totalInvoices = dataPoints.stream().mapToLong(TrendDataResponse.TrendDataPoint::getTotalInvoices).sum();
        double totalAmount = dataPoints.stream().mapToDouble(TrendDataResponse.TrendDataPoint::getAmount).sum();
        double avgSuccessRate = dataPoints.stream().mapToDouble(TrendDataResponse.TrendDataPoint::getSuccessRate).average().orElse(0.0);
        
        // Calculate trends (simplified)
        TrendDataResponse.TrendDirection invoiceTrend = calculateTrendDirection(
            dataPoints.stream().map(TrendDataResponse.TrendDataPoint::getTotalInvoices).collect(Collectors.toList()));
        
        return TrendDataResponse.TrendSummary.builder()
            .totalInvoices(totalInvoices)
            .totalAmount(totalAmount)
            .averageSuccessRate(avgSuccessRate)
            .invoiceTrend(invoiceTrend)
            .period(String.format("%s to %s", startDate, endDate))
            .build();
    }

    private List<MetricsTrendResponse.MetricDataPoint> generateAmountDataPoints(
            Map<LocalDate, List<Invoice>> groupedInvoices, LocalDate startDate, LocalDate endDate, 
            TrendDataResponse.Granularity granularity) {
        
        List<MetricsTrendResponse.MetricDataPoint> dataPoints = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<Invoice> dayInvoices = groupedInvoices.getOrDefault(currentDate, Collections.emptyList());
            
            List<Double> amounts = dayInvoices.stream()
                .filter(i -> i.getTotalAmount() != null)
                .map(i -> i.getTotalAmount().doubleValue())
                .collect(Collectors.toList());
            
            double totalAmount = amounts.stream().mapToDouble(Double::doubleValue).sum();
            double avgAmount = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double minAmount = amounts.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double maxAmount = amounts.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            
            dataPoints.add(MetricsTrendResponse.MetricDataPoint.builder()
                .date(currentDate.format(formatter))
                .rawDate(currentDate)
                .value(totalAmount)
                .average(avgAmount)
                .minimum(minAmount)
                .maximum(maxAmount)
                .count((long) amounts.size())
                .build());
            
            // Move to next period
            switch (granularity) {
                case WEEKLY:
                    currentDate = currentDate.plusWeeks(1);
                    break;
                case MONTHLY:
                    currentDate = currentDate.plusMonths(1);
                    break;
                default:
                    currentDate = currentDate.plusDays(1);
            }
        }
        
        return dataPoints;
    }

    private List<MetricsTrendResponse.MetricDataPoint> generateProcessingTimeDataPoints(
            Map<LocalDate, List<Invoice>> groupedInvoices, LocalDate startDate, LocalDate endDate, 
            TrendDataResponse.Granularity granularity) {
        
        List<MetricsTrendResponse.MetricDataPoint> dataPoints = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<Invoice> dayInvoices = groupedInvoices.getOrDefault(currentDate, Collections.emptyList());
            
            // Calculate processing times (mock calculation - you'd implement real logic)
            List<Double> processingTimes = dayInvoices.stream()
                .map(i -> calculateProcessingTime(i))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            double avgTime = processingTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double minTime = processingTimes.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double maxTime = processingTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            
            dataPoints.add(MetricsTrendResponse.MetricDataPoint.builder()
                .date(currentDate.format(formatter))
                .rawDate(currentDate)
                .value(avgTime)
                .average(avgTime)
                .minimum(minTime)
                .maximum(maxTime)
                .count((long) processingTimes.size())
                .build());
            
            // Move to next period
            switch (granularity) {
                case WEEKLY:
                    currentDate = currentDate.plusWeeks(1);
                    break;
                case MONTHLY:
                    currentDate = currentDate.plusMonths(1);
                    break;
                default:
                    currentDate = currentDate.plusDays(1);
            }
        }
        
        return dataPoints;
    }

    private MetricsTrendResponse.MetricsSummary calculateAmountSummary(List<MetricsTrendResponse.MetricDataPoint> dataPoints) {
        double totalValue = dataPoints.stream().mapToDouble(MetricsTrendResponse.MetricDataPoint::getValue).sum();
        double avgValue = dataPoints.stream().mapToDouble(MetricsTrendResponse.MetricDataPoint::getValue).average().orElse(0.0);
        double minValue = dataPoints.stream().mapToDouble(MetricsTrendResponse.MetricDataPoint::getValue).min().orElse(0.0);
        double maxValue = dataPoints.stream().mapToDouble(MetricsTrendResponse.MetricDataPoint::getValue).max().orElse(0.0);
        
        TrendDataResponse.TrendDirection trend = calculateTrendDirection(
            dataPoints.stream().map(MetricsTrendResponse.MetricDataPoint::getValue).collect(Collectors.toList()));
        
        return MetricsTrendResponse.MetricsSummary.builder()
            .totalValue(totalValue)
            .averageValue(avgValue)
            .minimumValue(minValue)
            .maximumValue(maxValue)
            .trend(trend)
            .interpretation(generateTrendInterpretation(trend, totalValue))
            .build();
    }

    private MetricsTrendResponse.MetricsSummary calculateProcessingTimeSummary(List<MetricsTrendResponse.MetricDataPoint> dataPoints) {
        double avgValue = dataPoints.stream().mapToDouble(MetricsTrendResponse.MetricDataPoint::getValue).average().orElse(0.0);
        double minValue = dataPoints.stream().mapToDouble(MetricsTrendResponse.MetricDataPoint::getValue).min().orElse(0.0);
        double maxValue = dataPoints.stream().mapToDouble(MetricsTrendResponse.MetricDataPoint::getValue).max().orElse(0.0);
        
        TrendDataResponse.TrendDirection trend = calculateTrendDirection(
            dataPoints.stream().map(MetricsTrendResponse.MetricDataPoint::getValue).collect(Collectors.toList()));
        
        return MetricsTrendResponse.MetricsSummary.builder()
            .averageValue(avgValue)
            .minimumValue(minValue)
            .maximumValue(maxValue)
            .trend(trend)
            .interpretation(generateProcessingTimeInterpretation(trend, avgValue))
            .build();
    }

    private TrendDataResponse.TrendDirection calculateTrendDirection(List<? extends Number> values) {
        if (values.size() < 2) return TrendDataResponse.TrendDirection.STABLE;
        
        double first = values.get(0).doubleValue();
        double last = values.get(values.size() - 1).doubleValue();
        
        double change = (last - first) / first * 100;
        
        if (change > 5) return TrendDataResponse.TrendDirection.UP;
        if (change < -5) return TrendDataResponse.TrendDirection.DOWN;
        return TrendDataResponse.TrendDirection.STABLE;
    }

    private String formatStatusLabel(Invoice.ProcessingStatus status) {
        return Arrays.stream(status.name().split("_"))
            .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }

    private String getStatusDescription(Invoice.ProcessingStatus status) {
        switch (status) {
            case PENDING: return "Invoices waiting to be processed";
            case PROCESSING: return "Invoices currently being processed";
            case COMPLETED: return "Successfully processed invoices";
            case FAILED: return "Invoices that failed processing";
            default: return "Unknown status";
        }
    }

    private Double calculateProcessingTime(Invoice invoice) {
        // Mock calculation - in real implementation, you'd calculate from created to completed timestamps
        if (invoice.getProcessingStatus() == Invoice.ProcessingStatus.COMPLETED) {
            return 5.0 + Math.random() * 25.0; // Random processing time between 5-30 minutes
        }
        return null;
    }

    private String generateTrendInterpretation(TrendDataResponse.TrendDirection trend, double totalValue) {
        switch (trend) {
            case UP: return String.format("Invoice amounts are trending upward. Total: $%.2f", totalValue);
            case DOWN: return String.format("Invoice amounts are trending downward. Total: $%.2f", totalValue);
            case STABLE: return String.format("Invoice amounts are stable. Total: $%.2f", totalValue);
            default: return String.format("Trend data insufficient. Total: $%.2f", totalValue);
        }
    }

    private String generateProcessingTimeInterpretation(TrendDataResponse.TrendDirection trend, double avgTime) {
        switch (trend) {
            case UP: return String.format("Processing times are increasing. Average: %.1f minutes", avgTime);
            case DOWN: return String.format("Processing times are improving. Average: %.1f minutes", avgTime);
            case STABLE: return String.format("Processing times are stable. Average: %.1f minutes", avgTime);
            default: return String.format("Processing time trend unclear. Average: %.1f minutes", avgTime);
        }
    }

    private String generateCsvExport(LocalDate startDate, LocalDate endDate, String dataType) {
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Value,Type\n");
        
        // Mock CSV data - in real implementation, you'd generate actual CSV
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            csv.append(String.format("%s,%.2f,%s\n", current, Math.random() * 1000, dataType));
            current = current.plusDays(1);
        }
        
        return csv.toString();
    }
}
