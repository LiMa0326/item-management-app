# 技术栈推荐
## 1. 总览（一页版）
- Android（V0）默认：原生 Android（Kotlin）+ 单向数据流 UI + 本地离线优先。
- 数据层默认：Repository + UseCase 分层；核心实体固定为 Category / Item / ItemPhoto，字段与版本策略对齐文档（含 `purchaseCurrency`、`customAttributes` JSON、`contentType`）。
- 本地存储默认：SQLite（含索引与迁移）+ 应用私有文件目录存图（原图与缩略图分离）。
- 备份打包默认：ZIP（`manifest.json` + `data.json` + 可选 `photos/`，可选 `checksums.json`），V1 云端直接复用同一包。
- 可选云组件：V1 用 Entra External ID + Blob Storage + Azure Functions（手动备份/恢复）；V2 再加增量同步与可选 AI（均可关闭）。

- 先把“本地可用”做扎实，V0 不依赖云，符合离线优先与成本约束。
- 备份格式从 V0 固化并平台无关，后续 iOS/Web 可直接复用数据资产。
- V1 只做“登录 + 云备份/恢复”，不做实时同步，显著降低开发和运维复杂度。
- 云侧全部按需触发，无常驻服务、无默认定时任务，Azure credit 消耗可控。
- V2 的同步和 AI 作为独立开关能力，保证可降级，不影响核心记录/检索流程。

## 2. V0：Android 本地 Demo 技术栈
- UI 实现思路（默认）：列表页/详情页/编辑页/设置页四块，状态单向流动（ViewModel 持状态，UI 只渲染与派发事件）；表单先“少字段高频可用”，高级字段折叠。
- UI 备选 1：传统 XML + MVVM（适合已有模板或对新 UI 框架不熟）。
- UI 备选 2：跨平台 UI 框架先行（仅当你确定 3-6 个月内马上做 iOS 同款 UI）。

- 架构/分层思路（默认）：`UI -> UseCase -> Repository -> Local DB/File`，禁止 UI 直接访问存储；备份与图片处理走独立 Service。
- 本地数据库/存储方案（默认）：SQLite（items/category/item_photos 表 + 常用查询索引）；图片存应用私有目录，仅在数据库保存逻辑引用（`item_photos` 建议持久化 `localUri`、`thumbnailUri`、`contentType`）。
- 本地数据库备选：对象数据库（仅当你明确接受后续跨端迁移成本）。

- 图片存储与缩略图策略（默认）：
  - 原图入库前做尺寸上限压缩并去 EXIF。
  - 同步生成缩略图（长边 1280px，JPEG，质量 85，可配置）。
  - 列表只读缩略图，详情再读高清图。
  - 删除采用“软删记录 + 延迟清理文件”。

- 搜索实现方案（默认）：先做 SQLite 字段匹配（name/description/purchasePlace/tags），大小写不敏感子串匹配；tags 按整词匹配；当本地数据量 > 3k 且搜索延迟明显，再升级全文检索（FTS）。

- 备份导出/导入实现思路（严格对齐 `memory-bank/BACKUP_FORMAT.md`）：
  - 导出产物固定为 `backup.zip`，包含必需 `manifest.json`、`data.json`，可选 `photos/`、`checksums.json`。
  - `manifest.json` 必含：`formatVersion`、`createdAt`、`exportMode`、`app`、`stats`。
  - `data.json` 必含：`schemaVersion`、`exportedAt`、`categories`、`items`、`itemPhotos`。
  - `exportMode` 默认：本地导出 `full`（用户显式选择时可改），云备份默认 `thumbnails`。
  - V0 导入模式默认 `replace_all`；导入前自动快照，并清理本地照片与孤儿文件；导入时忽略未知字段并做版本告警（不阻塞可识别字段导入）。
  - 校验策略：V0 可不生成 `checksums.json`；V1/V2 云备份默认生成并在导入前校验（SHA-256）。

- 测试策略（最小但有效）：
  - 单测：Repository CRUD、搜索、软删除恢复、备份 JSON 序列化/反序列化一致性。
  - 集成：空库导入 ZIP、导出后再导入回归、包含/不包含 `photos/` 两种包都能成功。
  - 性能：V0 对 1k 搜索先记录基线，不设置硬阈值；后续版本再引入量化 SLA。

## 3. V1：最小云备份 + 登录（强成本控制）
- 身份认证/登录方案（默认）：Microsoft Entra External ID（面向外部用户）。
- 备选 1：Firebase Auth（仅当你决定弱化 Azure 统一性、追求更快接入）。
- 备选 2：自建账号（仅当有明确合规/自控需求；不建议个人开发者 MVP）。
- 明确不推荐：Azure AD B2C 新项目路线（避免走旧路径）。

- 云存储方案（默认）：Azure Blob Storage 存备份 ZIP；对象命名按 `userId/yyyy/MM/dd/<timestamp>.zip`；默认上传 `thumbnails` 模式。
- API/后端形态（默认）：Azure Functions（HTTP 触发）提供三类能力：申请上传授权、列出备份、下载授权；App 直传/直下 Blob。

- 成本与运维控制（默认）：
  - 无自动备份，只有手动触发。
  - 无定时任务、无常驻容器、无后台轮询。
  - 只保留最近 N 份备份（如 10 份）做生命周期清理。
  - 可观测性最小集合：函数调用失败率、备份成功率、Blob 容量、月度调用量。
  - 日志采样与短保留期，先控成本再加细粒度观测。

## 4. V2：可选同步 + 可选 AI
- 同步（默认最小可用）：
  - 增量变更跟踪：每条记录维护 `updatedAt` + `version` + `deletedAt`。
  - 离线优先：本地先写成功，再异步 push/pull。
  - 冲突策略默认 LWW（Last-Write-Wins）；冲突提示 UI 作为后续增强。
  - 开关策略：用户可关闭同步；关闭后保留本地完整能力。

- AI（必须可开关、隐私优先、数据最小化）：
  - 本地优先能力：条码/二维码识别、OCR、基础图片分类候选生成。
  - 何时需要云端 AI：仅在本地结果置信度低、或用户主动点“增强识别/语义问答”时。
  - 上传策略默认：缩略图优先；可选“仅 Wi-Fi 上传”；可选“仅文本不上图”。
  - 降级策略：云 AI 超时/限额时自动回落本地规则与关键词检索。

## 5. 面向未来的跨平台（iOS / Web）兼容建议
- 为了 iOS/Web 必须稳定：
  - 数据模型字段（含 `customAttributes` 扩展机制）。
  - 备份包格式与版本语义（`formatVersion` / `schemaVersion`）。
  - 云 API 契约（备份上传/列表/下载；V2 的 sync push/pull）。

- 需要隔离避免重写：
  - 业务规则、校验、导入导出、同步合并策略放 Domain/Data 层。
  - UI 与平台能力（相机、权限、文件选择器）放 Platform Adapter 层。

- 推荐跨平台主路线（默认）：Kotlin Multiplatform 共享 Domain/Data + Android/iOS 各自原生 UI，Web 走独立前端复用同一 API/备份协议。
- 替代路线 1：Flutter 全端（适合“单人维护、UI 一致性优先、可接受 Dart 全栈”）。
- 替代路线 2：React Native + Web 同技术栈（适合“Web 提前上线、团队前端经验强”）。

## 6. 风险、取舍与“下次复盘点”
- Top 5 风险：
  - 备份包版本升级处理不严谨，导致旧包/新包兼容性问题。
  - 图片体积失控（原图过多）导致备份慢、云存储超预算。
  - V2 同步只用 LWW 时，少量场景会出现“静默覆盖”。
  - 认证/授权边界处理不当，出现跨账号读写风险。
  - AI 调用不设上限，触发不可控 token/推理成本。

- 触发“重新评估技术栈”的条件：
  - 活跃用户 > 5k 或单用户平均物品数 > 3k，现有本地搜索与同步延迟不可接受。
  - 云备份月成本连续 2 个月超预算阈值（如预算的 120%）。
  - 多设备同步冲突率升高（如 > 1% 操作涉及冲突）且用户投诉明显。
  - AI 月调用量或单次延迟超目标，影响核心录入体验。
  - iOS/Web 进入正式开发，现有共享层复用率低于预期（如 < 40%）。
