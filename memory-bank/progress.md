# 进度记录（Progress）

## 记录规则
- 每完成一个 Step，更新：状态、关键产出、测试结果、阻塞项、下一步。
- 状态枚举：`todo` / `in_progress` / `done` / `blocked`。

## 当前总览（截至 2026-03-05）
- 当前阶段：V0 Step 02 已完成，等待用户验证放行
- 总状态：`in_progress`
- 说明：已完成 Step 02（本地数据模型与数据库 schema v1），已提交代码与 schema 校验测试；等待用户在 Android Studio 编译并机测确认后再进入 Step 03。

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
