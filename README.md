# OpenCode Mobile

A Kotlin Multiplatform mobile client for [OpenCode](https://github.com/sst/opencode), built with Compose Multiplatform for Android and iOS.

## Features

- **Cross-platform** — Shared Kotlin codebase targeting Android & iOS via Compose Multiplatform
- **Session management** — Browse, create, and manage OpenCode sessions
- **Real-time chat** — Interact with AI assistants through a conversational UI
- **Settings** — Configure server URL, username/password auth, and preferences
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

- JDK 17+ (set `org.gradle.java.home` in `gradle.properties`)
- Android Studio Hedgehog or later (for Android)
- Xcode 15+ (for iOS)

### JDK Configuration

If you have multiple Java versions, configure Gradle to use JDK 17 by setting `org.gradle.java.home` in `gradle.properties`:

```properties
org.gradle.java.home=C\\:\\Program Files\\Java\\jdk-17
```

### Build APK

**Debug build:**

```bash
./gradlew :composeApp:assembleDebug
```

**Release build (signed):**

A release keystore is included at `opencode-release.jks`. Build with:

```bash
./gradlew :composeApp:assembleRelease
```

The signed APK is output at:
```
composeApp/build/outputs/apk/release/composeApp-release.apk
```

**Generating a new release keystore** (if needed):

```bash
keytool -genkeypair -v \
  -keystore opencode-release.jks \
  -keyalg RSA -keysize 2048 -validity 9125 \
  -alias opencode \
  -storepass android -keypass android \
  -dname "CN=OpenCode, OU=Mobile, O=OpenCode, L=Saigon, ST=HoChiMinh, C=VN"
```

Then update `composeApp/build.gradle.kts` `signingConfigs` section with the new keystore details.

### Build iOS

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

Open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.

### Running Tests

```bash
./gradlew :composeApp:allTests
```

## Configuration

### App Settings

On first launch, configure the connection in **Settings**:

| Field | Description |
|---|---|
| Server URL | Your OpenCode server URL (e.g. `http://192.168.1.100:4096` or `https://your-tunnel.trycloudflare.com`) |
| Username | Optional. Basic Auth username (default: `opencode`) |
| Password | Optional. Basic Auth password |

Settings are persisted across app restarts (SharedPreferences on Android, NSUserDefaults on iOS).

### Server Authentication

To secure your OpenCode server with authentication, set the `OPENCODE_SERVER_PASSWORD` environment variable when starting the server:

```bash
OPENCODE_SERVER_PASSWORD=your-secret-password npx opencode serve
```

This enables HTTP Basic Auth on all API endpoints. The default username is `opencode`. Set a custom username with `OPENCODE_SERVER_USERNAME`:

```bash
OPENCODE_SERVER_PASSWORD=your-secret-password \
OPENCODE_SERVER_USERNAME=myuser \
npx opencode serve
```

If `OPENCODE_SERVER_PASSWORD` is not set, the server runs without authentication.

### Remote Access (Cloudflare Tunnel)

To connect to an OpenCode server behind a Cloudflare tunnel:

1. Start the OpenCode server: `OPENCODE_SERVER_PASSWORD=mypass npx opencode serve`
2. Expose via tunnel: `cloudflared tunnel --url http://localhost:4096`
3. In the app Settings, enter the tunnel URL (e.g. `https://random-word.trycloudflare.com`) and your credentials

## License

This project is proprietary software.