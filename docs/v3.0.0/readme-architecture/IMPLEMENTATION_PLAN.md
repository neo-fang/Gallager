# CtrlX README 架构图实施计划

## 目标

在英文与简体中文 README 的首屏加入同一张简洁架构图，一眼表达 CtrlX 的四个核心事实：

1. tmux 是持续运行的终端状态源，CtrlX 断开不会结束任务。
2. 系统终端、Mac Viewer 和 iPhone Viewer 复用同一 tmux 工作区。
3. tmux pane 可以原样承载 Codex、Claude Code、Shell 和任意 TUI。
4. 跨设备链路采用 E2EE，Relay 只转发密文，不能读取终端内容。

## 设计约束

- 使用仓库原生 SVG，确保 README 缩放清晰、文字准确且可维护。
- 使用固定深色石墨背景，兼容 GitHub 明暗主题。
- 不把 Relay 画成 session 宿主，不把 iPhone 画成 Host，不宣称纯 P2P。
- 不增加第二张本地化图片；中英文 README 共用同一张以避免内容漂移。
- SVG 包含 `title`、`desc`，README 图片包含准确的替代文本。

## 验收标准

- SVG XML 校验通过，可正常渲染为预览图。
- 320px 缩放下仍能理解 tmux、Agent、continuity 和 E2EE 四层关系。
- README 与 README_ZH 的图片引用有效。
- `git diff --check` 通过。
