# macOS Host 拖放修复 TODO

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 3/5 tasks
- **Dependencies**: Stage 8 远程 Host 顺序 ✅

## Tasks

- [x] 复核真机失败路径并确定拖放容器根因
- [x] 用进程内几何命中拖拽替换 Transferable 接线
- [x] 覆盖拖放状态清理、顺序与持久化测试
- [ ] 完成 macOS 构建并覆盖本机 App 验证
- [ ] 完成 code review、合入与发布收尾

## Blockers

- 当前无阻塞项。

## Verification

- Host 顺序、设置持久化与拖拽目标测试：8/8 通过。
- macOS arm64 Release App 构建与深度签名校验通过。
- 本机已覆盖安装 `75dcabb`，CtrlX CLI 返回 `pong`。
- 覆盖安装前后 tmux server PID 与 pane 快照保持不变。
- SwiftLint 未安装；Xcode 构建仅报告既有安装提示。
- 待完成真实侧边栏拖拽交互验收。
