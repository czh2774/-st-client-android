# st-client-android

本目录用于 **Android 原生客户端（Kotlin/Jetpack Compose）**。

目标：
- 在 Android 上以原生体验实现 “SillyTavern / 酒馆” 的核心玩法（会话/消息/流式生成/重生/继续/变量系统）。
- 与本仓库的 `st-server-go` 作为 **唯一权威后端** 对齐（鉴权、会话、计费、钱包、脚本变量落库等）。

## 文档导航

### Repo 级文档（跨模块）

- [../docs/README.md](../docs/README.md)：**跨模块文档总入口（从这里开始）**

### Android 模块文档（本模块）

- [docs/README.md](docs/README.md)：Android 文档索引

## 开发建议

- **语言/框架**：推荐使用 Kotlin + Jetpack Compose。
- **架构**：推荐 MVVM 或 MVI。
- **网络**：使用 OkHttp / Retrofit，需处理 cookie-based auth（HttpOnly refresh cookie）。
- **同步**：与服务端 `/api/v1/chat/*` 接口对齐。

### 本地开发（Base URL）

默认情况下，Android 会使用模拟器访问宿主机服务端：

- `BuildConfig.API_BASE_URL` 默认：`http://10.0.2.2:8080/api/v1/`
- 可通过 Gradle 参数覆盖：
  - `./gradlew :app:installDebug -PST_API_BASE_URL=http://10.0.2.2:8080/api/v1/`
  - `./gradlew :app:installDebug -PST_API_BASE_URL=http://10.0.2.2:8080/api/v1/ -PST_DEFAULT_CHARACTER_ID=char-123`
  - `./gradlew :app:installDebug -PST_PRIVACY_URL=https://example.com/privacy -PST_TERMS_URL=https://example.com/terms`

Debug 构建允许明文 HTTP（便于连本地 dev server），Release 默认关闭明文流量。
Release 构建需要显式设置 HTTPS 的 `ST_API_BASE_URL`，否则会在启动时终止。
Release 构建需要显式设置 HTTPS 的 `ST_PRIVACY_URL` 与 `ST_TERMS_URL`，否则会在启动时终止。

聊天会话创建需要默认角色 ID：
- `ST_DEFAULT_CHARACTER_ID`：用于 `POST /api/v1/chats` 的 `members[0]`

## 代码风格

- 使用 `ktlint` 或 `detekt` 进行静态检查。
- 遵循官方 Kotlin 编码规范。
