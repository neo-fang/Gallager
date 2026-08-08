# Stage 23 TODO：iOS 终端双击宽字符选区

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 5/6 tasks
- **Dependencies**: Stage 19 ✅, Stage 22 ✅

## Tasks

- [x] 审计 iOS 双击手势、嵌套滚动坐标和 SwiftTerm 选词链路。
- [x] 为双列字符左右半格命中增加失败测试。
- [x] 修复续格归一与连续宽字符选词扫描。
- [x] 更新并统一 Gallager 的 SwiftTerm 固定 revision。
- [x] 完成聚焦测试、完整测试与 iOS device 构建。
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
- 修复前测试稳定复现：中文左半格只选中一个字，右半续格及宽 Emoji 续格复制为空；
  修复后双列字符左右半格、连续中文、中文与 ASCII 组合和宽 Emoji 均通过。
- SwiftTerm fork commit `0a664be86b78d0d16bb1dbd5ae5020f6e353070c` 已推送，Gallager
  `Package.swift` 与三个 `Package.resolved` 已统一固定到该 revision。
- SwiftTerm：457 tests / 39 suites 通过；Gallager：1,642 tests / 232 suites 通过。
- iPhoneOS generic device 无签名构建通过，产物为 `Gallager.app`；真机当前在
  `devicectl` 中显示 unavailable，待设备恢复连接后覆盖安装验收。
