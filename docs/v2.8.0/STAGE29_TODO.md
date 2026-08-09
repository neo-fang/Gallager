# Stage 29 TODO：本地安装包依赖锁定

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 5/5 tasks
- **Dependencies**: Stage 28 ✅

## Tasks

- [x] 复现 macOS 打包期间隐式更新锁定依赖并因网络失败。
- [x] 锁定 macOS 与 iOS 本地打包的 Package.resolved。
- [x] 完成 shell、解析模式及 Git 洁净性验证。
- [x] 从主 worktree 构建并校验 macOS Release DMG。
- [x] 合入 `develop/v2.8.0` 并清理 worktree。

## Decisions

- 不修改依赖版本或锁文件。失败来自打包命令允许 Xcode 查询更新，而非锁文件不可用。
- 同时约束 macOS 和 iOS 脚本，避免同一仓库生成依赖图不同、source revision 相同的两个包。

## Blockers

- None.

## Verification

- `xcodebuild -resolvePackageDependencies -onlyUsePackageVersionsFromResolvedFile` 已使用现有锁文件
  成功解析，未修改 Git worktree。
- `bash -n scripts/package-local-macos.sh scripts/package-local-ios.sh` 通过。
- `git diff --check` 通过。
- 主 worktree 的零参数 macOS 打包通过，产物为 2.7 (40)、arm64，签名和 DMG checksum
  有效；build stamp 为 `20260809-020939`，source revision 为 `2ed0b162b77f`。
- 打包使用锁定的 `swift-certificates 1.19.3`、`swift-log 1.14.0` 和
  `swift-system 1.7.5`，前后两个 `Package.resolved` 与 Git worktree 均未变化。
