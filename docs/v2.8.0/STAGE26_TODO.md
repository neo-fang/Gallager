# Stage 26 TODO：主仓库可重现打包与可见构建标识

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 7/8 tasks
- **Dependencies**: Stage 25 ✅

## Tasks

- [x] 审计现有 macOS/iOS 打包、签名、DerivedData 和版本展示路径。
- [x] 实现并测试共享 `AppBuildInfo`。
- [x] 在 macOS About 和 iOS Settings 展示共享构建标识。
- [x] 实现 primary-worktree 校验、主仓库缓存与构建元数据公共脚本。
- [x] 实现零参数 macOS DMG 与 iOS IPA 本地打包脚本。
- [x] 将正式 macOS/TestFlight 脚本收敛到同一主仓库缓存和元数据路径。
- [x] 完成聚焦测试、完整测试、shell 校验和两端构建。
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

- `AppBuildInfoTests`：4/4 通过。
- Swift Package 完整测试：1652/1652 通过（235 suites）。
- `bash -n`：`common.sh`、两个本地打包脚本及两个正式发布脚本均通过。
- `plutil -lint`：macOS/iOS `Info.plist` 均通过。
- iOS generic device build 与 macOS signed Release build 均通过；两端产物中的构建
  时间戳和源码提交号均与构建参数一致。
- 四个打包/发布入口在 Stage worktree 均于调用 Xcode 前正确拒绝执行。
