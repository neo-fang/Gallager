# Stage 23 TODO：iOS 终端双击宽字符选区

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 1/6 tasks
- **Dependencies**: Stage 19 ✅, Stage 22 ✅

## Tasks

- [x] 审计 iOS 双击手势、嵌套滚动坐标和 SwiftTerm 选词链路。
- [ ] 为双列字符左右半格命中增加失败测试。
- [ ] 修复续格归一与连续宽字符选词扫描。
- [ ] 更新并统一 Gallager 的 SwiftTerm 固定 revision。
- [ ] 完成聚焦测试、完整测试与 iOS device 构建。
- [ ] 完成 iPhone 真机双击选区验收。

## Decisions

- 根因位于 SwiftTerm 选择服务：双列字符右半格是 `code == 0`、`width == 0` 的续格，
  当前选词把它当成空字符。
- UIKit 已将内外滚动状态折算进 `location(in:)`，不增加偏移补丁。
- 修复放在 SwiftTerm fork 的单一选词边界，不在 Gallager 重写选词逻辑或增加竞争手势。

## Blockers

- None.

## Verification

- 源码审计确认 `calculateTapHit` 返回显示缓冲区列；`selectWordOrExpression` 直接读取该列，
  尚未像链接检测一样把双列字符续格回退到左侧本体。
