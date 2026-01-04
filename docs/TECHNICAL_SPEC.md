# Android Client Technical Specification (Native)

## 1. 概述
本文档定义了 `st-client-android` 的原生技术实现方案。

为避免“文档写的是目标态，但代码还是脚手架”的误导，本文件分为两部分：
- **现状 (Phase 0 / Current)**：仓库中已经落地并可运行的实现
- **目标态 (Phase 1+ / Target)**：规划中的架构与能力（路线图）

## 2. 技术栈 (Technical Stack)
- **语言**: Kotlin 2.1+
- **UI 框架**: Jetpack Compose (Material 3)
- **架构**:
  - **现状**：MVVM（`ViewModel + StateFlow<UiState>`）
  - **目标态**：MVI (Model-View-Intent) + Clean Architecture（按 feature contract 收敛）
- **依赖注入**: Hilt
- **异步处理**: Kotlin Coroutines + Flow (StateFlow)
- **网络**: Retrofit 2 + OkHttp 4 (含 SSE 支持)
- **持久化**:
  - **现状**：Room 已落地（聊天会话/消息缓存，多会话 + 驱逐）
  - **目标态**：Room Database（聊天会话/消息缓存 + 增量同步）
- **导航**:
  - **现状**：Compose Navigation 已覆盖 tabs 与主要流程（深链仍有限）
  - **目标态**：Compose Navigation（按 feature 拆分 + 完整深链）
- **图片加载**:
  - **现状**：Coil 已引入（头像/卡面/背景等）
  - **目标态**：Coil（性能与缓存策略细化）

## 3. 架构设计 (Architecture)

### 3.1 分层结构
- **`app/`**: Application 类、Hilt 依赖注入模块、主 Activity。
- **`data/`**: 
    - 数据源实现（Remote & Local）。
    - 仓库实现 (Repository Implementations)。
    - 数据模型 (Data Models/DTOs) 与 Mapper。
- **`domain/`**: 
    - 业务逻辑抽象 (UseCases)。
    - 仓库接口 (Repository Interfaces)。
    - 领域模型 (Domain Models)。
- **`features/`**: 
    - 按功能划分：`chat`, `character`, `auth`, `settings`。
    - **现状**：`ViewModel` + `UiState` + `Screen`
    - **目标态**：每个 feature 包含 `Contract` (State/Intent/Effect) 以统一为 MVI。

### 3.2 MVI 模式实现
目标态：每个功能模块遵循统一的 Contract：
- **State**: 唯一的 UI 状态对象（如 `ChatState`）
- **Intent**: 用户发出的动作（如 `SendMessage`）
- **Effect**: 一次性的副作用（如显示 Toast、页面跳转）

## 4. 关键功能实现方案

### 4.1 聊天流 (SSE) 处理
现状：
- 通过 OkHttp SSE 处理流式 completion（`stream=true`），增量写入本地缓存并更新 UI。
- 处理 `message_ids` / `session_recency` 元信息帧，记录服务端 messageId 与 sessionUpdatedAtMs。

目标态：
- **接口**: SSE completion endpoint（以 `st-server-go` 为准，避免文档与路由不一致）
- **实现**:
  - 使用 OkHttp 的 `EventSource.Factory`
  - 将 Event 流转换为 `Flow<ChatChunk>`
  - 在 ViewModel/MVI reducer 中累加消息内容并更新 state

### 4.2 鉴权与 Cookie 管理
现状：
- Access/refresh token 使用 EncryptedSharedPreferences 持久化。
- OkHttp Authenticator 在 401 时触发 `/auth/refresh`（`X-Auth-Client: android` + body `refreshToken`）。
- `PersistentCookieJar` 负责持久化非认证 cookie（session cookie 仅保存在内存）。
- 基础登录 UI 已对接 `/auth/login`，成功后写入 access/refresh tokens。
- logout 会 best-effort 调用 `/auth/logout`，并清理本地 tokens、持久化 cookies、聊天会话与本地缓存（支持多账号切换）。

目标态（与 server-native 模式对齐）：
- 维持 refreshToken body 模式；需要 cookie-only 的场景再引入 CSRF double-submit 流程。

### 4.3 离线与同步
现状：
- 使用 Room 缓存会话消息（多会话 + 驱逐），重启可恢复最近对话。
- 会话 ID 缺失时通过 `POST /chats` 创建，并持久化到加密 SharedPreferences。
  - `members[0]` 依赖 `ST_DEFAULT_CHARACTER_ID`。
- 支持恢复最近一次会话与本地多会话列表。

目标态：
- **本地优先**: 使用 Room 缓存最近的聊天会话
- **同步策略**: 进入聊天室时增量拉取缺失的消息记录

## 5. UI/UX 规范
- **设计语言**: Material Design 3。
- **深色模式**: 原生支持。
- **列表性能**: 聊天列表使用 `LazyColumn`，配合 `key` 优化重绘。

## 6. 开发与测试
现状：
- **静态检查**: Ktlint（通过 `.editorconfig` 约束；detekt 未落地）
- **单元测试**: JUnit 4 + MockK (ViewModel & UseCases)，协程使用 `kotlinx-coroutines-test`。
- **UI 测试**: Compose Test Rule + Espresso。
