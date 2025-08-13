# Invoice Automation System - UI/UX Wireframes and Mockups

## Document Information
- **Created**: [Current Date]
- **Phase**: Phase 1 - Requirements and Design
- **Task**: 1.2.4 - Create UI/UX wireframes and mockups
- **Status**: ✅ COMPLETED

---

## Design Overview

### Design Philosophy
- **User-Centric**: Designed around the 4 primary user personas
- **Efficiency-Focused**: Streamlined workflows for daily invoice processing
- **Error Prevention**: Clear validation and confirmation patterns
- **Accessibility**: WCAG 2.1 AA compliance for inclusive design
- **Responsive**: Mobile-friendly design for approval workflows

### Design System
- **Framework**: Material Design 3.0 principles
- **Color Palette**: Professional blue and gray with status colors
- **Typography**: Inter font family for clarity and readability
- **Components**: Consistent UI component library
- **Grid System**: 12-column responsive grid

### Target Devices
- **Desktop**: Primary interface for invoice processing (1920x1080+)
- **Tablet**: Approval workflows and review tasks (768x1024+)
- **Mobile**: Critical approvals and notifications (360x640+)

---

## Color Palette and Typography

### Primary Colors
```
Primary Blue:     #1976D2  (Main actions, headers)
Secondary Blue:   #64B5F6  (Supporting elements)
Success Green:    #4CAF50  (Approved, completed states)
Warning Orange:   #FF9800  (Pending, review required)
Error Red:        #F44336  (Rejected, errors)
Gray Scale:       #FAFAFA, #F5F5F5, #E0E0E0, #9E9E9E, #424242, #212121
```

### Typography
```
Headlines:        Inter Bold (24px, 20px, 18px)
Body Text:        Inter Regular (16px, 14px)
Captions:         Inter Medium (12px, 11px)
Buttons:          Inter Medium (14px, 16px)
```

### Status Indicators
```
✅ Approved:      Green (#4CAF50)
⏳ Pending:       Orange (#FF9800)
❌ Rejected:      Red (#F44336)
🔄 Processing:    Blue (#1976D2)
⚠️ Review:        Yellow (#FFC107)
```

---

## Navigation Structure

### Main Navigation (Left Sidebar)
```
📊 Dashboard
📄 Invoices
   ├── All Invoices
   ├── Pending Review
   ├── Waiting Approval
   └── Upload Invoice
✅ Approvals
   ├── My Pending
   ├── Team Queue
   └── History
🏢 Vendors
   ├── All Vendors
   ├── Add Vendor
   └── Categories
📈 Reports
   ├── Dashboard
   ├── Processing Stats
   └── Export Data
⚙️ Settings
   ├── User Profile
   ├── Approval Workflows
   └── System Config (Admin)
```

---

## User Interface Wireframes

### 1. **Login Page**

```
┌─────────────────────────────────────────────┐
│                                             │
│           Invoice Automation                │
│                                             │
│         ┌─────────────────────┐             │
│         │                     │             │
│         │   [Company Logo]    │             │
│         │                     │             │
│         └─────────────────────┘             │
│                                             │
│              Welcome Back                   │
│         Sign in to your account             │
│                                             │
│         ┌─────────────────────┐             │
│         │ Username/Email      │             │
│         └─────────────────────┘             │
│                                             │
│         ┌─────────────────────┐             │
│         │ Password      [👁]   │             │
│         └─────────────────────┘             │
│                                             │
│         ☐ Remember me                       │
│                                             │
│         ┌─────────────────────┐             │
│         │      Sign In        │             │
│         └─────────────────────┘             │
│                                             │
│              Forgot Password?               │
│                                             │
└─────────────────────────────────────────────┘
```

### 2. **Dashboard (Main Overview)**

```
┌─────────────────────────────────────────────────────────────────────┐
│ ☰ Invoice Automation                          🔔 [3]   👤 John Doe  │
├─────┬───────────────────────────────────────────────────────────────┤
│ 📊  │ Dashboard                                    📅 Today, Jan 16  │
│ 📄  │                                                                │
│ ✅  │ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌──────────┐  │
│ 🏢  │ │   PENDING   │ │ PROCESSING  │ │  APPROVED   │ │ REJECTED │  │
│ 📈  │ │     23      │ │      8      │ │     156     │ │    4     │  │
│ ⚙️  │ │ invoices    │ │ invoices    │ │ invoices    │ │ invoices │  │
│     │ └─────────────┘ └─────────────┘ └─────────────┘ └──────────┘  │
│     │                                                                │
│     │ Quick Actions                                                  │
│     │ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│     │ │📤 Upload    │ │✅ Approve   │ │📊 Generate  │               │
│     │ │  Invoice    │ │  Pending    │ │   Report    │               │
│     │ │             │ │   (5)       │ │             │               │
│     │ └─────────────┘ └─────────────┘ └─────────────┘               │
│     │                                                                │
│     │ Recent Activity                          Processing Queue      │
│     │ ┌──────────────────────────────────┐ ┌───────────────────────┐ │
│     │ │⏳ INV-2024-156 - $2,500.00      │ │🔄 Processing...       │ │
│     │ │   Office Supplies Inc           │ │   INV-2024-159        │ │
│     │ │   2 minutes ago                 │ │   📄 → 📝 → ✅        │ │
│     │ ├──────────────────────────────────┤ │                       │ │
│     │ │✅ INV-2024-155 - $1,800.00      │ │⏳ Waiting Review      │ │
│     │ │   IT Services Corp              │ │   INV-2024-158        │ │
│     │ │   5 minutes ago                 │ │   Confidence: 72%     │ │
│     │ ├──────────────────────────────────┤ │                       │ │
│     │ │🔄 INV-2024-154 - $950.00        │ │✅ Completed           │ │
│     │ │   Marketing Agency              │ │   INV-2024-157        │ │
│     │ │   15 minutes ago                │ │   Total: $3,200.00    │ │
│     │ └──────────────────────────────────┘ └───────────────────────┘ │
└─────┴───────────────────────────────────────────────────────────────┘
```

### 3. **Invoice List View**

```
┌─────────────────────────────────────────────────────────────────────┐
│ ☰ Invoice Automation                          🔔 [3]   👤 John Doe  │
├─────┬───────────────────────────────────────────────────────────────┤
│ 📊  │ All Invoices                                   [📤 Upload]     │
│ 📄* │                                                                │
│ ✅  │ Filters: [All Status ▼] [All Vendors ▼] [Date Range] [🔍]     │
│ 🏢  │                                                                │
│ 📈  │ ┌──────────────────────────────────────────────────────────┐  │
│ ⚙️  │ │Invoice#    │Vendor        │Amount   │Date    │Status   │   │ │
│     │ ├──────────────────────────────────────────────────────────┤  │
│     │ │INV-2024-156│Office Sup..  │$2,500.00│Jan 16  │⏳Pending│📄 │ │
│     │ │INV-2024-155│IT Services   │$1,800.00│Jan 15  │✅Approved│📄│ │
│     │ │INV-2024-154│Marketing Ag..│$950.00  │Jan 15  │🔄Process│📄 │ │
│     │ │INV-2024-153│Cloud Hosting │$2,200.00│Jan 14  │✅Approved│📄│ │
│     │ │INV-2024-152│Office Rental │$5,000.00│Jan 14  │⚠️Review │📄 │ │
│     │ │INV-2024-151│Legal Consult │$3,500.00│Jan 13  │✅Approved│📄│ │
│     │ │INV-2024-150│Utilities     │$890.00  │Jan 13  │✅Approved│📄 │ │
│     │ │INV-2024-149│Software Lic..│$1,200.00│Jan 12  │❌Reject │📄 │ │
│     │ └──────────────────────────────────────────────────────────┘  │
│     │                                                                │
│     │ Showing 1-8 of 156 invoices    [← 1 2 3 ... 20 →]            │
└─────┴───────────────────────────────────────────────────────────────┘
```

### 4. **Invoice Detail View with PDF Viewer**

```
┌─────────────────────────────────────────────────────────────────────┐
│ ☰ Invoice Automation                          🔔 [3]   👤 John Doe  │
├─────┬───────────────────────────────────────────────────────────────┤
│ 📊  │ ← Back to Invoices     INV-2024-156     [Edit] [Approve] [❌] │
│ 📄* │                                                                │
│ ✅  │ ┌─────────────────────┐ ┌─────────────────────────────────────┐ │
│ 🏢  │ │                     │ │ Invoice Details                     │ │
│ 📈  │ │                     │ │                                     │ │
│ ⚙️  │ │                     │ │ Invoice #: INV-2024-156            │ │
│     │ │     PDF VIEWER      │ │ Vendor: Office Supplies Inc        │ │
│     │ │                     │ │ Amount: $2,500.00                   │ │
│     │ │   [Invoice PDF]     │ │ Date: January 16, 2024              │ │
│     │ │                     │ │ Due: February 15, 2024              │ │
│     │ │                     │ │ Department: Marketing                │ │
│     │ │                     │ │ Status: ⏳ Pending Approval         │ │
│     │ │ [🔍+] [🔍-] [↻] [📥] │ │ Confidence: 95%                     │ │
│     │ │                     │ │                                     │ │
│     │ └─────────────────────┘ │ Line Items:                         │ │
│     │                         │ ┌─────────────────────────────────┐ │ │
│     │ Processing Log:         │ │Description    │Qty│Price │Total │ │ │
│     │ ✅ Email received       │ │Office Chairs  │ 5 │$400  │$2000││ │ │
│     │ ✅ PDF extracted        │ │Delivery       │ 1 │$500  │$500 ││ │ │
│     │ ✅ OCR completed (Tika) │ │               │   │      │     ││ │ │
│     │ ✅ Data validated       │ │Subtotal: $2,000   Tax: $500    ││ │ │
│     │ ⏳ Waiting approval     │ │Total: $2,500.00                ││ │ │
│     │                         │ └─────────────────────────────────┘ │ │
│     │                         └─────────────────────────────────────┘ │
└─────┴───────────────────────────────────────────────────────────────┘
```

### 5. **Invoice Upload Interface**

```
┌─────────────────────────────────────────────────────────────────────┐
│ ☰ Invoice Automation                          🔔 [3]   👤 John Doe  │
├─────┬───────────────────────────────────────────────────────────────┤
│ 📊  │ Upload New Invoice                                             │
│ 📄* │                                                                │
│ ✅  │ ┌─────────────────────────────────────────────────────────────┐ │
│ 🏢  │ │                                                             │ │
│ 📈  │ │              📤                                             │ │
│ ⚙️  │ │                                                             │ │
│     │ │        Drag and drop PDF files here                        │ │
│     │ │               or click to browse                            │ │
│     │ │                                                             │ │
│     │ │         Supported: PDF files up to 50MB                    │ │
│     │ │                                                             │ │
│     │ └─────────────────────────────────────────────────────────────┘ │
│     │                                                                │
│     │ Optional Information:                                          │
│     │ ┌─────────────────┐ ┌─────────────────┐ ┌──────────────────┐  │
│     │ │Department    ▼  │ │Cost Center   ▼  │ │Project Code   ▼  │  │
│     │ └─────────────────┘ └─────────────────┘ └──────────────────┘  │
│     │                                                                │
│     │ ┌─────────────────────────────────────────────────────────────┐ │
│     │ │ Notes (optional)                                            │ │
│     │ │                                                             │ │
│     │ └─────────────────────────────────────────────────────────────┘ │
│     │                                                                │
│     │                              [Cancel] [Upload & Process]      │
│     │                                                                │
│     │ Upload Progress:                                               │
│     │ ┌─────────────────────────────────────────────────────────────┐ │
│     │ │ invoice_001.pdf                           █████████▒░ 92%   │ │
│     │ │ Uploading... Estimated time: 8 seconds                     │ │
│     │ └─────────────────────────────────────────────────────────────┘ │
└─────┴───────────────────────────────────────────────────────────────┘
```

### 6. **Approval Workflow Interface**

```
┌─────────────────────────────────────────────────────────────────────┐
│ ☰ Invoice Automation                          🔔 [3]   👤 John Doe  │
├─────┬───────────────────────────────────────────────────────────────┤
│ 📊  │ Pending Approvals (5)                     [Filter ▼] [Sort ▼] │
│ 📄  │                                                                │
│ ✅* │ ┌─────────────────────────────────────────────────────────────┐ │
│ 🏢  │ │🔴 OVERDUE - INV-2024-152 - $5,000.00                      │ │
│ 📈  │ │Office Rental Payment - Due 2 days ago                      │ │
│ ⚙️  │ │[View Details] [Approve] [Reject] [Delegate]                 │ │
│     │ └─────────────────────────────────────────────────────────────┘ │
│     │                                                                │
│     │ ┌─────────────────────────────────────────────────────────────┐ │
│     │ │🟡 DUE TODAY - INV-2024-156 - $2,500.00                    │ │
│     │ │Office Supplies Inc - Marketing Dept                        │ │
│     │ │[View Details] [Approve] [Reject] [Delegate]                 │ │
│     │ └─────────────────────────────────────────────────────────────┘ │
│     │                                                                │
│     │ ┌─────────────────────────────────────────────────────────────┐ │
│     │ │⚪ INV-2024-158 - $1,200.00                                 │ │
│     │ │Software License - IT Dept - Due in 2 days                  │ │
│     │ │[View Details] [Approve] [Reject] [Delegate]                 │ │
│     │ └─────────────────────────────────────────────────────────────┘ │
│     │                                                                │
│     │ Quick Actions:                                                 │
│     │ [Bulk Approve Selected] [Bulk Reject] [Export List]           │
└─────┴───────────────────────────────────────────────────────────────┘
```

### 7. **Approval Detail Modal**

```
┌─────────────────────────────────────────────┐
│ ✅ Approve Invoice                       ×  │
├─────────────────────────────────────────────┤
│                                             │
│ Invoice: INV-2024-156                       │
│ Vendor: Office Supplies Inc                 │
│ Amount: $2,500.00                           │
│ Department: Marketing                       │
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │ Approval Comments (optional)            │ │
│ │                                         │ │
│ │                                         │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ Approved Amount:                            │
│ ┌─────────────────┐                        │
│ │ $2,500.00       │ (Edit if different)    │
│ └─────────────────┘                        │
│                                             │
│ Approval Conditions:                        │
│ ☐ Requires receipt verification             │
│ ☐ Budget manager notification               │
│ ☐ Additional documentation needed           │
│                                             │
│              [Cancel] [Approve Invoice]     │
└─────────────────────────────────────────────┘
```

### 8. **Vendor Management Interface**

```
┌─────────────────────────────────────────────────────────────────────┐
│ ☰ Invoice Automation                          🔔 [3]   👤 John Doe  │
├─────┬───────────────────────────────────────────────────────────────┤
│ 📊  │ Vendor Management                              [+ Add Vendor]  │
│ 📄  │                                                                │
│ ✅  │ Search: [🔍 Search vendors...] [Category ▼] [Status ▼]        │
│ 🏢* │                                                                │
│ 📈  │ ┌──────────────────────────────────────────────────────────┐  │
│ ⚙️  │ │Code   │Name             │Category    │Invoices│Status   │  │ │
│     │ ├──────────────────────────────────────────────────────────┤  │
│     │ │OS001  │Office Supplies  │Office Supp │   45   │✅Active │✏️│ │
│     │ │IT002  │IT Services Corp │IT Services │   32   │✅Active │✏️│ │
│     │ │MK003  │Marketing Agency │Marketing   │   18   │✅Active │✏️│ │
│     │ │CH004  │Cloud Hosting    │IT Services │   24   │✅Active │✏️│ │
│     │ │OR005  │Office Rental    │Real Estate │   12   │✅Active │✏️│ │
│     │ │LC006  │Legal Consulting │Legal       │    8   │⏸️Inactive│✏️│ │
│     │ │UT007  │Utilities Corp   │Utilities   │   15   │✅Active │✏️│ │
│     │ └──────────────────────────────────────────────────────────┘  │
│     │                                                                │
│     │ Vendor Categories:                                             │
│     │ [Office Supplies: 15] [IT Services: 23] [Marketing: 8]        │
│     │ [Legal: 4] [Utilities: 6] [Real Estate: 3]                    │
└─────┴───────────────────────────────────────────────────────────────┘
```

### 9. **Error Handling and Validation**

```
┌─────────────────────────────────────────────────────────────────────┐
│ ☰ Invoice Automation                          🔔 [3]   👤 John Doe  │
├─────┬───────────────────────────────────────────────────────────────┤
│ 📊  │ Invoice Validation - INV-2024-159                             │
│ 📄* │                                                                │
│ ✅  │ ⚠️ Validation Issues Detected (3)                             │
│ 🏢  │                                                                │
│ 📈  │ ┌─────────────────────────────────────────────────────────────┐ │
│ ⚙️  │ │❌ Critical Error - Invoice Number                           │ │
│     │ │   Extracted: "INV 2024-159" (includes space)               │ │
│     │ │   Expected: "INV-2024-159"                                  │ │
│     │ │   [Fix Automatically] [Edit Manually]                      │ │
│     │ └─────────────────────────────────────────────────────────────┘ │
│     │                                                                │
│     │ ┌─────────────────────────────────────────────────────────────┐ │
│     │ │⚠️ Warning - Vendor Not Found                               │ │
│     │ │   Extracted: "Acme Corp Ltd"                               │ │
│     │ │   Suggestions: [Acme Corporation] [ACME Corp] [+ Add New]   │ │
│     │ │   Confidence: 85%                                           │ │
│     │ └─────────────────────────────────────────────────────────────┘ │
│     │                                                                │
│     │ ┌─────────────────────────────────────────────────────────────┐ │
│     │ │ℹ️ Information - Amount Verification                        │ │
│     │ │   Total: $1,250.00                                          │ │
│     │ │   Line items total: $1,200.00 + Tax: $50.00 = $1,250.00   │ │
│     │ │   ✅ Calculation verified                                   │ │
│     │ └─────────────────────────────────────────────────────────────┘ │
│     │                                                                │
│     │ Actions: [Save Corrections] [Queue for Manual Review] [Cancel]│
└─────┴───────────────────────────────────────────────────────────────┘
```

---

## Mobile-Responsive Design

### 10. **Mobile Dashboard**

```
┌─────────────────────┐
│ ☰ Invoices     [🔔3]│
├─────────────────────┤
│                     │
│ Quick Stats         │
│ ┌─────┐ ┌─────────┐ │
│ │ 23  │ │    5    │ │
│ │Pend │ │ Need    │ │
│ │     │ │Approval │ │
│ └─────┘ └─────────┘ │
│                     │
│ ┌─────────────────┐ │
│ │ 📤 Upload       │ │
│ │    Invoice      │ │
│ └─────────────────┘ │
│                     │
│ Recent Activity     │
│ ┌─────────────────┐ │
│ │⏳ INV-2024-156 │ │
│ │$2,500 • 2m ago  │ │
│ │Office Supplies  │ │
│ ├─────────────────┤ │
│ │✅ INV-2024-155 │ │
│ │$1,800 • 5m ago  │ │
│ │IT Services      │ │
│ ├─────────────────┤ │
│ │🔄 INV-2024-154 │ │
│ │$950 • 15m ago   │ │
│ │Marketing        │ │
│ └─────────────────┘ │
│                     │
│ [📊] [📄] [✅] [👤] │
└─────────────────────┘
```

### 11. **Mobile Approval Interface**

```
┌─────────────────────┐
│ ← Approval Details  │
├─────────────────────┤
│                     │
│ INV-2024-156        │
│ Office Supplies Inc │
│                     │
│ Amount              │
│ $2,500.00           │
│                     │
│ Department          │
│ Marketing           │
│                     │
│ Due Date            │
│ Today (⚠️ Urgent)   │
│                     │
│ ┌─────────────────┐ │
│ │   View PDF      │ │
│ └─────────────────┘ │
│                     │
│ Comments:           │
│ ┌─────────────────┐ │
│ │                 │ │
│ │                 │ │
│ └─────────────────┘ │
│                     │
│ ┌─────────────────┐ │
│ │   ✅ APPROVE    │ │
│ └─────────────────┘ │
│                     │
│ ┌─────────────────┐ │
│ │   ❌ REJECT     │ │
│ └─────────────────┘ │
│                     │
└─────────────────────┘
```

---

## High-Fidelity Mockups

### 1. **Main Dashboard (High-Fidelity)**

```
┌─────────────────────────────────────────────────────────────────────┐
│ [≡] Invoice Automation System        🔔 3   👤 John Doe    [⚙️] [🚪] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│ ┌─📊─┐  Good morning, John! Here's your invoice overview            │
│ │    │                                                              │
│ └────┘                                                              │
│                                                                     │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │
│ │   PENDING   │ │ PROCESSING  │ │  APPROVED   │ │  REJECTED   │    │
│ │     23      │ │      8      │ │     156     │ │      4      │    │
│ │ ⏳ invoices  │ │ 🔄 invoices │ │ ✅ invoices │ │ ❌ invoices │    │
│ │             │ │             │ │             │ │             │    │
│ │ ↗ +12%      │ │ ↘ -5%       │ │ ↗ +23%      │ │ ↗ +1        │    │
│ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘    │
│                                                                     │
│ ┌─ Quick Actions ──────────────────────────────────────────────────┐ │
│ │ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │ │
│ │ │📤 Upload New│ │✅ Review     │ │📊 Generate  │ │⚙️ Manage    │ │ │
│ │ │  Invoice    │ │ Pending (5) │ │  Report     │ │ Workflows   │ │ │
│ │ │             │ │ 🔴 2 Overdue│ │             │ │             │ │ │
│ │ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │ │
│ └─────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│ ┌─ Recent Activity ──────────────┐ ┌─ Processing Queue ────────────┐ │
│ │ ⏳ INV-2024-156 • $2,500.00   │ │ 🔄 Currently Processing       │ │
│ │    Office Supplies Inc        │ │    INV-2024-159               │ │
│ │    Marketing • 2 min ago      │ │    📄 Text extraction...      │ │
│ │                               │ │    ███████░░░ 70%             │ │
│ │ ✅ INV-2024-155 • $1,800.00   │ │                               │ │
│ │    IT Services Corp           │ │ ⏳ Queued for Review          │ │
│ │    IT Department • 5 min ago  │ │    INV-2024-158               │ │
│ │                               │ │    Confidence: 72% ⚠️         │ │
│ │ 🔄 INV-2024-154 • $950.00     │ │    Requires validation        │ │
│ │    Marketing Agency           │ │                               │ │
│ │    Marketing • 15 min ago     │ │ ✅ Recently Completed         │ │
│ │                               │ │    INV-2024-157 • $3,200     │ │
│ │ [View All Activity →]         │ │    Processing time: 12s       │ │
│ └───────────────────────────────┘ └───────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

### 2. **Invoice Detail with Split View (High-Fidelity)**

```
┌─────────────────────────────────────────────────────────────────────┐
│ [≡] ← Back to Invoices    INV-2024-156                  [Edit] [•••] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│ ┌─ PDF Viewer ──────────────────┐ ┌─ Invoice Information ──────────┐ │
│ │                               │ │                                │ │
│ │        📄 INVOICE             │ │ 📋 Details                     │ │
│ │                               │ │ Invoice #: INV-2024-156        │ │
│ │   OFFICE SUPPLIES INC         │ │ Status: ⏳ Pending Approval    │ │
│ │   123 Business Street         │ │                                │ │
│ │   City, ST 12345              │ │ 🏢 Vendor                      │ │
│ │                               │ │ Office Supplies Inc            │ │
│ │   Bill To:                    │ │ 📧 accounts@officesup.com      │ │
│ │   Company Name                │ │ 📞 (555) 123-4567              │ │
│ │   456 Client Ave              │ │                                │ │
│ │                               │ │ 💰 Financial                   │ │
│ │   Invoice: INV-2024-156       │ │ Total: $2,500.00               │ │
│ │   Date: 01/16/2024            │ │ Subtotal: $2,000.00            │ │
│ │   Due: 02/15/2024             │ │ Tax (25%): $500.00             │ │
│ │                               │ │ Currency: USD                  │ │
│ │   ┌─────────────────────────┐ │ │                                │ │
│ │   │Desc    │Qty│Rate │Total│ │ │ 📅 Dates                       │ │
│ │   │Chairs  │ 5 │$400 │$2000│ │ │ Invoice: Jan 16, 2024          │ │
│ │   │Delivery│ 1 │$500 │$500 │ │ │ Due: Feb 15, 2024              │ │
│ │   │        │   │     │     │ │ │ Received: Jan 16, 2024         │ │
│ │   │   Total:      $2,500.00│ │ │                                │ │
│ │   └─────────────────────────┘ │ │ 🏢 Business                    │ │
│ │                               │ │ Department: Marketing          │ │
│ │ [🔍] [📥] [🖨️] [📋]           │ │ Cost Center: MKT001            │ │
│ │ Zoom: 100% | Page 1 of 1     │ │ Project: Q1-Campaign           │ │
│ └───────────────────────────────┘ │                                │ │
│                                   │ 🔍 Processing                  │ │
│ ┌─ Processing History ────────────┐ │ Method: Apache Tika            │ │
│ │ ✅ 09:30 Email received         │ │ Confidence: 95%                │ │
│ │ ✅ 09:30 PDF extracted (2.1MB)  │ │ Extraction: 1.2s               │ │
│ │ ✅ 09:31 OCR completed (Tika)   │ │ Validation: Passed             │ │
│ │ ✅ 09:31 Data parsed & validated│ │                                │ │
│ │ ✅ 09:31 Vendor matched (98%)   │ │ 👥 Approval                    │ │
│ │ ⏳ 09:32 Waiting for approval   │ │ Required: Dept Manager         │ │
│ │ ⏳ 10:15 Assigned to John Doe   │ │ Assigned: John Doe             │ │
│ │                                 │ │ Due: Today 5:00 PM             │ │
│ │ [View Full Log →]               │ │                                │ │
│ └─────────────────────────────────┘ └────────────────────────────────┘ │
│                                                                     │
│ ┌─ Line Items ──────────────────────────────────────────────────────┐ │
│ │ Description        │ Quantity │ Unit Price │ Total     │ Tax Rate │ │
│ │ Office Chairs      │    5     │   $400.00  │ $2,000.00 │   0%     │ │
│ │ Delivery Service   │    1     │   $500.00  │   $500.00 │   0%     │ │
│ │                    │          │            │           │          │ │
│ │                    │          │  Subtotal: │ $2,000.00 │          │ │
│ │                    │          │       Tax: │   $500.00 │          │ │
│ │                    │          │     Total: │ $2,500.00 │          │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│ ┌─ Actions ───────────────────────────────────────────────────────────┐ │
│ │ [✅ Approve] [❌ Reject] [✏️ Edit] [👥 Delegate] [📧 Contact Vendor] │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

---

## User Experience Flows

### 1. **Invoice Processing Flow**

```
Start → Email Received → PDF Extracted → Type Detection
  ↓
OCR/Text Extraction → Data Parsing → Validation → Vendor Matching
  ↓
✅ High Confidence (>85%) → Auto-Approve → Notification
  ↓
⚠️ Medium Confidence (70-85%) → Manual Review Queue → User Validation
  ↓
❌ Low Confidence (<70%) → Correction Interface → User Input → Re-validation
  ↓
Approval Workflow → Manager Review → Approve/Reject → Final Processing
```

### 2. **Approval Workflow**

```
Invoice Ready → Workflow Rules Check → Assign Approver(s)
  ↓
Notification Sent → Approver Login → Review Interface
  ↓
✅ Approve → Comments → Confirm → Next Level (if required) → Complete
  ↓
❌ Reject → Reason Required → Comments → Confirm → Back to Submitter
  ↓
👥 Delegate → Select User → Reason → Transfer → Notification
```

### 3. **Error Recovery Flow**

```
Processing Error → Error Classification → Recovery Strategy
  ↓
📄 PDF Issues → Repair Attempt → Alternative OCR → Manual Queue
  ↓
🔍 Data Issues → Validation Rules → Correction Interface → Re-process
  ↓
👥 System Issues → Retry Logic → Fallback Method → Admin Alert
```

---

## Accessibility Features

### 1. **WCAG 2.1 Compliance**
- **Keyboard Navigation**: Full tab-based navigation
- **Screen Reader**: ARIA labels and semantic HTML
- **Color Contrast**: Minimum 4.5:1 ratio for all text
- **Focus Indicators**: Clear visual focus states
- **Alt Text**: All images and icons have descriptive text

### 2. **Accessibility Components**
```html
<!-- Example: Accessible invoice status -->
<div class="invoice-status" role="status" aria-live="polite">
  <span class="status-icon" aria-hidden="true">⏳</span>
  <span class="status-text">Pending approval</span>
  <span class="sr-only">Invoice INV-2024-156 is currently pending approval</span>
</div>

<!-- Example: Accessible action buttons -->
<button type="button" 
        class="btn btn-approve" 
        aria-describedby="approve-help"
        data-testid="approve-btn">
  ✅ Approve Invoice
</button>
<div id="approve-help" class="sr-only">
  Clicking this button will approve the invoice and move it to the next workflow step
</div>
```

### 3. **Progressive Enhancement**
- **Base Functionality**: Works without JavaScript
- **Enhanced Features**: Rich interactions with JS enabled
- **Offline Support**: Basic functionality available offline
- **Performance**: Optimized for low-bandwidth connections

---

## Design System Components

### 1. **Button Variations**
```
Primary:    [Primary Action]     (Blue background)
Secondary:  [Secondary Action]   (Gray outline)
Success:    [✅ Approve]         (Green background)
Danger:     [❌ Reject]          (Red background)
Warning:    [⚠️ Review]          (Orange background)
Link:       [Text Link]          (Blue text, no background)
Icon:       [📤]                 (Icon only with tooltip)
```

### 2. **Status Indicators**
```
Processing: 🔄 Processing        (Blue, animated)
Pending:    ⏳ Pending           (Orange)
Approved:   ✅ Approved          (Green)
Rejected:   ❌ Rejected          (Red)
Review:     ⚠️ Needs Review      (Yellow)
Complete:   🎉 Complete          (Green)
Error:      ⛔ Error             (Red)
```

### 3. **Form Elements**
```
Text Input:     [                    ]
Dropdown:       [Select Option    ▼ ]
Date Picker:    [01/16/2024      📅 ]
File Upload:    [Choose File... 📎  ]
Search:         [🔍 Search...       ]
Amount:         [$      .00          ]
Textarea:       [                    ]
                [                    ]
Checkbox:       ☐ Remember me
Radio:          ○ Option 1  ⦿ Option 2
```

---

## Performance Considerations

### 1. **Loading States**
- **Skeleton Screens**: Show content structure while loading
- **Progressive Loading**: Load critical content first
- **Lazy Loading**: Load images and non-critical content on demand
- **Optimistic UI**: Show expected results immediately

### 2. **Responsive Images**
```html
<picture>
  <source media="(min-width: 1200px)" srcset="invoice-large.jpg">
  <source media="(min-width: 768px)" srcset="invoice-medium.jpg">
  <img src="invoice-small.jpg" alt="Invoice preview" loading="lazy">
</picture>
```

### 3. **Animation Guidelines**
- **Duration**: 200-300ms for micro-interactions
- **Easing**: Smooth transitions with ease-out curves
- **Reduced Motion**: Respect user preferences
- **Performance**: Use CSS transforms and opacity

---

## Implementation Guidelines

### 1. **Frontend Framework**
- **React 18**: Component-based architecture
- **TypeScript**: Type safety and better development experience
- **Material-UI**: Component library with customization
- **Redux Toolkit**: State management for complex workflows

### 2. **CSS Architecture**
```scss
// Design tokens
$primary-color: #1976D2;
$secondary-color: #64B5F6;
$success-color: #4CAF50;
$warning-color: #FF9800;
$error-color: #F44336;

$font-family: 'Inter', sans-serif;
$border-radius: 8px;
$spacing-unit: 8px;

// Component structure
.invoice-card {
  border-radius: $border-radius;
  padding: $spacing-unit * 3;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  
  &--pending {
    border-left: 4px solid $warning-color;
  }
  
  &--approved {
    border-left: 4px solid $success-color;
  }
}
```

### 3. **Testing Strategy**
- **Unit Tests**: Component behavior and business logic
- **Integration Tests**: API integration and data flow
- **E2E Tests**: Complete user workflows
- **Accessibility Tests**: Automated a11y testing
- **Visual Regression**: Screenshot comparison tests

---

**UI/UX Wireframes Status**: ✅ COMPLETE  
**Wireframes Created**: 11 major interface screens  
**Design System**: Complete with colors, typography, and components  
**Responsive Design**: Mobile, tablet, and desktop layouts  
**Accessibility**: WCAG 2.1 AA compliance built-in  
**User Flows**: Complete workflows for all major functions  

*This comprehensive UI/UX design provides a user-friendly, accessible, and efficient interface for the Invoice Automation System with focus on the needs of all user personas.*
