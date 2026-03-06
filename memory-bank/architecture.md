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
- 2026-03-05
  - 完成 Step 02：落地 Room Schema v1（`categories`、`items`、`item_photos`），字段与约束对齐本文件第 4 章定义。
  - 在 `data/local/entity` 固化三类实体映射，统一 snake_case 列名与 ISO-8601 时间字符串存储策略。
  - 在 `data/local/db` 新增数据库契约、版本常量与迁移入口占位，冻结当前数据库版本为 `1`。
  - 新增 `DatabaseSchemaV1Test`，对三张核心表、关键 NOT NULL 约束与 `PRAGMA user_version` 做仪器测试校验。
  - 完成 Step 03：建立 Category 数据层闭环（DAO/Repository/UseCase），覆盖 `list/create/update/archive/reorder` 与默认分类初始化能力。
  - 在应用启动链路接入 `EnsureDefaultCategoryUseCase`，首启自动写入默认分类“电子产品”（`is_system_default=1`）。
  - 新增 `CategoryRepositoryImplTest`（JVM），校验默认分类幂等初始化、Category CRUD、归档过滤与 reorder 排序更新逻辑。
  - 完成 Step 04：建立 Item 数据层闭环（DAO/Repository/UseCase），覆盖 `list/get/create/update/softDelete/restore`。
  - 在 `ItemRepositoryImpl` 固化 `tags_json` 与 `custom_attributes_json` 的编码/解码策略，并通过 `org.json` 限制 `customAttributes` 值类型为 `string|number|boolean`。
  - 固化软删除幂等语义：删除仅写入 `deleted_at`，恢复仅清空 `deleted_at`，两者均更新 `updated_at`。
  - 新增 `ItemRepositoryImplTest`（JVM），覆盖 Item CRUD、软删除/恢复可见性、幂等行为与 JSON 字段往返校验。
- 2026-03-06
  - 完成 Step 05：建立 ItemPhoto 数据层闭环（DAO/Repository/UseCase），覆盖 `listByItem/get/add/remove/listDeferredCleanupCandidates`。
  - 在 `data/local/dao` 新增 `ItemPhotoDao` 与 `DeferredPhotoCleanupRow`，以 JOIN `items.deleted_at` 的方式派生“待延迟清理”候选，不改动 `schema v1`。
  - 在 `domain/model` 固化 `ItemPhoto`、`ItemPhotoDraft` 与 `DeferredPhotoCleanupCandidate`，并冻结 marker 常量 `ITEM_SOFT_DELETED`。
  - 新增 `PhotoRepositoryImpl`，统一执行字段归一化、宽高合法性校验（`> 0`）与删除幂等返回语义。
  - 在 `photo` 模块新增 `PhotoBackupFileNameMapper`，固化备份文件名映射与扩展名解析优先级（`contentType -> uri extension -> jpg`）。
  - 新增 `PhotoRepositoryImplTest` 与 `PhotoBackupFileNameMapperTest`，覆盖照片关联、删除幂等、延迟清理候选可见性与文件名映射规则。
  - 完成 Step 06：建立 UI 全局导航状态与页面级状态管理闭环，覆盖 6 页面 `ViewModel + UiState` 最小真实数据读取链路。
  - 将导航状态容器从 `AppNavigatorState` 迁移到 `AppNavigationViewModel + AppNavigationUiState`，统一提供 `navigate/goBack/canGoBack/currentRoute`。
  - 在 `ui/di` 新增 `AppDependencies`，集中装配并暴露 `ListCategoriesUseCase`、`ListItemsUseCase`、`GetItemUseCase`、`ListItemPhotosUseCase`，防止 UI 直连 DAO/Repository。
  - 在 `ui/viewmodel` 新增 `singleViewModelFactory`，统一页面 ViewModel 构建方式，保持非 Hilt 依赖注入路径可维护。
  - 重构 `ItemManagementApp` 与 6 个 `Screen`，统一 Composable 签名为“state + callbacks”，并将 Back 行为绑定 `canGoBack`。
  - 新增 `NavigationFlowIntegrationTest`（设备）与 `AppNavigationViewModelTest`（JVM），验证导航主链路与导航状态幂等逻辑。

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

## 9. Step 02 新增文件职责（2026-03-05）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 9.1 构建配置
- `app/build.gradle.kts`
  - 接入 Room 与 KSP；配置 Room schema 导出参数（`room.schemaLocation`）；补充 `room-testing` 供 `androidTest` 使用。
- `../build.gradle.kts`
  - 注册 KSP 根插件（`apply false`），允许 app 模块启用代码生成。
- `../../gradle/libs.versions.toml`
  - 新增 Room/KSP 版本与依赖别名，固定 Step 02 使用的构建契约。
- `../gradle.properties`
  - 增加 `android.disallowKotlinSourceSets=false` 兼容开关，解决 AGP 9 内建 Kotlin 与 KSP 的当前校验冲突。
- `app/schemas/.gitkeep`
  - 预留 Room schema 导出目录，保证路径稳定可追踪。

### 9.2 数据实体映射
- `main/java/com/example/itemmanagementandroid/data/local/entity/CategoryEntity.kt`
  - `categories` 表映射；包含默认分类标记列 `is_system_default` 与排序/归档索引。
- `main/java/com/example/itemmanagementandroid/data/local/entity/ItemEntity.kt`
  - `items` 表映射；覆盖 V0 核心字段（含 `purchase_currency`、`tags_json`、`custom_attributes_json`、`deleted_at`）与索引定义。
- `main/java/com/example/itemmanagementandroid/data/local/entity/ItemPhotoEntity.kt`
  - `item_photos` 表映射；维护物品与本地照片 URI、缩略图 URI、媒体类型与尺寸元数据。

### 9.3 数据库契约
- `main/java/com/example/itemmanagementandroid/data/local/db/ItemManagementDatabase.kt`
  - Room 数据库根定义；注册 3 个实体并固定 `version = 1`，提供数据库构建入口。
- `main/java/com/example/itemmanagementandroid/data/local/db/DatabaseVersions.kt`
  - 数据库版本常量定义（`SCHEMA_V1`、`CURRENT`）。
- `main/java/com/example/itemmanagementandroid/data/local/db/DatabaseMigrations.kt`
  - 迁移列表入口占位；为后续 schema 升级预留挂载点。

### 9.4 测试文件
- `androidTest/java/com/example/itemmanagementandroid/data/local/db/DatabaseSchemaV1Test.kt`
  - Schema v1 仪器测试：校验表存在性、字段集、NOT NULL 约束与数据库版本号。

## 10. Step 03 新增文件职责（2026-03-05）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 10.1 DAO 与数据库接入
- `main/java/com/example/itemmanagementandroid/data/local/dao/CategoryDao.kt`
  - Category 的 Room DAO；提供有序查询、按 ID 查询、默认分类计数、插入/更新与排序更新能力。
- `main/java/com/example/itemmanagementandroid/data/local/db/ItemManagementDatabase.kt`
  - 新增 `categoryDao()` 访问入口，允许 Repository 通过数据库契约接入 Category 表操作。
- `main/java/com/example/itemmanagementandroid/data/local/db/DatabaseProvider.kt`
  - 提供进程级数据库单例，避免 Activity 重建时重复创建 Room 实例。

### 10.2 Domain 与 Repository
- `main/java/com/example/itemmanagementandroid/domain/model/Category.kt`
  - Category 领域模型，作为 UseCase/UI 的稳定输入输出类型。
- `main/java/com/example/itemmanagementandroid/domain/model/DefaultCategories.kt`
  - 默认分类常量（`cat_electronics` / `电子产品`），集中管理默认值约束。
- `main/java/com/example/itemmanagementandroid/domain/repository/CategoryRepository.kt`
  - Category 仓储接口，定义 `list/create/update/setArchived/reorder/ensureDefaultCategory`。
- `main/java/com/example/itemmanagementandroid/data/repository/CategoryRepositoryImpl.kt`
  - Category 仓储实现；封装字段校验、时间戳写入、排序重排与默认分类幂等初始化逻辑。

### 10.3 UseCase 与应用启动初始化
- `main/java/com/example/itemmanagementandroid/domain/usecase/category/ListCategoriesUseCase.kt`
  - 类别列表查询入口（支持是否包含归档）。
- `main/java/com/example/itemmanagementandroid/domain/usecase/category/CreateCategoryUseCase.kt`
  - 新增类别入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/category/UpdateCategoryUseCase.kt`
  - 重命名类别入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/category/SetCategoryArchivedUseCase.kt`
  - 归档/反归档入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/category/ReorderCategoriesUseCase.kt`
  - 类别排序重排入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/category/EnsureDefaultCategoryUseCase.kt`
  - 默认分类初始化入口，用于首启写入系统默认类别。
- `main/java/com/example/itemmanagementandroid/MainActivity.kt`
  - 在 `onCreate` 中通过 `Dispatchers.IO` 调用默认分类初始化 UseCase，确保启动阶段完成默认分类落地。

### 10.4 测试文件
- `test/java/com/example/itemmanagementandroid/data/repository/CategoryRepositoryImplTest.kt`
  - Category 数据层单测；覆盖默认分类幂等初始化、Category CRUD + archive 行为、reorder 排序更新结果。

## 11. Step 04 新增文件职责（2026-03-05）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 11.1 DAO 与数据库接入
- `main/java/com/example/itemmanagementandroid/data/local/dao/ItemDao.kt`
  - Item 的 Room DAO；提供全量/未删除有序查询、按 ID 查询、插入与更新能力（按 `updated_at DESC, created_at DESC` 排序）。
- `main/java/com/example/itemmanagementandroid/data/local/db/ItemManagementDatabase.kt`
  - 新增 `itemDao()` 访问入口，使 Item Repository 可通过数据库契约访问 `items` 表。

### 11.2 Domain 与 Repository
- `main/java/com/example/itemmanagementandroid/domain/model/Item.kt`
  - Item 领域模型，统一 Item 数据层对外输出结构（含软删除字段 `deletedAt`）。
- `main/java/com/example/itemmanagementandroid/domain/model/ItemDraft.kt`
  - Item 创建/更新输入模型，承载可写字段集合。
- `main/java/com/example/itemmanagementandroid/domain/repository/ItemRepository.kt`
  - Item 仓储接口，定义 `list/get/create/update/softDelete/restore`。
- `main/java/com/example/itemmanagementandroid/data/repository/ItemRepositoryImpl.kt`
  - Item 仓储实现；封装名称/分类与 JSON 字段校验、UTC 时间戳写入、软删除与恢复幂等逻辑。
- `main/java/com/example/itemmanagementandroid/data/repository/json/ItemJsonCodec.kt`
  - Item JSON 编解码契约，隔离 `tags/customAttributes` 与存储字符串之间的转换细节。
- `main/java/com/example/itemmanagementandroid/data/repository/json/OrgJsonItemJsonCodec.kt`
  - 基于 `org.json` 的默认 JSON 编解码实现，用于生产路径下的 `tags/customAttributes` 序列化与反序列化。

### 11.3 UseCase
- `main/java/com/example/itemmanagementandroid/domain/usecase/item/ListItemsUseCase.kt`
  - Item 列表查询入口（支持是否包含软删除）。
- `main/java/com/example/itemmanagementandroid/domain/usecase/item/GetItemUseCase.kt`
  - 按 ID 获取单个 Item 入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/item/CreateItemUseCase.kt`
  - 新增 Item 入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/item/UpdateItemUseCase.kt`
  - 更新 Item 入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/item/SoftDeleteItemUseCase.kt`
  - 软删除 Item 入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/item/RestoreItemUseCase.kt`
  - 恢复 Item 入口。

### 11.4 测试文件
- `test/java/com/example/itemmanagementandroid/data/repository/ItemRepositoryImplTest.kt`
  - Item 数据层单测；覆盖 create/get/update、软删除/恢复、幂等行为与 `tags/customAttributes` JSON 往返及非法值校验。

## 12. Step 05 新增文件职责（2026-03-06）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 12.1 DAO 与数据库接入
- `main/java/com/example/itemmanagementandroid/data/local/dao/ItemPhotoDao.kt`
  - ItemPhoto 的 Room DAO；提供按 `item_id` 升序查询、按 `id` 查询、插入、按 `id` 删除与延迟清理候选查询能力。
- `main/java/com/example/itemmanagementandroid/data/local/dao/model/DeferredPhotoCleanupRow.kt`
  - 延迟清理候选 DAO 行映射；承载 `item_photos` 与 `items` JOIN 查询结果。
- `main/java/com/example/itemmanagementandroid/data/local/db/ItemManagementDatabase.kt`
  - 新增 `itemPhotoDao()` 访问入口，使 Photo Repository 可通过数据库契约访问 `item_photos` 表。

### 12.2 Domain 与 Repository
- `main/java/com/example/itemmanagementandroid/domain/model/ItemPhoto.kt`
  - ItemPhoto 领域模型，统一照片元数据对外输出结构。
- `main/java/com/example/itemmanagementandroid/domain/model/ItemPhotoDraft.kt`
  - ItemPhoto 创建输入模型，承载 `itemId/localUri/thumbnailUri/contentType/width/height` 可写字段集合。
- `main/java/com/example/itemmanagementandroid/domain/model/DeferredPhotoCleanupCandidate.kt`
  - 延迟清理候选领域模型，固化 marker 常量 `ITEM_SOFT_DELETED`。
- `main/java/com/example/itemmanagementandroid/domain/repository/PhotoRepository.kt`
  - 照片仓储接口，定义 `listByItem/get/add/remove/listDeferredCleanupCandidates`。
- `main/java/com/example/itemmanagementandroid/data/repository/PhotoRepositoryImpl.kt`
  - 照片仓储实现；封装字段归一化、尺寸校验、创建时间写入、删除幂等返回与延迟清理候选映射逻辑。

### 12.3 UseCase 与工具
- `main/java/com/example/itemmanagementandroid/domain/usecase/photo/ListItemPhotosUseCase.kt`
  - 按物品查询照片列表入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/photo/GetItemPhotoUseCase.kt`
  - 按照片 ID 查询入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/photo/AddItemPhotoUseCase.kt`
  - 新增照片入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/photo/RemoveItemPhotoUseCase.kt`
  - 删除照片入口（返回是否成功删除）。
- `main/java/com/example/itemmanagementandroid/domain/usecase/photo/ListDeferredPhotoCleanupCandidatesUseCase.kt`
  - 延迟清理候选列表查询入口。
- `main/java/com/example/itemmanagementandroid/photo/PhotoBackupFileNameMapper.kt`
  - 备份文件名映射工具；输出 `full`/`thumbnails` 模式文件名并按既定优先级解析扩展名。

### 12.4 测试文件
- `test/java/com/example/itemmanagementandroid/data/repository/PhotoRepositoryImplTest.kt`
  - Photo 数据层单测；覆盖多照片关联、按 ID 回读、删除幂等、延迟清理候选可见性与输入校验。
- `test/java/com/example/itemmanagementandroid/photo/PhotoBackupFileNameMapperTest.kt`
  - 文件名映射单测；覆盖 contentType 映射、URI 扩展名回退与 `jpg` 兜底规则。

## 13. Step 06 新增文件职责（2026-03-06）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 13.1 构建与依赖装配
- `../../gradle/libs.versions.toml`
  - 新增 `lifecycle-viewmodel-ktx` 与 `lifecycle-viewmodel-compose` 依赖别名，支持 ViewModel + Compose 集成。
- `app/build.gradle.kts`
  - 接入 ViewModel 相关依赖，实现页面状态由 ViewModel 驱动。
- `main/java/com/example/itemmanagementandroid/ui/di/AppDependencies.kt`
  - 应用级依赖装配层；封装 DB/Repository 到 UseCase 的构建过程，仅对 UI 暴露 UseCase。
- `main/java/com/example/itemmanagementandroid/ui/viewmodel/ViewModelFactory.kt`
  - 通用 ViewModel Factory 工具；为非 DI 框架场景提供类型安全的 ViewModel 创建入口。

### 13.2 导航状态管理
- `main/java/com/example/itemmanagementandroid/ui/navigation/AppNavigationUiState.kt`
  - 导航 UI 状态模型；统一承载 `backStack/currentRoute/canGoBack`。
- `main/java/com/example/itemmanagementandroid/ui/navigation/AppNavigationViewModel.kt`
  - 导航状态 ViewModel；实现 `navigate`、`goBack` 与路由状态发布。
- `main/java/com/example/itemmanagementandroid/ui/navigation/AppNavigatorState.kt`（已删除）
  - Step 01 内存态导航容器；在 Step 06 被导航 ViewModel 方案替代。
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - 应用 UI 根壳重构：改为消费导航 ViewModel 状态，按路由绑定页面 ViewModel 与 `UiState` 渲染。

### 13.3 页面状态与 ViewModel
- `main/java/com/example/itemmanagementandroid/ui/screens/home/HomeUiState.kt`
  - 首页状态模型；承载类别数、物品数、加载态与错误态。
- `main/java/com/example/itemmanagementandroid/ui/screens/home/HomeViewModel.kt`
  - 首页状态管理；通过 `ListCategoriesUseCase`、`ListItemsUseCase` 读取最小真实读数。
- `main/java/com/example/itemmanagementandroid/ui/screens/category/CategoryUiState.kt`
  - 类别页状态模型；承载类别列表、includeArchived 开关、加载态与错误态。
- `main/java/com/example/itemmanagementandroid/ui/screens/category/CategoryViewModel.kt`
  - 类别页状态管理；通过 `ListCategoriesUseCase` 支持刷新与归档可见性切换。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListUiState.kt`
  - 列表页状态模型；承载物品列表、includeDeleted 开关、加载态与错误态。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListViewModel.kt`
  - 列表页状态管理；通过 `ListItemsUseCase` 支持刷新与软删除可见性切换。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailUiState.kt`
  - 详情页状态模型；承载当前展示物品信息与照片数量。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailViewModel.kt`
  - 详情页状态管理；采用“首条可见物品占位”策略，通过 `ListItemsUseCase + GetItemUseCase + ListItemPhotosUseCase` 组装最小详情数据。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditUiState.kt`
  - 编辑页状态模型；承载编辑模式、目标物品占位名称、可用类别数。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditViewModel.kt`
  - 编辑页状态管理；采用“首条可见物品占位”策略，通过 `ListCategoriesUseCase + ListItemsUseCase + GetItemUseCase` 组装最小编辑上下文。
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsUiState.kt`
  - 设置页状态模型；承载离线优先文案与备份/同步占位开关状态。
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsViewModel.kt`
  - 设置页状态管理；提供设置页稳定状态源。

### 13.4 页面渲染重构
- `main/java/com/example/itemmanagementandroid/ui/screens/home/HomeScreen.kt`
  - 改为接收 `HomeUiState` 与回调；展示最小真实读数并触发导航/刷新。
- `main/java/com/example/itemmanagementandroid/ui/screens/category/CategoryScreen.kt`
  - 改为接收 `CategoryUiState` 与回调；展示类别数据、归档可见开关与导航。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreen.kt`
  - 改为接收 `ItemListUiState` 与回调；展示物品数据、软删除可见开关与导航。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailScreen.kt`
  - 改为接收 `ItemDetailUiState` 与回调；展示详情占位数据、照片数与导航。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditScreen.kt`
  - 改为接收 `ItemEditUiState` 与回调；展示编辑上下文占位状态与导航。
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsScreen.kt`
  - 改为接收 `SettingsUiState` 与回调；展示离线优先与占位配置状态。

### 13.5 测试文件
- `androidTest/java/com/example/itemmanagementandroid/NavigationFlowIntegrationTest.kt`
  - 导航流程集成测试；断言 `Home -> Category -> ItemList -> ItemDetail -> ItemEdit -> Back` 主链路可达并可回退。
- `androidTest/java/com/example/itemmanagementandroid/StartupSmokeTest.kt`
  - 启动冒烟测试更新；断言首页关键入口按钮可见。
- `test/java/com/example/itemmanagementandroid/ui/navigation/AppNavigationViewModelTest.kt`
  - 导航状态单测；断言初始状态、幂等导航与回退行为正确。
