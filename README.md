# Home Item Manager

[中文](#chinese) | [English](#english)

<a id="chinese"></a>
## 中文

### 1. 应用简介
Home Item Manager 是一个 Android 家庭物品管理应用，面向个人与家庭场景，帮助你清晰记录“家里有什么”。  
应用坚持离线优先，核心能力在无网络时也可使用。

### 2. 当前已实现
- 分类管理：内置默认分类（Electronics），支持重命名、归档、排序。
- 物品管理：新增、编辑、详情查看、软删除与恢复。
- 图片管理：支持拍照与图库导入，物品图预览与详情展示。
- 列表能力：支持搜索、分类筛选、排序、已删项切换。
- 备份能力：本地导出、整包覆盖导入，设置页支持共享目录选择与备份文件导入入口。

### 3. 最近完成（Step 17A）
- `Item Detail`：图片区保持在顶部，多图改为横向滑动，减少纵向空间挤占。
- `Item Edit`：
  - 图片入口合并为 `Take Photo` + `Pick From Library`，并统一按钮尺寸。
  - `Purchase Date` 支持宽松输入并保存为 `YYYY-MM-DD`。
  - `Purchase Currency` 改为下拉（10 种常用货币，`USD/CNY` 置顶，默认 `USD`）。
  - `Purchase Price` 保存时统一两位小数，且允许 `0`（仅限制不能小于 `0`）。
  - `Custom Attributes` 的 `Remove` 按钮显示修复，底部 `Refresh` 已移除（保留 TopBar Overflow 的 Refresh）。

### 4. 当前状态
- 当前阶段：V0 Step 17A 已完成。
- 验证状态：自动化测试通过（真机 `connectedAndroidTest` 51/51），且你已确认手机手工验证通过。
- 进度策略：按你的要求，当前暂停，不进入下一步开发，等待新指令。

### 5. 最后更新
最后更新：2026-03-15

### 6. 了解更多
- 产品范围与路线图：[memory-bank/design-document.md](memory-bank/design-document.md)
- 执行进度：[memory-bank/progress.md](memory-bank/progress.md)
- 架构说明：[memory-bank/architecture.md](memory-bank/architecture.md)
- 备份格式：[memory-bank/BACKUP_FORMAT.md](memory-bank/BACKUP_FORMAT.md)
- 技术方向：[memory-bank/tech-stack.md](memory-bank/tech-stack.md)
- 分步计划：[memory-bank/implementation-plan.md](memory-bank/implementation-plan.md)

---

<a id="english"></a>
## English

### 1. App Overview
Home Item Manager is an Android app for personal and family item tracking.  
It is offline-first, so core flows work even without internet.

### 2. Implemented Features
- Category management with a default Electronics category, rename/archive/reorder support.
- Item lifecycle: create, edit, detail view, soft delete, and restore.
- Photo flows: camera capture, library import, thumbnails, and detail display.
- List capabilities: search, category filter, sorting, and deleted-item toggle.
- Backup flows: local export, full-replace import, and shared-folder based import entry in Settings.

### 3. Recently Completed (Step 17A)
- `Item Detail`: photo section remains at the top; multi-photo display is now horizontal swipe.
- `Item Edit`:
  - Photo actions merged into `Take Photo` and `Pick From Library` with consistent button sizing.
  - `Purchase Date` accepts loose input and is normalized to `YYYY-MM-DD`.
  - `Purchase Currency` is now a dropdown (10 common currencies, `USD/CNY` first, default `USD`).
  - `Purchase Price` is normalized to two decimals and now allows `0` (must not be negative).
  - `Remove` button display in Custom Attributes was fixed; bottom `Refresh` was removed.

### 4. Current Status
- Current phase: V0 Step 17A completed.
- Validation: automated tests passed (`connectedAndroidTest` 51/51 on device), and mobile manual verification is also confirmed passed.
- Execution policy: paused here by request; no next step will start until new instructions.

### 5. Last Updated
Last updated: 2026-03-15

### 6. Learn More
- Product scope and roadmap: [memory-bank/design-document.md](memory-bank/design-document.md)
- Implementation status: [memory-bank/progress.md](memory-bank/progress.md)
- Architecture details: [memory-bank/architecture.md](memory-bank/architecture.md)
- Backup format: [memory-bank/BACKUP_FORMAT.md](memory-bank/BACKUP_FORMAT.md)
- Technical direction: [memory-bank/tech-stack.md](memory-bank/tech-stack.md)
- Step plan: [memory-bank/implementation-plan.md](memory-bank/implementation-plan.md)
