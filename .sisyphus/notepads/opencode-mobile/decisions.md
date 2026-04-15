# OpenCode Mobile - Architectural Decisions

## 2026-04-15 Initial Architecture Decision

### Navigation: Decompose
- **Decision**: Use Decompose for navigation
- **Rationale**: Native iOS swipe-back support, full lifecycle control, production-proven
- **Alternatives considered**: Voyager (simpler but iOS swipe-back landmine), Navigation 3 (alpha on non-Android)

### State Management: ViewModel + StateFlow
- **Decision**: Unidirectional data flow with ViewModel + MutableStateFlow exposed as StateFlow
- **Rationale**: Standard Android pattern now multiplatform, testable, survives navigation
- **Pattern**: Single state object per screen (never scatter multiple StateFlows)

### Networking: Ktor + Kotlinx Serialization
- **Decision**: Use Ktor HTTP client for API communication with OpenCode server
- **Rationale**: KMP-native, supports all platforms, OkHttp engine on Android, Darwin engine on iOS
- **API**: Connect to OpenCode server (localhost:4096 or remote tunnel)

### Architecture: Clean Architecture Lite
- **Decision**: 3-layer architecture (presentation/domain/data) in shared module
- **Rationale**: Separation of concerns, testable, but not over-engineered for MVP
- **Layers**: 
  - `ui/` - Compose screens, components, theme
  - `viewmodel/` - ViewModels, state holders
  - `repository/` - Data repositories
  - `network/` - API client, DTOs
  - `model/` - Domain models

### UI Design: Material3 with custom theme
- **Decision**: Material3 as base with custom OpenCode-themed color scheme
- **Rationale**: Production-ready, accessible, theming support
- **Theme**: Dark-first (matches OpenCode's terminal roots), with light mode support

### Dependency Injection: Manual DI
- **Decision**: Manual dependency injection (no Koin/Kodein for MVP)
- **Rationale**: Simpler, fewer dependencies, sufficient for current scope
- **Pattern**: Factory functions passing dependencies through constructors