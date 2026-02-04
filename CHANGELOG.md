# Changelog

<details>
<summary><strong>v0.3 Latest Major</strong></summary>

<details>
<summary><strong>v0.3.2 Latest Patch</strong></summary>

### UI and UX Improvements
- Sticky badges now stay visible during scrolling
- Bottom of screen no longer cuts off the Themes section
- Badges are clickable
  - Version badge copies the GitHub link to clipboard
  - Changelog badge opens CHANGELOG.md
  - Issues badge opens the GitHub Issues page
- Full rotation support added in all four directions

### Theme System Overhaul
- Themes now initialize before setContentView in all activities
- Theme selections persist across app restarts
- Theme renames
  - Gaming Neon changed to Reactor Neon
  - Retro Console changed to Neon Ember
- New themes added
  - SNES
  - SNES Rainbow
    - 🟦 X Button Blue `#3A66FF` Battery Monitoring
    - 🟩 Y Button Green `#3CB44A` RAM Monitoring
    - 🟨 B Button Yellow `#E6C32F` Battery Test
    - 🟥 A Button Red `#D93A3A` App Themes
- Cyberpunk theme updated with neon yellow `#FFE600`
- Total themes 8
- Added applySNESRainbowColors function for section header color application

### Battery Improvements
- Battery estimation now shows correct states
  - Calculating when gathering data
  - N A when charging or full
  - Time estimate when discharging
- Improved estimation logic using voltage readings and time deltas
- Fixed notification icon using ic_lock_idle_charging
- Added estimation state tracking in BatteryInfo

### RAM Improvements
- RAM usage now calculated using Total minus Available
- Cache and buffer memory excluded from usage
- Added quarter heart display support for themed RAM indicators

### Crash Protection
- All activities now use nullable binding pattern with safe cleanup
- Added try catch blocks for critical calls
- Added isFinishing and isDestroyed checks to prevent crashes
- Added onConfigurationChanged handling
- Proper resource cleanup in onDestroy
- Activities protected
  - MainActivity
  - HudOverlayActivity
  - GameCollectionActivity
  - BatteryTestResultsActivity

### Activity Enhancements
- Themes now apply consistently across all activities
- Orientation changes fully supported for all screens
- Improved memory handling across UI components

### Technical Changes
- Updated BatteryMonitorService with corrected estimation logic
- Updated BatteryInfo with state tracking
- Updated BatteryUtils with voltage based estimation logic
- Updated RAMUtils with corrected calculation
- Updated RamThemeRenderer with quarter heart logic
- Updated ThemeManager with all new themes and corrected application
- Updated MainActivity with improved badge references and SNES Rainbow support
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
- Fixed TextView casting errors in collapsible sections
- Fixed screen bottom clipping
- Fixed theme color application failures
- Fixed battery estimation always showing zero
- Fixed theme persistence issues

### Version Updates
- Version Code 4
- Version Name 0.3.2

</details>

---

<details>
<summary><strong>v0.3.1</strong></summary>

### UI and UX Improvements
- Added clickable version badges at the top of the screen
- Fixed overlay toggle button positioning
- Improved button alignment and spacing across the UI
- Version number now automatically updates from build.gradle

### Battery and Performance Fixes
- Battery voltage now updates correctly
- Battery life estimation fixed
- Calculating indicator added for initial readings
- Battery life estimation requires minimum 5 seconds of data
- Added battery status detection during charging

### RAM Monitoring Fixes
- RAM totals fixed for 16 GB devices
- Accurate MB to GB conversion
- Improved RAM display in all themes

### Overlay Improvements
- Movement inversion fixed
- Fixed crashes caused during configuration changes
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
