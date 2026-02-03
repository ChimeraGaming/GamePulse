> This is a test of GitHub's New CoPilot Assist to see if it's as smart as coders.
> The entirety fo this HUD Tracker was built using only Github Copilot with directions from me ChimeraGaming and slight adjustments to make it actually work.
> It was not 100% on its own and threw several errors but, at the end of the day it was able to successfully build an app to assist others who may not have basic knowledge of coding

# HUDTracker

HUDTracker is an Android app designed to monitor battery voltage, RAM usage, and estimate battery life based on usage patterns. It features real-time updates every 10 or 30 seconds and includes a 1-hour tracking mode to analyze battery consumption. Built using Kotlin and Material Design 3 principles.

## Features

### 1. Battery Monitoring
- **Real-time Voltage Display**: Shows current battery voltage in volts
- **Battery Level**: Displays current battery percentage
- **Battery Status**: Shows charging/discharging state
- **Battery Health**: Monitors battery health status
- **Temperature**: Displays battery temperature
- **Life Estimation**: Estimates remaining battery life based on voltage drop over time
- **Configurable Update Interval**: Choose between 10 or 30-second updates

### 2. RAM Monitoring
- **Current RAM Usage**: Displays used RAM vs. total RAM
- **Usage Percentage**: Shows RAM utilization as a percentage
- **Visual Progress Bar**: Easy-to-read visual representation of RAM usage
- **Real-time Updates**: Synchronized with battery monitoring interval

### 3. 1-Hour Battery Analysis
- **Button-triggered Analysis**: Start comprehensive 1-hour battery consumption tracking
- **Voltage Drop Analysis**: Tracks voltage changes over the analysis period
- **Battery Drain Calculation**: Measures exact battery percentage consumed
- **Drain Rate Estimation**: Calculates drain per hour for future predictions
- **Progress Tracking**: Real-time progress updates during the analysis
- **Background Operation**: Runs as a foreground service with notifications

### 4. Performance Analytics
- **App Energy Usage Tracking**: Identifies top energy-consuming apps
- **Foreground Time Analysis**: Tracks screen-on time per app
- **CPU Usage Estimation**: Estimates CPU consumption by app
- **Energy Score Calculation**: Comprehensive energy impact scoring
- **Top 10 Apps Display**: Shows the most energy-intensive applications
- **Game Impact Analysis**: Special tracking for games (e.g., via Eden launcher)

### 5. Modern UI Design
- **Material Design 3**: Latest Material Design components and styling
- **Responsive ConstraintLayout**: Auto-resizing UI elements for different screen sizes
- **Card-based Layout**: Clean, organized information cards
- **Dark Mode Support**: Automatic theme switching
- **RecyclerView Lists**: Efficient display of app usage data
- **Custom Color Scheme**: Professional green-themed color palette

## Technical Architecture

### Components
- **MainActivity**: Main UI controller with ViewBinding
- **MainViewModel**: State management using Android ViewModel
- **BatteryMonitorService**: Foreground service for continuous battery monitoring
- **BatteryAnalysisService**: Service for 1-hour battery analysis
- **Data Models**: BatteryInfo, RAMInfo, AppEnergyUsage, BatteryAnalysisResult
- **Utility Classes**: BatteryUtils, RAMUtils, PerformanceUtils
- **RecyclerView Adapter**: AppUsageAdapter for displaying app energy usage

### Technologies
- **Language**: Kotlin
- **UI**: XML layouts with ViewBinding
- **Architecture**: MVVM (Model-View-ViewModel)
- **Async Operations**: Kotlin Coroutines and Flow
- **Lifecycle**: AndroidX Lifecycle components
- **Material Design**: Material Design 3 components
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

## Installation & Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK with API 34
- Gradle 8.0 or later

### Steps
1. **Clone the repository**:
   ```bash
   git clone https://github.com/ChimeraGaming/AndroidApps
   cd AndroidApps/HUDTracker
   ```

2. **Open in Android Studio**:
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `HUDTracker` folder
   - Click "OK"

3. **Sync Gradle**:
   - Android Studio will automatically sync Gradle
   - Wait for dependencies to download

4. **Build the project**:
   ```bash
   ./gradlew build
   ```

5. **Run on device/emulator**:
   - Connect an Android device or start an emulator
   - Click the "Run" button in Android Studio
   - Or use: `./gradlew installDebug`

## Permissions

The app requires the following permissions:

- **BATTERY_STATS**: Read battery statistics
- **ACCESS_NETWORK_STATE**: Monitor network connectivity
- **INTERNET**: For potential future features
- **FOREGROUND_SERVICE**: Run monitoring services in background
- **POST_NOTIFICATIONS**: Display service notifications (Android 13+)
- **WAKE_LOCK**: Keep monitoring active
- **PACKAGE_USAGE_STATS**: Track app usage for energy analysis (requires user grant)

### Granting Usage Stats Permission
The app will prompt you to grant Usage Access permission for app energy analysis:
1. Tap "Start 1-Hour Analysis"
2. When prompted, tap "Grant Permission"
3. Find "HUDTracker" in the list
4. Toggle the permission ON

## Usage Guide

### Basic Monitoring
1. Launch the app
2. Battery and RAM information updates automatically
3. Choose update interval (10 or 30 seconds) from the dropdown

### Battery Life Estimation
- The app requires at least two readings to estimate battery life
- Estimation improves over time as more voltage data is collected
- Works best when battery is actively draining (not charging)

### 1-Hour Analysis
1. Tap "Start 1-Hour Analysis"
2. Grant Usage Stats permission if prompted
3. Confirm the 1-hour tracking dialog
4. The app runs in the background with a notification
5. After 1 hour, view detailed results including:
   - Total battery drain percentage
   - Voltage drop
   - Estimated remaining battery life
   - Top energy-consuming apps
6. Tap "Stop Analysis" to end tracking early

### Interpreting Results
- **Energy Score**: Higher scores indicate more energy consumption
- **Drain Rate**: Shows battery % consumed per hour
- **Voltage Drop**: Normal drop is 0.001-0.003V per minute
- **App Impact**: Apps with longer screen time typically consume more energy

## Project Structure

```
HUDTracker/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/chimeragaming/hudtracker/
│           │   ├── model/
│           │   │   ├── AppEnergyUsage.kt
│           │   │   ├── BatteryAnalysisResult.kt
│           │   │   ├── BatteryInfo.kt
│           │   │   └── RAMInfo.kt
│           │   ├── service/
│           │   │   ├── BatteryAnalysisService.kt
│           │   │   └── BatteryMonitorService.kt
│           │   ├── ui/
│           │   │   ├── AppUsageAdapter.kt
│           │   │   └── MainActivity.kt
│           │   ├── utils/
│           │   │   ├── BatteryUtils.kt
│           │   │   ├── PerformanceUtils.kt
│           │   │   └── RAMUtils.kt
│           │   └── viewmodel/
│           │       └── MainViewModel.kt
│           └── res/
│               ├── drawable/
│               ├── layout/
│               │   ├── activity_main.xml
│               │   └── item_app_usage.xml
│               ├── mipmap-*/
│               ├── values/
│               │   ├── colors.xml
│               │   ├── strings.xml
│               │   └── themes.xml
│               └── xml/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── README.md
```

## Dependencies

Key dependencies used in this project:

- **AndroidX Core KTX**: 1.12.0
- **AppCompat**: 1.6.1
- **Material Design**: 1.11.0
- **ConstraintLayout**: 2.1.4
- **Lifecycle Components**: 2.7.0
- **Kotlin Coroutines**: 1.7.3
- **WorkManager**: 2.9.0

## Future Enhancements

Potential features for future releases:

- Historical data tracking and charts
- Battery health degradation analysis
- Export analysis results to CSV/JSON
- Widget for home screen monitoring
- Customizable alert thresholds
- Dark mode refinements
- Network usage tracking integration
- More detailed CPU analysis
- Battery optimization recommendations

## Troubleshooting

### App crashes on launch
- Ensure minimum SDK version (API 26) is met
- Check Android Studio logs for specific error
- Rebuild project: Build > Clean Project > Rebuild Project

### Battery estimation shows 0
- Wait for at least 2 monitoring cycles (20-60 seconds)
- Ensure battery is draining (not charging)
- Check if battery voltage is changing

### Usage stats not showing
- Grant Usage Access permission in Settings
- Restart the app after granting permission
- Ensure apps have been used during the analysis period

### Service notification not appearing
- Grant notification permission (Android 13+)
- Check if battery optimization is disabled for HUDTracker
- Verify foreground service is running in Settings > Apps

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Commit changes: `git commit -am 'Add feature'`
4. Push to branch: `git push origin feature-name`
5. Submit a pull request

## License

This project is licensed under the MIT License. See the LICENSE file for details.

## Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Contact: ChimeraGaming on GitHub

## Acknowledgments

- Material Design 3 guidelines by Google
- Android Jetpack libraries
- Kotlin Coroutines documentation
