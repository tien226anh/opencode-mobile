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

## OpenCode Server API (from source code research)

### Authentication
- HTTP Basic Auth: username "opencode", password from OPENCODE_SERVER_PASSWORD env var
- Optional auth_token query param converted to Authorization header
- Mobile clients pass Basic Auth in headers when configured

### Core Endpoints
- GET/POST /session - List/create sessions
- GET /session/{id} - Get session details
- POST /session/{id}/message - Send chat message (body: SessionChatParams)
- GET /session/{id}/message - List messages for session
- POST /session/{id}/init - Initialize session
- POST /session/{id}/abort - Abort session
- POST /session/{id}/revert - Undo (body: {messageID, partID?})
- POST /session/{id}/unrevert - Redo
- POST /session/{id}/share, DELETE /session/{id}/share - Share/unshare
- GET /project, GET /project/current - Project info
- GET /config, PATCH /config - Config management
- GET /global/health - Health check
- GET /global/event - SSE stream for global events

### Streaming
- SSE at /global/event is the primary streaming mechanism
- Mobile clients connect with optional Basic Auth headers
- Events include session updates, message updates, etc.

### SessionChatParams (request body)
```json
{
  "modelID": "string",
  "parts": [{"type": "text", "text": "message"}],
  "providerID": "string",
  "messageID": "string (optional)",
  "mode": "string (optional)",
  "system": "string (optional)",
  "tools": {"toolName": true/false (optional)}
}
```

### Session Model
- Fields: id, slug, projectID, workspaceID?, directory, parentID?, summary, share, title, version, time, permission?, revert?

### Message Model
- Fields: id, role ("user"|"assistant"), parts[]
- Part types: TextPart, ReasoningPart, ToolInvocationPart, SourceUrlPart, FilePart, StepStartPart

### Project Model
- Fields: id, worktree, vcs, name, icon, commands, time, sandboxes