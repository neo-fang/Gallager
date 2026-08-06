# Stage 7 Code Review Report

## Summary

- **Scope**：macOS terminal layout 刷新、URL 检测缓存、SwiftTerm CoreGraphics 行级缓存。
- **Objective**：降低复杂 Codex transcript 小范围刷新时的主线程 CPU，同时保持终端交互和视觉正确性。
- **Assessment**：✅ Approved。

## Findings

### Critical / High

- 无。

### Medium

- 初版 AppKit dirty-region 跟踪没有稳定收益，已删除，未进入提交。
- 初版行缓存裁剪只在链接高亮回调触发，可能随 scrollback 增长；已改为每次绘制入口按完整 viewport 裁剪。

### Low

- Gallager 需要维护 SwiftTerm fork，依赖已锁定完整 commit SHA，避免分支头漂移。

## Verification

- Gallager 完整 Swift package：1559 tests passed。
- Gallager URL row cache：5/5 passed。
- SwiftTerm CoreGraphics cache：8/8 passed。
- 隔离 Xcode Release 构建通过，并确认 SwiftTerm checkout 为
  `2944bf55392500e165be43ed5481b7066b58f3cc`。
- 同场景 30 秒 Release CPU：平均 98.2% 降至 83.7%。
- 本机覆盖安装后，输入、滚动、选择、链接、主题和窗口 resize 人工验收通过。

## Recommendation

- 可以合入 `develop/v2.8.0`。后续升级 SwiftTerm 时先 rebase fork 补丁并重复 Stage 7 性能与交互回归。
