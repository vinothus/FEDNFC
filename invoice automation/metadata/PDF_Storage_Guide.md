# 📂 PDF Storage Location Guide

## ✅ **PDF Storage Now Implemented!**

I've just implemented the PDF storage functionality. Your PDFs will now be automatically saved when the email polling system runs.

## 📍 **Where to Find Your PDFs**

### **Primary Location**
```
📂 Directory: C:\NFC\invoice automation\invoice-automation-system\dev-pdf-storage\
```

### **How PDFs Are Named**
PDFs are saved with timestamped filenames to prevent conflicts:
```
Format: YYYYMMDD_HHMMSS_original_filename.pdf

Examples:
📄 20250806_235930_LDWK_YM12528_Grp1_28072025.PDF
📄 20250806_235930_Invoice_12345.pdf
📄 20250806_235930_Weekly_Statement.pdf
```

## 🔧 **Implementation Details**

### **What Happens Now When Email Is Processed:**
1. ✅ **Email Found**: System finds unread emails with PDF attachments
2. ✅ **PDF Extracted**: PDF content read into memory
3. ✅ **PDF Validated**: Size, type, and format checks
4. ✅ **PDF Saved**: File written to `dev-pdf-storage/` directory ⭐ **NEW!**
5. ✅ **Email Marked**: Email marked as processed
6. ✅ **Logging**: Detailed logs show save location

### **Enhanced Logging**
You'll now see these new log messages:
```
📁 Created PDF storage directory: C:\NFC\invoice automation\invoice-automation-system\dev-pdf-storage
💾 Saved PDF: LDWK_YM12528_Grp1_28072025.PDF (36627 bytes) -> C:\NFC\...\20250806_235930_LDWK_YM12528_Grp1_28072025.PDF
💾 Saved PDF to local storage: C:\NFC\...\20250806_235930_LDWK_YM12528_Grp1_28072025.PDF
```

## 🚀 **Start the Application to Test**

Run the application to start saving PDFs:
```powershell
# Set Maven path
$env:PATH += ";C:\Users\vipaul\Downloads\apache-maven-3.9.10\bin"

# Start application
mvn spring-boot:run -pl invoice-api -Pdev
```

## 📊 **Check Current Directory**

Let's see what's in your PDF storage directory right now:

```powershell
# Navigate to project directory
cd "C:\NFC\invoice automation\invoice-automation-system"

# Check if PDF storage directory exists
dir dev-pdf-storage

# If empty, wait for email polling cycle (every 2 minutes)
```

## 🕐 **When PDFs Are Saved**

- **Email Polling**: Every 2 minutes
- **First Run**: Will process existing unread emails from last 2 days
- **Subsequent Runs**: Only new unread emails since last check
- **Timing**: Next cycle ~2 minutes after previous completes

## 🔍 **How to Monitor PDF Saving**

Watch the application logs for these messages:

### **Successful PDF Save**
```
🔄 Starting email polling cycle for invoice PDFs
📬 Found 1 emails with PDF attachments to process
📊 Processing PDF attachment: LDWK_YM12528_Grp1_28072025.PDF (36627 bytes)
💾 Saved PDF: LDWK_YM12528_Grp1_28072025.PDF (36627 bytes) -> C:\...\20250806_235930_LDWK_YM12528_Grp1_28072025.PDF
✅ Successfully processed email: Weekly Statement
🎉 Email polling cycle completed successfully! Processed 1/1 emails
```

### **No New PDFs**
```
🔄 Starting email polling cycle for invoice PDFs
📧 No new emails with PDF attachments found
✅ Email polling cycle completed - no emails to process
```

## 📂 **Directory Structure**

Your project now looks like this:
```
C:\NFC\invoice automation\invoice-automation-system\
├── dev-pdf-storage/                    ⭐ YOUR PDFs ARE HERE
│   ├── 20250806_235930_Invoice1.pdf
│   ├── 20250806_235930_Invoice2.pdf
│   └── ...
├── invoice-api/
├── invoice-email/
├── invoice-data/
├── invoice-ocr/
├── invoice-service/
├── invoice-common/
└── pom.xml
```

## ⚠️ **Important Notes**

1. **First Time**: Directory created automatically when first PDF is saved
2. **Permissions**: Make sure the application has write access to the directory
3. **Space**: Monitor disk space as PDFs accumulate
4. **Backup**: Consider backing up important PDFs
5. **Cleanup**: Old PDFs are not automatically deleted

## 🐛 **Troubleshooting**

### **If PDFs Aren't Appearing:**
1. **Check logs** for error messages
2. **Verify email polling** is running (look for 🔄 messages)
3. **Check directory permissions**
4. **Confirm Gmail has new unread emails with PDFs**

### **If Directory Doesn't Exist:**
The application will create it automatically, but you can create manually:
```powershell
mkdir "C:\NFC\invoice automation\invoice-automation-system\dev-pdf-storage"
```

## 📧 **Configuration**

The PDF storage location is configured in `application-dev.yml`:
```yaml
invoice:
  storage:
    pdf-directory: ./dev-pdf-storage
```

You can change this path if needed, but remember to update the configuration and restart the application.
