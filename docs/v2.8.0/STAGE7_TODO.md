# Stage 7 TODO：macOS Terminal 行级渲染缓存

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 10/12 tasks
- **Dependencies**: Stage 3 ✅，Stage 6 🟡

## Tasks

- [x] 在本地 211 × 59 Codex spinner pane 复现 Release 约 100% CPU。
- [x] 通过 `sample` 确认主线程热点位于 SwiftTerm CoreGraphics 行构建与绘制。
- [x] 排除简单 ASCII 压测：它没有复现复杂 Codex transcript 的排版热点。
- [x] 将内容更新所需的 URL 装饰刷新与 AppKit/SwiftUI layout 解耦。
- [x] 为 CoreGraphics renderer 增加只保留 viewport 的行级缓存。
- [x] 为 Gallager URL 检测增加只保留 viewport 的行级缓存。
- [x] 覆盖内容 generation、行 identity、row/cols 和全局样式失效。
- [x] selection 和动态 link highlight 期间绕过 CoreGraphics 缓存。
- [x] 增加 SwiftTerm 与 Gallager 聚焦测试。
- [x] 运行 Gallager 完整 Swift package 测试及 macOS Release 构建。
- [x] 用同一 Codex pane 完成 Release CPU 与主线程采样 A/B。
- [ ] 人工验证输入、滚动、选择、链接、主题和窗口 resize 无回归。
- [ ] 将 SwiftTerm 补丁发布到可复现的 fork/revision，并替换本机临时 package path。

## Baseline

- Build：macOS Release，SwiftTerm 1.15.0。
- Pane：211 × 59，Codex `Working` spinner 约每 100 ms 更新一行。
- 30 秒 Release CPU：平均 98.2%。
- 10 秒采样：主线程约 835/1000 样本位于 `TerminalView.draw`；数据读取、过滤和
  `TerminalView.feed` 不是主要热点。

## Result

- 最终候选 30 秒 Release CPU：平均 83.7%，最高 92.6%，相对基线下降约 14.8%。
- 只增加 CoreText 行缓存而保留内容驱动 layout 时平均 94.1%，说明 layout 解耦是
  必要组成部分，不能用更多缓存掩盖错误的刷新边界。
- AppKit dirty-region 跟踪实验没有稳定收益，已删除；没有进入发布代码。
- Gallager 完整 Swift package 测试通过；聚焦 URL cache 测试 5/5 通过；SwiftTerm
  CoreGraphics cache 聚焦测试 8/8 通过；macOS Release 构建和签名校验通过。

## Decisions

- 优先优化已测得的 CoreGraphics 路径，不默认启用实验性 Metal renderer。
- 缓存正确性依赖 SwiftTerm 已有的 `BufferLine.generation`，不逐帧比较整行 cells。
- 动态交互状态宁可短暂绕过缓存，也不维护第二套复杂的 selection/link revision 状态机。
- 性能结果必须来自相同 Release 场景；Debug 数据只用于定位，不作为验收数字。

## Blockers

- SwiftTerm 是外部 Swift package；完成可复现集成前需要一个承载补丁的 Git fork/revision。
