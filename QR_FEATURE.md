# Project QR transparency

The existing Kotlin/Compose application now connects the Official and Public surfaces through the permanent `Project.id`. QR generation does not write to the repository or encode changing project information.

## Usage

1. Log in as an OFFICER and open **Generate Project QR** in the Official Workspace quick actions.
2. Select an existing project. The screen displays its name, permanent ID, municipality, department, and a 1024 × 1024 UTF-8 QR image with a four-module white quiet zone.
3. Select **Print QR Code**. Android PrintManager opens the native print interface with an A4-friendly page. Choose an available printer or Save as PDF. Cancel to return to the generator.
4. In the Public Surface, tap the highlighted middle **Scan** button. Google Code Scanner opens the phone camera, restricted to QR codes with auto-zoom enabled.
5. A matching ID opens the existing ProjectDetailsScreen using the latest repository record. The scanner destination is removed from the back stack; Back returns to the preceding public screen. Cancelling the scanner also returns there. Unrelated codes and unknown project IDs produce localized messages with a retry option.

The final bottom-bar order is **Home | Projects | Scan | Map | Complaints**. Scan uses a 56 dp circular Material 3 FloatingActionButton, a 32 dp icon, the theme's contrasting blue secondary color, and an English/Tamil label. Each of the five items gets equal horizontal space. My Complaints now keeps this bar visible for consistent navigation.

The citizen drawer includes **Profile** with a person icon. Both Profile and the preserved Saved Projects item open the existing Screen.CitizenProfile. No profile or project-details implementation was replaced.

## Existing access and storage

OfficerMyProjectsScreen currently exposes all registered projects to the OFFICER role; there is no per-project authorization filter there. QR selection preserves this existing scope and requires an active OFFICER account, including a fresh check before printing. No new municipality role or assignment policy was introduced.

CivicDataRepository remains an in-memory repository. Newly registered projects are not shared across devices and do not persist across process restarts. A printed QR identifies a project only if that exact project ID exists in the scanning device's current repository. A shared persistent backend would be needed for real cross-device deployment; no backend migration was performed.

Google Code Scanner requires compatible Google Play services and may need internet access for its initial scanner-module download. The manifest requests installation of the barcode UI module. The app does not request CAMERA permission: scanning is delegated to Google Play services. Camera/module/service failures show a retryable message. Native scanner UI follows device/Google Play services localization; app-owned labels, errors, and print pages follow the existing English/Tamil language setting.

## Files

New production Kotlin files:

- `app/src/main/java/com/example/civic/ui/screens/officer/ProjectQRGeneratorScreen.kt`
- `app/src/main/java/com/example/civic/ui/screens/citizen/ProjectQRScannerScreen.kt`
- `app/src/main/java/com/example/civic/ui/qr/ProjectQRUtils.kt`
- `app/src/main/java/com/example/civic/ui/qr/ProjectQRPrintHelper.kt`

New focused tests:

- `app/src/test/java/com/example/civic/ProjectQRTest.kt`
- `app/src/androidTest/java/com/example/civic/ProjectQRIntegrationTest.kt`

Existing files modified only for integration:

- `CivicBottomNavigation.kt`: replace Profile with the highlighted third Scan item.
- `CivicDrawer.kt`: add Profile; retain Saved Projects and all other items.
- `CivicNavHost.kt`: add two QR destinations, reuse ProjectDetails, remove scanner after success, and keep the bar visible on My Complaints.
- `Screen.kt`: add two QR routes.
- `OfficialDashboardScreen.kt`: add the officer quick action.
- `app/build.gradle.kts` and `gradle/libs.versions.toml`: add only ZXing core 3.5.3 and Google Play services code scanner 16.1.0.
- `AndroidManifest.xml`: request the Google barcode UI module installation.

Authentication, roles, registration, Project IDs, repository/model definitions, complaint workflows, progress reporting, conflicts, contractors, administrator features, handovers, and the existing details/profile screens were not edited.

## Verification

- Final command `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` succeeded. Both APKs compiled; lint reported zero errors (warnings remain for version/deprecation/resource advisories).
- All 15 JVM unit tests passed, including five QR tests. They verify stable exact payloads, high-resolution UTF-8 round-trip decoding, unrelated/unknown codes, current-record lookup without mutation, and officer access.
- All four device test cases passed on the connected Samsung SM-S928B (Android 16), including three new integration tests. They verify bottom-bar order, horizontal centering, larger Scan icon, all five navigation callbacks, removal of Profile from the bar, Profile/Saved Projects navigation, Tamil drawer labels, and decoding the rendered A4 PDF in both languages.
- The connected-test Gradle task nevertheless failed during post-test APK cleanup because the phone disconnected. The XML report contains zero test failures and zero test errors, plus the device-disconnection cleanup error. This is not a fully successful connected Gradle run.
- This workspace has no Git repository metadata, so a Git working-tree diff could not be produced. The changed integration points and new files were reviewed directly.

## Remaining device checks

The automated print test renders the same PDF writer used by PrintDocumentAdapter and decodes its QR. It does not test a physical printed sheet or complete the native print dialog.

- Open Generate Project QR through officer login; select each project; regenerate; verify the readable ID matches the payload.
- Use Print QR Code; verify native preview, Save as PDF, printer output, print cancellation, and physical-paper scanning.
- From both citizen login and guest flow, scan a real project QR and confirm current information appears in the existing details screen. Repeat the scan and check Back behavior.
- Test unrelated/empty/unknown QR content, scanner cancellation, unavailable camera, unavailable Google Play services, and first-use module installation/retry.
- Check the bar and drawer on small screens, landscape, and enlarged accessibility fonts in English and Tamil.
- Smoke-test the existing complaint submission, project registration, official dashboards, coordinator, contractor, administrator, conflict, progress, and handover flows. Existing JVM repository tests passed, but these full UI flows were not all exercised on a device.

Reports: `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/androidTests/connected/debug/index.html`, and `app/build/reports/lint-results-debug.html`.
