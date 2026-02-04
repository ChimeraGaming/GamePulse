# GamePulse

![Release](https://img.shields.io/github/v/release/ChimeraGaming/GamePulse?include_prereleases)
![Build](https://img.shields.io/badge/build-Android_Studio-blue)
![License](https://img.shields.io/github/license/ChimeraGaming/GamePulse)
![Issues](https://img.shields.io/github/issues/ChimeraGaming/GamePulse)

Battery and performance monitor for Android gaming.

GamePulse provides clear insights into how individual games impact battery drain, RAM usage, and overall device performance. The interface is designed for simplicity, fast scanning, and consistent data tracking across gaming sessions.

---

## Overview
GamePulse tracks gameplay performance in real time and organizes battery, RAM, and playtime metrics into a readable system. Sort, compare, and analyze games to understand how each title behaves on your device.

---

## Project Links
Issues: https://github.com/ChimeraGaming/GamePulse/issues  
Releases: https://github.com/ChimeraGaming/GamePulse/releases  
Changelog: https://github.com/ChimeraGaming/GamePulse/blob/main/CHANGELOG.md  
License: https://github.com/ChimeraGaming/GamePulse/blob/main/LICENSE  

---

## Features

### Game Collection
Each tracked game includes:
- Battery drain per hour
- RAM usage
- Total playtime
- Platform tag (Android or emulator)

Sorting options:
- Battery Best First
- Battery Worst First
- RAM Lowest First
- RAM Highest First
- Alphabetical

### Battery Monitoring
Live battery observations with adjustable polling and update intervals.

### RAM Monitoring
Real time memory usage while games are active.

### Battery Test
A controlled drain test that measures runtime performance and thermal behavior.

### App Themes
Theme presets that adjust the UI layout and visual style.

---

## Installation (Development Build)

Requirements:
- Android Studio
- Android device or emulator

Steps:
1. Clone the repository: git clone https://github.com/ChimeraGaming/GamePulse.git
2. Open the folder in Android Studio
3. Allow Gradle to sync
4. Select a device or emulator
5. Run the build using Android Studio

---

## Development Status and Roadmap
This project is currently in beta. Upcoming improvements include:
- Full code cleanup and structure refinements for v1.0
- Optimized battery and RAM polling
- Expanded game metadata collection
- Optional overlay or HUD monitoring tools
- Exportable performance summaries
- Additional themes and visuals

---

## Screenshots

Free Movement Overlay

<img width="286" height="163" alt="Overlay" src="https://github.com/user-attachments/assets/bf36b554-fb57-4d08-9f87-01c760f9682d" />

Monitoring

<img width="274" height="293" alt="Monitoring" src="https://github.com/user-attachments/assets/1a04b1bc-35a4-4ed2-94c0-92ee86621539" />

HUD

<img width="1013" height="311" alt="HUD" src="https://github.com/user-attachments/assets/cc2e5dd1-5ee4-4b35-8c83-b912d5594b79" />

Theme Example

<img width="289" height="516" alt="Rainbow_SNES" src="https://github.com/user-attachments/assets/9b71eb5c-36d2-4aec-a5c7-e28b149106fe" />

Game Collection (Coming in v.0.4)

<img width="359" height="800" alt="Game_Collection" src="https://github.com/user-attachments/assets/1582e926-d075-46da-9ccf-92905ebff84c" />

---

## Disclaimer
> ⚠️ This is my first Android app, so all code sections still contain personal notes and temporary structures. These will be cleaned during the v1.0 refactor.
