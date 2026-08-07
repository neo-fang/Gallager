# Gallager 2.8.0 Issues

## Mac viewer 在高频刷新 pane 上持续占用一个 CPU 核

- **状态**：Stage 3 部分修复（payload 扫描已消除，SwiftTerm 绘制瓶颈仍存在）
- **复现**：打开带 Codex 状态动画的本地 tmux pane；即使用户不输入，Gallager
  仍持续约 100% CPU。
- **证据**：pipe-pane 约 8 KB/s；进程采样热点为
  `InteractiveTerminalView.feedPreservingScroll` →
  `TerminalPayloadCache.extractAndClear`。
- **根因**：第一层为 OSC 8 payload 全缓冲区扫描；消除后，高频输出下的
  SwiftTerm CoreGraphics 文字绘制成为剩余主要瓶颈。
- **约束**：不得通过降低 terminal stream 实时性掩盖问题，也不得改变 relay 或 tmux
  协议；未带来实测收益的实验性渲染路径不得默认开启。

## Mac viewer 打开远程 pane 时显示历史回刷

- **状态**：Stage 16 处理中。
- **复现**：Mac viewer 打开、切换或重连远程 pane，首屏历史和捕获期间输出分阶段
  显示，而不是准备完成后一次出现。
- **证据**：viewer 收到 initial state 后立即进入 streaming；Host 的
  `flushBuffer()` 只排入异步 delegate 队列便返回，start command success 不能代表
  bootstrap 数据已发送。initial 和增量数据还会广播给未订阅该 pane 的 Viewer。
- **根因**：远程流缺少明确的 bootstrap 完成屏障，并把终端恢复的中间状态直接暴露
  给 SwiftTerm 绘制。
- **约束**：不得常驻全部 window、丢弃捕获期间输出、缩减为仅可见屏幕或使用固定等待。
