# Stage 30 TODO：macOS tmux window 快捷导航

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 1/5 tasks
- **Dependencies**: Stage 29 ✅

## Tasks

- [x] 核对现有 tab/session 菜单命令、window 选择和分屏状态模型。
- [ ] 增加纯 window 导航决策与聚焦测试。
- [ ] 接入 scene-scoped macOS 菜单快捷键及本地/远程选择。
- [ ] 完成完整测试、构建与代码审查。
- [ ] 合入 `develop/v2.8.0` 并清理 worktree。

## Decisions

- 保留现有 `⌘⇧[` / `⌘⇧]` 全 tab 导航；新命令只处理 tmux window。
- 分屏只导航左侧主区域，右侧内容保持固定，避免在没有 side-focus 单一真相时猜测用户焦点。
- 第一版使用固定的 macOS 惯用快捷键，不引入快捷键录制、冲突解析或持久化配置。

## Blockers

- None.

## Verification

- Pending.
