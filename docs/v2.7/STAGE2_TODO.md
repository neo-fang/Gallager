# Stage 2 TODO

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 5/5 tasks
- **Dependencies**: Stage 1 ✅

## Tasks

- [x] 复现并定位 macOS 普通字符/IME 与空格、回车输入路径差异。
- [x] 将 first responder 归还给 SwiftTerm `TerminalView`。
- [x] 增加焦点、marked text 和 Shift+Enter 回归测试。
- [x] 构建并替换本机 Gallager Mac App。
- [x] 使用独立 tmux session 完成英文、数字、中文和控制键验收。

## Decisions

- 不在外层视图重复实现 `NSTextInputClient`。
- 不改 tmux 发送层；自动化逐键输入已证明该链路正常。
- 不改 iOS 输入与 Relay 协议。
