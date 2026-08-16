# iOS 图片输入 Stage 2 TODO

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 6/7 tasks
- **Dependencies**: Stage 1 ✅

## Tasks

- [x] 定义 Stage 2 范围、交互和验收标准
- [x] 提供相册多选、图片文件、相机和剪贴板入口
- [x] 实现多图共享 Relay 预算的规范化
- [x] 实现发送前缩略图、大小、删除和确认交互
- [x] 完成单元测试及 iOS/macOS 构建验证
- [x] 完成 code review 并清零 P1/P2/P3
- [ ] 完成 iPhone 真机验收

## Blockers

- 当前无阻塞项。

## Verification

- `RelayImagePreparerTests`：9/9 通过，含多图共享预算、剩余预算重分配与无效成员回归。
- iPhoneOS arm64 Debug 无签名构建通过。
- macOS arm64 Debug 无签名构建通过。
- 构建设置已包含 `NSCameraUsageDescription`。
- 真机发现并修复预览 sheet 误清空草稿导致的 `0 images` 回归。
- 多图改为逐张单路径 paste，待真机确认 TUI 恢复 `Image #N` 附件显示。
- `git diff --check` 通过；SwiftLint 未安装，Xcode 仅报告既有构建脚本提示。
