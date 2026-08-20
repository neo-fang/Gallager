# CtrlX 3.0.0 Issues

## External prerequisites

- [ ] 创建公开源码仓库并确认最终 Git remote。
- [ ] 确认 CtrlX 官网、Relay、更新和下载域名。
- [ ] 准备 CtrlX 专用 macOS Developer ID、Notary 和 Sparkle EdDSA 配置。
- [ ] 准备 CtrlX 专用 iOS bundle、App Group、APNs 与 provisioning profile。
- [ ] 在 TestFlight/App Store 前完成 AGPL 与 Apple 条款法律审查。

## Engineering follow-ups

- [x] UserDefaults 默认 domain 由新 Bundle ID 隔离；显式 suite/App Group 已切换到 CtrlX。
- [x] CtrlX 3.0.0 使用新的 E2EE salt 与最低协议版本，明确不与 Gallager 2.x 建立加密会话；消息模型继续复用以降低维护成本。
- [ ] 评估历史文档是保留为上游资料还是迁移到 `docs/upstream/`，避免机械重写历史记录。
