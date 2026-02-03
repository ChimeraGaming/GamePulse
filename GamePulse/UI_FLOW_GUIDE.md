# HUDTracker UI Flow and Features

## Main Screen (Portrait Mode)
```
┌─────────────────────────────────────┐
│         HUDTracker                  │
│  Battery & Performance Monitor      │
│                                     │
│      [Enable HUD] Button            │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  Update Interval: [10 seconds ▼]   │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│     Battery Monitoring              │
│  Voltage: 4.20 V                    │
│  Battery Level: 85.0%               │
│  Status: Discharging                │
│  Health: Good                       │
│  Temperature: 28.5°C                │
│  Estimated Life: 5h 30m             │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│     RAM Monitoring                  │
│  RAM Usage: 5.12 GB                 │
│  Total RAM: 16 GB                   │
│  [████████░░░░░░░░] 32.0%          │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│     Battery Analysis                │
│  [Start Battery Test]               │
│  [Stop Analysis]                    │
└─────────────────────────────────────┘
```

## Battery Test Setup Dialog
```
┌─────────────────────────────────────┐
│    Battery Test Setup               │
├─────────────────────────────────────┤
│  Test Duration:                     │
│  [1 Hour            ▼]              │
│                                     │
│  Are you playing a game?            │
│  [Yes/No Toggle]                    │
│                                     │
│  (If Yes:)                          │
│  What System/Emulator?              │
│  [Eden Emulator           ]         │
│                                     │
│  What Game?                         │
│  [Mario Kart 8            ]         │
│                                     │
│        [Cancel]  [Start Test]       │
└─────────────────────────────────────┘
```

## Battery Test Results (with Game Info)
```
┌─────────────────────────────────────┐
│    Analysis Results                 │
├─────────────────────────────────────┤
│  Game: Mario Kart 8                 │
│  System: Eden Emulator              │
│                                     │
│  Duration: 1 Hour                   │
│  Battery Drain: 5.0%                │
│  Voltage Drop: 0.050 V              │
│  Est. Remaining Life: 17.0 hours    │
│  Drain Rate: 5.0%/hour              │
│                                     │
│  Top Energy-Consuming Apps          │
│  ┌─────────────────────────────┐   │
│  │ Eden Emulator    4.1%       │   │
│  │ Screen          0.8%        │   │
│  │ Android System  0.1%        │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

## HUD Overlay (Landscape Mode)
```
┌───────────────────────────────────────────────────────┐
│ RAM: 5.12/16 GB                          [⚙️] [X]    │
│ ●●●●●○○○○○○○○○○○                                     │
├───────────────────────────────────────────────────────┤
│ Battery - 4500 mAh | Health: Good                     │
│ Status: Discharging | Voltage: 4.2V                   │
│                                                        │
│           Estimated Life: 5h 30m                      │ ← 2x font
└───────────────────────────────────────────────────────┘
```

## HUD Settings Dialog
```
┌─────────────────────────────────────┐
│       HUD Settings                  │
├─────────────────────────────────────┤
│  Refresh Rate:                      │
│  [10 Seconds        ▼]              │
│                                     │
│  RAM Theme:                         │
│  [Power Cores       ▼]              │
│   • Power Cores (●●●●●○○○○○)      │
│   • Heart Containers (♥♥♥♥♥♡♡♡♡♡) │
│   • Diamonds (◆◆◆◆◆◇◇◇◇◇)        │
│   • Hexagons (⬢⬢⬢⬢⬢⬡⬡⬡⬡⬡)      │
│   • Progress Bar (████░░ 32%)      │
│   • Off                             │
│                                     │
│  Battery Theme:                     │
│  [Stats Panel       ▼]              │
│   • Stats Panel (default)           │
│   • Power Cell (visual battery)     │
│   • Gauge (circular meter)          │
│   • Minimal (life only)             │
│   • Off                             │
│                                     │
│  Note: Both themes cannot be Off    │
│                                     │
│        [Cancel]  [Save]             │
└─────────────────────────────────────┘
```

## RAM Theme Examples

### Power Cores (Default)
```
●●●●●○○○○○○○○○○○
```

### Heart Containers (Zelda Style)
```
♥♥♥♥♥♡♡♡♡♡♡♡♡♡♡♡
```

### Diamonds
```
◆◆◆◆◆◇◇◇◇◇◇◇◇◇◇◇
```

### Hexagons
```
⬢⬢⬢⬢⬢⬡⬡⬡⬡⬡⬡⬡⬡⬡⬡⬡
```

### Progress Bar
```
████████░░░░░░░░ 32%
```

## Battery Theme Examples

### Stats Panel (Default)
```
Battery - 4500 mAh | Health: Good
Status: Discharging | Voltage: 4.2V

    Estimated Life: 5h 30m
```

### Power Cell
```
┌─────────────┐
│             │
│     85%     │  ← Color: Green if >25%, Yellow if ≤25%
│             │
└─────────────┘
```

### Gauge
```
     85%
   5h 30m
```

### Minimal
```
     5h 30m     ← Large text only
```

## Color Scheme (GitHub Dark)
- **Background**: #0d1117 (Dark charcoal)
- **Cards**: #161b22 (Slightly lighter)
- **Accent**: #58a6ff (Bright blue)
- **Text**: #c9d1d9 (Light gray)
- **Border**: #30363d (Subtle gray)
- **Success**: #3fb950 (Green)
- **Warning**: #f9c513 (Yellow)
- **Error**: #f85149 (Red)

## Key Features

### 1. Flexible Battery Testing
- Test durations: 5 minutes, 10 minutes, 1 hour, 2 hours
- Optional game tracking for gaming performance analysis
- Detailed results with per-app energy consumption

### 2. Customizable HUD Overlay
- Landscape mode for gaming
- 6 RAM visualization themes
- 5 Battery visualization themes
- Configurable refresh rates (1s, 10s, 1min)

### 3. Theme Persistence
- Settings saved across app restarts
- User preferences stored in SharedPreferences
- Intelligent validation (both themes can't be "Off")

### 4. Battery Optimization
- Efficient monitoring with minimal overhead
- Configurable update intervals
- Low-priority foreground services
- Smart battery life estimation

### 5. Gaming Focus
- Optimized for AYN Odin Thor
- Dark theme reduces eye strain
- High contrast for outdoor visibility
- Game-specific battery tracking

## Navigation Flow
```
MainActivity (Portrait)
    │
    ├─→ [Enable HUD] → HudOverlayActivity (Landscape)
    │                       │
    │                       └─→ [⚙️] → HudSettingsDialog
    │                       └─→ [X] → Back to MainActivity
    │
    └─→ [Start Battery Test] → BatteryTestSetupDialog
                                    │
                                    └─→ [Start Test] → Battery Analysis Running
                                                           │
                                                           └─→ Results Displayed
```

## Technical Architecture
- **Language**: Kotlin
- **UI**: Material Design 3 with dark theme
- **Architecture**: MVVM (Model-View-ViewModel)
- **Async**: Coroutines with StateFlow
- **Services**: Foreground services for monitoring
- **Persistence**: SharedPreferences
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
