# OpenCode Mobile - Compose Multiplatform Implementation Plan

## Overview
Port OpenCode (the open-source AI coding agent) to mobile using Compose Multiplatform (Kotlin Multiplatform). The app connects to an OpenCode server instance (localhost:4096 or remote tunnel) and provides a touch-friendly mobile interface for chatting with AI, viewing sessions, managing tools/permissions, and reviewing code changes.

## Architecture
- **Client/Server**: Mobile app is a client connecting to OpenCode server via HTTP API
- **Shared UI**: Compose Multiplatform sharing ~95% code between Android + iOS
- **Navigation**: Decompose (native iOS swipe-back, lifecycle control)
- **State**: ViewModel + StateFlow (unidirectional data flow)
- **Networking**: Ktor HTTP client + Kotlinx Serialization
- **DI**: Manual dependency injection (factory functions)

## Target Platforms
- Android (API 26+, JDK 17)
- iOS (13+, 64-bit only)

## Tech Stack
| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 2.2.20 |
| UI Framework | Compose Multiplatform | 1.10.3 |
| Navigation | Decompose | 3.3.0 |
| Networking | Ktor | 3.1.3 |
| Serialization | Kotlinx Serialization | 1.8.1 |
| Image Loading | Coil 3 | 3.0.4 |
| UI Components | Material3 | (via compose.material3) |
| Lifecycle | AndroidX Lifecycle | 2.10.0 |
| Build | Gradle + Version Catalog | AGP 8.9.0 |
| Testing | Kotlin Test + Compose UI Test | (via compose.uiTest) |

---

## TODOs

### Phase 1: Project Foundation
- [x] T1: Scaffold KMP project structure with CMP (Android + iOS targets)
  - Acceptance Criteria: Project builds with `./gradlew assembleDebug` (Android) and opens in Xcode (iOS). `composeApp` module configured with all dependencies. Version catalog populated.
- [x] T2: Configure Gradle build system (version catalog, plugins, dependencies)
  - Acceptance Criteria: `libs.versions.toml` has all versions. Root and module `build.gradle.kts` files configured. `settings.gradle.kts` has correct repositories. `./gradlew tasks` succeeds.

### Phase 2: Core Data Layer
- [ ] T3: Implement domain models (Session, Message, ToolCall, Project, Config)
  - Acceptance Criteria: Serializable data classes in `commonMain/model/`. Models match OpenCode server API schema. Unit tests pass.
- [ ] T4: Implement networking layer (Ktor HTTP client, API endpoints, DTOs)
  - Acceptance Criteria: `OpenCodeApiClient` class with methods for sessions, messages, streaming. Platform-specific HTTP engines configured. Error handling. Unit tests with mock responses.

### Phase 3: Presentation Foundation
- [ ] T5: Implement theme and design system (colors, typography, components)
  - Acceptance Criteria: `AppTheme` composable with dark/light themes. OpenCode brand colors. Material3 components styled consistently. Typography scale defined.
- [ ] T6: Implement navigation graph (Decompose setup, routes, root component)
  - Acceptance Criteria: `RootComponent` with stack navigation. Routes defined for all screens. iOS swipe-back works. Deep link support structure in place.

### Phase 4: Core Screens
- [ ] T7: Implement connection/settings screen (server URL, API key configuration)
  - Acceptance Criteria: Form to enter server URL and optional auth. Connection test button. Settings persisted via DataStore. Works on both platforms.
- [ ] T8: Implement session list screen (browse, create, switch sessions)
  - Acceptance Criteria: List of sessions with project name, date, preview. Pull-to-refresh. Create new session FAB. Navigate to chat on tap. Empty state.
- [ ] T9: Implement chat screen (messages, streaming, input, tool results)
  - Acceptance Criteria: Message list with user/assistant messages. Streaming response display. Input bar with send button. Tool call details expandable. Auto-scroll to latest message. Markdown rendering for code blocks.
- [ ] T10: Implement tool result and diff viewer components
  - Acceptance Criteria: Tool call cards (file read, edit, bash). Inline diff view with added/removed highlighting. Expandable/collapsible details. Permission prompt dialog.

### Phase 5: Platform & Polish
- [ ] T11: Implement platform-specific code (expect/actual for platform info, sharing, haptics)
  - Acceptance Criteria: `PlatformInfo` expect/actual. Share functionality. Haptic feedback on interactions. Platform-specific navigation bar styling.
- [ ] T12: Add tests for shared logic and UI components
  - Acceptance Criteria: Unit tests for ViewModels and repositories. Compose UI tests for key screens. Test coverage for core flows. `./gradlew allTests` passes.

---

## Final Verification Wave
- [ ] F1: Oracle Review - Architecture & Code Quality
- [ ] F2: Oracle Review - Security & Error Handling
- [ ] F3: Hands-on QA - Build, Run, Navigate on Android
- [ ] F4: Code Quality Review - No AI slops, no stubs, no placeholders

---

## Project Structure

```
opencode-mobile/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/kotlin/
│   │   │   ├── App.kt                    # Root composable
│   │   │   ├── model/                    # Domain models
│   │   │   │   ├── Session.kt
│   │   │   │   ├── Message.kt
│   │   │   │   ├── ToolCall.kt
│   │   │   │   ├── Project.kt
│   │   │   │   └── ServerConfig.kt
│   │   │   ├── network/                 # API client layer
│   │   │   │   ├── OpenCodeApiClient.kt
│   │   │   │   ├── ApiEndpoints.kt
│   │   │   │   └── dto/                 # DTOs for API responses
│   │   │   ├── repository/              # Data repositories
│   │   │   │   ├── SessionRepository.kt
│   │   │   │   ├── MessageRepository.kt
│   │   │   │   └── SettingsRepository.kt
│   │   │   ├── viewmodel/              # ViewModels
│   │   │   │   ├── ChatViewModel.kt
│   │   │   │   ├── SessionListViewModel.kt
│   │   │   │   └── SettingsViewModel.kt
│   │   │   ├── ui/                      # Shared Compose UI
│   │   │   │   ├── screens/
│   │   │   │   │   ├── ChatScreen.kt
│   │   │   │   │   ├── SessionListScreen.kt
│   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   ├── components/
│   │   │   │   │   ├── MessageBubble.kt
│   │   │   │   │   ├── ChatInputBar.kt
│   │   │   │   │   ├── ToolResultCard.kt
│   │   │   │   │   ├── DiffViewer.kt
│   │   │   │   │   ├── SessionCard.kt
│   │   │   │   │   ├── PermissionDialog.kt
│   │   │   │   │   └── MarkdownText.kt
│   │   │   │   └── theme/
│   │   │   │       ├── Theme.kt
│   │   │   │       ├── Color.kt
│   │   │   │       └── Typography.kt
│   │   │   └── navigation/
│   │   │       ├── RootComponent.kt
│   │   │       └── Routes.kt
│   │   ├── androidMain/kotlin/
│   │   │   ├── Platform.android.kt
│   │   │   └── MainActivity.kt
│   │   ├── iosMain/kotlin/
│   │   │   └── Platform.ios.kt
│   │   ├── commonTest/kotlin/
│   │   │   ├── model/
│   │   │   ├── viewmodel/
│   │   │   └── ui/
│   │   └── ...
│   └── build.gradle.kts
├── iosApp/
│   └── iosApp/                          # Xcode project
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradlew / gradlew.bat
```