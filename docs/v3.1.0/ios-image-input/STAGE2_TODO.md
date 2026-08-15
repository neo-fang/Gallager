# iOS 图片输入 Stage 2 TODO

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 4/7 tasks
- **Dependencies**: Stage 1 ✅

## Tasks

- [x] 定义 Stage 2 范围、交互和验收标准
- [x] 提供相册多选、图片文件、相机和剪贴板入口
- [x] 实现多图共享 Relay 预算的规范化
- [x] 实现发送前缩略图、大小、删除和确认交互
- [ ] 完成单元测试及 iOS/macOS 构建验证
- [ ] 完成 code review 并清零 P1/P2/P3
- [ ] 完成 iPhone 真机验收

## Blockers

- 当前无阻塞项。

## Verification

- `RelayImagePreparerTests`：8/8 通过，含多图共享预算与无效成员回归。
- iPhoneOS arm64 Debug 无签名构建通过。
- 构建设置已包含 `NSCameraUsageDescription`。
