# Stage 1 Code Review Report

## Review Scope

- Range: `54c8683..82c5d16`
- iOS photo selection and image-upload lifecycle
- Shared Apple-platform image normalization
- Existing `SendDroppedFiles` E2EE and Host landing-path contract

## Findings

### P2 / Resolved：HEIC orientation 在格式转换时可能丢失

初版沿用 macOS clipboard 实现，以 `CGImageSourceCreateImageAtIndex` 直接解码后重编码。iPhone
竖拍 HEIC 经常以横向像素配合 EXIF orientation 存储，这条路径可能把图片旋转后交给 Agent。

**Resolution**：所有需要重编码的格式先通过 ImageIO thumbnail transform 应用 orientation，再
输出 PNG/JPEG；增加带 orientation=6 的 HEIC 回归测试，验证输出尺寸由 64×48 正确变为
48×64。

### P2 / Resolved：新增照片状态使现有 SwiftUI 页面超过类型检查复杂度

首版把选择、上传和错误状态直接加入 `WindowLayoutView`，iPhoneOS 编译在原有大型 View
表达式上超时。

**Resolution**：将完整生命周期收敛到 `ImageUploadToolbarButton`；父页面只传当前 pane ID 和
现有 Relay client，没有新增 ViewModel、协议或全局状态。

## Verification

- 图片规范化测试：6/6 通过。
- iPhoneOS arm64 Debug 无签名构建：通过。
- macOS arm64 Debug 构建：通过。
- 真机签名与深度校验：通过。
- iPhone 覆盖安装：成功，build stamp `20260815-134019`，revision `82c5d16b4c46`。
- `git diff --check`：通过。
- SwiftLint 未安装；Xcode 只报告既有安装提示。

## Design Review

- 目标 pane 在照片读取前捕获，异步压缩期间切换 pane 不会误投。
- 一次只允许一个上传；读取、压缩、上传和页面退出都有确定的结束路径。
- 只使用系统 `PhotosPicker` 的用户选择结果，不申请或遍历照片库权限。
- 图片不自动追加 Enter，Host 仍以一次 bracketed paste 插入可读临时路径。
- Wire model、Relay、Host 落盘规则和 tmux session 均未改变。

## Assessment

代码审查通过，无剩余 P1、P2 或 P3。Stage 集成仍以 iPhone 真机完成一次照片选择并确认远端
pane 收到图片路径为最终验收条件。

