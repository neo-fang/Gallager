# 纯终端 Window 拖拽修复 TODO

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 4/4 tasks
- **Dependencies**: CtrlX 3.0.0 distribution ✅

## Tasks

- [x] 定位纯终端 session 状态缺失的静默失败路径
- [x] 建立本地 session tab 状态不变量
- [x] 增加纯终端拖拽回归覆盖
- [x] 完成构建与测试验证

## Verification

- `WindowReorderTests`：5/5 通过，包含真实隔离 tmux 重排。
- macOS Debug App 构建通过。
- `Tab Reorder` 真实 App E2E：在创建 Browser/File 标签之前，拖拽 `winC`
  到 `winA` 前方成功；tmux 实际顺序变为 `winC,winA,winB`，UI 动态 ID
  同步更新，Session 往返后顺序保持。
- 完整旧场景后续仍会被既有的自动命名 Window 动态 AX 文本断言阻塞；该问题已在
  `docs/v2.9.0/window-selection-hotfix/STAGE1_TODO.md` 记录，与本修复无关。

## Blockers

- 当前无产品功能阻塞。

