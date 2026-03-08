# 进度记录（Progress）

## 记录规则
- 每完成一个 Step，更新：状态、关键产出、测试结果、阻塞项、下一步。
- 状态枚举：`todo` / `in_progress` / `done` / `blocked`。

## 当前总览（截至 2026-03-08）
- 当前阶段：V0 Step 14A 后续紧凑化已完成（导出模式下拉 + 单一滚动容器）
- 总状态：`in_progress`
- 说明：已完成 Step 14A 与后续 Settings 紧凑化修正，并通过 JVM + 定向设备测试 + 全量设备回归（43/43）；用户已反馈“测试完成”，当前保持不进入 Step 15，等待下一步指令。

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

### 2026-03-07 - Step 10：物品详情页与删除/恢复闭环
- 状态：`done`
- 关键产出：
  - 将 `AppRoute.ItemDetail` 从对象路由升级为 `AppRoute.ItemDetail(itemId: String?)`，支持“指定 item 详情 / 空参兜底”双入口。
  - 在 `ItemListScreen` 增加“行点击进入详情”能力：每行点击跳转 `AppRoute.ItemDetail(item.id)`；保留 `Go To Item Detail` 按钮作为 `itemId=null` 兜底入口。
  - 优化新建/编辑入口文案语义：列表页新建按钮文案调整为 `New Item`，详情页编辑按钮文案调整为 `Edit Item`。
  - 在 `AppDependencies` 新增并暴露 `SoftDeleteItemUseCase`、`RestoreItemUseCase`。
  - 重构 `ItemDetailUiState`：扩展为完整字段视图（基础字段、`tags`、`customAttributes`、`createdAt`、`updatedAt`、`deletedAt`、照片元数据墙、操作状态与错误/结果提示）。
  - 重构 `ItemDetailViewModel`：接收 `initialItemId`，优先加载指定 item；空参回退首条可见 item；新增 `softDelete/restore` 事件并保持详情上下文完成删除恢复闭环。
  - 重构 `ItemDetailScreen`：从占位页升级为完整详情页，新增删除/恢复入口与 `ItemDetailScreenTestTags`；编辑入口支持无 item 时回退新建模式（`ItemEdit(null)`）。
  - 在 `ItemManagementApp` 中按 `itemId` 隔离 `ItemDetailViewModel` key，并在列表页路由进入时通过 `LaunchedEffect` 刷新，保证删除返回列表后默认模式不显示已删项。
  - 新增设备测试 `ItemDetailScreenInteractionTest`；更新 `ItemListScreenInteractionTest`（适配 `ItemDetail(itemId)` 路由断言并新增行点击用例）；更新 `AppNavigationViewModelTest`（覆盖 data class 路由幂等行为）。
- 测试结果：
  - 自动化（Agent）通过：
    - `:app:testDebugUnitTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.ui.screens.itemdetail.ItemDetailScreenInteractionTest`（3 项）
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.NavigationFlowIntegrationTest`（1 项）
    - `:app:connectedDebugAndroidTest`（全量，26 项设备测试，设备 `SM-S901U1 - Android 14`）
  - 执行备注：
    - 首次 `:app:testDebugUnitTest` 命中编译缓存异常（`ItemManagementApp` unresolved 链式报错），执行 `:app:clean :app:compileDebugKotlin` 后恢复并通过。
    - 首次设备测试编译失败为 `assertDoesNotExist` API 兼容问题，改为兼容断言后通过。
    - 首次导航集成测试失败为详情页无 item 时编辑按钮禁用，按 Step 10 要求将编辑入口保持可用（空参走 `ItemEdit(null)`）后通过。
- 阻塞项：无代码阻塞（等待用户 Android Studio 编译与机测复验）
- 下一步：
  1. 用户在 Android Studio 编译并机测 Step 10（详情字段/照片墙展示、删除后列表隐藏、显示已删除后详情恢复、恢复后列表重现）。
  2. 用户明确“机测通过”前，不进入 Step 11。

### 2026-03-07 - Step 10 Bugfix：`New Item` 空表单重入 + 全局重名拦截
- 状态：`done`
- 关键产出：
  - 修复 `ItemEdit` 路由重入生命周期：在 `ItemEditViewModel` 新增 `onRouteEntered(itemId)` 与 `requestedItemId`，并在 `ItemManagementApp` 的 `ItemEdit` 分支通过 `LaunchedEffect(currentRoute.itemId)` 每次进入同步路由状态，确保连续点击 `New Item` 始终回到 `CREATE` 空表单。
  - 修复 `refresh()` 回退逻辑：按 `requestedItemId` 重载，避免回退到历史 `editingItemId` 导致“新建复用旧记录”。
  - 调整保存后行为：`CREATE` 保存成功后保持 `CREATE` 模式并清空表单（保留 `Item created.` 提示）；`EDIT` 保存成功保持编辑态。
  - 新增全局重名规则（仅未删除范围，`trim + case-insensitive`）并下沉数据层：
    - `ItemDao` 新增 `countActiveByNormalizedName` 与 `countActiveByNormalizedNameExcludingId`。
    - `ItemRepositoryImpl.create/update` 新增冲突检查并抛出 `DuplicateItemNameException`。
    - `ItemEditViewModel.save()` 捕获重名异常并映射到 `fieldErrors.name`，不走通用错误弹层。
  - 新增/更新测试：
    - JVM：`ItemRepositoryImplTest`（创建重名拒绝、软删后允许同名、更新排除自身）；
    - JVM：新增 `ItemEditViewModelTest`（连续 `ItemEdit(null)` 为空表单、`CREATE` 保存后仍空表单、重名映射名称字段错误）；
    - 设备：新增 `ItemEditFlowIntegrationTest`（`ItemList -> New Item -> Save -> Back -> New Item` 名称输入为空；重名创建显示 `Item name already exists.`）。
- 测试结果：
  - 自动化（Agent）通过：
    - `:app:testDebugUnitTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.ui.screens.itemedit.ItemEditFlowIntegrationTest`（1 项）
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.NavigationFlowIntegrationTest`（1 项）
    - `:app:connectedDebugAndroidTest`（全量，27 项设备测试，设备 `SM-S901U1 - Android 14`）
  - 执行备注：
    - 首次定向设备测试编译失败：`assertExists` 在当前 Compose 测试版本不可用，已改为兼容断言。
    - 首次流程断言失败是测试断言口径问题（`assertTextEquals` 误把 `TextField` 标签文本算入值），已改为仅断言 `EditableText` 为空，复跑通过。
- 阻塞项：无代码阻塞（等待用户 Android Studio 编译与机测复验）
- 下一步：
  1. 用户在 Android Studio 编译并机测本次 bugfix（连续 `New Item` 为空表单、保存后新建不串态、重名拦截文案）。
  2. 用户明确“机测通过”前，不进入 Step 11。

### 2026-03-07 - Step 11：V0 搜索（最小实现）
- 状态：`done`
- 关键产出：
  - 扩展 `ItemListQuery`，新增 `searchKeyword` 查询参数；保持默认值向后兼容。
  - 在 `ItemDao` 新增统一查询入口 `listByQuery(...)`，支持 `includeDeleted + categoryId + searchKeyword` 组合过滤。
  - 固化搜索语义：
    - `name/description/purchasePlace`：大小写不敏感 + 子串匹配（`LIKE %kw%`）
    - `tags`：整词匹配（`lower(tags_json) LIKE %"kw"%`）
    - 空关键词跳过搜索条件。
  - 在 `ItemRepositoryImpl` 下沉关键词标准化与 LIKE 转义（处理 `%`、`_`、`\`），并保持 Step 08 既有排序策略与空值后置规则不变。
  - 重构 `ItemListUiState` / `ItemListViewModel` / `ItemListScreen`：
    - 新增 `searchKeyword` 状态与 `setSearchKeyword(...)` 事件；
    - 列表页新增搜索输入框（输入即搜索）；
    - `hasAnyItemsInCurrentMode` 口径更新为“同 includeDeleted + 同类别 + 不带 keyword”。
  - 在 `ItemManagementApp` 绑定 `onSearchKeywordChanged` 到 `ItemListViewModel`。
  - 新增设备级 SQLite 语义回归测试：`ItemSearchQueryIntegrationTest`。
  - 文档化升级触发规则：当本地数据量 `> 3000` 且搜索响应不可接受时，进入 FTS 升级任务（本步不实现 FTS）。
- 测试结果：
  - 自动化（Agent）通过：
    - `:app:testDebugUnitTest --tests "com.example.itemmanagementandroid.data.repository.ItemRepositoryImplTest"`
    - `:app:testDebugUnitTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.ui.screens.itemlist.ItemListScreenInteractionTest`
    - `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.data.repository.ItemSearchQueryIntegrationTest`
    - `:app:connectedDebugAndroidTest`（全量，30 项设备测试）
  - 设备与环境备注：
    - 设备：`SM-S901U1 - Android 14`。
    - `ItemListScreenInteractionTest` 的行点击用例在小屏下因可视区域不足出现不稳定，已改为语义点击断言稳定通过。
  - 性能基线（Step 11 记录）：
    - JVM 抽样基线（`ItemRepositoryImplTest.searchBaseline_1000Items_recordsElapsedMillis`）：`SEARCH_BASELINE_1000_MS=3`（Fake DAO 环境，仅用于趋势记录，不作为 SLA）。
- 阻塞项：无代码阻塞（等待用户 Android Studio 编译与机测复验）
- 下一步：
  1. 用户在 Android Studio 编译并机测 Step 11（50 条关键词命中/未命中、搜索+类别过滤、搜索后排序切换、软删除开关组合）。
  2. 用户明确“机测通过”前，不进入 Step 12。

### 2026-03-07 - Step 12：照片处理策略（编辑页入口）
- 状态：`done`
- 关键产出：
  - 新增照片处理子系统：`Uri -> EXIF 方向矫正 -> 原图 JPEG(无 EXIF) + 缩略图 JPEG(长边 1280, 质量 85) -> 私有目录 files/photos/full|thumbs`。
  - 新增导入编排与结果模型：`ImportItemPhotosUseCase`、`PhotoImportSummary`、`PhotoImportFailure`，支持部分失败汇总与重试。
  - 扩展 `PhotoRepository` 与 DAO，新增按 itemIds 批量封面缩略图查询，供 `ItemList` 一次性加载封面，避免 N+1。
  - 重构 `ItemEdit`：新增 `拍照/选单张/选多张/重试失败导入` 入口，新增导入状态文案与照片缩略图区。
  - 落地“新建态加图自动建项”：若名称为空，自动生成 `New Item_yyyyMMdd_HHmmss` 后继续导图并切换编辑态。
  - 更新 `ItemList` 与 `ItemDetail`：列表显示封面缩略图；详情照片墙展示实际图片（优先原图路径）。
  - 完成 Android 接入：新增 `FileProvider` 与 `res/xml/file_paths.xml`，支持拍照落盘流程。
  - 新增/更新测试：
    - JVM：`ImportItemPhotosUseCaseTest`、`ItemEditViewModelTest`（自动建项与重试流转）、`PhotoRepositoryImplTest`（封面查询）
    - 设备：`AndroidPhotoAssetProcessorIntegrationTest`（20 图处理）、`ItemEditScreenInteractionTest`、`ItemListScreenInteractionTest`、`ItemDetailScreenInteractionTest`
- 测试结果：
  - 自动化（Agent）通过：
    - `.\gradlew.bat test`
    - `.\gradlew.bat connectedAndroidTest`（全量 33 项）
  - 真机设备：
    - `SM-S901U1 - Android 15`（ADB 直连）
    - 已执行设备端全量自动化链路，包含 Step 12 新增 20 图处理集成测试与页面交互测试。
  - 执行备注：
    - 首次设备测试出现 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`（历史签名冲突），执行 `:app:uninstallDebug :app:uninstallDebugAndroidTest` 后恢复。
    - 首轮 Step 12 新增 UI 用例有 3 项小屏可视区断言失败，已调整为滚动/语义兼容断言后全量通过。
- 阻塞项：无代码阻塞（等待用户 Android Studio 编译与机测复验）
- 下一步：
  1. 用户在 Android Studio 编译并机测 Step 12（拍照/单选/多选导入、失败重试、列表封面滚动、详情高清图查看）。
  2. 用户明确“机测通过”前，不进入 Step 13。

### 2026-03-08 - Step 12 后续修正：ItemEdit/Detail/Category 一致性
- 状态：`done`
- 关键更新：
  - 修复 ItemEdit 草稿稳定性：在编辑页导入照片后，不再清空用户已输入字段（如 `Purchase Date`、`Purchase Price`）。
  - 修复新建态“先加图”行为：用户尚未输入文本即添加照片时，可自动创建并绑定 `itemId` 供照片落库，但不再把自动生成名称回填到名称输入框。
  - 修复保存后的导航行为：
    - 在 ItemEdit 点击 `Save`（新建或编辑）后，自动跳转到该条目的 `Item Detail`。
    - 在该详情页点击 `Back` 后，返回 `Item List`。
  - 修复返回页自动刷新行为：
    - 重新进入 `Item Detail(itemId)` 时自动刷新，编辑保存返回后无需手动 `Refresh` 即可看到最新字段。
    - 重新进入 `Category` 时自动刷新，`Item count` 无需手动 `Refresh` 即可更新。
  - 新增回归测试覆盖（`ItemEditFlowIntegrationTest`）：
    - `editFromDetail_saveShouldAutoRefreshDetail`
    - `backToCategory_shouldAutoRefreshItemCount`
- 测试结果：
  - `.\gradlew.bat test` 通过。
  - `.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.ui.screens.itemedit.ItemEditFlowIntegrationTest` 通过（4/4）。
  - `.\gradlew.bat connectedAndroidTest` 通过（36/36）。
  - 设备：`SM-S901U1 - Android 15`。
- 人工验证：
  - 用户已确认：上述 Step 12 后续修正在手机实机验证通过。
- 阻塞项：无。
- 下一步：
  1. 在用户明确放行前，继续停留在 Step 12，不进入 Step 13。

### 2026-03-08 - Step 13：本地备份导出（严格对齐 BACKUP_FORMAT）
- 状态：`done`
- 关键产出：
  - 新增 `backup/export` 模块，落地 `BackupService.exportLocalBackup(exportMode)` 与 `ExportMode`（`metadata_only` / `thumbnails` / `full`）契约。
  - 新增导出结果与错误分类：`BackupExportResult`、`BackupStats`、`BackupExportException`（参数错误 / I/O 错误 / 缺图错误）。
  - 新增 checksums 扩展点：`BackupChecksumGenerator` + `NoOpBackupChecksumGenerator`，V0 默认不生成 `checksums.json`，但流程可挂载扩展。
  - 新增导出实现链路（按职责拆分）：
    - `BackupSnapshotCollector`：采集全量类别（含归档）、物品（含软删）、照片（按 item 汇总）；
    - `BackupPhotoPreparer`：`full` 读取 `localUri`、`thumbnails` 读取 `thumbnailUri`，缺图直接失败中止；
    - `BackupJsonBuilder` + `BackupJsonEncoder`：生成 `manifest.json` / `data.json`，字段对齐 `BACKUP_FORMAT.md`；
    - `BackupZipWriter`：固定写入顺序 `manifest -> data -> photos -> checksums(optional)`；
    - `LocalBackupService`：统一编排导出，文件输出为时间戳命名 zip。
  - 导出目录策略：优先 `getExternalFilesDir("backups")`，为空时回退 `files/backups`。
  - 应用接线：
    - 新增 `ExportLocalBackupUseCase` 并注入 `AppDependencies`；
    - Settings 页面升级为“单入口导出”：模式选择 + 导出按钮 + 状态文案 + 最近导出路径/时间；
    - `SettingsViewModel` 接入导出状态机与错误映射，UI 不直接访问文件系统。
  - 新增测试：
    - JVM：`LocalBackupServiceTest`（三模式结构、必填字段、缺图失败、checksums 扩展点）；
    - 设备：`BackupExportIntegrationTest`（真机三模式导出解包校验）；
    - 设备：`SettingsScreenInteractionTest`（模式切换、单按钮导出触发、状态文案）。
- 测试结果：
  - 自动化（Agent）通过：
    - `.\gradlew.bat testDebugUnitTest`
    - `.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.backup.BackupExportIntegrationTest`
    - `.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.ui.screens.settings.SettingsScreenInteractionTest`
    - `.\gradlew.bat connectedAndroidTest`（全量 39/39）
  - 真机设备：
    - `SM-S901U1 - Android 15`
  - 执行备注：
    - 定向设备测试首次失败为 `@Before` 返回类型不满足 JUnit4（`setUp() should be void`），改为 block body 后通过；
    - 全量设备回归首次失败为新测试清库后未恢复默认分类，补充 `BackupExportIntegrationTest` 的 `@After` 基线恢复（`cat_electronics`）后通过。
- 阻塞项：无代码阻塞。
- 下一步：
  1. 等待用户在 Android Studio 编译并上传手机完成人工验证。
  2. 在用户明确“Step 13 验证通过”前，不进入 Step 14。

### 2026-03-08 - Step 14：本地备份导入（V0 `replace_all`）
- 状态：`done`
- 关键产出：
  - 新增 `backup/importing` 模块，落地导入链路：`BackupArchiveReader`（ZIP 读取与路径校验）+ `BackupJsonParser`（已知字段解析/未知字段忽略）+ `LocalBackupImporter`（事务导入编排）。
  - 扩展统一契约：`BackupService` 新增 `importLocalBackup(backupFilePath)`；`LocalBackupService` 新增导入代理能力。
  - 落地 `replace_all` 行为：
    - 导入前自动执行一次本地 `full` 快照作为回滚点；
    - 事务内执行“清空核心表 + 按顺序重建 categories/items/item_photos”；
    - 恢复包内照片到私有目录 `files/photos/full|thumbs`；
    - 导入完成后清理旧引用照片与目录孤儿文件。
  - 固化兼容策略：
    - 未知字段全部忽略，不阻塞导入；
    - `formatVersion/schemaVersion` 更高版本时执行“尽力导入 + 告警”；
    - 缺失必需结构或 `itemPhotos.fileName` 引用缺图时按无效包失败并提示。
  - 数据层补充导入辅助能力：`CategoryDao/ItemDao/ItemPhotoDao` 新增 `insertOrReplace` 与 `deleteAll`（`ItemPhotoDao` 额外新增 `listAll`）。
  - 新增 `ImportLocalBackupUseCase` 并在 `AppDependencies` 接线（导入前回滚快照复用 `exportLocalBackup(ExportMode.FULL)`）。
- 测试结果：
  - 自动化（Agent）通过：
    - `.\gradlew.bat testDebugUnitTest`
    - `.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.backup.BackupImportIntegrationTest`
    - `.\gradlew.bat connectedAndroidTest`（全量 41/41）
  - 真机设备：
    - `SM-S901U1 - Android 15`
  - 新增设备测试 `BackupImportIntegrationTest` 覆盖：
    - `replace_all` 导入一致性（旧数据被替换、旧照片与孤儿文件清理、回滚快照文件存在）；
    - 高版本 + 未知字段兼容导入（导入成功并输出版本告警）。
- 阻塞项：无代码阻塞。
- 下一步：
  1. 等待用户在 Android Studio 编译并上传手机完成人工验证。
  2. 在用户明确“Step 14 验证通过”前，不进入 Step 15。

### 2026-03-08 - Step 14A：共享目录备份与导入入口落地
- 状态：`done`
- 关键产出：
  - 新增 `backup/storage` SAF 桥接层：目录 URI 持久化（`backup_tree_uri`）、持久化读写授权、目录 zip 列表（仅顶层、按 `lastModified DESC`）、本地文件与文档文件双向复制。
  - 新增 Step 14A 用例链路：`Set/GetBackupDirectoryUseCase`、`ListImportableBackupsUseCase`、`ExportBackupToSharedDirectoryUseCase`、`ImportBackupFromDocumentUseCase`。
  - 升级 Settings 状态机与 UI：
    - 新增目录状态、目录内可导入列表、导入中状态、导入告警、待确认导入 URI；
    - 新增 `OpenDocumentTree` 与 `OpenDocument` 接线；
    - 新增“选择目录 / 导出到共享目录 / 从目录导入（zip 列表）/ 单文件导入兜底”入口；
    - 导入前统一确认弹窗并明确 `replace_all` 覆盖行为。
  - 保持备份核心引擎复用：不改 `exportLocalBackup/importLocalBackup` 业务核心，仅做私有临时文件与 SAF 文档桥接。
  - 更新依赖注入与构建依赖：`AppDependencies` 新增 5 个用例注入出口，Gradle 新增 `androidx.documentfile` 依赖。
- 测试结果：
  - 自动化（Agent）通过：
    - `.\gradlew.bat :app:testDebugUnitTest`
    - `.\gradlew.bat :app:testDebugUnitTest --tests "com.example.itemmanagementandroid.ui.screens.settings.SettingsViewModelTest"`
    - `.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.ui.screens.settings.SettingsScreenInteractionTest`（3/3）
    - `.\gradlew.bat connectedAndroidTest`（全量 42/42）
  - 真机设备：
    - `SM-S901U1 - Android 15`
  - 执行备注：
    - 首次运行 `testDebugUnitTest` 时因沙箱网络限制拉取依赖失败（`Permission denied: getsockopt`），提权重跑后通过；
    - `connectedAndroidTest` 在已连接设备完成全量通过，覆盖 Step 14A 新增设置页交互路径。
- 人工验证：
  - 待用户在 Android Studio 编译并上传手机执行手工验证清单（卸载重装后重授权目录、历史 zip 列出并导入恢复）。
- 阻塞项：无代码阻塞。
- 下一步：
  1. 等待用户人工验证 Step 14A（含卸载重装与共享目录恢复场景）。
  2. 在用户明确“Step 14A 验证通过”前，不进入 Step 15。

### 2026-03-08 - Step 14A 后续修正：Settings 紧凑化布局
- 状态：`done`
- 关键产出：
  - 将 `Backup Export Mode` 从 3 个按钮改为单个下拉选单，默认值继续由 `selectedExportMode=FULL` 驱动。
  - 将 Settings 页面从 `Column + 内层 LazyColumn` 重构为单一外层 `LazyColumn`，消除嵌套滚动导致的列表与 `Back` 按钮不可达问题。
  - 同步调整测试标签契约：新增 `EXPORT_MODE_DROPDOWN`、`exportModeMenuItem(...)`、`SCROLL_CONTAINER`、`BACK_BUTTON`。
  - 更新 `SettingsScreenInteractionTest`：
    - 模式选择改为“打开下拉 -> 选择 `THUMBNAILS`”；
    - 保留导出与目录导入回调断言；
    - 新增“滚动到 `Back` 按钮可见”断言，验证单一滚动容器可达性。
- 测试结果：
  - 自动化（Agent）通过：
    - `.\gradlew.bat :app:testDebugUnitTest`
    - `.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.itemmanagementandroid.ui.screens.settings.SettingsScreenInteractionTest`（4/4）
    - `.\gradlew.bat connectedAndroidTest`（全量 43/43）
  - 真机设备：
    - `SM-S901U1 - Android 15`
  - 执行备注：
    - 定向设备测试首次失败为签名冲突（`INSTALL_FAILED_UPDATE_INCOMPATIBLE`），执行 `:app:uninstallDebug :app:uninstallDebugAndroidTest` 后重跑通过。
- 人工验证：
  - 用户已反馈本轮“测试完成”。
- 阻塞项：无代码阻塞。
- 下一步：
  1. 等待用户下达下一步开发指令。
