# Email Date Filtering Configuration

## Overview
The email monitoring service has been updated to only search for unread emails from a configurable number of days instead of searching all unread messages. This significantly improves performance and reduces processing overhead.

## Configuration Changes

### 1. Application Configuration Files

#### `application.yml` (Base Configuration)
```yaml
invoice:
  email:
    monitoring:
      enabled: true
      poll-interval: 120000 # 2 minutes
      days-to-check: 2 # Number of days to look back for unread emails
```

#### `application-dev.yml` (Development Environment)
```yaml
invoice:
  email:
    monitoring:
      enabled: true
      poll-interval: 120000 # 2 minutes for development
      days-to-check: 2 # Check emails from last 2 days in development
```

#### `application-prod.yml` (Production Environment)
```yaml
invoice:
  email:
    monitoring:
      enabled: ${EMAIL_MONITORING_ENABLED:true}
      poll-interval: ${EMAIL_POLL_INTERVAL:120000}
      days-to-check: ${EMAIL_DAYS_TO_CHECK:2}
```

### 2. Environment Variables (Production)
For production deployments, you can override the configuration using environment variables:

- `EMAIL_MONITORING_ENABLED`: Enable/disable email monitoring (default: true)
- `EMAIL_POLL_INTERVAL`: Polling interval in milliseconds (default: 120000)
- `EMAIL_DAYS_TO_CHECK`: Number of days to look back (default: 2)

Example:
```bash
export EMAIL_DAYS_TO_CHECK=1  # Check only today's emails
export EMAIL_DAYS_TO_CHECK=7  # Check last week's emails
```

## How It Works

### Date Filtering Logic
The email service now creates a compound search criteria:

1. **Unread Flag Filter**: `FlagTerm(SEEN, false)` - Only unread messages
2. **Date Filter**: `ReceivedDateTerm(GE, fromDate)` - Messages received after the calculated date
3. **Combined Filter**: `AndTerm(unreadFilter, dateFilter)` - Both conditions must be met

### Date Calculation
```java
Calendar calendar = Calendar.getInstance();
calendar.add(Calendar.DAY_OF_MONTH, -daysToCheck);
Date fromDate = calendar.getTime();
```

## Logging Improvements

The service now provides better logging information:

### Connection Log
```
Successfully connected to email server. Inbox contains 1,250 messages. 
Will check for unread messages from the last 2 days
```

### Search Results Log
```
Found 15 unread messages from the last 2 days (since 2025-08-04 23:30:00)
```

### Processing Log
```
Found email with 1 PDF attachments: Invoice #INV-2025-001
```

## Performance Benefits

### Before (All Unread Messages)
- Searches through potentially thousands of unread emails
- Higher memory usage
- Longer processing time
- More network bandwidth

### After (Date-Filtered Messages)
- Searches only recent unread emails (last 2 days by default)
- Lower memory usage
- Faster processing
- Reduced network bandwidth
- More focused processing

## Configuration Examples

### Conservative (Today Only)
```yaml
invoice:
  email:
    monitoring:
      days-to-check: 0  # Today only
```

### Standard (2 Days - Default)
```yaml
invoice:
  email:
    monitoring:
      days-to-check: 2  # Last 2 days
```

### Extended (1 Week)
```yaml
invoice:
  email:
    monitoring:
      days-to-check: 7  # Last week
```

### High Volume (1 Month)
```yaml
invoice:
  email:
    monitoring:
      days-to-check: 30  # Last month
```

## Testing

To test the configuration changes:

1. **Start the Application**:
   ```bash
   mvn spring-boot:run -pl invoice-api -Pdev
   ```

2. **Check Logs**: Look for connection and search result messages
3. **Monitor Processing**: Watch for PDF attachment processing logs
4. **Verify Date Range**: Confirm the "since" date in logs matches your expectation

## Technical Implementation

### Updated EmailMonitoringServiceImpl

**New Properties:**
```java
@Value("${invoice.email.monitoring.days-to-check:2}")
private int daysToCheck;
```

**New Imports:**
```java
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.AndTerm;
```

**Updated Search Logic:**
- Combines unread flag filter with date range filter
- Uses `ReceivedDateTerm.GE` (Greater than or Equal) for date comparison
- Logs the actual date range being searched

## Migration Notes

- **Backward Compatible**: Existing configurations will work with default 2-day setting
- **No Breaking Changes**: All existing functionality remains the same
- **Immediate Effect**: Changes take effect on next polling cycle (2 minutes)
- **Dynamic**: Can be changed without code modifications via environment variables

## Troubleshooting

### Issue: No emails found when expected
**Solution**: Increase `days-to-check` value

### Issue: Too many emails being processed
**Solution**: Decrease `days-to-check` value

### Issue: Performance issues
**Solution**: Reduce `days-to-check` to 1 or 0

### Issue: Missing recent emails
**Solution**: Ensure your email server time is synchronized
