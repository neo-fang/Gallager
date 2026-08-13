# CtrlX 3.0.0 Issues

## External prerequisites

- [ ] 创建公开源码仓库并确认最终 Git remote。
- [ ] 确认 CtrlX 官网、Relay、更新和下载域名。
- [ ] 准备 CtrlX 专用 macOS Developer ID、Notary 和 Sparkle EdDSA 配置。
- [ ] 准备 CtrlX 专用 iOS bundle、App Group、APNs 与 provisioning profile。
- [ ] 在 TestFlight/App Store 前完成 AGPL 与 Apple 条款法律审查。

## Engineering follow-ups

- [ ] 审计 UserDefaults 是否完全依赖 bundle domain，确保与 Gallager 共存。
- [ ] 明确 CtrlX 3.0.0 与 Gallager 2.x 的 wire compatibility；身份必须隔离，消息模型尽量兼容。
- [ ] 评估历史文档是保留为上游资料还是迁移到 `docs/upstream/`，避免机械重写历史记录。
