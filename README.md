# OpenCode Mobile

A Kotlin Multiplatform mobile client for [OpenCode](https://github.com/sst/opencode), built with Compose Multiplatform for Android and iOS.

## Features

- **Cross-platform** — Shared Kotlin codebase targeting Android & iOS via Compose Multiplatform
- **Session management** — Browse, create, and manage OpenCode sessions
- **Real-time chat** — Interact with AI assistants through a conversational UI
- **Settings** — Configure server URL, API keys, and preferences
- **Markdown rendering** — Rich message display with syntax highlighting
- **Diff viewer** — Visualize code changes inline in chat

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Compose Multiplatform (Material 3) |
| Navigation | Decompose |
| Networking | Ktor |
| Serialization | kotlinx.serialization |
| Image loading | Coil 3 |
| Architecture | MVVM + Repository pattern |

## Project Structure

```
composeApp/src/
├── commonMain/kotlin/ai/opencode/mobile/
│   ├── model/          # Data models (ApiModels, Message, Project, Session)
│   ├── navigation/     # Decompose navigation (Config, RootComponent)
│   ├── network/        # Ktor API client (OpenCodeApiClient)
│   ├── platform/       # Platform abstraction (PlatformInfo)
│   ├── repository/     # Data repositories (SessionRepository, SettingsRepository)
│   ├── ui/
│   │   ├── components/ # Reusable UI (DiffViewer, MarkdownText, ToolResultCard)
│   │   ├── screens/    # Screens (ChatScreen, SessionListScreen, SettingsScreen)
│   │   └── theme/      # Theme, colors, typography
│   └── viewmodel/      # ViewModels (ChatViewModel, SessionListViewModel, SettingsViewModel)
├── androidMain/         # Android-specific implementations
├── iosMain/             # iOS-specific implementations
└── commonTest/          # Shared unit tests
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Xcode 15+ (for iOS)
- JDK 17+

### Build & Run

**Android:**

```bash
./gradlew :composeApp:assembleDebug
```

Open the project in Android Studio and run on an emulator or device.

**iOS:**

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

Open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.

### Running Tests

```bash
./gradlew :composeApp:allTests
```

## Configuration

On first launch, configure the OpenCode server URL in **Settings** to point to your running OpenCode instance.

## License

This project is proprietary software.