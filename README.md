# Home Item Manager

[中文](#chinese) | [English](#english)

<a id="chinese"></a>
## 中文

### 1. 应用简介
Home Item Manager 是一个 Android 家庭物品管理应用。
它面向个人和家庭场景，用来清楚记录“家里有什么”。
应用以离线优先为核心，没有网络也可以使用主要功能。

### 2. 已实现功能
- 创建和管理分类（包含默认“电子产品”分类）。
- 新增、编辑、查看物品信息（名称、日期、价格、地点、备注、标签等）。
- 为物品保存照片并查看缩略预览。
- 按关键词和分类搜索、筛选与排序物品。
- 软删除与恢复物品。
- 本地导出备份。
- 以“整包覆盖”方式导入备份。
- 在设置页选择共享备份目录、列出目录中的备份 zip，并支持目录导入或单文件导入。

### 3. 未来计划
- Step 15（尚未开始）：V0 最终验收与发布准备。
- V1：登录 + 低成本云备份/云恢复。
- V2：多设备同步 + 可选 AI 辅助。
- 后续扩展：支持 iOS 与 Web。

### 4. 当前开发状态
- 当前阶段：V0 Step 14A 与设置页紧凑化后续修正已完成。
- 最近进展：设置页已具备备份导出/导入完整入口。
- 验证状态：自动化回归测试通过（连接设备 43/43）。

### 5. 更新日期
更新日期：2026-03-08

### 6. 了解更多
- 产品范围与路线图：[memory-bank/design-document.md](memory-bank/design-document.md)
- 实施进度：[memory-bank/progress.md](memory-bank/progress.md)
- 架构说明：[memory-bank/architecture.md](memory-bank/architecture.md)
- 备份格式：[memory-bank/BACKUP_FORMAT.md](memory-bank/BACKUP_FORMAT.md)
- 技术方向：[memory-bank/tech-stack.md](memory-bank/tech-stack.md)
- 分步计划：[memory-bank/implementation-plan.md](memory-bank/implementation-plan.md)

---

<a id="english"></a>
## English

### 1. App at a Glance
Home Item Manager is an Android app that helps people keep a clear record of household items.
It is designed for personal and family use.
It works offline first, so you can still use core features without internet.

### 2. What You Can Do Now
- Create and manage categories (including a default Electronics category).
- Add, edit, and view item details (name, date, price, place, notes, tags).
- Save photos for items and view photo previews.
- Search and filter items by keyword, category, and sort options.
- Soft-delete and restore items when needed.
- Export backups locally.
- Import backups with full replace behavior.
- Choose a shared backup folder, list backup zip files, and import from folder or single file.

### 3. What’s Coming Next
- Step 15 (not started): final V0 acceptance and release preparation.
- V1: login + low-cost cloud backup/restore.
- V2: device sync + optional AI assistance.
- Future expansion: iOS and Web support.

### 4. Current Development Status
- Current phase: V0 Step 14A + compact settings UI follow-up completed.
- Recent result: backup export/import entry flow is available in Settings.
- Validation status: automated test regression passed (43/43 on connected Android device).

### 5. Last Updated
Last updated: 2026-03-08

### 6. Learn More
- Product and roadmap: [memory-bank/design-document.md](memory-bank/design-document.md)
- Implementation status: [memory-bank/progress.md](memory-bank/progress.md)
- Architecture details: [memory-bank/architecture.md](memory-bank/architecture.md)
- Backup format: [memory-bank/BACKUP_FORMAT.md](memory-bank/BACKUP_FORMAT.md)
- Technical direction: [memory-bank/tech-stack.md](memory-bank/tech-stack.md)
- Step plan: [memory-bank/implementation-plan.md](memory-bank/implementation-plan.md)
