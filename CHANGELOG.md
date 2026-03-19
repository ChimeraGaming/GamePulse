# Changelog

<details>
<summary><strong>v0.3 Latest Major</strong></summary>

<details>
<summary><strong>v0.3.2.2 Latest Patch</strong></summary>

### Fixes
- Fixed Battery Testing screen scrolling issues
- Fixed text sizing inconsistencies in Battery Testing

### Version Updates
- Version Name 0.3.2.2

</details>

---

<details>
<summary><strong>v0.3.2.1</strong></summary>

### Notes
- Scraped release build

### Version Updates
- Version Name 0.3.2.1

</details>

---

<details>
<summary><strong>v0.3.2</strong></summary>

### UI and UX Improvements
- Sticky badges now remain visible during scrolling
- Fixed screen bottom clipping (Themes section fully visible)
- Badges are now interactive:
  - Version badge copies GitHub release link to clipboard
  - Changelog badge opens CHANGELOG.md
  - Issues badge opens GitHub Issues page
- Full rotation support added (all four orientations)

### Theme System Overhaul
- Themes now initialize before `setContentView` in all activities
- Theme selections persist across app restarts
- Theme renames:
  - Gaming Neon → Reactor Neon
  - Retro Console → Neon Ember
- New themes added:
  - SNES
  - SNES Rainbow
    - 🟦 X Button Blue `#3A66FF` Battery Monitoring
    - 🟩 Y Button Green `#3CB44A` RAM Monitoring
    - 🟨 B Button Yellow `#E6C32F` Battery Test
    - 🟥 A Button Red `#D93A3A` App Themes
- Cyberpunk theme enhanced with neon yellow `#FFE600`
- Total themes: 8
- Added `applySNESRainbowColors()` for dynamic section header styling

### Battery Improvements
- Battery estimation now reflects correct states:
  - Calculating when gathering data
  - N A when charging or full
  - Time estimate when discharging
- Improved estimation accuracy using voltage and time deltas
- Fixed notification icon using ic_lock_idle_charging
- Added estimation state tracking in BatteryInfo

### RAM Improvements
- RAM usage now calculated as Total minus Available
- Cache and buffer memory excluded from usage
- Added quarter heart rendering support for themed indicators

### Crash Protection
- Implemented nullable binding pattern across all activities
- Added try catch protection for critical operations
- Added isFinishing and isDestroyed safety checks
- Added onConfigurationChanged handling
- Ensured proper cleanup in onDestroy

**Protected Activities:**
- MainActivity
- HudOverlayActivity
- GameCollectionActivity
- BatteryTestResultsActivity

### Activity Enhancements
- Consistent theme application across all activities
- Full orientation support across all screens
- Improved memory handling and lifecycle stability

### Technical Changes
- Updated BatteryMonitorService with improved estimation logic
- Updated BatteryInfo with state tracking
- Updated BatteryUtils for voltage based calculations
- Updated RAMUtils with corrected usage logic
- Updated RamThemeRenderer with quarter heart support
- Updated ThemeManager with new themes and persistence fixes
- Updated MainActivity with badge actions and SNES Rainbow support
- Updated activity_main layout to keep badges visible during scroll
- Updated AndroidManifest to enable full rotation support
- Added applySNESRainbowColors function

### Theme Color Palettes

#### Cyberpunk Enhanced
- Primary `#FF0080`
- Secondary `#00D9FF`
- Tertiary `#FFE600`
- Background `#1E1E3F`
- Surface `#2A2A5A`

#### Reactor Neon
- Primary `#00FF41`
- Secondary `#39FF14`
- Background `#0A0E0A`
- Surface `#0D1F0D`

#### Neon Ember
- Primary `#FF6600`
- Secondary `#FFAA00`
- Background `#1A0F00`
- Surface `#2D1A00`

#### SNES
- Primary `#8B89C6`
- Secondary `#BABABA`
- Background `#2C2A3D`
- Surface `#3D3B52`

#### SNES Rainbow
- Battery Monitoring `#3A66FF`
- RAM Monitoring `#3CB44A`
- Battery Test `#E6C32F`
- App Themes `#D93A3A`

### Bug Fixes
- Fixed missing battery drawable
- Fixed version badge ID mismatch
- Fixed TextView casting issues in collapsible sections
- Fixed bottom screen clipping
- Fixed theme application inconsistencies
- Fixed battery estimation showing zero
- Fixed theme persistence issues

### Version Updates
- Version Code 4
- Version Name 0.3.2

</details>

---

<details>
<summary><strong>v0.3.1</strong></summary>

### UI and UX Improvements
- Added clickable version badges
- Fixed overlay toggle positioning
- Improved button alignment and spacing
- Version number now auto updates from build.gradle

### Battery and Performance Fixes
- Fixed battery voltage updates
- Fixed battery life estimation
- Added calculating indicator for initial readings
- Battery life estimation requires minimum 5 seconds of data
- Added battery status detection during charging

### RAM Monitoring Fixes
- Fixed RAM totals on 16GB devices
- Accurate MB to GB conversion
- Improved RAM display in all themes

### Overlay Improvements
- Movement inversion fixed
- Fixed crashes during configuration changes
- Fixed top screen crashes on dual screen devices
- Added crash safety checks for WindowManager
- Improved rotation handling and screen mode transitions

### Stability and Crash Protection
- Added service lifecycle protection
- Added isDestroyed and isAttachedToWindow checks
- Fixed overlay and HUD memory leaks
- Improved handler cleanup

### Technical Improvements
- Code cleanup
- Added Kotlin Parcelize
- Added BuildConfig versioning support
- Updated build.gradle to 0.3.1
- Improved error handling across services
- Improved dual screen configuration support

### Device Compatibility
- Fixed crashes on AYN Odin Thor Max
- Improved support for foldable and dual screen devices

</details>

---

<details>
<summary><strong>v0.3</strong></summary>

### UI and UX
- Major interface overhaul
- HUD redesigned for better readability
- Added floating game overlay for live metrics

### Database and Telemetry
- Added Room database
- Added telemetry consent dialog
- Removed KAPT and old telemetry modules

### New Activities
- GameLibraryActivity
- GameDetailActivity
- BatteryTestResultsActivity

### Monitoring
- Added app specific detection service

### Fixes and Refinements
- Fixed HUD color inconsistencies
- Improved overlay behavior
- General bug fixes

</details>

</details>

---

<details>
<summary><strong>v0.2</strong></summary>

### UI Fixes
- Fixed battery life calculation issues
- Improved layout behavior on AYN Odin Thor devices

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
- Base HUD system built

### Visuals
- Full dark theme included

### Code Quality
- Replaced magic numbers with constants

</details>
