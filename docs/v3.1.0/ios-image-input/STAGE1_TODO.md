# iOS 图片输入 Stage 1 TODO

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 5/6 tasks
- **Dependencies**: macOS `SendDroppedFiles` 图片投递链路 ✅

## Tasks

- [x] 审计现有图片准备、E2EE 传输与 Host 落盘链路
- [x] 下沉并测试跨 Apple 平台图片规范化器
- [x] 实现 iOS 照片选择、目标 pane 固定与发送生命周期
- [x] 增加上传进度、取消和错误反馈
- [x] 完成聚焦测试及 macOS/iPhoneOS 构建
- [ ] 完成真机验收与 code review

## Blockers

- 当前无阻塞项。

## Verification

- 图片规范化测试：6/6 通过，覆盖 PNG、JPEG、TIFF、HEIC orientation、大图压缩和无效输入。
- iPhoneOS arm64 Debug 无签名构建通过。
- macOS arm64 Debug 构建通过，现有 Mac 图片粘贴继续使用同一规范化器。
- `git diff --check` 通过。
- SwiftLint 未安装；两端 Xcode 构建仅报告既有安装提示。
- 待 iPhone 真机验证照片选择、上传进度和远端 pane 路径插入。
