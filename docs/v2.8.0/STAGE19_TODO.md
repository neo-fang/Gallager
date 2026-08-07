# Stage 19 TODO：macOS 终端选择与链接激活隔离

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 1/6 tasks
- **Dependencies**: Stage 18 ✅

## Tasks

- [x] 记录误触根因、修复边界和验收标准。
- [ ] 为 Gallager 鼠标手势增加单击/拖动/多击判定。
- [ ] 阻止远端 file URL 回退到本机系统打开器。
- [ ] 修复 SwiftTerm fork 的底层链接误触和状态复位。
- [ ] 增加并通过聚焦回归测试。
- [ ] 完成完整测试、macOS Release 构建和本机验收安装。

## Decisions

- 链接激活的唯一合法手势是 `clickCount == 1` 且本次手势未发生拖动。
- 选择与复制是同一手势的后续行为，不通过关闭自动复制规避误触。
- 远端 Host 的文件路径没有本机打开语义；`file://` 必须在 Viewer 边界被消费。
- Gallager 与 SwiftTerm 两层使用相同判定，防止任一事件路径绕过上层保护。

## Blockers

- 无。

## Verification

- 待完成。
