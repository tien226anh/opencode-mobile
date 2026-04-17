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

## OpenCode Server API (from source code research + community analysis 2025-04-17)

### TWO ARCHITECTURE PATTERNS IN THE WILD

**Pattern A: DIRECT client** (our app, opencode.nvim)
- Connect directly to the OpenCode server (Hono HTTP, default port 54321)
- Call REST API endpoints: /session, /event, /app, /mode, /config/providers
- Subscribe to SSE at GET /event for real-time updates
- Auth: Basic Auth header when server password is set

**Pattern B: Proxy/Bridge server** (CodeNomad, OpenChamber, OpenWork, kimaki)
- Runs its OWN server (Fastify/Hono) that proxies/bridges to OpenCode instances
- Spawns/manages OpenCode processes as child instances
- Example: CodeNomad's consumeStream() reads from http://127.0.0.1:{port}/global/event
- Example: OpenWork uses @opencode-ai/sdk/v2/client to connect + SSE
- Still calls OpenCode REST API internally

### Architecture Comparison
| Project | Stack | API Access | SSE Endpoint | Auth |
|---------|-------|-----------|-------------|------|
| Our app | KMP/Ktor | Direct REST | /event | Basic Auth |
| CodeNomad | Fastify+SolidJS | Own server→OpenCode | /global/event (internal) /api/events (UI) | Own auth layer |
| OpenChamber | Hono+React/Tauri | Own server→OpenCode | Proxied | UI password+tunnels |
| OpenWork | Tauri+React | Own server→OpenCode | Uses SDK | Managed |
| kimaki | Node CLI | @opencode-ai/sdk | SDK stream | Local process |
| opencode.nvim | Lua+curl | Direct REST | /event | Local (no auth) |
| ai-sdk-provider | TypeScript | @opencode-ai/sdk | SDK stream | SDK auth |
| opencode-obsidian | TypeScript | Embeds web view | N/A | --cors flag |

### KEY FINDING: SSE Event Types (official SDK)
Full list from opencode-sdk-js src/resources/event.ts:
- message.updated (properties: { info: Message })
- message.part.updated (properties: { part: Part })
- message.part.removed (properties: { messageID, partID })
- message.removed (properties: { messageID, sessionID })
- session.updated (properties: { info: Session })
- session.deleted (properties: { info: Session })
- session.idle (properties: { sessionID })
- session.error (properties: { error: ProviderAuthError|UnknownError|..., sessionID? })
- file.edited (properties: { file })
- file.watcher.updated (properties: { event: rename|change, file })
- permission.updated (properties: { id, metadata, sessionID, time, title })
- installation.updated, lsp.client.diagnostics, ide.installed, storage.write

### KEY FINDING: Our app is ALIGNED
- Our SSEEvent sealed class matches the official SDK types ✓
- Our API endpoints match the SDK (/session, /event, /app, /mode, /config/providers) ✓
- Our auth approach (Basic Auth) matches the server middleware ✓
- Missing compared to community: file.watcher.updated, lsp.client.diagnostics, ide.installed, storage.write, installation.updated — but these are not critical for mobile

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

### Streaming (SSE)
- SSE endpoint: GET /event (NOT /global/event)
  - Official SDK: `this._client.get('/event', { stream: true })` — confirmed in opencode-sdk-js
  - CodeNomad uses `/global/event` when connecting to OpenCode instances (their OWN server proxies at `/api/events`)
  - opencode.nvim uses `/event` — confirmed in `sse_subscribe()` method
- CRITICAL: Must use ByteReadChannel for real-time SSE, NOT bodyAsText() — which buffers everything
- Ktor 3.1.3 does NOT have built-in SSE client — must parse manually
- ByteReadChannel approach: read line-by-line using `readUTF8Line()` in a while loop
- SSE format: `event: type\ndata: json\n\n` with blank lines separating events
- Event types from official SDK (event.ts):
  - message.updated, message.part.updated, message.part.removed, message.removed
  - session.updated, session.deleted, session.idle, session.error
  - file.edited, file.watcher.updated, permission.updated
  - installation.updated, lsp.client.diagnostics, ide.installed, storage.write
- Our app currently uses `/event` — CORRECT per the official SDK
- opencode.nvim also includes: session.diff, session.heartbeat, server.connected, server.instance.disposed, permission.replied

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