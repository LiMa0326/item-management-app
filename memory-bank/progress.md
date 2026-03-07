# 进度记录（Progress）

## 记录规则
- 每完成一个 Step，更新：状态、关键产出、测试结果、阻塞项、下一步。
- 状态枚举：`todo` / `in_progress` / `done` / `blocked`。

## 当前总览（截至 2026-03-07）
- 当前阶段：V0 Step 09 已完成，等待用户验证放行
- 总状态：`in_progress`
- 说明：已完成 Step 09（物品新增/编辑页面），已提交代码并完成单测 + 编辑页交互设备测试 + 导航/启动回归；等待用户在 Android Studio 编译并机测确认后再进入 Step 10。

## 里程碑日志
### 2026-03-03 - 文档一致性修订
- 状态：`done`
- 关键产出：
  - 更新 `implementation-plan.md` 以对齐 14 条澄清结果。
  - 更新 `design-document.md`，将开放问题改为已冻结决策。
  - 更新 `tech-stack.md` 与 `BACKUP_FORMAT.md`，固化导出默认、导入策略、校验策略与缩略图规格。
  - 补全 `architecture.md`（文件职责 + 完整数据库结构 + 架构洞察日志）。
- 阻塞项：无
- 下一步：
  1. 按 `implementation-plan.md` 从 Step 01 开始执行。
  2. 每完成一个 Step 同步更新本文件与 `architecture.md`。

### 2026-03-04 - Step 01：V0 工程骨架与模块边界
- 状态：`done`
- 关键产出：
  - 在 Android 工程内建立分层模块边界占位：`ui`、`domain`、`data`、`backup`、`photo`，并新增模块标记文件用于固定目录职责。
  - 新增最小路由与导航状态：`AppRoute`、`AppNavigatorState`，形成内存态页面切换骨架。
  - 新增 6 个独立页面壳：`Home`、`Category`、`ItemList`、`ItemDetail`、`ItemEdit`、`Settings`，每页具备标题、空状态文案与导航/返回入口。
  - 新增 `ItemManagementApp` 作为 UI 根壳并接管页面路由；更新 `MainActivity` 入口由模板 `Greeting` 切换到应用壳。
  - 新增启动冒烟测试 `StartupSmokeTest`，校验启动后首页壳文案可见。
- 测试结果：
  - 用户验证：编译通过；真机手工页面切换验证通过（无明显问题）。
  - 自动化（Agent）：
    - `:app:testDebugUnitTest`：通过。
    - `:app:connectedDebugAndroidTest`：通过（设备 `SM-S901U1 - Android 14`，执行 2 项测试）。
    - `:app:lintDebug`：通过（`0 error, 11 warnings`）。
  - lint 主要告警：
    - `AndroidManifest.xml` 的 Activity `label` 与 Application 重复（`RedundantLabel`）。
    - 依赖版本可升级提示（AGP/Kotlin/Compose BOM）。
    - `res/values/colors.xml` 存在未使用资源（模板遗留）。
- 阻塞项：无（测试执行阶段曾遇到沙箱 Java/Gradle 访问限制，已通过本机环境执行规避，不影响代码结果）。
- 下一步：
  1. 等待用户明确放行后再进入 Step 02（本地数据模型与数据库 v1）。
  2. Step 02 开始前保持当前 Step 01 代码与文档基线不变。

### 2026-03-05 - Step 02：本地数据模型与数据库 Schema v1
- 状态：`done`
- 关键产出：
  - Gradle 接入 Room + KSP：新增 `room-runtime`、`room-ktx`、`room-compiler`、`room-testing`，并启用 KSP `room.schemaLocation` 参数。
  - 新增 3 个持久化实体：
    - `CategoryEntity`（`categories`，含 `is_system_default` 与排序/归档索引）
    - `ItemEntity`（`items`，含 `purchase_currency`、`tags_json`、`custom_attributes_json`、`deleted_at` 与多列索引）
    - `ItemPhotoEntity`（`item_photos`，含 `local_uri`、`thumbnail_uri`、`content_type`）
  - 新增数据库契约：
    - `ItemManagementDatabase`（Room `@Database`，`version = 1`）
    - `DatabaseVersions`（`SCHEMA_V1 = 1`，`CURRENT = 1`）
    - `DatabaseMigrations`（迁移入口占位，当前为空）
  - 新增 `androidTest`：`DatabaseSchemaV1Test`，覆盖表存在性、字段完整性、NOT NULL 约束与 `PRAGMA user_version` 校验。
  - 创建 `app/schemas/` 目录占位（`.gitkeep`），用于 Room schema 导出路径。
- 测试结果：
  - 自动化（Agent）通过：
    - `:app:compileDebugKotlin`：通过
    - `:app:testDebugUnitTest`：通过
    - `:app:compileDebugAndroidTestKotlin`：通过
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.data.local.db.DatabaseSchemaV1Test`：通过（设备 `SM-S901U1 - Android 14`，执行 5 项测试）
  - 构建备注：
    - 新增 `gradle.properties` 开关 `android.disallowKotlinSourceSets=false`，用于兼容 AGP 9 内建 Kotlin 与 KSP/Room 的当前校验限制（编译期告警可见，功能不受影响）。
- 阻塞项：无
- 下一步：
  1. 等待用户在 Android Studio 复测并确认 Step 02 验证通过。
  2. 在用户明确“验证通过”前，不进入 Step 03。

### 2026-03-05 - Step 03：Category 数据层与默认分类初始化
- 状态：`done`
- 关键产出：
  - 新增 `CategoryDao`，落地类别有序查询、按 ID 查询、默认分类计数、插入更新与排序更新 SQL。
  - 新增 Category 领域模型与仓储接口：`Category`、`DefaultCategories`、`CategoryRepository`。
  - 新增 `CategoryRepositoryImpl`，实现 `list/create/update/setArchived/reorder/ensureDefaultCategory` 六类能力，补齐名称非空校验与统一 ISO-8601 时间戳写入。
  - 新增 Category UseCase 组：`ListCategoriesUseCase`、`CreateCategoryUseCase`、`UpdateCategoryUseCase`、`SetCategoryArchivedUseCase`、`ReorderCategoriesUseCase`、`EnsureDefaultCategoryUseCase`。
  - 在 `MainActivity` 启动链路接入默认分类初始化：应用首启自动写入默认分类“电子产品”（`cat_electronics`，`is_system_default=1`）。
  - 新增 `CategoryRepositoryImplTest`，覆盖默认分类幂等初始化、Category CRUD/归档过滤、reorder 排序结果校验。
- 测试结果：
  - 自动化（Agent）尝试执行：
    - `:app:testDebugUnitTest`：未执行成功（环境缺少 Java 运行时，报错 `JAVA_HOME is not set and no 'java' command could be found in your PATH`）。
  - 当前可执行测试结论：受本机终端 JDK 环境限制，Gradle 测试未能在 CLI 内完成；待用户在 Android Studio/JDK 完整环境复测。
- 阻塞项：
  - 终端环境未配置 JDK（`java` 命令缺失）。
- 下一步：
  1. 用户在 Android Studio 编译并机测 Step 03（默认分类初始化、类别新增/修改/归档/排序）。
  2. 用户明确“机测通过”前，不进入 Step 04。

### 2026-03-05 - Step 04：Item 数据层（含软删除与恢复）
- 状态：`done`
- 关键产出：
  - 新增 `ItemDao`，落地 Item 全量/未删除查询、按 ID 查询、插入与更新 SQL，查询排序固定为 `updated_at DESC, created_at DESC`。
  - 在 `ItemManagementDatabase` 新增 `itemDao()` 接口，完成 Item 数据层数据库接入。
  - 新增 Item 领域模型与输入模型：`Item`、`ItemDraft`。
  - 新增 Item 仓储接口与实现：`ItemRepository`、`ItemRepositoryImpl`，覆盖 `list/get/create/update/softDelete/restore` 六类能力。
  - 在 `ItemRepositoryImpl` 固化 JSON 字段策略：`tags` <-> `tags_json`，`customAttributes` <-> `custom_attributes_json`，并校验自定义属性值类型仅允许 `string|number|boolean`。
  - 新增 Item UseCase 组：`ListItemsUseCase`、`GetItemUseCase`、`CreateItemUseCase`、`UpdateItemUseCase`、`SoftDeleteItemUseCase`、`RestoreItemUseCase`。
  - 新增 `ItemRepositoryImplTest`，覆盖 create/get/update、软删除/恢复可见性、删除/恢复幂等行为、非法 customAttributes 拦截与 JSON 往返校验。
- 测试结果：
  - 自动化（Agent）通过：
    - `:app:testDebugUnitTest`
    - `:app:testDebugUnitTest --tests "com.example.itemmanagementandroid.data.repository.ItemRepositoryImplTest"`
    - `:app:compileDebugKotlin`
  - 构建备注：
    - 当前 CLI 通过设置 `GRADLE_OPTS=-Dkotlin.compiler.execution.strategy=in-process` 稳定执行 Gradle；Kotlin daemon 在本机权限受限时会自动回退到非 daemon 编译，不影响结果。
    - AGP 初始化阶段会打印 `CodexSandboxOffline/.android` 指标目录告警，但不阻断构建与测试。
    - `adb` 未在 PATH，因此设备侧测试仍由用户在 Android Studio 执行。
- 阻塞项：无代码阻塞（等待用户机测放行）
- 下一步：
  1. 用户在 Android Studio 编译并机测 Step 04（Item 新增/编辑/软删除/恢复）。
  2. 用户明确“机测通过”前，不进入 Step 05。

### 2026-03-06 - Step 05：ItemPhoto 数据层与本地文件索引
- 状态：`done`
- 关键产出：
  - 新增 `ItemPhotoDao` 与 `DeferredPhotoCleanupRow`，落地按物品查询、按 ID 查询、插入、按 ID 删除，以及基于 `items.deleted_at IS NOT NULL` 的延迟清理候选查询。
  - 在 `ItemManagementDatabase` 新增 `itemPhotoDao()` 接口，完成 ItemPhoto 数据层数据库接入（`schema v1` 无迁移变更）。
  - 新增照片领域模型与仓储契约：`ItemPhoto`、`ItemPhotoDraft`、`DeferredPhotoCleanupCandidate`、`PhotoRepository`。
  - 新增 `PhotoRepositoryImpl`，实现 `listByItem/get/add/remove/listDeferredCleanupCandidates`，补齐字段归一化、尺寸合法性校验、删除幂等返回与延迟清理 marker 映射（`ITEM_SOFT_DELETED`）。
  - 新增 `photo/PhotoBackupFileNameMapper`，固化备份文件名映射规则：
    - `full` -> `<photoId>.<ext>`
    - `thumbnails` -> `<photoId>_thumb.<ext>`
    - 扩展名优先级：`contentType -> uri extension -> jpg`
  - 新增 Photo UseCase 组：`ListItemPhotosUseCase`、`GetItemPhotoUseCase`、`AddItemPhotoUseCase`、`RemoveItemPhotoUseCase`、`ListDeferredPhotoCleanupCandidatesUseCase`。
  - 新增单元测试：`PhotoRepositoryImplTest`、`PhotoBackupFileNameMapperTest`。
- 测试结果：
  - 自动化（Agent）通过：
    - `:app:compileDebugKotlin`
    - `:app:testDebugUnitTest --tests "com.example.itemmanagementandroid.data.repository.PhotoRepositoryImplTest"`
    - `:app:testDebugUnitTest --tests "com.example.itemmanagementandroid.photo.PhotoBackupFileNameMapperTest"`
    - `:app:testDebugUnitTest`
  - 构建备注：
    - 在 CLI 中需设置 `ANDROID_USER_HOME` 到可写目录与 `GRADLE_OPTS=-Dkotlin.compiler.execution.strategy=in-process`。
    - 并行执行 Gradle 任务时曾触发一次 KSP 目录创建冲突，改为串行后全部通过。
- 阻塞项：无代码阻塞（等待用户机测放行）
- 下一步：
  1. 用户在 Android Studio 编译并机测 Step 05（同一物品多照片、详情回读、删除幂等、软删除后延迟清理候选可见、恢复后候选消失）。
  2. 用户明确“机测通过”前，不进入 Step 06。

### 2026-03-06 - Step 06：V0 导航骨架与全局状态管理
- 状态：`done`
- 关键产出：
  - 将应用导航状态从 `AppNavigatorState` 迁移到 `AppNavigationViewModel + AppNavigationUiState`，统一管理 `currentRoute`、`canGoBack` 与 back stack。
  - 新增应用级依赖装配层 `ui/di/AppDependencies`，集中构建并暴露 `ListCategoriesUseCase`、`ListItemsUseCase`、`GetItemUseCase`、`ListItemPhotosUseCase`，确保 UI 不直接访问 DAO/Repository/DB。
  - 为 6 个页面落地 `UiState + ViewModel`：
    - `Home`：读取类别数与物品数；
    - `Category`：读取类别列表（支持 includeArchived 开关）；
    - `ItemList`：读取物品列表（支持 includeDeleted 开关）；
    - `ItemDetail`：读取“首条可见物品 + 照片数量”；
    - `ItemEdit`：读取“编辑模式 + 目标物品占位 + 可用类别数”；
    - `Settings`：提供离线优先与备份/同步占位状态文案。
  - 重构 6 个 `Screen` 的 Composable 签名为“`state + callbacks`”模式，页面只渲染状态并派发事件。
  - 固化导航主链路按钮流：`Home -> Category -> ItemList -> ItemDetail -> ItemEdit -> Back`，并统一 `Back` 按钮受 `canGoBack` 约束。
  - 新增测试：
    - `NavigationFlowIntegrationTest`（androidTest）：覆盖主链路跳转与回退断言。
    - `AppNavigationViewModelTest`（JVM）：覆盖导航状态与幂等导航行为。
  - 更新 `StartupSmokeTest` 断言，改为校验首页关键入口按钮（`Go To Category`、`Refresh`）。
- 测试结果：
  - 自动化（Agent）通过：
    - `:app:compileDebugKotlin`
    - `:app:testDebugUnitTest`
    - `:app:uninstallDebug :app:uninstallDebugAndroidTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.NavigationFlowIntegrationTest`
  - 设备与环境备注：
    - 设备：`SM-S901U1 - Android 14`。
    - 设备测试首次执行时，PowerShell 对 `-Pandroid.testInstrumentationRunnerArguments.class=...` 参数发生拆分；改为 `gradlew --%` 后命令稳定执行并通过。
- 阻塞项：无代码阻塞（等待用户 Android Studio 编译与机测复验）
- 下一步：
  1. 用户在 Android Studio 编译并机测 Step 06（导航主链路、回退行为、页面状态展示）。
  2. 用户明确“机测通过”前，不进入 Step 07。

### 2026-03-06 - Step 07：类别管理页面
- 状态：`done`
- 关键产出：
  - 扩展 `ui/di/AppDependencies`，新增并暴露 `CreateCategoryUseCase`、`UpdateCategoryUseCase`、`SetCategoryArchivedUseCase`、`ReorderCategoriesUseCase`，保留原有 `ListCategoriesUseCase`、`ListItemsUseCase`。
  - 在 `ItemManagementApp` 完成 `CategoryViewModel` 新依赖注入与 `CategoryScreen` 新回调绑定。
  - 新增 `CategoryListItemUiModel`，并将 `CategoryUiState.categories` 从 `List<Category>` 切换为带 `itemCount` 的 UI 模型列表。
  - 重构 `CategoryViewModel`：新增 `createCategory/renameCategory/setCategoryArchived/moveCategoryUp/moveCategoryDown`，并实现“仅重排可见项、隐藏项相对顺序保持不变”的排序策略；类别列表加载时联动 `ListItemsUseCase(includeDeleted=false)` 统计每类物品数量。
  - 重构 `CategoryScreen`：实现类别列表、创建/重命名对话框、归档/取消归档、上移/下移、物品数量显示、系统默认标识；继续保留导航主链路按钮。
  - 新增 `CategoryScreenTestTags` 统一控件标识，并新增 `CategoryScreenInteractionTest` 覆盖创建、重命名、归档切换、上移/下移、includeArchived 切换交互回调。
- 测试结果：
  - 自动化（Agent）通过：
    - `:app:compileDebugKotlin`
    - `:app:testDebugUnitTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.ui.screens.category.CategoryScreenInteractionTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.NavigationFlowIntegrationTest`
    - `:app:connectedDebugAndroidTest`
  - 设备与环境备注：
    - 设备：`SM-S901U1 - Android 14`。
    - `CategoryScreenInteractionTest` 初版 `moveUp` 用例在小屏设备上因列表可视区域问题未触发；测试中补充 `performScrollToNode` 后稳定通过。
- 阻塞项：无代码阻塞（等待用户 Android Studio 编译与机测复验）
- 下一步：
  1. 用户在 Android Studio 编译并机测 Step 07（新增类别、重命名、归档/取消归档、排序、物品数量显示）。
  2. 用户明确“机测通过”前，不进入 Step 08。

### 2026-03-06 - Step 08：物品列表页面（过滤与排序）
- 状态：`done`
- 关键产出：
  - 新增列表查询契约：`ItemListQuery`、`ItemListSortOption`，并扩展 `ItemRepository`/`ListItemsUseCase` 支持 query 入口，同时保留 `includeDeleted` 兼容调用。
  - 在 `ItemRepositoryImpl` 落地 Step 08 列表语义：
    - 类别过滤（`categoryId`）；
    - 排序项 `RECENTLY_ADDED`、`RECENTLY_UPDATED`、`PURCHASE_DATE`、`PURCHASE_PRICE`；
    - 购买日期与价格空值统一后置；
    - 稳定兜底排序 `updatedAt DESC -> createdAt DESC -> id ASC`。
  - 重构 `ItemListUiState` 与 `ItemListViewModel`：
    - 新增 `sortOption`、`selectedCategoryId`、`categoryFilters`、`hasAnyItemsInCurrentMode`；
    - 新增交互入口 `setCategoryFilter`、`setSortOption`；
    - 接入 `ListCategoriesUseCase(includeArchived=true)`，筛选项支持归档类别显示。
  - 重构 `ItemListScreen`：新增类别筛选区（含 `All`）、排序切换区、空状态与无结果状态文案，并补充 `ItemListScreenTestTags`；保留导航按钮 `Go To Item Detail`、`Back`。
  - 新增设备测试 `ItemListScreenInteractionTest`，覆盖筛选/排序回调、空状态/无结果状态、导航按钮回调。
  - 扩展 `ItemRepositoryImplTest`，新增 Step 08 数据层用例：类别过滤、日期排序空值后置、价格降序空值后置、软删除与类别过滤组合。
- 测试结果：
  - 自动化（Agent）通过：
    - `:app:compileDebugKotlin`
    - `:app:testDebugUnitTest --tests "com.example.itemmanagementandroid.data.repository.ItemRepositoryImplTest"`
    - `:app:testDebugUnitTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.ui.screens.itemlist.ItemListScreenInteractionTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.NavigationFlowIntegrationTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.StartupSmokeTest`
    - `:app:connectedDebugAndroidTest`（全量，19 项设备测试）
  - 设备与环境备注：
    - 设备：`SM-S901U1 - Android 14`。
    - 首次运行设备测试出现 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`（历史安装包签名不一致）；执行 `:app:uninstallDebug :app:uninstallDebugAndroidTest` 后恢复正常并全部通过。
- 阻塞项：无代码阻塞（等待用户 Android Studio 编译与机测复验）
- 下一步：
  1. 用户在 Android Studio 编译并机测 Step 08（类别过滤、四种排序、空状态/无结果状态、默认隐藏软删除项）。
  2. 用户明确“机测通过”前，不进入 Step 09。

### 2026-03-07 - Step 09：物品新增/编辑页面
- 状态：`done`
- 关键产出：
  - 将 `AppRoute.ItemEdit` 从固定对象路由升级为 `AppRoute.ItemEdit(itemId: String?)`，支持新建与编辑双入口。
  - 在 `ItemDetailScreen` 编辑入口传递 `selectedItemId`，在 `ItemListScreen` 新增“Go To Item Edit”按钮作为新建入口。
  - 扩展 `AppDependencies`，新增 `CreateItemUseCase`、`UpdateItemUseCase` 并注入 `ItemEditViewModel`。
  - 重构 `ItemEditUiState` 与 `ItemEditViewModel`，实现完整表单字段、字段级错误、保存状态、保存结果回显、取消不落库。
  - 新增 `ItemEditFormMapper`，统一处理：
    - `tags` 逗号拆分去重
    - `purchasePrice` 可选数值解析
    - `customAttributes` key-value 行解析（`boolean -> number -> string`）
  - 重构 `ItemEditScreen` 为真实表单页面，补齐 `Save/Cancel`、属性行增删、错误提示与 `testTag` 契约。
  - 新增测试：
    - JVM：`ItemEditFormMapperTest`
    - 设备：`ItemEditScreenInteractionTest`
    - 更新：`NavigationFlowIntegrationTest`（编辑页回退断言改用 `Cancel` + 滚动）
- 测试结果：
  - 自动化（Agent）通过：
    - `:app:testDebugUnitTest`
    - `:app:testDebugUnitTest --tests "com.example.itemmanagementandroid.data.repository.ItemRepositoryImplTest"`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.ui.screens.itemedit.ItemEditScreenInteractionTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.NavigationFlowIntegrationTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.StartupSmokeTest`
    - `:app:connectedDebugAndroidTest`（全量，22 项设备测试）
  - 环境备注：
    - 设备测试在沙箱内会被 ADB 权限拦截，需提权执行；
    - 首次提权全量执行曾出现失败，已定位并修复为测试用例问题，最终全量设备测试通过。
- 阻塞项：无代码阻塞（等待用户 Android Studio 编译与机测复验）
- 下一步：
  1. 用户在 Android Studio 编译并机测 Step 09（空名称拦截、合法保存、扩展字段保存回显、新建/编辑入口行为）。
  2. 用户明确“机测通过”前，不进入 Step 10。
