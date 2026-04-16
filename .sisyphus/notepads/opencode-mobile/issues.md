# OpenCode Mobile - Issues & Gotchas

## 2026-04-16 Build Issues & Fixes

- `luminance()` on Compose Color is NOT available in CMP common code. Use manual RGB calculation: `0.2126f * red + 0.7152f * green + 0.0722f * blue`
- `LocalConfiguration` is Android-specific, NOT available in CMP common. Remove unused imports.
- `System.currentTimeMillis()` is NOT available in Kotlin/Native (iOS). Use counter-based IDs or `kotlinx.datetime.Clock` instead.
- `TestDispatcher()` constructor is internal in `kotlinx-coroutines-test`. Use `StandardTestDispatcher()` or `UnconfinedTestDispatcher()` instead.
- Kotlin/Native does NOT support `private` top-level declarations in some contexts. Use `internal` for test fakes.
- `OpenCodeApiClient` is a concrete class (not an interface), so you can't create a fake implementing it for repository tests. Use `FakeSessionRepository` implementing `SessionRepository` interface instead.
- `ServerConfig` is NOT `@Serializable` — don't try to serialize it in tests.
- Session model uses `TimeInfo` (not `SessionTimeInfo`).
- Subagent delegation consistently fails (7/7 timeouts across `deep`, `quick`, `visual-engineering` categories) — direct implementation was necessary.
- `visual-engineering` category fails with "Model not found: google/gemini-3.1-pro-preview"
- Oracle reviewer fails with "Model not found: opencode/claude-opus-4-6"
- There's a spurious `NUL` file that causes `git add -A` to fail; use selective `git add` instead