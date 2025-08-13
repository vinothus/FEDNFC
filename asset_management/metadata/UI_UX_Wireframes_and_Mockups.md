# Asset Management System - UI/UX Wireframes and Mockups

## Document Information
- **Created**: [Current Date]
- **Phase**: Phase 1 - Requirements and Design
- **Task**: 1.2.4 - Create UI/UX wireframes and mockups
- **Status**: ✅ COMPLETED
- **Design System**: Material Design 3.0 + Custom Asset Management Theme

---

## Design Philosophy

### User Experience Principles
1. **Mobile-First**: Prioritize mobile experience for field workers
2. **Accessibility**: WCAG 2.1 AA compliance
3. **Efficiency**: Minimize clicks and taps for common tasks
4. **Clarity**: Clear information hierarchy and visual feedback
5. **Consistency**: Unified design language across all platforms

### Visual Design Principles
1. **Modern & Clean**: Minimalist interface with strategic use of whitespace
2. **Color Psychology**: Status-driven color coding for quick recognition
3. **Typography**: Clear, readable fonts optimized for mobile screens
4. **Iconography**: Intuitive icons with text labels for clarity
5. **Responsive**: Seamless experience across all device sizes

---

## Design System

### Color Palette

#### Primary Colors
```
Primary Blue: #1976D2
Primary Blue Light: #42A5F5
Primary Blue Dark: #1565C0
Secondary Orange: #FF7043
Secondary Orange Light: #FF8A65
Secondary Orange Dark: #F4511E
```

#### Status Colors
```
Success Green: #4CAF50
Warning Yellow: #FF9800
Error Red: #F44336
Info Blue: #2196F3
```

#### Neutral Colors
```
Text Primary: #212121
Text Secondary: #757575
Background: #FAFAFA
Surface White: #FFFFFF
Border Light: #E0E0E0
Border Medium: #BDBDBD
```

### Typography
```
Display Large: Roboto 36px/44px Bold
Display Medium: Roboto 28px/36px Bold
Headline Large: Roboto 24px/32px Medium
Headline Medium: Roboto 20px/28px Medium
Title Large: Roboto 18px/24px Medium
Title Medium: Roboto 16px/24px Medium
Body Large: Roboto 16px/24px Regular
Body Medium: Roboto 14px/20px Regular
Label Large: Roboto 14px/20px Medium
Label Medium: Roboto 12px/16px Medium
```

### Spacing System
```
4px, 8px, 16px, 24px, 32px, 48px, 64px
```

### Component Specifications

#### Buttons
- **Primary**: Blue background, white text, 8px border radius
- **Secondary**: White background, blue border and text
- **Text**: No background, blue text
- **Icon**: 24x24px icon with optional text label
- **Height**: 48px (touch-friendly)

#### Cards
- **Elevation**: 2dp shadow
- **Border Radius**: 8px
- **Padding**: 16px
- **Background**: White surface

#### Form Elements
- **Text Input**: Outlined style, 56px height
- **Dropdown**: Material design select with chevron
- **Checkboxes**: 20x20px with rounded corners
- **Radio Buttons**: 20x20px circular

---

## Web Application Wireframes

### 1. Login Page Wireframe

```
┌─────────────────────────────────────────────────────────────┐
│                    Asset Management System                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    [LOGO]                                   │
│                                                             │
│              Welcome to Asset Management                    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Email Address                                       │   │
│  │ [_________________________________]                │   │
│  │                                                     │   │
│  │ Password                                            │   │
│  │ [_________________________________] [👁]          │   │
│  │                                                     │   │
│  │ □ Remember me                                       │   │
│  │                                                     │   │
│  │           [Sign In]                                 │   │
│  │                                                     │   │
│  │            Forgot Password?                         │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2. Dashboard Page Wireframe

```
┌─────────────────────────────────────────────────────────────┐
│ [≡] Asset Management    [🔔] [👤] John Doe        [Logout] │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Dashboard                                   [+ Add Asset]   │
│                                                             │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────┐ │
│ │   Total     │ │   Active    │ │ Maintenance │ │ Recent  │ │
│ │   Assets    │ │   Assets    │ │   Assets    │ │ Scans   │ │
│ │             │ │             │ │             │ │         │ │
│ │    1,247    │ │    1,156    │ │      23     │ │   156   │ │
│ │             │ │             │ │             │ │ Today   │ │
│ └─────────────┘ └─────────────┘ └─────────────┘ └─────────┘ │
│                                                             │
│ Recent Activity                              [View All →]   │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ [📱] Dell Laptop scanned by John Doe        2 min ago  │ │
│ │ [📍] MacBook moved to Floor 2               15 min ago │ │
│ │ [👤] iPad assigned to Jane Smith            1 hour ago │ │
│ │ [🔧] Printer status changed to maintenance  2 hour ago │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ Asset Categories                            [Manage →]      │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 💻 Laptops (156)  📱 Tablets (45)  🖥️ Monitors (89)    │ │
│ │ 🖨️ Printers (23)  📞 Phones (234)  🪑 Furniture (567)  │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3. Asset List Page Wireframe

```
┌─────────────────────────────────────────────────────────────┐
│ [≡] Asset Management    [🔔] [👤] John Doe        [Logout] │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Assets                                      [+ Add Asset]   │
│                                                             │
│ [🔍 Search assets...        ] [Filter ▼] [Sort ▼] [⚙️]    │
│                                                             │
│ Filters: [Category: All ▼] [Status: All ▼] [Location ▼]    │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Header Row                                              │ │
│ │ Name ↕ | Category | Status | Location | Last Scan | ⚙️ │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ 💻 Dell Laptop - Marketing                              │ │
│ │    Laptops | ●Active | Floor 2 | 2 min ago | [⚙️]     │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ 📱 iPad Pro - Sales                                     │ │
│ │    Tablets | ●Active | Floor 1 | 1 hour ago | [⚙️]    │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ 🖨️ HP Printer - Office                                  │ │
│ │    Printers | 🔧Maintenance | Floor 3 | 1 day ago | [⚙️]│ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ ... (more rows)                                         │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ [← Previous]  Page 1 of 15  [Next →]       Showing 1-20    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4. Asset Detail Page Wireframe

```
┌─────────────────────────────────────────────────────────────┐
│ [≡] Asset Management    [🔔] [👤] John Doe        [Logout] │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ [← Back] Dell Laptop - Marketing            [Edit] [⚙️]    │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ UUID: 550e8400-e29b-41d4-a716-446655440000             │ │
│ │ Status: ●Active | Condition: Good | Category: Laptops   │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────┐ ┌─────────────────────────────────────┐ │
│ │ Basic Info      │ │ Location & Assignment               │ │
│ │                 │ │                                     │ │
│ │ Name: Dell...   │ │ Current Location:                   │ │
│ │ Serial: DL123   │ │ 📍 Marketing Office - Floor 2       │ │
│ │ Model: XPS 13   │ │                                     │ │
│ │ Manufacturer:   │ │ Assigned To:                        │ │
│ │ Dell            │ │ 👤 Jane Smith (Marketing)           │ │
│ │                 │ │                                     │ │
│ │ Purchase:       │ │ Last Scanned:                       │ │
│ │ $1,299.99       │ │ 🕐 2 minutes ago by John Doe        │ │
│ │ 2023-05-15      │ │                                     │ │
│ └─────────────────┘ └─────────────────────────────────────┘ │
│                                                             │
│ Recent Activity                              [View All →]   │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 📱 Scanned at Marketing Office - Floor 2   2 min ago   │ │
│ │ 📍 Moved to Marketing Office - Floor 2     1 day ago   │ │
│ │ 👤 Assigned to Jane Smith                  5 days ago  │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5. Web Tag Writing Workflow

#### 5.1 Create New Asset with Tag Writing

```
┌─────────────────────────────────────────────────────────────┐
│ [≡] Asset Management    [🔔] [👤] John Doe        [Logout] │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ [← Back] Create New Asset                                   │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Step 1: Asset Information                               │ │
│ │ ● Asset Details  ○ Tag Writing  ○ Verification         │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────┐ ┌─────────────────────┐ │
│ │ Basic Information               │ │ Category & Location │ │
│ │                                 │ │                     │ │
│ │ Asset Name*                     │ │ Category*           │ │
│ │ [Dell Laptop - Marketing_____] │ │ [Laptops        ▼] │ │
│ │                                 │ │                     │ │
│ │ Serial Number                   │ │ Current Location*   │ │
│ │ [DL123456789________________] │ │ [Floor 2        ▼] │ │
│ │                                 │ │                     │ │
│ │ Model                           │ │ Assigned User       │ │
│ │ [XPS 13____________________] │ │ [Jane Smith     ▼] │ │
│ │                                 │ │                     │ │
│ │ Manufacturer                    │ │ Department          │ │
│ │ [Dell______________________] │ │ [Marketing      ▼] │ │
│ └─────────────────────────────────┘ └─────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Financial Information (Optional)                        │ │
│ │ Purchase Price: [$1299.99] Purchase Date: [2024-01-15] │ │
│ │ Warranty Date: [2027-01-15]  Currency: [USD        ▼] │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🏷️ NFC Tag Writing                                      │ │
│ │ □ Write to NFC tag immediately after creation          │ │
│ │   (Requires mobile device with NFC capability)         │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│            [Cancel]                    [Save & Continue →] │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 5.2 Tag Writing Instructions Page

```
┌─────────────────────────────────────────────────────────────┐
│ [≡] Asset Management    [🔔] [👤] John Doe        [Logout] │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ [← Back] Write NFC Tag                                      │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Step 2: Tag Writing                                     │ │
│ │ ✓ Asset Details  ● Tag Writing  ○ Verification         │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 📱 Mobile Device Required                               │ │
│ │                                                         │ │
│ │ To write the NFC tag, you'll need to use the mobile    │ │
│ │ app on a device with NFC capability.                   │ │
│ │                                                         │ │
│ │ Asset Created Successfully:                             │ │
│ │ ✅ Dell Laptop - Marketing                             │ │
│ │ 🆔 Asset ID: 456                                       │ │
│ │ 🔑 UUID: 550e8400-e29b-41d4-a716-446655440000         │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 📱 Next Steps:                                          │ │
│ │                                                         │ │
│ │ 1. Open the Asset Management mobile app                │ │
│ │ 2. Navigate to "Write NFC Tag"                         │ │
│ │ 3. Search for this asset: "Dell Laptop - Marketing"   │ │
│ │ 4. Follow the app instructions to write the tag        │ │
│ │ 5. Return here to verify the tag was written           │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 📲 QR Code for Quick Access                            │ │
│ │                                                         │ │
│ │         ▓▓▓▓▓▓▓ ▓   ▓ ▓▓▓▓▓▓▓                          │ │
│ │         ▓     ▓ ▓▓▓ ▓ ▓     ▓                          │ │
│ │         ▓ ▓▓▓ ▓ ▓▓  ▓ ▓ ▓▓▓ ▓                          │ │
│ │         ▓ ▓▓▓ ▓   ▓▓▓ ▓ ▓▓▓ ▓                          │ │
│ │         ▓ ▓▓▓ ▓ ▓ ▓ ▓ ▓ ▓▓▓ ▓                          │ │
│ │         ▓     ▓ ▓▓ ▓▓ ▓     ▓                          │ │
│ │         ▓▓▓▓▓▓▓ ▓ ▓ ▓ ▓▓▓▓▓▓▓                          │ │
│ │                                                         │ │
│ │ Scan with mobile app to access this asset directly     │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│     [← Previous]            [Tag Written ✓]     [Skip →]   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Mobile Application Wireframes

### 1. Mobile Login Screen

```
┌─────────────────────┐
│    ☰ Asset Mgmt    │
├─────────────────────┤
│                     │
│       [LOGO]        │
│                     │
│   Welcome Back      │
│                     │
│ ┌─────────────────┐ │
│ │ Email           │ │
│ │ [_____________] │ │
│ │                 │ │
│ │ Password        │ │
│ │ [_____________] │ │
│ │                 │ │
│ │ □ Remember me   │ │
│ │                 │ │
│ │   [Sign In]     │ │
│ │                 │ │
│ │ Forgot Password?│ │
│ └─────────────────┘ │
│                     │
│                     │
└─────────────────────┘
```

### 2. Mobile Dashboard

```
┌─────────────────────┐
│ ☰ Dashboard    [🔔] │
├─────────────────────┤
│                     │
│ Quick Actions       │
│ ┌─────┐ ┌─────────┐ │
│ │ 📱  │ │ 🏷️      │ │
│ │Scan │ │ Write   │ │
│ │Asset│ │ Tag     │ │
│ └─────┘ └─────────┘ │
│                     │
│ Asset Stats         │
│ ┌─────────────────┐ │
│ │ Total: 1,247    │ │
│ │ Active: 1,156   │ │
│ │ Maintenance: 23 │ │
│ │ Recent: 156     │ │
│ └─────────────────┘ │
│                     │
│ Recent Activity     │
│ ┌─────────────────┐ │
│ │ 📱 Dell Laptop  │ │
│ │    2 min ago    │ │
│ ├─────────────────┤ │
│ │ 📍 MacBook      │ │
│ │    15 min ago   │ │
│ ├─────────────────┤ │
│ │ 👤 iPad         │ │
│ │    1 hour ago   │ │
│ └─────────────────┘ │
│                     │
└─────────────────────┘
```

### 3. Mobile NFC Scan Screen

```
┌─────────────────────┐
│ ← Scan Asset   [?] │
├─────────────────────┤
│                     │
│  Scan NFC Tag       │
│                     │
│ ┌─────────────────┐ │
│ │       📱        │ │
│ │    ╭─────╮     │ │
│ │    │ NFC │     │ │
│ │    ╰─────╯     │ │
│ │                 │ │
│ │ Hold your phone │ │
│ │ near the asset  │ │
│ │    NFC tag      │ │
│ └─────────────────┘ │
│                     │
│   [Manual Entry]    │
│                     │
│ Tips:               │
│ • Keep phone steady │
│ • Move slowly       │
│ • Try different     │
│   angles if needed  │
│                     │
└─────────────────────┘
```

### 4. Mobile Asset Details

```
┌─────────────────────┐
│ ← Asset Detail [⚙️] │
├─────────────────────┤
│                     │
│ Dell Laptop         │
│ Marketing           │
│                     │
│ ●Active | Good      │
│                     │
│ ┌─────────────────┐ │
│ │ Serial: DL123   │ │
│ │ Location:       │ │
│ │ 📍 Floor 2      │ │
│ │ Assigned:       │ │
│ │ 👤 Jane Smith   │ │
│ │ Last Scan:      │ │
│ │ 🕐 2 min ago    │ │
│ └─────────────────┘ │
│                     │
│ [📱 Scan Again]     │
│ [📍 Update Location]│
│ [✏️ Add Note]       │
│                     │
│ Recent Activity     │
│ ┌─────────────────┐ │
│ │ 📱 Scanned      │ │
│ │ 📍 Moved        │ │
│ │ 👤 Assigned     │ │
│ └─────────────────┘ │
│                     │
└─────────────────────┘
```

### 5. Mobile Tag Writing Workflow

#### 5.1 Asset Type Selection Screen

```
┌─────────────────────┐
│ ← Write NFC Tag [?] │
├─────────────────────┤
│                     │
│  Select Asset Type  │
│                     │
│ Choose the type of  │
│ asset you want to   │
│ tag:                │
│                     │
│ ┌─────────────────┐ │
│ │ 💻 Laptop       │ │
│ │ Equipment       │ │
│ ├─────────────────┤ │
│ │ 📱 Tablet       │ │
│ │ Portable Device │ │
│ ├─────────────────┤ │
│ │ 🖥️ Monitor      │ │
│ │ Display         │ │
│ ├─────────────────┤ │
│ │ 🖨️ Printer      │ │
│ │ Office Equipment│ │
│ ├─────────────────┤ │
│ │ 📞 Phone        │ │
│ │ Communication   │ │
│ ├─────────────────┤ │
│ │ 🪑 Furniture    │ │
│ │ Office Items    │ │
│ └─────────────────┘ │
│                     │
│      [Continue]     │
│                     │
└─────────────────────┘
```

#### 5.2 Asset Selection from Database

```
┌─────────────────────┐
│ ← Select Asset [?] │
├─────────────────────┤
│                     │
│ 💻 Laptops          │
│                     │
│ Select existing     │
│ asset to write      │
│ NFC tag:            │
│                     │
│ ┌─────────────────┐ │
│ │ Asset Name   ▼  │ │
│ │ ┌─────────────┐ │ │
│ │ │Dell Laptop  │ │ │
│ │ │- Marketing  │ │ │
│ │ │ID: 456      │ │ │
│ │ ├─────────────┤ │ │
│ │ │MacBook Pro  │ │ │
│ │ │- Development│ │ │
│ │ │ID: 457      │ │ │
│ │ ├─────────────┤ │ │
│ │ │ThinkPad X1  │ │ │
│ │ │- Finance    │ │ │
│ │ │ID: 458      │ │ │
│ │ └─────────────┘ │ │
│ └─────────────────┘ │
│                     │
│ Selected:           │
│ 💻 Dell Laptop     │
│ Marketing (ID: 456) │
│                     │
│   [Write to Tag]    │
│                     │
└─────────────────────┘
```

#### 5.3 NFC Tag Writing Screen

```
┌─────────────────────┐
│ ← Write to Tag [?]  │
├─────────────────────┤
│                     │
│   Writing NFC Tag   │
│                     │
│ ┌─────────────────┐ │
│ │                 │ │
│ │       📱        │ │
│ │    ╭─────────╮  │ │
│ │    │ Writing │  │ │
│ │    │ ⟳ ⟳ ⟳ ⟳ │ │ │
│ │    ╰─────────╯  │ │
│ │                 │ │
│ │ Hold phone near │ │
│ │ the NFC tag     │ │
│ │                 │ │
│ │ 📝 Writing UUID │ │
│ │ 🔄 Progress: 60%│ │
│ └─────────────────┘ │
│                     │
│ Writing asset data  │
│ to NFC tag...       │
│                     │
│ Asset: Dell Laptop  │
│ UUID: 550e8400...   │
│                     │
│   [Cancel Write]    │
│                     │
└─────────────────────┘
```

#### 5.4 Write Success & Verification Screen

```
┌─────────────────────┐
│ ← Tag Written  [✓]  │
├─────────────────────┤
│                     │
│   ✅ Write Success  │
│                     │
│ NFC tag has been    │
│ successfully        │
│ written!            │
│                     │
│ ┌─────────────────┐ │
│ │ Asset Created:  │ │
│ │ 💻 Dell Laptop  │ │
│ │                 │ │
│ │ UUID:           │ │
│ │ 550e8400...     │ │
│ │                 │ │
│ │ Location:       │ │
│ │ 📍 Floor 2      │ │
│ │                 │ │
│ │ Status: Active  │ │
│ └─────────────────┘ │
│                     │
│  [📱 Verify Scan]   │
│  [✏️ Edit Details]  │
│  [🏠 Go to Home]    │
│                     │
│ Next: Scan the tag  │
│ to verify it works  │
│                     │
└─────────────────────┘
```

#### 5.5 Verification Scan Screen

```
┌─────────────────────┐
│ ← Verify Tag   [?]  │
├─────────────────────┤
│                     │
│  Verify NFC Tag     │
│                     │
│ ┌─────────────────┐ │
│ │                 │ │
│ │       📱        │ │
│ │    ╭─────────╮  │ │
│ │    │   NFC   │  │ │
│ │    │ ✓ ✓ ✓ ✓ │ │ │
│ │    ╰─────────╯  │ │
│ │                 │ │
│ │ Scan the tag    │ │
│ │ you just wrote  │ │
│ │ to verify       │ │
│ │                 │ │
│ │ 🔍 Verifying... │ │
│ └─────────────────┘ │
│                     │
│ This confirms the   │
│ tag works correctly │
│ and data is saved   │
│ to the database.    │
│                     │
│   [Skip Verify]     │
│                     │
└─────────────────────┘
```

#### 5.6 Complete Workflow Success

```
┌─────────────────────┐
│ ← Complete     [✓]  │
├─────────────────────┤
│                     │
│  🎉 All Complete!   │
│                     │
│ Your asset has been │
│ successfully:       │
│                     │
│ ┌─────────────────┐ │
│ │ ✅ Created in   │ │
│ │    Database     │ │
│ │                 │ │
│ │ ✅ Written to   │ │
│ │    NFC Tag      │ │
│ │                 │ │
│ │ ✅ Verified &   │ │
│ │    Working      │ │
│ │                 │ │
│ │ Asset ID: 456   │ │
│ │ UUID: 550e8400  │ │
│ └─────────────────┘ │
│                     │
│   [View Asset]      │
│   [Create Another]  │
│   [🏠 Dashboard]    │
│                     │
│ Ready for field use!│
│                     │
└─────────────────────┘
```

---

## Complete NFC Tag Writing Workflow

### Workflow Overview
The NFC tag writing process is designed to be seamless across web and mobile platforms, ensuring data integrity and user feedback at every step.

### Step-by-Step Process

#### 📱 **Mobile NFC Writing Workflow** (Streamlined)
1. **Asset Type Selection** → User selects category from predefined list
2. **Asset Selection** → Choose existing asset from database dropdown
3. **NFC Tag Writing** → UUID written to physical tag
4. **Verification Scan** → Immediate verification that tag works
5. **Completion Confirmation** → Success feedback with next steps

#### 🖥️ **Web-Initiated Workflow** (Alternative)
1. **Web Asset Creation** → Complete asset details entered on web
2. **Database Storage** → Asset saved with generated UUID
3. **Mobile Tag Writing** → User switches to mobile app
4. **Tag Writing** → Mobile app writes UUID to tag
5. **Web Verification** → Return to web to confirm completion

### Technical Process Flow

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Mobile App    │    │    Database     │    │   NFC Tag       │
│                 │    │                 │    │                 │
│ 1. Asset Type   │───▶│ 2. Query        │    │                 │
│    Selection    │    │    Assets       │    │                 │
│                 │    │                 │    │                 │
│ 3. Asset        │◀───│ 4. Return       │    │                 │
│    Selection    │    │    Asset List   │    │                 │
│                 │    │                 │    │                 │
│ 5. Get UUID     │───▶│ 6. Return UUID  │───▶│ 7. Write UUID   │
│                 │    │    for Asset    │    │    to Tag       │
│                 │    │                 │    │                 │
│ 9. Scan & Read  │◀───│ 8. Log Write    │◀───│                 │
│    Confirmation │    │    Success      │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Data Flow Specifications

#### 1. Asset Type Query (Mobile API Call)
```
GET /api/v1/assets?category_id=1&status=active&nfc_tag_id=null

Response:
{
  "success": true,
  "data": [
    {
      "id": 456,
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Dell Laptop - Marketing",
      "serial_number": "DL123456789",
      "category": {
        "id": 1,
        "name": "Laptops"
      },
      "current_location": {
        "id": 3,
        "name": "Floor 2"
      },
      "assigned_user": {
        "id": 789,
        "name": "Jane Smith"
      }
    },
    {
      "id": 457,
      "uuid": "123e4567-e89b-12d3-a456-426614174000",
      "name": "MacBook Pro - Development",
      "serial_number": "MBP987654321",
      "category": {
        "id": 1,
        "name": "Laptops"
      }
    }
  ]
}
```

#### 2. NFC Tag Writing and Update (Mobile Process)
```
1. Read UUID from selected asset record
2. Format UUID as NDEF message
3. Write NDEF to NFC tag
4. Verify write operation success
5. Update asset record with tag information

PUT /api/v1/assets/456
{
  "nfc_tag_id": "NFC123456789",
  "nfc_tag_written_at": "2024-01-15T15:30:00Z",
  "nfc_tag_written_by": 123
}

Response:
{
  "success": true,
  "data": {
    "id": 456,
    "nfc_tag_id": "NFC123456789",
    "nfc_tag_written_at": "2024-01-15T15:30:00Z",
    "updated_at": "2024-01-15T15:30:00Z"
  }
}
```

#### 3. Verification Scan (API Call)
```
POST /api/v1/assets/{id}/scan
{
  "scan_method": "nfc",
  "location_id": 3,
  "device_info": {
    "device_id": "mobile_device_123",
    "device_type": "android",
    "app_version": "1.2.3"
  },
  "verification_scan": true
}
```

### Error Handling & Recovery

#### Common Error Scenarios
1. **NFC Write Failure** → Retry mechanism with user guidance
2. **Tag Already Written** → Warning with overwrite option
3. **Database Connection Lost** → Queue operation for sync
4. **Invalid Asset Data** → Validation errors with correction prompts
5. **Tag Not Found During Verify** → Instructions to retry scan

#### Recovery Actions
- **Automatic Retry**: Failed NFC operations retry up to 3 times
- **Manual Override**: Option to skip verification if needed
- **Data Sync**: Offline operations queued for later sync
- **Error Logging**: All failures logged for system improvement

### User Experience Considerations

#### Feedback Mechanisms
- **Progress Indicators**: Clear progress through workflow steps
- **Success Animations**: Visual confirmation of successful operations
- **Error Messages**: User-friendly error descriptions with solutions
- **Help Context**: Tooltips and help buttons throughout process

#### Accessibility Features
- **Voice Guidance**: Audio instructions for NFC positioning
- **Haptic Feedback**: Vibration confirmation on successful operations
- **Large Touch Targets**: 48px minimum for all interactive elements
- **Screen Reader Support**: Full ARIA labels and semantic markup

#### Performance Optimization
- **Local Validation**: Immediate feedback on form errors
- **Prefetching**: Asset categories and locations cached locally
- **Offline Support**: Core functionality available without network
- **Background Sync**: Non-critical operations happen in background

---

## Detailed UI Mockups

### 1. Web Dashboard (High-Fidelity)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ ≡ Asset Management System         🔔 Notifications    👤 John Doe ▼    [Logout] │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│ Dashboard                                                     [+ Add Asset]     │
│                                                                                 │
│ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐       │
│ │   📊 Total    │ │   ✅ Active   │ │  🔧 Maint.    │ │  📱 Recent    │       │
│ │    Assets     │ │    Assets     │ │   Assets      │ │    Scans      │       │
│ │               │ │               │ │               │ │               │       │
│ │    1,247      │ │    1,156      │ │      23       │ │     156       │       │
│ │  +2.3% ↗️     │ │  98.2% ✅     │ │  -12% ↙️      │ │   Today       │       │
│ └───────────────┘ └───────────────┘ └───────────────┘ └───────────────┘       │
│                                                                                 │
│ 📈 Asset Status Overview                                                        │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ [Doughnut Chart showing: Active 93%, Maintenance 2%, Inactive 5%]          │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                 │
│ 🕐 Recent Activity                                            [View All →]     │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ 📱 Dell Laptop - Marketing scanned by John Doe               2 minutes ago  │ │
│ │ 📍 MacBook Pro - Development moved to Floor 2 Storage       15 minutes ago │ │
│ │ 👤 iPad Pro - Sales assigned to Jane Smith                   1 hour ago    │ │
│ │ 🔧 HP Printer - Office status changed to maintenance         2 hours ago   │ │
│ │ ✨ New asset: Surface Tablet added by Mike Wilson            3 hours ago   │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                 │
│ 📂 Asset Categories                                          [Manage →]        │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ 💻 Laptops (156)    📱 Tablets (45)     🖥️ Monitors (89)                   │ │
│ │ 🖨️ Printers (23)    📞 Phones (234)    🪑 Furniture (567)                  │ │
│ │ 📷 Cameras (12)     🔧 Tools (89)       📦 Equipment (145)                 │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 2. Mobile Asset Selection Screen (High-Fidelity)

```
┌─────────────────────────────────┐
│ ← Write NFC Tag          [?]    │
├─────────────────────────────────┤
│                                 │
│    💻 Laptops Category          │
│                                 │
│ Select Asset to Tag:            │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🔍 Search assets...        │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ✅ Dell Laptop - Marketing │ │
│ │    ID: 456 | DL123456789   │ │
│ │    📍 Floor 2 | 👤 Jane S.  │ │
│ │    Status: Active          │ │
│ ├─────────────────────────────┤ │
│ │ ○ MacBook Pro - Development│ │
│ │    ID: 457 | MBP987654321  │ │
│ │    📍 Floor 3 | 👤 John D.  │ │
│ │    Status: Active          │ │
│ ├─────────────────────────────┤ │
│ │ ○ ThinkPad X1 - Finance    │ │
│ │    ID: 458 | TP111222333   │ │
│ │    📍 Floor 1 | 👤 Sarah M. │ │
│ │    Status: Active          │ │
│ └─────────────────────────────┘ │
│                                 │
│ Selected Asset:                 │
│ 💻 Dell Laptop - Marketing     │
│ UUID: 550e8400-e29b-41d4...    │
│                                 │
│      [Continue to Write]        │
│                                 │
└─────────────────────────────────┘
```

### 3. Mobile NFC Scan Screen (High-Fidelity)

```
┌─────────────────────────────────┐
│ ← Scan Asset              [?]   │
├─────────────────────────────────┤
│                                 │
│         Scan NFC Tag            │
│                                 │
│ ┌─────────────────────────────┐ │
│ │                             │ │
│ │           📱                │ │
│ │        ╭─────────╮          │ │
│ │        │   NFC   │          │ │
│ │        │ ▪ ▪ ▪ ▪ │ ← Pulse  │ │
│ │        ╰─────────╯          │ │
│ │                             │ │
│ │    Hold your phone near     │ │
│ │     the asset NFC tag       │ │
│ │                             │ │
│ │     🔵 Ready to scan        │ │
│ └─────────────────────────────┘ │
│                                 │
│      [Manual Entry Mode]        │
│                                 │
│ 💡 Tips for better scanning:    │
│ • Keep phone steady             │
│ • Move slowly over the tag      │
│ • Try different angles          │
│ • Ensure NFC is enabled         │
│                                 │
│ ✅ Last scan: 2 minutes ago     │
│                                 │
└─────────────────────────────────┘
```

### 3. Asset Detail Page (High-Fidelity)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ ≡ Asset Management System         🔔 Notifications    👤 John Doe ▼    [Logout] │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│ ← Back to Assets    Dell Laptop - Marketing      [✏️ Edit]  [⚙️ Actions ▼]     │
│                                                                                 │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ 🆔 UUID: 550e8400-e29b-41d4-a716-446655440000                              │ │
│ │ 🟢 Active | 😊 Good Condition | 💻 Laptops Category                         │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                 │
│ ┌─────────────────────────────┐ ┌─────────────────────────────────────────────┐ │
│ │ 📋 Basic Information        │ │ 📍 Location & Assignment                    │ │
│ │                             │ │                                             │ │
│ │ Name: Dell Laptop - Mktg    │ │ Current Location:                           │ │
│ │ Serial: DL123456789         │ │ 🏢 Marketing Office - Floor 2               │ │
│ │ Model: XPS 13               │ │ 📍 Desk #24, Window Side                    │ │
│ │ Manufacturer: Dell          │ │                                             │ │
│ │ Description: Dell XPS 13... │ │ Assigned To:                                │ │
│ │                             │ │ 👤 Jane Smith                               │ │
│ │ 💰 Financial Info:          │ │ 📧 jane.smith@company.com                   │ │
│ │ Purchase: $1,299.99 USD     │ │ 🏢 Marketing Department                     │ │
│ │ Date: May 15, 2023          │ │                                             │ │
│ │ Warranty: May 15, 2026      │ │ 🕐 Last Scanned:                            │ │
│ │                             │ │ 2 minutes ago by John Doe                   │ │
│ │ 🏷️ Tags: laptop, portable   │ │ 📱 Via NFC scan                             │ │
│ └─────────────────────────────┘ └─────────────────────────────────────────────┘ │
│                                                                                 │
│ 📊 Asset History & Activity                                   [View All →]     │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ 📱 Scanned at Marketing Office - Floor 2          2 minutes ago | John Doe  │ │
│ │ 📍 Location updated to Marketing Office - Floor 2  1 day ago    | Jane Smith│ │
│ │ 👤 Assigned to Jane Smith                          5 days ago   | Mike Admin│ │
│ │ ✨ Asset created and tagged                        2 months ago | Admin     │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                 │
│ 📎 Custom Fields                                                               │ │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ Asset Tag: ACME-LAP-001                                                     │ │
│ │ Department Budget Code: MKT-2024-001                                        │ │
│ │ Warranty Provider: Dell Premium Support                                     │ │
│ │ Last Maintenance: January 15, 2024                                          │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Component Library

### 1. Status Indicators

```
Asset Status:
🟢 Active     (Green)
🟡 Inactive   (Yellow)
🔧 Maintenance (Orange)
🔴 Disposed   (Red)
❓ Lost       (Gray)

Condition:
😍 Excellent  (Green)
😊 Good      (Light Green)
😐 Fair      (Yellow)
😟 Poor      (Orange)
💔 Broken    (Red)
```

### 2. Action Buttons

```
Primary Actions:
[+ Add Asset]     (Blue background, white text)
[📱 Scan]         (Orange background, white text)
[💾 Save]         (Green background, white text)

Secondary Actions:
[✏️ Edit]         (White background, blue border)
[📍 Locate]       (White background, gray border)
[🗑️ Delete]       (White background, red border)
```

### 3. Cards and Lists

```
Asset Card:
┌─────────────────────────────────┐
│ 💻 Dell Laptop - Marketing     │
│ 🆔 DL123...    📍 Floor 2      │
│ 👤 Jane Smith  🟢 Active       │
│ 🕐 2 min ago                   │
│               [👁️] [✏️] [⚙️]   │
└─────────────────────────────────┘

List Item:
┌─────────────────────────────────┐
│ 📱 [Asset Name]                │
│ └─ Status | Location | Time     │
└─────────────────────────────────┘
```

### 4. Form Elements

```
Text Input:
┌─────────────────────────────────┐
│ Asset Name                      │
│ [Dell Laptop - Marketing___]    │
└─────────────────────────────────┘

Dropdown:
┌─────────────────────────────────┐
│ Category                        │
│ [Laptops                    ▼]  │
└─────────────────────────────────┘

Date Picker:
┌─────────────────────────────────┐
│ Purchase Date                   │
│ [01/15/2024]            [📅]    │
└─────────────────────────────────┘
```

---

## Responsive Design Breakpoints

### Desktop (1200px+)
- Full dashboard with 4-column stats
- Sidebar navigation always visible
- Data tables with all columns visible
- Large asset detail cards side-by-side

### Tablet (768px - 1199px)
- 2-column stat cards
- Collapsible sidebar navigation
- Simplified data tables with priority columns
- Stacked asset detail sections

### Mobile (320px - 767px)
- Single-column layout
- Bottom navigation tabs
- Card-based asset lists
- Full-screen modals for details
- Touch-optimized buttons (minimum 48px)

---

## Accessibility Features

### WCAG 2.1 AA Compliance
1. **Color Contrast**: Minimum 4.5:1 ratio for normal text
2. **Focus Indicators**: Clear keyboard navigation
3. **Alt Text**: All icons and images have descriptive text
4. **Screen Reader**: Semantic HTML and ARIA labels
5. **Keyboard Navigation**: All functions accessible via keyboard

### Mobile Accessibility
1. **Touch Targets**: Minimum 48px clickable areas
2. **Voice Control**: Support for voice commands
3. **High Contrast**: Support for system high contrast mode
4. **Text Scaling**: Support for system text size settings

### Universal Design
1. **Simple Language**: Clear, concise interface text
2. **Consistent Navigation**: Predictable interaction patterns
3. **Error Prevention**: Clear validation and confirmation
4. **Help Context**: Inline help and tooltips available

---

## Animation and Interactions

### Micro-Interactions
1. **Button Hover**: Subtle scale and color change
2. **Loading States**: Skeleton screens and progress indicators
3. **Success/Error**: Toast notifications with icons
4. **Scan Animation**: Pulsing NFC indicator
5. **Data Refresh**: Smooth loading animations

### Page Transitions
1. **Navigation**: Slide transitions between sections
2. **Modal**: Fade in/out with backdrop
3. **Asset Details**: Expand from list item
4. **Form Submission**: Progress indication

### Performance Considerations
1. **60fps Target**: All animations run smoothly
2. **Reduced Motion**: Respect user preferences
3. **Battery Optimization**: Minimize animations on low battery
4. **Network Aware**: Reduce animations on slow connections

---

## Implementation Guidelines

### Development Handoff
1. **Design Tokens**: Centralized color, spacing, typography
2. **Component Specs**: Detailed component documentation
3. **Interaction Specs**: Hover states, animations, transitions
4. **Asset Library**: SVG icons, images at multiple resolutions

### Testing Requirements
1. **Cross-Browser**: Chrome, Firefox, Safari, Edge
2. **Device Testing**: iOS/Android across different screen sizes
3. **Accessibility Testing**: Screen readers, keyboard navigation
4. **Performance Testing**: Load times, animation smoothness

### Future Enhancements
1. **Dark Mode**: Alternative color scheme
2. **Customization**: User-selectable themes
3. **Advanced Animations**: Lottie animations for complex interactions
4. **AR Integration**: Augmented reality for asset identification

---

**UI/UX Design Status**: ✅ COMPLETE  
**Total Screens**: 15+ wireframes and mockups  
**Design System**: Complete with colors, typography, components  
**Responsive**: Mobile-first with 3 breakpoints  
**Accessibility**: WCAG 2.1 AA compliant  

*These wireframes and mockups provide a comprehensive foundation for development and will be refined during user testing and implementation.*
