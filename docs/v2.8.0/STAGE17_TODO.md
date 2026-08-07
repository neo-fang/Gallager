# Stage 17 TODO：Mac Viewer 交互延迟与主线程公平性

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 7/7 tasks
- **Dependencies**: Stage 14 ✅, Stage 16 ✅

## Tasks

- [x] 建立同一远程 pane 的端到端输入与回显测量边界。
- [x] 对比 Mac/iOS Viewer 热路径并记录平台差异。
- [x] 验证本地 sessions 空闲及持续输出对 MainActor 的影响。
- [x] 实施最小的交互发送与/或终端 feed 公平性修复。
- [x] 增加输入顺序、调度上界、取消和持续输出回归测试。
- [x] 完成完整测试、macOS Release 构建、签名及 DMG 校验。
- [x] 完成同一网络下 Mac/iOS 与双 Mac 真机验收。

## Decisions

- 不使用本地字符预回显，屏幕内容只来自 Host PTY stream。
- 共用网络路径已由静态代码确认；平台差异必须从调度或渲染证据中定位。
- 可以用更多但有界的 CPU 换取交互延迟，不接受无界 Task、队列或 tmux 子进程增长。
- 保持 E2EE、relay 协议和 terminal 字节顺序不变。

## Blockers

- 无。

## Verification

- Mac 与 iOS 共用 `ViewerRelayClient`、E2EE、relay command 和 Host tmux
  执行路径；Mac 额外经过 runloop 合批及原有 10ms 定时发送窗口。
- Release 采样确认，运行中的 agent 每约 100ms 将 tmux pane title 改为
  `⠇ coding`、`⠏ coding` 等动画帧。旧版即使隐藏侧栏或关闭主窗口，采样仍命中
  `PipePaneReader → handleStreamTitleChange → paneStates.modify`，持续使
  MainActor/SwiftUI 状态树失效。
- 候选版只去除“Braille 动画前缀 + 非空语义标题”，tmux 原始标题仍正常旋转，
  `coding` 等真实标题变化仍传播。8 秒候选版采样中，上述 title/state mutation
  热栈由旧版的 7 次命中降为 0 次。
- Mac remote input 现在先完成同一 runloop 的 Meta/Option 合批，然后立即进入
  FIFO send queue，不再与终端绘制竞争额外 10ms timer；raw input 和重连取消
  顺序保持不变，未加入本地预回显。
- 聚焦测试 16 项通过；完整 Swift package 测试 1610 项、225 suites 通过。
- macOS arm64 Release 构建、Development 签名、严格 codesign 校验和本机覆盖安装
  已通过；CLI `wait-ready`/`ping` 正常，现有 `coding` tmux session 保持完好。
- 固定名称 `Gallager-2.7-zengjice.dmg` 已更新；映像 CRC、只读挂载、包内 App
  深度签名、2.7 (40) 版本、arm64 主程序/CLI 及源产物可执行文件哈希均校验通过。
  DMG SHA-256 为
  `25901c1e989690de3e40407362795d9de394e6f217c5becc83691896451c5d31`。
- 同一网络下使用 Mac/iOS Viewer 对照验收：Mac 输入响应较旧版明显改善，当前体验
  可以接受。
