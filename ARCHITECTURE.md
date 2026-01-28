# Architecture Documentation

## 1. Module Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│                         :app                                │
│  ┌────────────────────────────────────────────────────┐    │
│  │ • MainActivity                                      │    │
│  │ • TemplateApplication (@HiltAndroidApp + Timber)   │    │
│  │ • AppNavigatorImpl                                  │    │
│  │ • AppNavGraph                                       │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
         │              │              │              │
         ▼              ▼              ▼              ▼
    ┌─────────┐   ┌──────────┐   ┌──────────┐   ┌──────────────┐
    │ :core:ui│   │:core:    │   │:core:    │   │:feature:     │
    │         │   │network   │   │navigation│   │videoplayer   │
    └─────────┘   └──────────┘   └──────────┘   └──────────────┘
```

---

## 2. VideoPlayer Feature - Clean Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                               │
│  ┌────────────────┐  ┌──────────────────────┐                      │
│  │VideoGallery    │  │VideoGalleryViewModel │                      │
│  │Screen.kt       │◄─│(@HiltViewModel)      │                      │
│  │• Coil thumbs   │  │• VideoGalleryUiState │                      │
│  │• Loading/Error │  └──────────────────────┘                      │
│  └────────────────┘           │                                     │
│                               │ collects Flow                       │
│  ┌────────────────┐  ┌──────────────────────┐                      │
│  │VideoPlayer     │  │VideoPlayerViewModel  │                      │
│  │Screen.kt       │◄─│(@HiltViewModel)      │                      │
│  │• ExoPlayer     │  │• PlayerUiState       │                      │
│  │• Gestures      │  │• Player controls     │                      │
│  └────────────────┘  └──────────────────────┘                      │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                │ calls
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                                   │
│  ┌────────────────────────┐     ┌──────────────────┐               │
│  │ VideoRepository        │     │ VideoItem        │               │
│  │ (interface)            │     │ (domain model)   │               │
│  │ • getAllVideos()       │     │ • id, uri, name  │               │
│  │ • getVideosInFolder()  │     │ • duration, size │               │
│  │ • refreshVideos()      │     │ • folderName     │               │
│  └────────────────────────┘     └──────────────────┘               │
└─────────────────────────────────────────────────────────────────────┘
                         ▲
                         │ implements
                         │
┌─────────────────────────────────────────────────────────────────────┐
│                      DATA LAYER                                     │
│  ┌──────────────────────────────┐                                  │
│  │ VideoRepositoryImpl          │                                  │
│  │ • Cache-first strategy       │                                  │
│  │ • Error handling with Timber │                                  │
│  │ • ApiResult wrapper          │                                  │
│  └──────────────────────────────┘                                  │
│           │                    │                                    │
│           ▼                    ▼                                    │
│  ┌─────────────────┐  ┌─────────────────┐                         │
│  │ Room Database   │  │ MediaStore      │                         │
│  │ • VideoEntity   │  │ • Query videos  │                         │
│  │ • VideoDao      │  │ • Get metadata  │                         │
│  │ • VideoDatabase │  └─────────────────┘                         │
│  └─────────────────┘                                               │
│           │                                                         │
│           ▼                                                         │
│  ┌─────────────────┐                                               │
│  │ VideoMapper     │                                               │
│  │ Entity ↔ Domain │                                               │
│  └─────────────────┘                                               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Data Flow (Gallery → Player)

```
┌──────────────┐
│ User opens   │
│ Gallery      │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ VideoGalleryScreen                                       │
│   • Requests permission                                  │
│   • Observes uiState via collectAsStateWithLifecycle()  │
└──────────────────────────────────────────────────────────┘
       │
       │ LaunchedEffect → viewModel.loadVideos()
       ▼
┌──────────────────────────────────────────────────────────┐
│ VideoGalleryViewModel                                    │
│   repository.getAllVideos().collect { result ->          │
│     when (result) {                                      │
│       Loading → show spinner                             │
│       Success → update folders map                       │
│       Error   → show error + retry                       │
│     }                                                    │
│   }                                                      │
└──────────────────────────────────────────────────────────┘
       │
       │ Flow<ApiResult<List<VideoItem>>>
       ▼
┌──────────────────────────────────────────────────────────┐
│ VideoRepositoryImpl                                      │
│                                                          │
│   emit(ApiResult.Loading)                               │
│                                                          │
│   // 1. Try cache first                                  │
│   val cached = videoDao.getAllVideos()                  │
│   if (cached.isNotEmpty()) {                            │
│     emit(ApiResult.Success(cached.toDomain()))          │
│   }                                                      │
│                                                          │
│   // 2. Refresh from MediaStore                         │
│   val fresh = fetchFromMediaStore()                     │
│   videoDao.insertAll(fresh.toEntity())                  │
│   emit(ApiResult.Success(fresh))                        │
└──────────────────────────────────────────────────────────┘
       │
       │ User clicks video
       ▼
┌──────────────────────────────────────────────────────────┐
│ Navigate to VideoPlayerScreen                            │
│   Route.VideoPlayer(startVideoId, folderName)           │
└──────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ VideoPlayerViewModel                                     │
│                                                          │
│   repository.getVideosInFolder(folderName).collect {    │
│     // Create ExoPlayer with playlist                    │
│     // Seek to selected video                            │
│   }                                                      │
└──────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ VideoPlayerScreen                                        │
│   • AndroidView with PlayerView                          │
│   • Shuffle/Next/Previous buttons                        │
│   • Gesture detection for seeking                        │
│   • Lifecycle-aware pause/resume                         │
└──────────────────────────────────────────────────────────┘
```

---

## 4. Dependency Injection (Hilt)

```
┌─────────────────────────────────────────────────────────────────┐
│                   @HiltAndroidApp                               │
│              TemplateApplication                                │
│              • Timber.plant(DebugTree())                       │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ generates
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Hilt Component Hierarchy                       │
│                                                                 │
│  SingletonComponent                                             │
│    ├─ NetworkModule                                             │
│    │   ├─ Retrofit                                              │
│    │   ├─ OkHttpClient                                          │
│    │   └─ Moshi                                                 │
│    │                                                             │
│    ├─ AppModule                                                 │
│    │   └─ AppNavigator → AppNavigatorImpl                       │
│    │                                                             │
│    └─ VideoPlayerModule                                         │
│        ├─ VideoDatabase (Room)                                  │
│        ├─ VideoDao                                              │
│        └─ VideoRepository → VideoRepositoryImpl                 │
│                                                                 │
│  ViewModelComponent                                             │
│    ├─ VideoGalleryViewModel                                     │
│    │   └─ VideoRepository                                       │
│    │                                                             │
│    └─ VideoPlayerViewModel                                      │
│        ├─ Application                                           │
│        └─ VideoRepository                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Room Database Schema

```
┌─────────────────────────────────────────────────────────────────┐
│                    VideoDatabase                                │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ TABLE: videos                                              │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │ id          │ LONG    │ PRIMARY KEY                       │ │
│  │ uri         │ TEXT    │ Content URI string                │ │
│  │ name        │ TEXT    │ Display name                      │ │
│  │ duration    │ LONG    │ Duration in milliseconds          │ │
│  │ size        │ LONG    │ File size in bytes                │ │
│  │ folderName  │ TEXT    │ Parent folder name                │ │
│  │ lastUpdated │ LONG    │ Cache timestamp                   │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  DAO Methods:                                                   │
│    • getAllVideos() → List<VideoEntity>                        │
│    • getVideosInFolder(folderName) → List<VideoEntity>         │
│    • insertAll(videos)                                          │
│    • clearAll()                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Player State Management

```
┌─────────────────────────────────────────────────────────────────┐
│                    PlayerUiState                                │
│                                                                 │
│  data class PlayerUiState(                                      │
│    val isLoading: Boolean = false,                              │
│    val error: String? = null,                                   │
│    val isShuffleEnabled: Boolean = false,                       │
│    val repeatMode: Int = REPEAT_MODE_OFF,                       │
│    val playbackSpeed: Float = 1.0f,                             │
│    val currentVideoTitle: String = "",                          │
│    val isPlaying: Boolean = false,                              │
│    val autoPlayEnabled: Boolean = true                          │
│  )                                                              │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ updated by
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Player.Listener                              │
│                                                                 │
│  onShuffleModeEnabledChanged(shuffleModeEnabled)               │
│  onRepeatModeChanged(repeatMode)                                │
│  onPlaybackParametersChanged(playbackParameters)                │
│  onIsPlayingChanged(isPlaying)                                  │
│  onMediaItemTransition(mediaItem, reason)                       │
│  onPlayerError(error)                                           │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ controls
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ViewModel Methods                            │
│                                                                 │
│  toggleShuffle()      → player.shuffleModeEnabled = !enabled   │
│  toggleRepeatMode()   → cycle OFF → ONE → ALL                  │
│  setPlaybackSpeed(f)  → player.setPlaybackSpeed(speed)         │
│  playNext()           → player.seekToNext()                    │
│  playPrevious()       → player.seekToPrevious()                │
│  seek(timeMs)         → player.seekTo(position)                │
│  togglePlayPause()    → pause() / play()                       │
│  toggleAutoPlay()     → toggle auto-play next video            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Error Handling Flow

```
┌──────────────┐
│ Operation    │
│ (Repository) │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ try {                                                    │
│   // Query MediaStore or Room                            │
│   emit(ApiResult.Success(data))                          │
│ } catch (e: SecurityException) {                         │
│   Timber.e(e, "Permission denied")                       │
│   emit(ApiResult.Error(e))                               │
│ } catch (e: Exception) {                                 │
│   Timber.e(e, "Failed to load")                          │
│   emit(ApiResult.Error(e))                               │
│ }                                                        │
└──────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ ViewModel collects result                                │
│                                                          │
│ when (result) {                                          │
│   ApiResult.Error → _uiState.update {                   │
│     it.copy(                                             │
│       isLoading = false,                                 │
│       error = exception.message                          │
│     )                                                    │
│   }                                                      │
│ }                                                        │
└──────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ UI shows error state                                     │
│                                                          │
│ if (uiState.error != null) {                            │
│   Column {                                               │
│     Text(uiState.error)                                  │
│     Button(onClick = { viewModel.retry() }) {            │
│       Text("Retry")                                      │
│     }                                                    │
│   }                                                      │
│ }                                                        │
└──────────────────────────────────────────────────────────┘
```

---

## Key Architecture Principles

### 1. **Separation of Concerns**
Each layer has a single responsibility:
- **Presentation**: UI rendering and user interaction
- **Domain**: Business logic and data contracts
- **Data**: Data access, caching, and external communication

### 2. **Dependency Inversion**
High-level modules (domain) don't depend on low-level modules (data).
Both depend on abstractions (interfaces).

### 3. **Single Source of Truth**
- UI state is managed in ViewModel using StateFlow
- Database is the single source of truth for cached data
- UI is a pure function of state

### 4. **Cache-First Strategy**
```
1. Emit Loading
2. Load from Room cache → Emit Success (if available)
3. Fetch fresh data from MediaStore
4. Update Room cache
5. Emit Success with fresh data
```

### 5. **Unidirectional Data Flow**
```
User Action → ViewModel → Repository → Data Source
                ↓
            StateFlow
                ↓
         UI Recomposition
```
