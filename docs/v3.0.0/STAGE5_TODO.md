# CtrlX 3.0.0 Stage 5 TODO：Relay 发行身份

## Stage Status

- **Status**：✅ Implementation Completed
- **Progress**：7/7 tasks
- **Dependencies**：Stage 3 ✅

## Tasks

- [x] Relay service、Docker、Compose、Caddy、APNs topic 与 metrics 使用 CtrlX 身份。
- [x] 增加 `/ready`、`/version` 与 `/source`，公开 version、commit、protocol、source 和 license。
- [x] Docker OCI labels 和运行环境嵌入相同的对应源码信息。
- [x] 提供 development/test/production 配置模板，真实配置与私钥保持外置。
- [x] 落实 `.env.local > .env.production > .env.development > .env.test` 单文件优先级。
- [x] 默认客户端 Relay URL 为空，Caddy 模板不包含未拥有的生产域名。
- [x] Relay Linux build、单元测试、配对/重连/首帧/大量输出回归全部通过。

## Acceptance

- [x] `/source` 能区分 development 与精确 commit 构建。
- [x] `/metrics` 默认拒绝访问，配置的 token 至少 32 字符。
- [x] Relay 继续只转发 E2EE 密文，不新增终端明文解析。
- [ ] 公网域名、APNs 与运维资源就绪后完成 staging 验收。
