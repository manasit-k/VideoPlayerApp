# VideoPlayerApp - Modern Android Video Player

A production-ready Android video player application following **Modern Android Best Practices (2026)** with **Clean Architecture** and **Feature-based Modularization**.

## Features

- Browse videos from device storage grouped by folder
- Video thumbnails using Coil
- ExoPlayer-based video playback
- Gesture controls (horizontal drag to seek)
- Shuffle, repeat, auto-play, and playback speed controls
- Offline caching with Room database
- Error handling with retry functionality
- Timber logging for debugging

## Architecture

This project implements:
- **Clean Architecture** (Presentation → Domain → Data)
- **Full Modularization** (Feature-based modules)
- **SOLID Principles**
- **Dependency Inversion** (Domain layer defines interfaces)
- **Unidirectional Data Flow** (UDF with StateFlow)

## Module Structure

```
VideoPlayerApp/
├── app/                          # Application module
│   ├── di/                       # App-level DI
│   ├── navigation/               # Navigation implementation
│   ├── MainActivity.kt           # Main entry point
│   └── TemplateApplication.kt    # Hilt + Timber initialization
│
├── core/
│   ├── ui/                       # Shared UI components & theme
│   ├── network/                  # Networking layer (Retrofit, ApiResult)
│   └── navigation/               # Navigation contracts & routes
│
├── feature/
│   ├── dashboard/                # Dashboard feature
│   └── videoplayer/              # Video player feature
│       ├── presentation/         # UI + ViewModels
│       │   ├── VideoPlayerScreen.kt
│       │   ├── VideoPlayerViewModel.kt
│       │   └── gallery/
│       │       ├── VideoGalleryScreen.kt
│       │       └── VideoGalleryViewModel.kt
│       ├── domain/               # Business logic
│       │   ├── VideoItem.kt
│       │   └── repository/
│       │       └── VideoRepository.kt (interface)
│       ├── data/                 # Data layer
│       │   ├── local/            # Room database
│       │   │   ├── VideoEntity.kt
│       │   │   ├── VideoDao.kt
│       │   │   └── VideoDatabase.kt
│       │   ├── mapper/
│       │   │   └── VideoMapper.kt
│       │   └── repository/
│       │       └── VideoRepositoryImpl.kt
│       └── di/
│           └── VideoPlayerModule.kt
│
└── gradle/
    └── libs.versions.toml        # Version Catalog
```

## Technical Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin | 2.1.0 |
| UI | Jetpack Compose | BOM 2024.12.01 |
| Architecture | Clean Architecture + MVVM | - |
| DI | Hilt | 2.54 |
| Navigation | Type-safe Compose Navigation | 2.8.5 |
| Async | Coroutines + Flow | 1.9.0 |
| Networking | Retrofit + OkHttp | 2.11.0 |
| JSON | Moshi | 1.15.2 |
| Video Player | Media3 ExoPlayer | 1.2.0 |
| Database | Room | 2.6.1 |
| Image Loading | Coil | 2.5.0 |
| Logging | Timber | 5.0.1 |
| Min SDK | Android 7.0 | 24 |
| Target SDK | Android 15 | 35 |

## Getting Started

### Prerequisites
- Android Studio Ladybug | 2024.2.1 or newer
- JDK 17
- Android SDK 35

### Build the Project
```bash
./gradlew assembleDebug
```

### Run the App
```bash
./gradlew :app:installDebug
```

## Key Features Implementation

### 1. Video Gallery with Thumbnails
- Queries MediaStore for device videos
- Groups videos by folder
- Displays thumbnails using Coil's VideoFrameDecoder
- Shows duration and file size

### 2. Video Caching with Room
- Caches video metadata locally
- Loads from cache first, then refreshes from MediaStore
- Offline support for previously loaded videos

### 3. ExoPlayer Integration
- Full playback controls (play, pause, seek)
- Playlist support (plays all videos in folder)
- Shuffle mode
- Repeat modes (off, one, all)
- Auto-play next video
- Playback speed control
- Gesture-based seeking (horizontal swipe)

### 4. Error Handling
- ApiResult wrapper for all data operations
- Loading states with progress indicators
- Error states with retry buttons
- Timber logging for debugging

## Dependency Flow

```
Presentation → Domain ← Data
     ↓           ↓        ↓
   :app    :core:*    :core:network
```

## Data Flow

```
UI → ViewModel → Repository (interface) → RepositoryImpl → MediaStore/Room
                          ↓
                     StateFlow
                          ↓
                   Recomposition
```

## Permissions

The app requires:
- `READ_MEDIA_VIDEO` (Android 13+)
- `READ_EXTERNAL_STORAGE` (Android 12 and below)
- `INTERNET` (for potential URL video playback)

## License

This project is free to use for any purpose.
