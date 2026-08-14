# CtrlX 3.0.0 Stage 4 TODO：构建、签名与更新边界

## Stage Status

- **Status**：🟡 In Progress
- **Progress**：6/7 tasks
- **Dependencies**：Stage 3 implementation complete; final test gate pending

## Tasks

- [x] 将版本提升为 3.0.0（build 1），产物统一命名为 `CtrlX-3.0.0.dmg` / `.ipa`。
- [x] 将构建标识、产物校验和源码 revision 切换到 `CTRLX_*`。
- [x] Sparkle feed/public key 未配置时禁用更新，不沿用 Gallager 基础设施。
- [x] 正式发布脚本改为零参数、干净 tag 构建，并强制签名、Notary 与 Sparkle 签名。
- [x] 为每个本地产物生成 SHA-256 和包含对应源码 commit 的 JSON manifest。
- [x] 完成 macOS 与 iOS 无签名客户端构建。
- [ ] 完成正式签名、Notary 和产物安装验收。

## Acceptance

- [x] 仓库不包含 Developer Team、证书、Provisioning Profile、Notary 或 Sparkle 私钥。
- [x] 未配置发行基础设施时安全失败，不回退旧更新通道。
- [x] 发布 manifest 指向完整且不可变的 Git commit。
- [ ] CtrlX 专用 Apple/Sparkle 外部资源就绪后完成正式发布验收。

## Local packaging verification

- Apple Development 证书显示名称中的括号值不是 Team ID；本地打包器改为读取证书 subject
  的 `OU`，再选择匹配 `CTRLX_MAC_DEVELOPMENT_TEAM` 的有效 identity，避免误报无签名证书。
- 该修复同时供 macOS DMG 与 iOS 本地重签名流程复用；正式 Developer ID、Notary 和 Sparkle
  发布门禁仍保持未完成状态。
