# Stage 7 TODO：macOS Terminal 行级渲染缓存

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 2/10 tasks
- **Dependencies**: Stage 3 ✅，Stage 6 🟡

## Tasks

- [x] 在本地 211 × 59 Codex spinner pane 复现 Release 约 100% CPU。
- [x] 通过 `sample` 确认主线程热点位于 SwiftTerm CoreGraphics 行构建与绘制。
- [ ] 测量单行变化时实际绘制行数与每帧耗时。
- [ ] 为 CoreGraphics renderer 增加有界行级缓存。
- [ ] 覆盖内容 generation、行 identity、row/cols 和全局样式失效。
- [ ] selection 和动态 link highlight 期间绕过缓存。
- [ ] 增加 SwiftTerm 聚焦测试。
- [ ] 运行 Gallager 聚焦测试及 macOS Release 构建。
- [ ] 用同一 Codex pane 完成 Release CPU 与主线程采样 A/B。
- [ ] 人工验证输入、滚动、选择、链接、主题和窗口 resize 无回归。

## Baseline

- Build：macOS Release，SwiftTerm 1.15.0。
- Pane：211 × 59，Codex `Working` spinner 约每 100 ms 更新一行。
- CPU：约 87%–105%。
- 10 秒采样：主线程约 835/1000 样本位于 `TerminalView.draw`；数据读取、过滤和
  `TerminalView.feed` 不是主要热点。

## Decisions

- 优先优化已测得的 CoreGraphics 路径，不默认启用实验性 Metal renderer。
- 缓存正确性依赖 SwiftTerm 已有的 `BufferLine.generation`，不逐帧比较整行 cells。
- 动态交互状态宁可短暂绕过缓存，也不维护第二套复杂的 selection/link revision 状态机。
- 性能结果必须来自相同 Release 场景；Debug 数据只用于定位，不作为验收数字。

## Blockers

- SwiftTerm 是外部 Swift package；完成可复现集成前需要一个承载补丁的 Git fork/revision。
