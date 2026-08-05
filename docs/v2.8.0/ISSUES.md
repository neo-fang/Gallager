# Gallager 2.8.0 Issues

## Mac viewer 在高频刷新 pane 上持续占用一个 CPU 核

- **状态**：Stage 3 修复中
- **复现**：打开带 Codex 状态动画的本地 tmux pane；即使用户不输入，Gallager
  仍持续约 100% CPU。
- **证据**：pipe-pane 约 8 KB/s；进程采样热点为
  `InteractiveTerminalView.feedPreservingScroll` →
  `TerminalPayloadCache.extractAndClear`。
- **根因**：为提取 OSC 8 链接，每个小数据块都会扫描完整 scrollback 与可见区域；
  动画输出把该 O(rows × columns) 操作放大为持续主线程负载。
- **约束**：不得通过降低 terminal stream 实时性掩盖问题，也不得改变 relay 或 tmux
  协议；优化应局限于 payload 缓存的扫描条件和失效检查。
