# Changelog

<details>
<summary><strong>v0.3 (Latest Major)</strong></summary>

<details>
<summary><strong>v0.3.1 (Latest Patch)</strong></summary>

### UI and UX Improvements
- Added clickable version badges (Version, Changelog, Issues) at the top of main screen
- Fixed Enable Overlay toggle button positioning
- Improved button alignment and spacing across all screens
- Version number now auto-updates from build.gradle

### Battery & Performance Fixes
- Fixed battery voltage not updating on main page
- Fixed estimated battery life calculation
- Shows "Calculating..." while waiting for initial battery readings
- Battery life estimation requires minimum 5 seconds between readings
- Added battery status check (won't show estimate while charging)

### RAM Monitoring Fixes
- Fixed RAM displaying incorrect totals (16GB devices now show 16GB, not 12GB)
- Fixed RAM calculation from MB to GB conversions
- Improved RAM display accuracy across all themes

### Overlay Improvements
- Fixed overlay movement inversion
- Fixed overlay service crashes on configuration changes
- Fixed top screen crashes on dual-screen devices (Thor Max)
- Added crash protection for WindowManager operations
- Added null-safety checks throughout overlay service
- Overlay now properly handles rotation and screen changes

### Stability & Crash Protection
- Added service lifecycle protection
- Added isDestroyed / isAttachedToWindow guards
- Fixed memory leaks in overlay and HUD
- Improved handler cleanup

### Technical Improvements
- Code cleanup and refactoring
- Added Kotlin Parcelize support
- Enabled BuildConfig versioning
- Updated build.gradle to 0.3.1
- Improved error handling
- Added dual-screen configuration logic

### Device Compatibility
- Fixed crashes on AYN Odin Thor Max
- Improved support for foldable / dual-screen devices

</details>

---

<details>
<summary><strong>v0.3</strong></summary>

### UI and UX
- Major interface overhaul
- HUD redesign for readability
- New floating game overlay for real time metrics

### Database and Telemetry
- Added Room database
- Telemetry consent dialog with privacy controls
- Removed KAPT and old telemetry modules

### New Activities
- GameLibraryActivity
- GameDetailActivity
- BatteryTestResultsActivity

### Monitoring
- Added app specific detection service

### Fixes and Refinements
- Fixed HUD color issues
- Improved overlay stability
- General bug fixes

</details>

</details>

---

<details>
<summary><strong>v0.2</strong></summary>

### UI Fixes
- Fixed battery life calculation issues
- Improved layout behavior on AYN Odin Thor

### Permissions
- Added FOREGROUND_SERVICE_DATA_SYNC permission

### Bug Fixes
- Fixed MainActivity.kt compilation issue

</details>

---

<details>
<summary><strong>v0.1</strong></summary>

### Initial Release
- First public release of HUDTracker

### Core Features
- Base HUD system

### Visuals
- Full dark theme

### Code Quality
- Replaced magic numbers with constants

</details>
