# Stage 3 TODO：Terminal Payload 缓存性能

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 7/8 tasks
- **Dependencies**: Stage 2 🟡（基于其最新提交开发，不修改 stream recovery 语义）

## 根因证据

- [x] 在真实 Codex pane 上复现 Gallager 持续约 100% CPU。
- [x] `sample` 定位到每个 pipe-pane 数据块都会执行
  `TerminalPayloadCache.extractAndClear`，并遍历约 1000 行 × 187 列的终端缓冲区。
- [x] 增加可处理跨块边界的 OSC 8 序列检测。
- [x] 将普通输出路径缩减为只校验已有缓存条目。
- [x] 增加聚焦单元测试并通过相关测试。
- [x] 完成 macOS 构建并用真实 Codex pane 复测；payload 热点已消除。
- [x] 使用不带调试插桩的 Release 构建复测总 CPU。
- [ ] 人工验证 OSC 8 链接、普通 URL 和终端输入无回归。

## 中间测量

- payload 优化后，8 秒采样中 `TerminalPayloadCache.update` 仅占 4 个样本，
  不再是 feed 热点。
- 总 CPU 仍约 102%–104%；新热点为 SwiftTerm `TerminalView.draw` 的
  CoreGraphics 文字绘制。该数据来自带 profile/coverage 插桩的 Debug 构建，
  不能直接代表发布包。
- 已实验 SwiftTerm Metal 渲染器，并从采样确认路径生效；Debug CPU 仍为
  101%–103%，热点转为每帧 `buildDrawData`。因无实际收益已回滚，不引入
  实验性默认渲染风险。
- 无覆盖率插桩的 Release 在同一 187 × 49 持续刷新 pane 上仍约
  89%–101% CPU。移除 wrapper layout 失效、50 ms feed 合并，以及合并后再启用
  Metal，都没有带来稳定收益，相关实验代码已全部回滚。

## Blockers

- 最终交互验收需要在安装新 Mac App 后完成。
