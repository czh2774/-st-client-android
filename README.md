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

## 代码风格

- 使用 `ktlint` 或 `detekt` 进行静态检查。
- 遵循官方 Kotlin 编码规范。

