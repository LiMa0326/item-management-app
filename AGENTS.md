# Repository Guidelines

## Always (始终应用)
- Enforce modular, multi-file implementation. Split features by responsibility (for example: UI, state, domain, data, backup, photo, network). Do not place a whole feature in one giant file.
- 禁止单体巨文件（monolith）。禁止把页面、状态、业务逻辑、数据访问、网络请求全部堆在单一文件中。
- # 重要提示：
- # 写任何代码前必须完整阅读 memory-bank/@architecture.md（包含完整数据库结构）
- # 写任何代码前必须完整阅读 memory-bank/@design-document.md
- # 每完成一个重大功能或里程碑后，必须更新 memory-bank/@architecture.md
- If `memory-bank/@architecture.md` or `memory-bank/@design-document.md` is missing, stop coding and create/restore the required document first.

## Project Structure & Module Organization
This repository is currently documentation-first. Core files are under `memory-bank/`:
- `memory-bank/design-document.md`: product scope, milestones (V0/V1/V2), and architecture direction.
- `memory-bank/tech-stack.md`: implementation recommendations (Android-first, offline-first, optional cloud/AI).
- `memory-bank/BACKUP_FORMAT.md`: canonical backup package spec (`manifest.json`, `data.json`, optional `photos/`).

Android project path:
- `apps/ItemManagementAndroid/`: Android Studio project root
- `apps/ItemManagementAndroid/app/`: application module
- `apps/ItemManagementAndroid/app/src/main/`: main source/resources
- `apps/ItemManagementAndroid/app/src/test/`: unit tests
- `apps/ItemManagementAndroid/app/src/androidTest/`: instrumented/integration tests

## Build, Test, and Development Commands
Run commands from `apps/ItemManagementAndroid/`:
- `.\gradlew.bat build` (Windows): compile and package.
- `.\gradlew.bat test`: run unit tests.
- `.\gradlew.bat connectedAndroidTest`: run device/emulator integration tests.

If you introduce another toolchain, document the exact local commands in this file.

## Coding Style & Naming Conventions
- Prefer Kotlin + layered architecture: `UI -> UseCase -> Repository -> Local DB/File`.
- Indentation: 4 spaces; UTF-8; keep files and APIs platform-neutral.
- Naming: `PascalCase` for classes, `camelCase` for fields/functions, `SCREAMING_SNAKE_CASE` for constants.
- Data contracts must stay compatible with `memory-bank/BACKUP_FORMAT.md`; unknown fields should be safely ignored on import.

## Recommended Practices (Non-Always)
- State management: keep unidirectional data flow (`UI State + Event + ViewModel`), avoid business logic in composables/views.
- Network: use repository boundary + timeout/retry/backoff + explicit error mapping; keep offline-first behavior as default.
- Persistence: store canonical data locally first, then sync/upload as optional flows; keep schema migrations explicit and reversible.
- Feature boundaries: one feature should be split across multiple focused files (screen, state, use cases, repository, models, tests).
- Performance: use thumbnail-first image rendering, lazy lists, and indexed search queries before introducing complex search engines.

## Testing Guidelines
- Focus first on Repository CRUD, search behavior, backup export/import, and soft-delete recovery.
- Test files: `*Test` for unit tests, `*IntegrationTest` for integration flows.
- Add at least one regression test for each bug fix that touches data, backup, or sync behavior.

## Commit & Pull Request Guidelines
Git history is not available in this workspace, so adopt this convention:
- Commit format: `type(scope): summary` (example: `feat(backup): add checksum validation`).
- Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.
- PRs should include: goal, key changes, test evidence, and doc updates if contracts changed.
- Link related issue/task IDs and include screenshots for UI changes.

## Security & Configuration Tips
- Treat cloud sync/AI as opt-in features; preserve offline-first behavior.
- Avoid committing secrets, tokens, or real user data.
- Keep backup/version fields explicit (`formatVersion`, `schemaVersion`) and document breaking changes before merge.
