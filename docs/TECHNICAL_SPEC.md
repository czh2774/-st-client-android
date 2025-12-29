# Android Client Technical Specification (Native)

## 1. 概述
本文档定义了 `st-client-android` 的原生技术实现方案。该模块专注于提供极致的 Android 原生体验，不考虑跨平台代码共享，完全采用现代 Android 开发 (MAD) 栈。

## 2. 技术栈 (Technical Stack)
- **语言**: Kotlin 2.1+
- **UI 框架**: Jetpack Compose (Material 3)
- **架构**: MVI (Model-View-Intent) + Clean Architecture
- **依赖注入**: Hilt
- **异步处理**: Kotlin Coroutines + Flow (StateFlow)
- **网络**: Retrofit 2 + OkHttp 4 (含 SSE 支持)
- **持久化**: Room Database
- **导航**: Compose Navigation (Type-safe)
- **图片加载**: Coil

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
    - 每个 feature 包含：`ViewModel` (MVI 控制器), `Screen` (Compose 组件), `Contract` (State/Intent/Effect)。

### 3.2 MVI 模式实现
每个功能模块遵循统一的 Contract：
- **State**: 唯一的 UI 状态对象（如 `ChatState`）。
- **Intent**: 用户发出的动作（如 `SendMessage`）。
- **Effect**: 一次性的副作用（如显示 Toast、页面跳转）。

## 4. 关键功能实现方案

### 4.1 聊天流 (SSE) 处理
- **接口**: 监听 `/api/v1/chat/completions`。
- **实现**:
    - 使用 OkHttp 的 `EventSource.Factory`。
    - 将 Event 流转换为 `Flow<ChatChunk>`。
    - 在 ViewModel 中累加消息内容并更新 `ChatState`。

### 4.2 鉴权与 Cookie 管理
- **认证模式**: Cookie-based Auth (与 `st-server-go` 对齐)。
- **CookieJar**: 实现 `PersistentCookieJar` 以持久化 HttpOnly 的 `refresh_token`。
- **拦截器**: 
    - `AuthInterceptor`: 自动附加 `X-Csrf-Token` 头。
    - `TokenAuthenticator`: 当 Access Token 过期时，利用 `refresh_token` cookie 触发自动刷新。

### 4.3 离线与同步
- **本地优先**: 使用 Room 缓存最近的聊天会话。
- **同步策略**: 进入聊天室时增量拉取缺失的消息记录。

## 5. UI/UX 规范
- **设计语言**: Material Design 3。
- **深色模式**: 原生支持。
- **列表性能**: 聊天列表使用 `LazyColumn`，配合 `key` 优化重绘。

## 6. 开发与测试
- **静态检查**: Ktlint + Detekt。
- **单元测试**: JUnit 4 + MockK (ViewModel & UseCases)，协程使用 `kotlinx-coroutines-test`。
- **UI 测试**: Compose Test Rule + Espresso。

