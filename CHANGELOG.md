# Changelog

<details>
<summary><strong>v0.3 (Latest)</strong></summary>

### UI and UX
- Major interface overhaul for clearer navigation and a more polished user experience.
- HUD mode redesigned for improved readability during gameplay.
- Added a floating game overlay that displays real time game metrics.

### Database and Telemetry
- Integrated Room database for persistent local storage.
- Added telemetry consent dialog with user controlled privacy settings.
- Removed KAPT and removed older telemetry modules.

### New Activities
- GameLibraryActivity (full game library view with leaderboards and tracking)
- GameDetailActivity (detailed analytics for individual games)
- BatteryTestResultsActivity (battery test result viewer with screenshot export)

### Monitoring
- Added app specific detection service for accurate performance tracking.

### Fixes and Refinements
- Fixed HUD color inconsistencies.
- Improved overlay behavior and general stability.
- Multiple additional bug fixes across the application.

</details>

---

<details>
<summary><strong>v0.2</strong></summary>

### UI Fixes
- Fixed battery life calculation issues.
- Improved layout behavior on AYN Odin Thor and similar devices.

### Permissions
- Added FOREGROUND_SERVICE_DATA_SYNC permission to the AndroidManifest.

### Bug Fixes
- Resolved compilation issue in MainActivity.kt caused by variable shadowing.

</details>

---

<details>
<summary><strong>v0.1</strong></summary>

### Initial Release
- First public release of HUDTracker.

### Core Features
- Initial project foundation and base structure.
- Basic HUD functionality.

### Visuals
- Added full dark theme support.

### Code Quality
- Replaced magic numbers with named constants.

</details>
