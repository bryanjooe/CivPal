# CivPal — Public Works Transparency & Coordination Portal

**CivPal** is a modern Android application designed to improve public works transparency, citizen complaint tracking, and inter-departmental coordination across municipal bodies in Tamil Nadu, India.

---

## 📌 Problem Statement

- **Lack of Public Transparency**: Citizens lack direct visibility into public works projects, budget allocations, contractor responsibilities, and completion timelines.
- **Unverified On-Site Progress**: Public infrastructure projects often experience unexplained delays without public explanations or verifiable progress evidence.
- **Inefficient Complaint Resolution**: Traditional citizen grievance mechanisms lack strict response deadlines, automated escalation, or real-time status tracking.
- **Inter-Departmental Coordination Silos**: Overlapping roadwork, water supply, and electrical infrastructure schedules between departments cause avoidable work conflicts, site damages, and project handoffs when officers are transferred.

---

## 💡 The Solution

CivPal provides a unified, transparent digital bridge between citizens and government officials:

1. **Public Surface & GIS Project Map**:
   - Explore public works projects across Tamil Nadu with real-time status badges, completion percentage, budget disclosures, and location details on interactive Google Maps.
2. **On-Site QR Code Verification**:
   - Scan QR code signage at project sites to instantly view verified project specifications, contractor details, and official progress updates without requiring an account.
3. **Citizen Complaint Tracking with 7-Day Response SLA**:
   - Submit public complaints with photo proof and location tagging. Complaints feature a mandatory **7-calendar-day response deadline** with automated escalation for overdue issues.
4. **Unified Official Workspace**:
   - Role-based access for **Government Officers**, **Department Coordinators**, **Contractors**, and **Administrators**.
   - Features a **Smart Action Inbox**, milestone progress verification, deadline extension requests, schedule conflict detection, and officer handover continuity.

---

## 🛠️ Open-Source Libraries & APIs Used

### 1. Backend & Data Layer
- **Supabase Kotlin SDK (`3.2.6`)**:
  - `auth-kt`: Email & Password authentication, password recovery, and anonymous identity management.
  - `postgrest-kt`: Real-time PostgreSQL querying, filtering, and RPC transaction execution.
  - `storage-kt`: Secure attachment management for site photos (`JPEG`, `PNG`, `WebP`) and official documents (`PDF`).
  - `functions-kt`: Edge function execution for administrator invitations.
- **Ktor Client OkHttp (`3.3.1`)**: Asynchronous HTTP engine powering network communications for Supabase services.

### 2. Maps & Location Services
- **Google Maps Compose SDK (`4.4.1`)**: Declarative Jetpack Compose wrapper for Google Maps rendering.
- **Google Play Services Maps (`19.0.0`)**: Vector tile rendering and interactive map markers (`Marker`) for project sites.
- **Google Play Services Location (`21.3.0`)**: Location provider services for spatial filtering.
- **Osmdroid (`6.1.18`)**: OpenStreetMap GIS tile provider (`TileSourceFactory.MAPNIK`).

### 3. QR Code Processing
- **Google ML Kit Barcode Scanning (`play-services-code-scanner`)**: On-device machine learning QR code scanner.
- **ZXing Core (`zxing:core`)**: Local QR code matrix generation for project signage printing.

### 4. UI Framework & Architecture
- **Jetpack Compose & Material 3 (`androidx.compose.bom`)**: Declarative UI toolkit with custom design tokens, 18dp rounded surfaces, and dark readable typography.
- **Navigation Compose (`androidx.navigation.compose`)**: Single-activity navigation routing.
- **AndroidX Browser (`1.8.0`)**: Custom Tabs integration for OAuth and authentication callbacks.
- **Java JDK Desugaring (`2.1.5`)**: Java 8+ API desugaring providing backward compatibility for Android 7.0+ (`minSdk 24`).

---

## 🚀 Setup & Build Instructions

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1+) or newer.
- **JDK**: Java 11 or Java 17.
- **Android SDK**: `compileSdk 35`, `minSdk 24`, `targetSdk 35`.

### 1. Clone the Repository
```bash
git clone https://github.com/bryanjooe/CivPal.git
cd CivPal
```

### 2. Configure Local Environment (`local.properties`)
Create or edit `local.properties` in the root directory:

```properties
sdk.dir=C:/Users/YOUR_USER/AppData/Local/Android/Sdk
SUPABASE_URL=https://wbczfllhlxyvziakznuy.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_vupxxicecI5o0ZgXeStK9A_45XlWbej
MAPS_API_KEY=AIzaSyBPAyyre0-E4EJAOZpZsUrm05_7ynoEm0s
```

### 3. Build & Run
- **Build Debug APK**:
  ```bash
  ./gradlew :app:assembleDebug
  ```
- **Run Unit Tests**:
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```

---

## 📄 License

This project is developed for public works transparency and government departmental coordination in Tamil Nadu.
