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
- V0 发布前 UI 统一规则：`Category` 作为根入口，`Home` 退出主导航主链路。
- 所有页面统一 TopAppBar + Overflow，且 Overflow 统一包含 `Refresh`。
- 除 `New Item/Edit Item` 外，移除页面底部 `Back` 按钮，避免重复导航控件。
- `Category -> ItemList` 支持初始分类筛选参数，并保持“用户手动修改优先”。
- Settings 固定三段结构（状态+目录 / Backup / Import），仅重构信息架构，不改变备份/导入语义。

## 7. 架构洞察日志
- 2026-03-10
  - 完成 Step 15：将导航根入口从 `Home` 切换为 `Category`，并在 `AppNavigationViewModel/AppNavigationUiState` 冻结默认根栈。
  - 新增可复用页面壳 `AppPageScaffold`，统一 TopAppBar + Overflow（固定 `Refresh`）与根/非根页返回行为。
  - 固化“非编辑页移除底部 Back”并保持编辑页底部 `Back/Cancel` 例外；`Settings` 页面移除 `Go To Home` 入口。
  - 新增 `navigateToCategoryRoot()` 导航语义，用于 `Settings` 页 Overflow 的 `Back To Category` 动作，避免把根页作为普通 push 页面。
  - 固化页面显示即刷新：`Category/ItemList/ItemDetail/Settings` 在路由进入时通过 `LaunchedEffect(currentRoute)` 触发 refresh，`SettingsViewModel` 新增显式 `refresh()` 入口复用同一路径。
  - 本轮仅修改 UI/导航骨架与测试契约，不改变数据库结构、备份格式、导入导出语义与数据层业务规则。
- 2026-03-09
  - 按发布前 UI Modernization 目标更新文档计划：在 `implementation-plan.md` 新增 Step 15-19（UI 重构）并将原验收步骤顺延为 Step 20。
  - 冻结路由与页面骨架方向：`Category` 根入口、统一 TopAppBar/Overflow、页面显示即 refresh、非编辑页移除底部 Back。
  - 明确本轮仅更新 memory-bank 文档，不变更数据库结构、备份格式与导入导出语义。
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
  - 完成 Step 07：建立类别页完整交互闭环，覆盖 `列表展示/创建/重命名/归档切换/排序调整`，并显示每类未软删物品数量。
  - 在 `CategoryViewModel` 接入 `Create/Update/SetArchived/Reorder` UseCase 与 `ListItemsUseCase`，固化计数统计口径 `includeDeleted=false`。
  - 固化排序规则：采用 Up/Down 交互，不做拖拽；`includeArchived=false` 时仅重排可见分类，隐藏分类保持相对顺序。
  - 在 `CategoryScreen` 新增创建与重命名对话框、系统默认标识、归档状态标识与每行操作按钮，并补充 `testTag` 契约。
  - 新增 `CategoryScreenInteractionTest`（设备），覆盖创建、重命名、归档切换、上移/下移与 includeArchived 切换交互回调；在小屏场景通过 `performScrollToNode` 稳定 `moveUp` 用例。
  - 完成 Step 08：建立物品列表页过滤与排序闭环，覆盖 `类别过滤 + 四种排序 + 空状态/无结果状态`。
  - 在 `domain/model` 新增 `ItemListQuery` 与 `ItemListSortOption`，统一 Repository 与 UseCase 的列表查询契约。
  - 在 `ItemRepositoryImpl` 固化排序语义：`RECENTLY_ADDED`、`RECENTLY_UPDATED`、`PURCHASE_DATE(null last)`、`PURCHASE_PRICE(null last)`，并统一稳定兜底排序 `updated_at DESC -> created_at DESC -> id ASC`。
  - 在 `ItemListViewModel` 接入 `ListCategoriesUseCase(includeArchived=true)`，固化类别筛选包含归档项且默认 `All`；保留 `includeDeleted=false` 默认行为。
  - 在 `ItemListScreen` 新增筛选/排序交互区、归档标识文案与 `ItemListScreenTestTags`，同时保留主链路按钮 `Go To Item Detail/Back`。
  - 新增 `ItemListScreenInteractionTest`（设备）与 `ItemRepositoryImplTest`（JVM）Step 08 用例，覆盖列表过滤、排序空值后置、回调交互与状态文案可见性。
- 2026-03-07
  - 完成 Step 09：将 `AppRoute.ItemEdit` 从固定路由升级为可选参数路由（`itemId: String?`），并在 `ItemManagementApp` 按 `itemId` 隔离 `ItemEditViewModel` 实例。
  - 在 `ui/di/AppDependencies` 新增 `CreateItemUseCase`、`UpdateItemUseCase` 暴露，编辑页保存流程统一走 UseCase，不允许 UI 直连 Repository。
  - 重构 `ItemEditUiState`、`ItemEditViewModel`、`ItemEditScreen`：落地完整表单字段、字段级错误、保存态、取消操作、`customAttributes` 动态 key-value 行录入。
  - 新增 `ItemEditFormMapper`，固化 Step 09 表单映射规则：`tags` 逗号拆分去重、价格可选数值解析、`customAttributes` 值按 `boolean -> number -> string` 解析。
  - 新增 `ItemEditFormMapperTest`（JVM）与 `ItemEditScreenInteractionTest`（设备），并更新 `NavigationFlowIntegrationTest`，覆盖编辑页新增交互与回退链路。
  - 在 `ItemListScreen` 新增“Go To Item Edit”入口（新建模式），在 `ItemDetailScreen` 改为携带当前 `selectedItemId` 跳转编辑（编辑模式优先）。
  - 完成 Step 10：将 `AppRoute.ItemDetail` 升级为可选参数路由（`itemId: String?`），支持“列表指定项详情 / 空参兜底详情”双路径。
  - 在 `ui/di/AppDependencies` 新增 `SoftDeleteItemUseCase`、`RestoreItemUseCase` 暴露，详情页删除与恢复流程统一走 UseCase。
  - 重构 `ItemDetailUiState`、`ItemDetailViewModel`、`ItemDetailScreen`：从占位详情升级为完整详情字段展示（含 `createdAt/updatedAt/deletedAt`、`tags`、`customAttributes`、照片元数据墙）与删除/恢复闭环交互。
  - 在 `ItemManagementApp` 按 `itemId` 隔离 `ItemDetailViewModel` key，并在列表页路由进入时触发刷新，确保删除后返回默认列表不显示已删除项。
  - 在 `ItemListScreen` 增加“行点击进入详情（携带 itemId）”行为，同时保留 `Go To Item Detail` 空参入口。
  - 新增 `ItemDetailScreenInteractionTest`（设备），更新 `ItemListScreenInteractionTest` 与 `AppNavigationViewModelTest`，覆盖 Step 10 路由与详情交互回归。
  - 完成 Step 10 bugfix：修复 `New Item` 重入复用旧数据问题，在 `ItemEditViewModel` 新增 `onRouteEntered(itemId)` 与 `requestedItemId` 并将 `refresh` 重载口径切换为当前路由请求态。
  - 在 `ItemManagementApp` 的 `ItemEdit` 分支新增 `LaunchedEffect(currentRoute.itemId)`，确保每次进入 `ItemEdit(null)` 都触发 `CREATE` 空表单重置。
  - 在数据层下沉“全局重名禁止（未删除范围，trim + case-insensitive）”：`ItemDao` 新增规范化名称冲突查询，`ItemRepositoryImpl.create/update` 统一抛出 `DuplicateItemNameException`，`ItemEditViewModel` 精准映射到 `fieldErrors.name`。
  - 新增 `ItemEditViewModelTest`（JVM）与 `ItemEditFlowIntegrationTest`（设备），覆盖“连续 New Item 空表单”与“重名创建拦截”回归链路。
  - 完成 Step 11：实现 V0 搜索最小闭环，在 `ItemListQuery` 新增 `searchKeyword` 并在 `ItemDao` 引入统一查询入口 `listByQuery(...)`，支持 `includeDeleted + category + keyword` 组合过滤。
  - 在 `ItemRepositoryImpl` 固化关键词标准化与 LIKE 转义策略（`%`、`_`、`\`），冻结搜索语义：`name/description/purchasePlace` 子串匹配 + `tags` 整词匹配。
  - 在 `ItemListUiState/ItemListViewModel/ItemListScreen` 接入“输入即搜索”链路，并将 `hasAnyItemsInCurrentMode` 口径更新为“同 includeDeleted + 同类别 + 不带 keyword”。
  - 新增 `ItemSearchQueryIntegrationTest`（设备）与 `ItemRepositoryImplTest` Step 11 用例，覆盖 SQLite 搜索语义、组合过滤、排序一致性与 1k 抽样基线记录。
  - 文档化搜索升级触发规则：当本地数据量 `> 3000` 且响应不可接受时，进入 FTS 升级任务（本步不引入 FTS）。
  - 完成 Step 12：建立“拍照/选图 -> 私有目录落盘 -> 原图+缩略图 -> DB 落库 -> 列表封面/详情图片展示”的完整照片链路，并在 `ItemEdit` 提供失败重试入口。
  - 在 `photo` 模块新增 `AndroidPhotoAssetProcessor`、`AppPrivatePhotoStorage` 与 `PhotoProcessingConfig`，固化 EXIF 方向矫正、统一 JPEG 输出（无 EXIF）与缩略图规格（长边 1280、质量 85）。
  - 在 `domain` 新增 `ImportItemPhotosUseCase`、`ListItemPhotoCoversUseCase` 与导入结果模型（`PhotoImportSummary`、`PhotoImportFailure`），并扩展 `PhotoRepository` 支持批量封面查询。
  - 在 `ui` 层完成照片能力接线：`ItemEdit` 新增拍照/单选/多选/失败重试与自动建项导图；`ItemList` 行内封面缩略图；`ItemDetail` 照片墙实际图片渲染。
  - 完成 Android 接入：新增 `FileProvider` 与 `res/xml/file_paths.xml`，支持相机拍照输出到应用私有缓存路径。
  - 新增 `AndroidPhotoAssetProcessorIntegrationTest`（20 图输入）并通过设备全量测试（`connectedAndroidTest` 33 项，设备 `SM-S901U1 - Android 15`）。
- 2026-03-08
  - 完成 Step 13：落地本地备份导出闭环，新增 `BackupService.exportLocalBackup(exportMode)` 与三模式导出（`metadata_only/thumbnails/full`）。
  - 在 `backup/export` 建立模块化实现链：快照采集、照片预处理、JSON 组装、ZIP 写入、错误分类与 checksums 扩展点（V0 默认 No-Op）。
  - 冻结 Step 13 行为：导出目录优先 `getExternalFilesDir("backups")`（空时回退 `files/backups`）；`thumbnails/full` 任一缺图即失败中止，不降级不跳过。
  - 在 `ui/di` 注入 `ExportLocalBackupUseCase`，将设置页升级为“单入口导出 + 模式选择 + 导出状态/路径展示”。
  - 新增 `LocalBackupServiceTest`（JVM）、`BackupExportIntegrationTest`（设备）、`SettingsScreenInteractionTest`（设备），并通过设备全量回归（`connectedAndroidTest` 39 项，设备 `SM-S901U1 - Android 15`）。
  - 完成 Step 14：落地 `backup/importing` 模块与 `BackupService.importLocalBackup(backupFilePath)`，实现 V0 `replace_all` 导入闭环。
  - 在导入链路固化回滚策略：导入前自动执行 `full` 本地快照，失败时抛出 `RollbackSnapshotFailed` 并中止导入。
  - 在导入链路固化兼容与清理策略：未知字段忽略、`formatVersion/schemaVersion` 更高版本“尽力导入 + 告警”，并清理旧照片文件与 `files/photos` 孤儿文件。
  - 新增 `BackupImportIntegrationTest`（设备），覆盖 `replace_all` 一致性与“高版本 + 未知字段”兼容；设备全量回归提升为 `connectedAndroidTest` 41 项（设备 `SM-S901U1 - Android 15`）。
  - 完成 Step 14A：在 Settings 落地共享目录备份与导入入口（`OpenDocumentTree/OpenDocument`），并新增 SAF 桥接层承接“私有临时 zip <-> 共享目录文档”复制。
  - 固化 Step 14A 目录策略：持久化 `backup_tree_uri` + `takePersistableUriPermission`；仅列出所选目录顶层 `.zip`，按 `lastModified DESC` 排序。
  - 复用现有备份核心引擎：导出链路 `exportLocalBackup -> copyLocalFileToDocument -> 删除私有临时文件`；导入链路 `copyDocumentToTempFile -> importLocalBackup -> 删除临时文件`。
  - 完成 Step 14A 测试回归：`SettingsViewModelTest`（JVM）+ `SettingsScreenInteractionTest`（设备）+ `connectedAndroidTest` 全量 42 项（设备 `SM-S901U1 - Android 15`）。
  - 完成 Step 14A 后续紧凑化修正：`SettingsScreen` 将导出模式从 3 按钮切换为 1 个下拉选单，并将页面重构为单一外层 `LazyColumn`，消除嵌套滚动遮挡。
  - 更新 Step 14A 设置页测试：`SettingsScreenInteractionTest` 新增“下拉模式选择”与“滚动到 Back 按钮可见”断言；设备全量回归更新为 `connectedAndroidTest` 43 项（设备 `SM-S901U1 - Android 15`）。

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

## 14. Step 07 新增/修改文件职责（2026-03-06）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 14.1 依赖装配与注入
- `main/java/com/example/itemmanagementandroid/ui/di/AppDependencies.kt`
  - 扩展 Category 用例装配，新增暴露 `CreateCategoryUseCase`、`UpdateCategoryUseCase`、`SetCategoryArchivedUseCase`、`ReorderCategoriesUseCase`。
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - 扩展 `CategoryViewModel` 注入参数，绑定类别页新增回调：创建、重命名、归档切换、上移、下移。

### 14.2 类别页状态与业务逻辑
- `main/java/com/example/itemmanagementandroid/ui/screens/category/CategoryListItemUiModel.kt`
  - 类别页行级 UI 模型，承载 `id/name/isArchived/isSystemDefault/itemCount`。
- `main/java/com/example/itemmanagementandroid/ui/screens/category/CategoryUiState.kt`
  - 将类别集合从领域模型切换为 `CategoryListItemUiModel`，服务于页面展示与交互。
- `main/java/com/example/itemmanagementandroid/ui/screens/category/CategoryViewModel.kt`
  - 新增类别页交互闭环方法：`createCategory`、`renameCategory`、`setCategoryArchived`、`moveCategoryUp`、`moveCategoryDown`。
  - 列表加载时联动 `ListItemsUseCase(includeDeleted=false)` 统计每类物品数量。
  - 固化重排算法：归档隐藏场景下仅重排可见分类，隐藏分类相对位置不变。

### 14.3 类别页 UI 渲染
- `main/java/com/example/itemmanagementandroid/ui/screens/category/CategoryScreen.kt`
  - 从“占位页”升级为可操作页面：类别列表、创建/重命名对话框、归档/反归档、上移/下移、系统默认标识、物品数量显示。
  - 保留 Step 06 导航主链路按钮（`Go To Item List`、`Back`）以确保主链路回归稳定。
  - 新增 `CategoryScreenTestTags`，统一定义关键控件测试标识。

### 14.4 测试文件
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/category/CategoryScreenInteractionTest.kt`
  - 类别页交互设备测试；覆盖创建、重命名、归档切换、上移/下移、includeArchived 切换触发回调。

## 15. Step 08 新增/修改文件职责（2026-03-06）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 15.1 领域查询契约
- `main/java/com/example/itemmanagementandroid/domain/model/ItemListSortOption.kt`
  - 物品列表排序枚举；固定 4 种排序策略（最近添加、最近更新、购买日期、价格）。
- `main/java/com/example/itemmanagementandroid/domain/model/ItemListQuery.kt`
  - 物品列表查询参数模型；统一承载 `includeDeleted/categoryId/sortOption`。
- `main/java/com/example/itemmanagementandroid/domain/repository/ItemRepository.kt`
  - 扩展 `list(query: ItemListQuery)` 主查询入口，并保留 `list(includeDeleted)` 兼容入口。
- `main/java/com/example/itemmanagementandroid/domain/usecase/item/ListItemsUseCase.kt`
  - 扩展 `invoke(query: ItemListQuery)` 入口，并保留 `invoke(includeDeleted)` 兼容入口。

### 15.2 数据层排序与过滤
- `main/java/com/example/itemmanagementandroid/data/repository/ItemRepositoryImpl.kt`
  - 在列表查询链路接入 category 过滤与 4 种排序语义，固化 `purchaseDate/purchasePrice` 空值后置规则与稳定兜底排序。

### 15.3 列表页状态与交互
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListCategoryFilterUiModel.kt`
  - 列表页类别筛选 UI 模型，承载 `id/name/isArchived`。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListUiState.kt`
  - 扩展列表状态：`sortOption`、`selectedCategoryId`、`categoryFilters`、`hasAnyItemsInCurrentMode`，并新增空状态/无结果状态派生字段。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListViewModel.kt`
  - 接入 `ListCategoriesUseCase`，新增 `setCategoryFilter`、`setSortOption`，统一加载类别筛选项与查询结果。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreen.kt`
  - 新增类别筛选区（含 All 与归档标识）、排序切换区、空状态/无结果状态文案与测试标识；保留 Step 06 主链路导航按钮。
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - 更新 `ItemListViewModel` 注入参数，补齐 `ListCategoriesUseCase` 依赖与列表页新回调绑定。

### 15.4 测试文件
- `test/java/com/example/itemmanagementandroid/data/repository/ItemRepositoryImplTest.kt`
  - 新增 Step 08 数据层用例：类别过滤、购买日期排序空值后置、价格降序空值后置、软删除与类别过滤组合。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreenInteractionTest.kt`
  - 新增列表页交互设备测试；覆盖类别筛选、排序切换、空状态/无结果状态文案与导航按钮回调。

## 16. Step 09 新增/修改文件职责（2026-03-07）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 16.1 路由与依赖装配
- `main/java/com/example/itemmanagementandroid/ui/navigation/AppRoute.kt`
  - 将 `ItemEdit` 路由升级为 `data class ItemEdit(itemId: String?)`，支持“新建（null）/编辑（itemId）”双模式入口。
- `main/java/com/example/itemmanagementandroid/ui/di/AppDependencies.kt`
  - 新增并暴露 `CreateItemUseCase`、`UpdateItemUseCase`，供编辑页保存链路注入。
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - 在 `ItemEdit` 分支按 `itemId` 构建并缓存 `ItemEditViewModel`，绑定完整表单事件回调（字段更新、保存、取消、属性行增删）。

### 16.2 编辑页状态、映射与业务逻辑
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditUiState.kt`
  - 扩展为完整表单状态：字段值、类别选项、动态属性行、字段错误、加载/保存状态与保存结果提示。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditFormMapper.kt`
  - 新增编辑表单映射工具，统一 `tags/purchasePrice/customAttributes` 的解析与校验策略。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditViewModel.kt`
  - 改造为真实编辑闭环：按 `itemId` 回填、字段更新事件、保存校验、创建/更新提交、保存后回显刷新、取消不落库。

### 16.3 编辑页 UI 与入口联动
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditScreen.kt`
  - 从占位页升级为可编辑表单 UI，新增 `Save/Cancel`、动态 `customAttributes` 行编辑、测试标签与字段错误展示。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailScreen.kt`
  - 编辑入口改为携带 `selectedItemId` 跳转 `AppRoute.ItemEdit(itemId)`。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreen.kt`
  - 新增“Go To Item Edit”按钮，作为新建模式入口（`AppRoute.ItemEdit()`）。

### 16.4 测试文件
- `test/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditFormMapperTest.kt`
  - 新增表单映射单测：空名称相关映射前置、价格解析、tags 归一化、`customAttributes` 类型解析与非法 key 校验。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditScreenInteractionTest.kt`
  - 新增编辑页设备交互测试：空名称禁用保存、合法名称保存触发、扩展字段输入后保存触发。
- `androidTest/java/com/example/itemmanagementandroid/NavigationFlowIntegrationTest.kt`
  - 更新编辑页回退断言路径，适配 Step 09 表单页滚动与 `Cancel` 回退行为。

## 17. Step 10 新增/修改文件职责（2026-03-07）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 17.1 路由与依赖装配
- `main/java/com/example/itemmanagementandroid/ui/navigation/AppRoute.kt`
  - 将 `ItemDetail` 路由升级为 `data class ItemDetail(itemId: String?)`，支持“指定 item 详情 / 空参兜底”双入口。
- `main/java/com/example/itemmanagementandroid/ui/di/AppDependencies.kt`
  - 新增并暴露 `SoftDeleteItemUseCase`、`RestoreItemUseCase`，供详情页删除/恢复链路注入。
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - 在 `ItemDetail` 分支按 `itemId` 构建并缓存 `ItemDetailViewModel`，并绑定删除/恢复回调；在 `ItemList` 路由进入时触发刷新，确保列表与删除状态一致。

### 17.2 详情页状态与业务逻辑
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailUiState.kt`
  - 扩展为完整详情状态：基础字段、`tags`、`customAttributes`、`createdAt`、`updatedAt`、`deletedAt`、照片元数据列表、操作进行中标记与错误/结果消息。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailViewModel.kt`
  - 接收 `initialItemId`，优先加载指定 item；为空时回退首条可见 item。
  - 新增 `softDelete`、`restore` 事件，删除后保持详情上下文并切换到可恢复态，恢复后回到可删除态。
  - 将 `ItemPhoto` 映射为详情页照片元数据 UI 模型，形成 Step 10 可读照片墙数据源。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailScreen.kt`
  - 从占位页升级为完整详情渲染：字段区、照片元数据墙、编辑入口、删除/恢复入口与回退入口。
  - 新增 `ItemDetailScreenTestTags`，统一详情页关键控件测试标签。

### 17.3 列表页联动
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreen.kt`
  - 新增行级点击导航，支持从列表直接进入指定 `itemId` 的详情页。
  - 保留 `Go To Item Detail` 空参入口用于无选中项兜底，并将新建入口文案明确为 `New Item`。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailScreen.kt`
  - 编辑入口文案明确为 `Edit Item`，与列表页新建入口语义区分。

### 17.4 测试文件
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailScreenInteractionTest.kt`
  - 新增详情页设备交互测试；覆盖字段展示、照片墙可见、删除入口触发、恢复入口触发。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreenInteractionTest.kt`
  - 更新路由断言以适配 `AppRoute.ItemDetail(itemId)`，并新增“列表行点击传递 itemId”用例。
- `androidTest/java/com/example/itemmanagementandroid/NavigationFlowIntegrationTest.kt`
  - 更新主链路断言：在详情页补充空态可见性校验，确保 `ItemDetail(itemId=null)` 兜底路径与编辑入口回退链路稳定。
- `test/java/com/example/itemmanagementandroid/ui/navigation/AppNavigationViewModelTest.kt`
  - 新增 `ItemDetail` data class 路由幂等导航断言，确保同参重复导航不重复入栈。

## 18. Step 10 Bugfix 新增/修改文件职责（2026-03-07）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 18.1 路由重入与编辑态重置
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditViewModel.kt`
  - 新增 `onRouteEntered(itemId)` 与 `requestedItemId`，将路由请求态作为唯一加载源；修复 `refresh()` 历史回退导致 `New Item` 复用旧数据的问题。
  - 调整保存后状态机：`CREATE` 保存成功后保留 `CREATE` 模式并回到空表单；`EDIT` 保存成功保持当前编辑态。
  - 捕获 `DuplicateItemNameException` 并映射到 `fieldErrors.name`，避免被通用错误消息吞掉字段级反馈。
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - 在 `ItemEdit` 分支追加 `LaunchedEffect(currentRoute.itemId)` 调用 `itemEditViewModel.onRouteEntered(...)`，确保每次路由进入都同步 ViewModel 状态。

### 18.2 数据层重名规则下沉
- `main/java/com/example/itemmanagementandroid/data/local/dao/ItemDao.kt`
  - 新增规范化名称冲突查询：
    - `countActiveByNormalizedName(name)`
    - `countActiveByNormalizedNameExcludingId(name, excludeItemId)`
  - 冻结判重口径：`deleted_at IS NULL` 且 `lower(trim(name))`。
- `main/java/com/example/itemmanagementandroid/data/repository/ItemRepositoryImpl.kt`
  - 在 `create/update` 内统一执行重名检查并抛出 `DuplicateItemNameException`，防止绕过 UI 直写数据层。
- `main/java/com/example/itemmanagementandroid/domain/model/DuplicateItemNameException.kt`
  - 新增业务异常类型，作为仓储层到 UI 层的重名语义契约。

### 18.3 测试与验证文件
- `test/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditViewModelTest.kt`
  - 新增 ViewModel 单测：连续 `ItemEdit(null)` 为空表单、`CREATE` 保存后保持空表单、重名异常映射到名称字段错误。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditFlowIntegrationTest.kt`
  - 新增设备流程测试：`ItemList -> New Item -> Save -> Back -> New Item` 名称输入为空，且重名创建被拦截并展示错误文案。
- `test/java/com/example/itemmanagementandroid/data/repository/ItemRepositoryImplTest.kt`
  - 扩展重名规则回归用例（创建重名拒绝、软删后同名允许、更新排除自身）。

## 19. Step 11 新增/修改文件职责（2026-03-07）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 19.1 查询契约与数据层
- `main/java/com/example/itemmanagementandroid/domain/model/ItemListQuery.kt`
  - 扩展 `searchKeyword` 查询参数，保持默认空值兼容既有调用路径。
- `main/java/com/example/itemmanagementandroid/data/local/dao/ItemDao.kt`
  - 新增统一查询入口 `listByQuery(includeDeleted, categoryId, hasSearchKeyword, likePattern, tagWordPattern)`；
  - 固化 SQL 搜索语义：`name/description/purchasePlace` 子串匹配 + `tags_json` 整词匹配；
  - 固化组合过滤能力：`includeDeleted + categoryId + keyword`。
- `main/java/com/example/itemmanagementandroid/data/repository/ItemRepositoryImpl.kt`
  - 列表查询改走 `ItemDao.listByQuery(...)`；
  - 下沉关键词规范化（trim + lowercase）与 LIKE 转义策略（`%`、`_`、`\`）；
  - 保持 Step 08 排序语义与空值后置规则不变。

### 19.2 列表页状态与交互
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListUiState.kt`
  - 新增 `searchKeyword` UI 状态字段。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListViewModel.kt`
  - 新增 `setSearchKeyword(...)` 事件并接入“输入即搜索”；
  - 将 `hasAnyItemsInCurrentMode` 查询口径更新为“同 includeDeleted + 同 category + searchKeyword=null”。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreen.kt`
  - 新增搜索输入框与测试标签 `SEARCH_INPUT`；
  - 保持筛选、排序、导航按钮链路不变。
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - 在 `ItemListScreen` 绑定 `onSearchKeywordChanged = itemListViewModel::setSearchKeyword`。

### 19.3 测试文件
- `test/java/com/example/itemmanagementandroid/data/repository/ItemRepositoryImplTest.kt`
  - 新增 Step 11 用例：字段命中、tags 整词匹配、搜索+类别组合过滤、搜索后排序一致性；
  - 新增 `searchBaseline_1000Items_recordsElapsedMillis`，记录 1k 数据搜索抽样基线。
- `androidTest/java/com/example/itemmanagementandroid/data/repository/ItemSearchQueryIntegrationTest.kt`
  - 新增设备集成测试，验证真实 SQLite 查询语义与 tags 整词匹配规则。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreenInteractionTest.kt`
  - 新增搜索输入交互回调测试（输入/清空）；
  - 调整行点击断言为语义点击以适配小屏可视区域。

## 20. Step 12 新增/修改文件职责（2026-03-07）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 20.1 领域契约与用例
- `main/java/com/example/itemmanagementandroid/domain/model/ItemPhotoCover.kt`
  - 物品封面缩略图模型（`itemId` + `thumbnailUri`），用于列表一次性回填封面。
- `main/java/com/example/itemmanagementandroid/domain/model/PhotoImportFailure.kt`
  - 照片导入失败项模型，记录失败来源与原因，供 UI 重试。
- `main/java/com/example/itemmanagementandroid/domain/model/PhotoImportSummary.kt`
  - 批量导图结果汇总模型（成功数/失败集合/导入项）。
- `main/java/com/example/itemmanagementandroid/domain/model/ProcessedPhotoAsset.kt`
  - 图片处理产物模型，承载 full/thumb 文件路径、尺寸与媒体类型。
- `main/java/com/example/itemmanagementandroid/domain/repository/PhotoAssetProcessor.kt`
  - 图片处理器抽象，定义 `process(uri)` 与失败清理能力。
- `main/java/com/example/itemmanagementandroid/domain/repository/PhotoRepository.kt`
  - 扩展封面查询接口：`listCoversByItemIds(itemIds)`。
- `main/java/com/example/itemmanagementandroid/domain/usecase/photo/ImportItemPhotosUseCase.kt`
  - 批量导图编排用例：处理 source uris，成功落库 `item_photos`，失败聚合可重试集合。
- `main/java/com/example/itemmanagementandroid/domain/usecase/photo/ListItemPhotoCoversUseCase.kt`
  - 批量封面查询用例，供列表页避免 N+1 查询。

### 20.2 数据层与查询
- `main/java/com/example/itemmanagementandroid/data/local/dao/model/ItemPhotoCoverRow.kt`
  - DAO 层封面查询 row 映射模型。
- `main/java/com/example/itemmanagementandroid/data/local/dao/ItemPhotoDao.kt`
  - 新增按 itemIds 批量查询首图缩略图 SQL（首图口径：`MIN(created_at)`）。
- `main/java/com/example/itemmanagementandroid/data/repository/PhotoRepositoryImpl.kt`
  - 实现 `listCoversByItemIds` 并完成 row 到领域模型映射。

### 20.3 照片处理与存储实现
- `main/java/com/example/itemmanagementandroid/photo/PhotoProcessingConfig.kt`
  - 照片处理配置常量（缩略图长边、JPEG 质量等）。
- `main/java/com/example/itemmanagementandroid/photo/AppPrivatePhotoStorage.kt`
  - 应用私有目录管理与文件命名落盘（`files/photos/full`、`files/photos/thumbs`）。
- `main/java/com/example/itemmanagementandroid/photo/AndroidPhotoAssetProcessor.kt`
  - Android 侧图片处理实现：读取 Uri、EXIF 方向矫正、原图/缩略图 JPEG 重编码并移除 EXIF。

### 20.4 UI 与依赖接线
- `main/java/com/example/itemmanagementandroid/ui/di/AppDependencies.kt`
  - 注入 `AndroidPhotoAssetProcessor`、`ImportItemPhotosUseCase`、`ListItemPhotoCoversUseCase`。
- `main/java/com/example/itemmanagementandroid/ui/components/UriImage.kt`
  - 新增通用 URI 图片渲染组件，统一 file/content uri 读取与占位图展示。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditUiState.kt`
  - 扩展照片导入状态：`photos`、`isImportingPhotos`、`photoImportFailures`、`photoImportMessage`。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditViewModel.kt`
  - 新增 `importPhotoUris` 与 `retryFailedPhotoImports`；实现新建态自动建项导图与失败重试状态流转。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditScreen.kt`
  - 新增拍照/单选/多选/失败重试入口与缩略图区，并补充测试标签。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListUiState.kt`
  - 新增 `coverUriByItemId`，承载列表封面路径映射。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListViewModel.kt`
  - 接入批量封面查询并合并到列表状态。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreen.kt`
  - 行内封面缩略图渲染与封面测试标签。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailUiState.kt`
  - 照片 UI 模型新增 `displayUri`，承载详情展示路径。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailViewModel.kt`
  - 详情照片映射更新为实际图片显示字段。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailScreen.kt`
  - 照片墙由元数据占位升级为实际图片展示。
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - 补齐 Step 12 新回调绑定（导图、失败重试、封面查询依赖注入）。

### 20.5 Android 配置
- `main/AndroidManifest.xml`
  - 新增 `FileProvider`（`{applicationId}.fileprovider`）用于相机拍照输出。
- `main/res/xml/file_paths.xml`
  - 声明 `camera-captures/` 缓存路径共享策略。

### 20.6 测试文件
- `test/java/com/example/itemmanagementandroid/domain/usecase/photo/ImportItemPhotosUseCaseTest.kt`
  - 覆盖导入成功与部分失败场景。
- `test/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditViewModelTest.kt`
  - 覆盖新建态自动建项导图与失败重试状态流转。
- `test/java/com/example/itemmanagementandroid/data/repository/PhotoRepositoryImplTest.kt`
  - 新增封面批量查询回归用例。
- `androidTest/java/com/example/itemmanagementandroid/photo/AndroidPhotoAssetProcessorIntegrationTest.kt`
  - 构造 20 图输入，校验处理成功率、缩略图尺寸上限、JPEG 输出与文件可访问性。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditScreenInteractionTest.kt`
  - 新增失败重试入口可见性与回调验证。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreenInteractionTest.kt`
  - 新增列表封面可见性回归。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailScreenInteractionTest.kt`
  - 适配详情实际图片展示链路断言。

## 21. Step 12 后续修正补充（2026-03-08）

### 21.1 后续修正固化的行为约束
- ItemEdit 保存流程固定为：`Save -> ItemDetail(savedItemId)`。
- 上述保存流程后的返回路径固定为：`ItemDetail -> ItemList`。
- 路由进入时自动刷新策略明确为：
  - 进入 `AppRoute.ItemDetail(itemId)` 必须触发详情刷新；
  - 进入 `AppRoute.Category` 必须触发分类刷新（包含 item count）。
- ItemEdit 新建态导图行为固定为：
  - 导图前后保持用户已输入草稿字段不变；
  - 若为导图绑定必须先创建 item，不得将自动生成名称回填到可见名称输入框。

### 21.2 文件职责更新
- `apps/ItemManagementAndroid/app/src/main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - 负责 `Category` 与 `ItemDetail` 的路由进入自动刷新触发。
  - 负责 ItemEdit 保存后到 `navigateToItemDetailAfterEdit(...)` 的导航衔接。
- `apps/ItemManagementAndroid/app/src/androidTest/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditFlowIntegrationTest.kt`
  - 负责以下回归覆盖：
    - 编辑保存返回详情后的自动刷新；
    - 从保存链路返回分类页后的 item count 自动刷新。

## 22. Step 13 新增/修改文件职责（2026-03-08）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 22.1 备份导出契约与实现
- `main/java/com/example/itemmanagementandroid/backup/export/BackupService.kt`
  - 备份导出统一契约，暴露 `exportLocalBackup(exportMode)`。
- `main/java/com/example/itemmanagementandroid/backup/export/ExportMode.kt`
  - 导出模式枚举与 wire 值映射（`metadata_only`、`thumbnails`、`full`）。
- `main/java/com/example/itemmanagementandroid/backup/export/BackupExportResult.kt`
  - 导出结果模型（输出路径、大小、模式、导出时间、统计信息）。
- `main/java/com/example/itemmanagementandroid/backup/export/BackupStats.kt`
  - 导出统计模型（`categories/items/photos`）。
- `main/java/com/example/itemmanagementandroid/backup/export/BackupExportException.kt`
  - 导出错误分类（参数错误、I/O 错误、缺图失败）。
- `main/java/com/example/itemmanagementandroid/backup/export/BackupChecksumGenerator.kt`
  - checksums 扩展点契约（供 V1/V2 开启 `checksums.json` 时复用）。
- `main/java/com/example/itemmanagementandroid/backup/export/NoOpBackupChecksumGenerator.kt`
  - V0 默认 checksums 实现（不生成 `checksums.json`）。
- `main/java/com/example/itemmanagementandroid/backup/export/BackupOutputDirectoryProvider.kt`
  - 导出目录提供器抽象。
- `main/java/com/example/itemmanagementandroid/backup/export/AndroidBackupOutputDirectoryProvider.kt`
  - Android 目录策略实现：优先 `getExternalFilesDir("backups")`，回退 `files/backups`。
- `main/java/com/example/itemmanagementandroid/backup/export/BackupSnapshot.kt`
  - 导出快照聚合模型（Category/Item/ItemPhoto）。
- `main/java/com/example/itemmanagementandroid/backup/export/BackupSnapshotCollector.kt`
  - 快照采集器：读取全量类别（含归档）、全量物品（含软删）与照片集合。
- `main/java/com/example/itemmanagementandroid/backup/export/PreparedPhotoEntry.kt`
  - 待打包照片描述模型（`fileName/kind/sourceFile` 等）。
- `main/java/com/example/itemmanagementandroid/backup/export/BackupPhotoPreparer.kt`
  - 照片导出预处理：按 mode 选源图，执行文件名映射，缺图直接抛错中止。
- `main/java/com/example/itemmanagementandroid/backup/export/BackupJsonEncoder.kt`
  - 平台无关 JSON 编码器（UTF-8 输出）。
- `main/java/com/example/itemmanagementandroid/backup/export/BackupJsonBuilder.kt`
  - 构建 `manifest.json` 与 `data.json`，字段对齐 `BACKUP_FORMAT.md`。
- `main/java/com/example/itemmanagementandroid/backup/export/BackupZipWriter.kt`
  - ZIP 写入器，固定顺序写入 `manifest -> data -> photos -> checksums(optional)`。
- `main/java/com/example/itemmanagementandroid/backup/export/LocalBackupService.kt`
  - Step 13 编排入口，串联采集/打包/写入并返回导出结果。

### 22.2 UseCase 与应用接线
- `main/java/com/example/itemmanagementandroid/domain/usecase/backup/ExportLocalBackupUseCase.kt`
  - UI 到备份服务的导出用例入口。
- `main/java/com/example/itemmanagementandroid/ui/di/AppDependencies.kt`
  - 注入 Step 13 备份导出依赖链并暴露 `exportLocalBackupUseCase`。
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsUiState.kt`
  - 扩展设置页状态：导出模式、导出中状态、成功/失败消息、最近导出路径与时间。
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsViewModel.kt`
  - 接入导出状态机与错误映射，统一处理模式切换与导出请求。
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsScreen.kt`
  - 升级为“单入口导出”界面：模式选择 + 导出按钮 + 状态展示。
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - Settings 路由分支改为工厂注入 `SettingsViewModel` 并绑定导出回调。

### 22.3 Step 13 测试文件
- `test/java/com/example/itemmanagementandroid/backup/export/LocalBackupServiceTest.kt`
  - 覆盖三模式结构、字段完整性、缺图失败与 checksums 扩展点。
- `androidTest/java/com/example/itemmanagementandroid/backup/BackupExportIntegrationTest.kt`
  - 真机三模式导出解包校验；覆盖 `manifest/data/photos` 结构与关键字段。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsScreenInteractionTest.kt`
  - 覆盖设置页模式切换、单入口导出点击与状态文案展示。

## 23. Step 14 新增/修改文件职责（2026-03-08）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 23.1 备份导入契约与实现
- `main/java/com/example/itemmanagementandroid/backup/export/BackupService.kt`
  - 从“仅导出”扩展为“导入+导出”统一契约：新增 `importLocalBackup(backupFilePath)`。
- `main/java/com/example/itemmanagementandroid/backup/export/LocalBackupService.kt`
  - 新增导入代理能力：接收 `BackupImporter` 并转发导入请求；未配置导入器时返回参数错误。
- `main/java/com/example/itemmanagementandroid/backup/importing/BackupImporter.kt`
  - 导入器抽象契约，统一 `importLocalBackup(...)` 入口。
- `main/java/com/example/itemmanagementandroid/backup/importing/BackupImportMode.kt`
  - V0 导入模式枚举，固定 `replace_all`。
- `main/java/com/example/itemmanagementandroid/backup/importing/BackupImportResult.kt`
  - 导入结果模型（源文件、模式、导入时间、回滚快照路径、导入统计、告警列表）。
- `main/java/com/example/itemmanagementandroid/backup/importing/BackupImportStats.kt`
  - 导入统计模型（`categories/items/photos`）。
- `main/java/com/example/itemmanagementandroid/backup/importing/BackupImportWarning.kt`
  - 导入告警模型（兼容告警、记录跳过原因）。
- `main/java/com/example/itemmanagementandroid/backup/importing/BackupImportException.kt`
  - 导入错误分类（参数错误、包格式错误、快照失败、I/O 错误）。
- `main/java/com/example/itemmanagementandroid/backup/importing/BackupImportPayload.kt`
  - 导入中间模型：manifest/data 映射、archive 载荷、解析结果。
- `main/java/com/example/itemmanagementandroid/backup/importing/BackupArchiveReader.kt`
  - ZIP 读取与解包器：读取 `manifest.json`/`data.json`，提取 `photos/`，并执行路径穿越校验。
- `main/java/com/example/itemmanagementandroid/backup/importing/BackupJsonParser.kt`
  - JSON 解析器：忽略未知字段，解析 Category/Item/ItemPhoto，记录“缺字段/非法值跳过”告警。
- `main/java/com/example/itemmanagementandroid/backup/importing/LocalBackupImporter.kt`
  - Step 14 导入编排入口：`rollback snapshot -> parse -> replace_all transaction -> photo restore -> orphan cleanup`。
  - 版本兼容策略：`formatVersion/schemaVersion` 高于 `1.0` 时继续导入并写入告警。
  - 照片策略：恢复后统一落盘到 `files/photos/full|thumbs`，并清理旧引用文件与目录孤儿文件。

### 23.2 数据层与依赖注入接线
- `main/java/com/example/itemmanagementandroid/data/local/dao/CategoryDao.kt`
  - 新增导入辅助接口：`insertOrReplace`、`deleteAll`。
- `main/java/com/example/itemmanagementandroid/data/local/dao/ItemDao.kt`
  - 新增导入辅助接口：`insertOrReplace`、`deleteAll`。
- `main/java/com/example/itemmanagementandroid/data/local/dao/ItemPhotoDao.kt`
  - 新增导入辅助接口：`listAll`、`insertOrReplace`、`deleteAll`。
- `main/java/com/example/itemmanagementandroid/domain/usecase/backup/ImportLocalBackupUseCase.kt`
  - 新增导入用例入口，供 UI/测试调用导入能力。
- `main/java/com/example/itemmanagementandroid/ui/di/AppDependencies.kt`
  - 注入 `LocalBackupImporter`，并通过 `exportOnlyBackupService.exportLocalBackup(ExportMode.FULL)` 提供导入前自动回滚快照。
  - 新增 `importLocalBackupUseCase` 暴露。

### 23.3 Step 14 测试文件
- `androidTest/java/com/example/itemmanagementandroid/backup/BackupImportIntegrationTest.kt`
  - 新增设备导入集成测试：
    - `replace_all`：导入后本地数据完整替换，旧照片与孤儿文件被清理，回滚快照生成成功；
    - 兼容性：高版本 `formatVersion/schemaVersion` 与未知字段包可导入并产生告警。
- `test/java/com/example/itemmanagementandroid/data/repository/CategoryRepositoryImplTest.kt`
  - 更新 Fake DAO 以适配 `insertOrReplace/deleteAll` 新契约。
- `test/java/com/example/itemmanagementandroid/data/repository/ItemRepositoryImplTest.kt`
  - 更新 Fake DAO 以适配 `insertOrReplace/deleteAll` 新契约。
- `test/java/com/example/itemmanagementandroid/data/repository/PhotoRepositoryImplTest.kt`
  - 更新 Fake DAO 以适配 `listAll/insertOrReplace/deleteAll` 新契约。

## 24. Step 14A 新增/修改文件职责（2026-03-08）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 24.1 共享目录与 SAF 文档桥接层
- `main/java/com/example/itemmanagementandroid/backup/storage/BackupDocumentEntry.kt`
  - 共享目录可导入文件 DTO：`uri/displayName/sizeBytes/lastModified`。
- `main/java/com/example/itemmanagementandroid/backup/storage/BackupDirectoryInfo.kt`
  - Settings 目录状态模型：`treeUri/displayName/hasPersistedPermission`。
- `main/java/com/example/itemmanagementandroid/backup/storage/BackupDirectoryPreferenceStore.kt`
  - 持久化目录 URI（`SharedPreferences`），固定键 `backup_tree_uri`。
- `main/java/com/example/itemmanagementandroid/backup/storage/BackupDocumentStorage.kt`
  - 共享目录访问抽象：目录保存、目录读取、zip 列表、本地->文档复制、文档->临时文件复制。
- `main/java/com/example/itemmanagementandroid/backup/storage/SharedBackupDocumentStorage.kt`
  - SAF 具体实现：
    - `takePersistableUriPermission` 读写授权；
    - 目录顶层 `.zip` 列表与 `lastModified DESC` 排序；
    - `copyLocalFileToDocument(...)` 与 `copyDocumentToTempFile(...)` 桥接。
- `main/java/com/example/itemmanagementandroid/backup/storage/BackupSharedExportResult.kt`
  - “导出到共享目录”结果模型，承载文档条目与导出统计。

### 24.2 Step 14A 备份用例与 DI 接线
- `main/java/com/example/itemmanagementandroid/domain/usecase/backup/SetBackupDirectoryUseCase.kt`
  - 保存并授权共享目录 URI。
- `main/java/com/example/itemmanagementandroid/domain/usecase/backup/GetBackupDirectoryUseCase.kt`
  - 读取已保存目录与权限状态。
- `main/java/com/example/itemmanagementandroid/domain/usecase/backup/ListImportableBackupsUseCase.kt`
  - 列出目录内可导入 zip（仅顶层）。
- `main/java/com/example/itemmanagementandroid/domain/usecase/backup/ExportBackupToSharedDirectoryUseCase.kt`
  - 编排 `exportLocalBackup -> copyLocalFileToDocument -> 删除本地临时 zip`。
- `main/java/com/example/itemmanagementandroid/domain/usecase/backup/ImportBackupFromDocumentUseCase.kt`
  - 编排 `copyDocumentToTempFile -> importLocalBackup -> 删除临时 zip`。
- `main/java/com/example/itemmanagementandroid/ui/di/AppDependencies.kt`
  - 注入 `BackupDirectoryPreferenceStore`、`SharedBackupDocumentStorage` 与 Step 14A 新增 5 个用例暴露。
- `app/build.gradle.kts`
  - 新增 `androidx.documentfile` 依赖接入。
- `gradle/libs.versions.toml`
  - 新增 `documentfile` 版本与 `androidx-documentfile` 库别名。

### 24.3 Settings 状态机与 UI 入口升级
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsUiState.kt`
  - 新增目录状态、可导入文件列表、导入确认态、导入告警与导入执行态字段。
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsViewModel.kt`
  - 新增事件：
    - `onBackupDirectorySelected/refreshImportableBackups/exportBackupToSharedDirectory`
    - `requestImport/confirmImport/cancelImport/importFromSingleDocument`
  - 导入前统一进入确认弹窗，导入后输出统计与告警。
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsScreen.kt`
  - 新增 `OpenDocumentTree` 与 `OpenDocument` launcher；
  - 新增“选择备份目录 / 导出到共享目录 / 刷新目录备份 / 单文件导入兜底 / 目录内 zip 列表导入”交互区；
  - 新增导入确认弹窗（明确 `replace_all` 覆盖本地数据）。
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - Settings 分支改为注入 Step 14A 新用例并绑定新增回调。

### 24.4 Step 14A 测试文件
- `test/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsViewModelTest.kt`
  - 覆盖未选目录错误、目录授权后列表加载、导入确认流转、导入成功告警展示与失败映射。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsScreenInteractionTest.kt`
  - 覆盖新入口可见性、目录列表导入回调、导入确认弹窗确认/取消回调。

### 24.5 Step 14A 后续紧凑化修正（Settings）
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsScreen.kt`
  - 导出模式控件从“3 按钮组”改为“1 个下拉选单（`selectedExportMode` 驱动）”。
  - 页面布局从 `Column + 内层 LazyColumn` 改为单一外层 `LazyColumn`，保证导入列表与 `Back` 按钮可滚动可达。
  - 更新设置页测试标签契约：新增 `SCROLL_CONTAINER`、`EXPORT_MODE_DROPDOWN`、`exportModeMenuItem(...)`、`BACK_BUTTON`。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsScreenInteractionTest.kt`
  - 模式选择路径改为“打开下拉 -> 选择 `THUMBNAILS`”。
  - 新增单一滚动容器可达性回归：滚动到 `BACK_BUTTON` 并断言可见。

## 25. Step 15 新增/修改文件职责（2026-03-10）
> 范围：`apps/ItemManagementAndroid/app/src/`

### 25.1 导航与统一页面骨架
- `main/java/com/example/itemmanagementandroid/ui/navigation/AppNavigationUiState.kt`
  - 默认导航栈改为 `Category` 根入口，`currentRoute` 默认值同步切换到 `AppRoute.Category`。
- `main/java/com/example/itemmanagementandroid/ui/navigation/AppNavigationViewModel.kt`
  - 默认 `backStack` 根路由改为 `Category`。
  - 新增 `navigateToCategoryRoot()`：用于在任意页面一键回到根入口并清理中间栈。
- `main/java/com/example/itemmanagementandroid/ui/components/AppPageScaffold.kt`
  - 新增可复用页面壳：统一 TopAppBar、根/非根返回行为、Overflow 菜单、固定 `Refresh` 动作与扩展动作。
  - 固化页面骨架测试标签：`TOP_BAR/BACK_BUTTON/OVERFLOW_BUTTON/OVERFLOW_REFRESH_ACTION/overflowAction(...)`。

### 25.2 应用接线与页面行为
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - `Home` 退出主链路（仅兜底重定向到 `Category`），主流程改为以 `Category` 开始。
  - 全页面统一接入 `AppPageScaffold`，并按路由差异化注入 Overflow 动作：
    - 非 `Settings` 页：`Settings`
    - `Settings` 页：`Back To Category`
  - 固化页面显示即刷新：`Category/ItemList/ItemDetail/Settings` 在 `LaunchedEffect(currentRoute)` 触发 refresh。
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsViewModel.kt`
  - 新增公开 `refresh()`，将 init 刷新路径与页面显示/Overflow `Refresh` 统一。

### 25.3 非编辑页底部 Back 清理
- `main/java/com/example/itemmanagementandroid/ui/screens/category/CategoryScreen.kt`
  - 移除页面内 `Refresh` 与底部 `Back`，保留业务区操作（创建/归档开关/列表/跳转 ItemList）。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreen.kt`
  - 移除页面内 `Refresh` 与底部 `Back`，保留业务区过滤/排序/列表/跳转动作。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailScreen.kt`
  - 移除页面内 `Refresh` 与底部 `Back`，保留详情业务操作（编辑/删除/恢复）。
- `main/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsScreen.kt`
  - 移除页面内 `Go To Home` 与底部 `Back`，设置页导航统一由 TopBar/Overflow 承担。

### 25.4 Step 15 测试文件
- `test/java/com/example/itemmanagementandroid/ui/navigation/AppNavigationViewModelTest.kt`
  - 默认根路由断言改为 `Category`；新增 `navigateToCategoryRoot()` 回归用例。
- `androidTest/java/com/example/itemmanagementandroid/StartupSmokeTest.kt`
  - 启动断言改为 `Category` 首屏，并验证统一 TopBar/Overflow 可见。
- `androidTest/java/com/example/itemmanagementandroid/NavigationFlowIntegrationTest.kt`
  - 主链路改为 `Category -> ItemList -> ItemDetail -> ItemEdit`。
  - 新增 Overflow 回归：验证 `Refresh`、`Settings`、`Back To Category` 动作可达。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemedit/ItemEditFlowIntegrationTest.kt`
  - 非编辑页返回行为切换为 TopBar 返回按钮测试标签。
  - 流程起点改为 `Category` 根页，移除对 Home 主链路依赖。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreenInteractionTest.kt`
  - 适配 ItemList 移除 `onBack/onRefresh` 参数与底部 Back 契约。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemdetail/ItemDetailScreenInteractionTest.kt`
  - 适配 ItemDetail 移除 `canGoBack/onBack/onRefresh` 参数。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/category/CategoryScreenInteractionTest.kt`
  - 适配 Category 移除 `canGoBack/onBack/onRefresh` 参数。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/settings/SettingsScreenInteractionTest.kt`
  - 适配 Settings 移除 `canGoBack/onNavigate/onBack` 契约。
  - 滚动可达性回归目标改为“目录末尾导入按钮可见”。

### 25.5 Step 15 后续微调：Category/ItemList Toggle 迁移到 Overflow（2026-03-10）
- `main/java/com/example/itemmanagementandroid/ui/ItemManagementApp.kt`
  - 在 `AppRoute.Category` 分支新增 Overflow 动作 `toggle_include_archived`，动作触发 `setIncludeArchived(!includeArchived)`。
  - 在 `AppRoute.ItemList` 分支新增 Overflow 动作 `toggle_include_deleted`，动作触发 `setIncludeDeleted(!includeDeleted)`。
  - 两个页面 Overflow 动作顺序统一为：`Refresh`（固定）-> `Toggle...` -> `Settings`。
- `main/java/com/example/itemmanagementandroid/ui/screens/category/CategoryScreen.kt`
  - 移除正文按钮 `Toggle Include Archived`，避免页面正文与 Overflow 双入口重复。
  - 删除 `onToggleIncludeArchived` 参数与对应测试标签常量，页面职责收敛为内容渲染与业务回调。
- `main/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreen.kt`
  - 移除正文按钮 `Toggle Include Deleted`。
  - 删除 `onToggleIncludeDeleted` 参数与对应测试标签常量，保持 ItemList 过滤/排序/列表主职责。
- `androidTest/java/com/example/itemmanagementandroid/NavigationFlowIntegrationTest.kt`
  - 新增 Overflow toggle 回归：校验
    - `overflowAction("toggle_include_archived")`
    - `overflowAction("toggle_include_deleted")`
  - 校验点击后状态文案即时变化（`Include archived/deleted`）。
  - 将 `navigateMainFlowAndBack` 中进入编辑页断言改为基于 `ItemDetailScreenTestTags.EDIT_BUTTON` 与 `ItemEditScreenTestTags.ROOT`，降低文本可见性抖动导致的误报。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/category/CategoryScreenInteractionTest.kt`
  - 新增正文不再显示 `Toggle Include Archived` 文案断言。
- `androidTest/java/com/example/itemmanagementandroid/ui/screens/itemlist/ItemListScreenInteractionTest.kt`
  - 新增正文不再显示 `Toggle Include Deleted` 文案断言。

### 25.6 架构洞察日志（Step 15 后续）
- 将“全局展示级开关”（include archived/deleted）统一迁移到 `AppPageScaffold` Overflow 后，页面正文组件可减少跨层导航壳耦合参数，Composable 签名更稳定。
- 该迁移不触及 Repository/UseCase/ViewModel 数据语义，仅调整交互入口；因此属于 UI 契约层变更，可通过现有状态流测试与导航回归覆盖风险。
