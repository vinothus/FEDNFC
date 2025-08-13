# Email Polling Diagnostics & Troubleshooting Guide

## Issue Analysis from Your Logs

Based on your logs, the email polling system is working, but there are some potential issues:

```
23:36:50.907 [scheduling-1] INFO  c.c.i.e.s.i.EmailMonitoringServiceImpl - Successfully connected to email server. Inbox contains 98488 messages. Will check for unread messages from the last 2 days
23:36:51.547 [scheduling-1] INFO  c.c.i.e.s.i.EmailMonitoringServiceImpl - Found 99 unread messages from the last 2 days (since 2025-08-04 23:36:50)
23:37:29.007 [scheduling-1] DEBUG c.c.i.e.s.i.EmailMonitoringServiceImpl - Extracted PDF attachment: LDWK_YM12528_Grp1_28072025.PDF (36627 bytes)
23:37:30.020 [scheduling-1] INFO  c.c.i.e.s.i.EmailMonitoringServiceImpl - Found email with 1 PDF attachments: Weekly Statement
23:37:49.757 [scheduling-1] INFO  c.c.i.e.s.EmailPollingScheduler - Found 1 emails with PDF attachments to process
23:37:49.757 [scheduling-1] DEBUG c.c.i.e.s.EmailPollingScheduler - Processing email: Weekly Statement with 1 PDF attachments
23:37:49.763 [scheduling-1] INFO  c.c.i.e.s.EmailPollingScheduler - Processing PDF attachment: LDWK_YM12528_Grp1_28072025.PDF (50120 bytes)
23:37:49.764 [scheduling-1] DEBUG c.c.i.e.s.EmailPollingScheduler - PDF attachment processing initiated: LDWK_YM12528_Grp1_28072025.PDF
```

## Potential Issues Identified

### 1. **Missing Completion Logs**
- ❌ No "Email polling cycle completed successfully" log
- ❌ No "Next email polling cycle will start in X seconds" log
- ❌ No success/failure count summary

### 2. **Possible Silent Failure**
- The scheduler processes the PDF but might be failing silently after
- The `markEmailAsProcessed()` call might be causing an exception
- Connection cleanup might be causing issues

### 3. **Fixed Delay Behavior**
- Using `fixedDelayString` means next cycle starts AFTER current one completes
- If current cycle hangs or fails, next cycle is delayed indefinitely

## Improved Diagnostics

I've enhanced the scheduler with much better logging. Here's what you should see now:

### Successful Cycle Logs
```
🔄 Starting email polling cycle for invoice PDFs
✅ Connected to email server successfully
📬 Found 1 emails with PDF attachments to process
📎 Processing email: Weekly Statement with 1 PDF attachments
📊 Processing PDF attachment: LDWK_YM12528_Grp1_28072025.PDF (50120 bytes)
✅ PDF attachment processing initiated: LDWK_YM12528_Grp1_28072025.PDF
📋 Email processing summary: 1/1 attachments processed successfully for 'Weekly Statement'
✅ Successfully processed email: Weekly Statement
🎉 Email polling cycle completed successfully! Processed 1/1 emails (took 2,456ms)
✅ Disconnected from email server
⏱️ Next email polling cycle will start in 120 seconds (current cycle took 2,456ms)
```

### No Emails Found Logs
```
🔄 Starting email polling cycle for invoice PDFs
✅ Connected to email server successfully
📧 No new emails with PDF attachments found
✅ Email polling cycle completed - no emails to process (took 847ms)
⏱️ Next email polling cycle will start in 120 seconds (current cycle took 847ms)
```

### Error Handling Logs
```
🔄 Starting email polling cycle for invoice PDFs
❌ Failed to connect to email server. Skipping this polling cycle.
⏱️ Next email polling cycle will start in 120 seconds (current cycle took 156ms)
```

## Diagnostic Steps

### 1. Check Application Health
```bash
curl http://localhost:8080/api/actuator/health
```

### 2. Monitor Live Logs
Watch the application logs for the new diagnostic messages:
```bash
tail -f logs/invoice-automation.log
```

### 3. Test Email Connection Manually
The improved scheduler will show exactly where it fails:
- Connection issues
- Search issues  
- Processing issues
- Cleanup issues

### 4. Verify Polling Schedule
Check if subsequent cycles are starting:
- Look for "🔄 Starting email polling cycle" every 2 minutes
- If missing, something is blocking the scheduler

## Configuration Check

Verify your email polling configuration:

```yaml
# application-dev.yml
invoice:
  email:
    monitoring:
      enabled: true
      poll-interval: 120000  # 2 minutes
      days-to-check: 2       # Last 2 days
```

## Common Issues & Solutions

### Issue 1: Email Connection Timeout
**Symptoms**: Long delays, connection errors
**Solution**: Check Gmail app password, firewall, network

### Issue 2: Too Many Emails
**Symptoms**: Long processing times, memory issues  
**Solution**: Reduce `days-to-check` to 1 or 0

### Issue 3: Silent Failures
**Symptoms**: Processing starts but never completes
**Solution**: Check the new error logs for specific failures

### Issue 4: Email Marking Failure
**Symptoms**: Same emails processed repeatedly
**Solution**: Check IMAP permissions, Gmail settings

## Expected Behavior After Restart

1. **First Cycle**: Process any unread emails with PDFs from last 2 days
2. **Subsequent Cycles**: Only process NEW unread emails (since emails are marked as read)
3. **Empty Cycles**: Should complete quickly when no new emails found
4. **Timing**: Next cycle starts exactly 2 minutes after previous cycle completes

## Next Steps

1. **Restart the application** with the new diagnostic logging
2. **Watch the logs** for the new emoji-enhanced messages
3. **Monitor timing** - cycles should complete and show next start time
4. **Check for specific error messages** if cycles stop

The enhanced logging should help us identify exactly where the failure is occurring!
