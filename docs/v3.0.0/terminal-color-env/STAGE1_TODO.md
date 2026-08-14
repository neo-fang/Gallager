# 终端禁色环境隔离修复 TODO

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 3/4 tasks
- **Dependencies**: CtrlX 3.0.0 distribution ✅；Window reorder hotfix ✅

## Tasks

- [x] 定位 `NO_COLOR` 从 CtrlX 启动环境进入新 tmux server 的路径
- [x] 在 session、window 和 pane 创建边界清除继承的 `NO_COLOR`
- [x] 增加回归测试
- [ ] 完成测试、构建及本机覆盖安装验证

## Blockers

- 当前无阻塞。
