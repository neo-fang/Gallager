# Stage 3 TODO

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 4/4 tasks
- **Dependencies**: Stage 2 ✅

## Tasks

- [x] 在 `zen_coding` 既有 pane 中复现并定位 Codex 的 `TERM=dumb` 误判。
- [x] 规范化 tmux control client 的缺失、空值和 `dumb` TERM。
- [x] 增加环境规范化回归测试。
- [x] 构建安装并在既有 pane 中完成 Codex TUI 验收。

## Decisions

- 不修改用户 tmux 配置或 shell 启动文件。
- 不向既有 pane 注入 `export TERM`。
- 不伪造 `TERM_PROGRAM`；实测只需为 control client 提供有效 `TERM`。
