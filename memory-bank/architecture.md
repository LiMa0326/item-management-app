# 架构文档（Architecture）

## 1. 文档目的与更新规则
- 本文件用于记录两类信息：
  1. 当前系统的结构化架构定义（尤其是数据库结构与备份映射）。
  2. 每次由 Agent/AI 新增或修改文件后的“文件职责说明”与架构洞察。
- 每完成一个 Step 或里程碑，必须在“架构洞察日志”追加一条记录。
- 若实现与文档冲突，优先级遵循：`design-document.md` > `BACKUP_FORMAT.md` > `tech-stack.md`。

## 2. 当前文档与文件职责
- `memory-bank/design-document.md`
  - 产品范围、V0/V1/V2 功能边界、数据模型抽象与验收目标。
- `memory-bank/implementation-plan.md`
  - 可执行分步计划（Step 01+），包含 DoD 与测试要求。
- `memory-bank/tech-stack.md`
  - 实现建议与技术选择基线（Android-first、offline-first）。
- `memory-bank/BACKUP_FORMAT.md`
  - 备份包契约（`manifest.json`、`data.json`、`photos/`、`checksums.json`）。
- `memory-bank/progress.md`
  - 执行进度、阻塞、下一步。
- `memory-bank/architecture.md`（本文件）
  - 架构总览、数据库结构、文件职责登记、架构洞察日志。
- `AGENTS.md`
  - 仓库级协作规则（分层、测试、文档更新约束）。

## 3. 目标代码分层（V0）
- 分层：`UI -> UseCase -> Repository -> Local DB/File`
- 模块边界（计划）：
  - `ui`: Screen、State、Event、ViewModel
  - `domain`: UseCase、实体校验规则
  - `data`: DAO、Repository 实现、数据库与迁移
  - `photo`: 原图/缩略图处理、EXIF 清理、路径管理
  - `backup`: 导出/导入、ZIP 组包、校验与版本兼容

## 4. 数据库结构（Schema v1）
> 说明：以下为 V0 基线结构，后续变更需追加 migration 记录。

### 4.1 `categories`
- `id` TEXT PRIMARY KEY
- `name` TEXT NOT NULL
- `sort_order` INTEGER NOT NULL DEFAULT 0
- `is_archived` INTEGER NOT NULL DEFAULT 0
- `is_system_default` INTEGER NOT NULL DEFAULT 0
- `created_at` TEXT NOT NULL
- `updated_at` TEXT NOT NULL

约束与规则：
- 系统默认分类（电子产品）`is_system_default=1`，不可删除。
- 默认分类允许重命名、归档、反归档。

索引建议：
- `idx_categories_sort_order` (`sort_order`)
- `idx_categories_archived` (`is_archived`)

### 4.2 `items`
- `id` TEXT PRIMARY KEY
- `category_id` TEXT NOT NULL REFERENCES `categories`(`id`)
- `name` TEXT NOT NULL
- `purchase_date` TEXT NULL      -- YYYY-MM-DD
- `purchase_price` REAL NULL
- `purchase_currency` TEXT NULL  -- e.g. USD
- `purchase_place` TEXT NULL
- `description` TEXT NULL
- `tags_json` TEXT NOT NULL DEFAULT '[]'                 -- string[]
- `custom_attributes_json` TEXT NOT NULL DEFAULT '{}'    -- object values: string|number|boolean
- `created_at` TEXT NOT NULL
- `updated_at` TEXT NOT NULL
- `deleted_at` TEXT NULL

约束与规则：
- `custom_attributes_json` 仅允许标量值（string/number/boolean）。
- 软删除使用 `deleted_at`，恢复时置空。

索引建议：
- `idx_items_category_id` (`category_id`)
- `idx_items_updated_at` (`updated_at`)
- `idx_items_purchase_date` (`purchase_date`)
- `idx_items_purchase_price` (`purchase_price`)
- `idx_items_deleted_at` (`deleted_at`)
- 搜索索引：`name`、`description`、`purchase_place`（tags 为整词匹配）

### 4.3 `item_photos`
- `id` TEXT PRIMARY KEY
- `item_id` TEXT NOT NULL REFERENCES `items`(`id`)
- `local_uri` TEXT NOT NULL
- `thumbnail_uri` TEXT NULL
- `content_type` TEXT NOT NULL        -- e.g. image/jpeg
- `width` INTEGER NULL
- `height` INTEGER NULL
- `created_at` TEXT NOT NULL

规则：
- 本地原图必须可追溯到 `local_uri`。
- 缩略图默认规格：长边 1280px、JPEG、质量 85、移除 EXIF。
- 备份文件名映射：
  - `full` -> `<photoId>.<ext>`
  - `thumbnails` -> `<photoId>_thumb.<ext>`

索引建议：
- `idx_item_photos_item_id` (`item_id`)

## 5. 备份与导入架构规则
- V0 本地导出默认 `full`；V1 云备份默认 `thumbnails`。
- V0 导入模式固定 `replace_all`：
  1. 自动创建本地快照（回滚点）。
  2. 清理本地核心表与本地照片文件（含孤儿文件）。
  3. 导入 `data.json`，未知字段忽略。
- `checksums.json` 策略：
  - V0：可不生成。
  - V1/V2：云备份默认生成并在恢复前校验（SHA-256）。

## 6. V0 已冻结行为决策
- 默认分类不可删除，仅可重命名/归档，且可反归档。
- 排序中购买日期/价格的空值统一放后。
- 搜索语义：大小写不敏感 + 子串匹配；tags 按整词匹配。
- V0 不强制单独回收站页；可通过“显示已删除”筛选 + 详情页恢复入口闭环。
- 1k 数据搜索在 V0 仅记录基线，不设硬阈值。

## 7. 架构洞察日志
- 2026-03-03
  - 建立 architecture 文档基线，补齐数据库结构、索引建议、备份导入策略。
  - 根据用户确认冻结关键决策：`purchaseCurrency`、默认导出模式、checksums 分阶段、搜索与排序语义。
  - 明确本文件作为“AI 生成文件职责登记 + 架构演进记录”的唯一入口。
- 2026-03-04
  - 完成 Step 01：建立 V0 工程骨架与模块边界，确保 `UI -> UseCase -> Repository -> Local DB/File` 分层目录可落地。
  - 用最小内存态导航（`AppRoute` + `AppNavigatorState`）替代模板单页，形成 6 页面壳可达链路。
  - 启动入口从模板 `Greeting` 迁移到 `ItemManagementApp`，为后续 Step 02+ 数据层接入预留稳定 UI 容器。
  - 新增启动冒烟测试并在设备端通过，验证 Step 01 的“可启动、可切换、不崩溃”目标。

## 8. Step 01 新增文件职责（2026-03-04）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 8.1 入口与应用壳
- `main/java/com/example/itemmanagementandroid/MainActivity.kt`
  - Android 入口 Activity；负责挂载主题与 `ItemManagementApp` 根 composable，不承载业务逻辑。
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - Step 01 的 UI 根壳；集中管理当前路由分发与页面壳装配，是后续接入 ViewModel/UseCase 的统一入口。

### 8.2 导航骨架
- `main/java/com/example/itemmanagementandroid/ui/navigation/AppRoute.kt`
  - 路由类型定义；固定 6 个页面壳（Home/Category/ItemList/ItemDetail/ItemEdit/Settings）。
- `main/java/com/example/itemmanagementandroid/ui/navigation/AppNavigatorState.kt`
  - 最小内存态导航状态容器；提供 `navigate`/`goBack`/`canGoBack`，通过栈实现页面切换。

### 8.3 页面壳（UI Screen Skeleton）
- `main/java/com/example/itemmanagementandroid/ui/screens/home/HomeScreen.kt`
  - 首页壳；展示空状态与到各页面壳的导航入口，作为手工验证起点。
- `main/java/com/example/itemmanagementandroid/ui/screens/category/CategoryScreen.kt`
  - 类别页壳；保留类别管理入口位与返回链路，不含数据实现。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreen.kt`
  - 物品列表页壳；保留列表能力入口位与详情/编辑跳转占位。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailScreen.kt`
  - 物品详情页壳；保留详情展示位与编辑跳转占位。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditScreen.kt`
  - 物品编辑页壳；保留保存流入口位（本步仅空状态 + 导航）。
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsScreen.kt`
  - 设置页壳；保留备份/账户/配置入口位（本步仅空状态 + 导航）。

### 8.4 分层边界占位（Module Marker）
- `main/java/com/example/itemmanagementandroid/ui/UiModuleMarker.kt`
  - UI 层边界标记，明确页面/状态逻辑归属。
- `main/java/com/example/itemmanagementandroid/domain/DomainModuleMarker.kt`
  - Domain 层边界标记，预留 UseCase 与业务规则实现位置。
- `main/java/com/example/itemmanagementandroid/data/DataModuleMarker.kt`
  - Data 层边界标记，预留 DAO/Repository/DB 访问实现位置。
- `main/java/com/example/itemmanagementandroid/backup/BackupModuleMarker.kt`
  - Backup 层边界标记，预留导入导出与 ZIP 组包实现位置。
- `main/java/com/example/itemmanagementandroid/photo/PhotoModuleMarker.kt`
  - Photo 层边界标记，预留图片存储、缩略图与 EXIF 处理实现位置。

### 8.5 测试文件
- `androidTest/java/com/example/itemmanagementandroid/StartupSmokeTest.kt`
  - 启动冒烟测试；断言应用启动到首页壳并显示空状态文案，用于保障 Step 01 回归稳定性。
