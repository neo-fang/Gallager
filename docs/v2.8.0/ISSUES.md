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
