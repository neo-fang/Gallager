# macOS Host 拖放修复 TODO

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 4/5 tasks
- **Dependencies**: Stage 8 远程 Host 顺序 ✅

## Tasks

- [x] 复核真机失败路径并确定拖放容器根因
- [x] 将 Host 标题迁移为普通 List 行并接入高优先级拖拽
- [x] 覆盖拖放状态清理、顺序与持久化测试
- [x] 完成 macOS 构建并覆盖本机 App 验证
- [ ] 完成 code review、合入与发布收尾

## Blockers

- 当前无阻塞项。

## Verification

- Host 顺序、设置持久化与拖拽目标测试：8/8 通过。
- macOS arm64 Release App 构建与深度签名校验通过。
- 本机已覆盖安装 `75dcabb`，CtrlX CLI 返回 `pong`。
- 覆盖安装前后 tmux server PID 与 pane 快照保持不变。
- SwiftLint 未安装；Xcode 构建仅报告既有安装提示。
- 首个直接手势版本 `75dcabb` 仍位于 Section header，真实侧边栏验收失败；该方案已废弃。
- 普通 List 行版本 `e40815d` 已完成 arm64 Release 构建、签名校验和本机覆盖安装。
- 普通 List 行版本真实验收为偶发成功：手柄和精确目标区域仍过小，交互尚不合格。
- 宽容命中版本 `6b7e129` 已通过专项测试、Release 构建、签名和本机覆盖安装。
- 宽容命中版本已通过用户真实侧边栏连续拖拽验收。
