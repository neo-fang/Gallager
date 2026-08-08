# Stage 26 TODO：主仓库可重现打包与可见构建标识

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 1/8 tasks
- **Dependencies**: Stage 25 ✅

## Tasks

- [x] 审计现有 macOS/iOS 打包、签名、DerivedData 和版本展示路径。
- [ ] 实现并测试共享 `AppBuildInfo`。
- [ ] 在 macOS About 和 iOS Settings 展示共享构建标识。
- [ ] 实现 primary-worktree 校验、主仓库缓存与构建元数据公共脚本。
- [ ] 实现零参数 macOS DMG 与 iOS IPA 本地打包脚本。
- [ ] 将正式 macOS/TestFlight 脚本收敛到同一主仓库缓存和元数据路径。
- [ ] 完成聚焦测试、完整测试、shell 校验和两端构建。
- [ ] 合入主仓库后从主仓库重新打包，完成本机 Mac 与 iPhone 验收。

## Decisions

- 可见后缀是展示元数据，不是语义版本的一部分。`VersionCompatibility` 继续只使用
  `CFBundleShortVersionString`，不让本地时间戳影响 Host/Viewer 协议兼容。
- 主仓库限制放在打包/发布入口，不禁止 feature worktree 做日常编译和测试；最终
  可安装产物必须在合入后从 primary worktree 重建。
- 本地 iOS 签名只读已忽略配置和 Keychain/provisioning profiles，不把个人开发团队信息
  提交到公共仓库。

## Blockers

- None.

## Verification

- Pending.
