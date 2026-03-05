# 进度记录（Progress）

## 记录规则
- 每完成一个 Step，更新：状态、关键产出、测试结果、阻塞项、下一步。
- 状态枚举：`todo` / `in_progress` / `done` / `blocked`。

## 当前总览（截至 2026-03-05）
- 当前阶段：V0 Step 04 已完成，等待用户验证放行
- 总状态：`in_progress`
- 说明：已完成 Step 04（Item 数据层含软删除/恢复），已提交代码与单测；等待用户在 Android Studio 编译并机测确认后再进入 Step 05。

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
