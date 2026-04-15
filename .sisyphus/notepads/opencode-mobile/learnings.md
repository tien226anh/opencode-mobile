# OpenCode Mobile - Learnings & Conventions

## OpenCode Architecture
- Client/Server architecture: TUI is just one client, server runs on port 4096 (Hono HTTP)
- SDK auto-generated from OpenAPI spec (`@opencode-ai/sdk`)
- Existing mobile ports: WhisperCode (Tauri wrap), doza62/opencode-mobile (React Native), georgi/opencode-mobile (Expo)
- Core features: Multi-session, chat, tool calls, diffs, undo/redo, plan/build agents, MCP, plugins

## Compose Multiplatform (CMP) Conventions
- Latest stable: 1.10.3 (based on Jetpack Compose 1.10.5)
- Kotlin 2.2.20 recommended
- AGP 8.9.0
- Lifecycle 2.10.0, Navigation 2.9.2
- ViewModel is multiplatform since lifecycle 2.8+
- `viewModel()` requires factory lambda in common code (no reflection on non-JVM)
- iOS ViewModels DON'T survive rotation - persist important state
- Compose Compiler version MUST match Kotlin version

## Navigation Decision
- Using **Decompose** for navigation (handles iOS swipe-back natively, full lifecycle control)
- Voyager is simpler but has iOS swipe-back issues
- Official Navigation 3 is alpha on non-Android

## Key Gotchas
- iOS Accessibility crash (EXC_BAD_ACCESS) - race condition in VoiceOver
- Popup/Dialog shadows can't draw outside bounds on iOS - use `platformLayers = false`
- No Apple x86_64 support in CMP 1.11.0+
- Android common tests must run as instrumented tests, not local unit tests

## Tech Stack
- Kotlin 2.2.20
- Compose Multiplatform 1.10.3
- Decompose for navigation
- Ktor for networking (HTTP client to OpenCode server)
- Kotlinx Serialization for JSON
- Coil 3 for image loading
- Material3 for UI components