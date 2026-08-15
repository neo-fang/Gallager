# README 架构图 Code Review Report

## Scope

- `docs/assets/ctrlx-architecture.svg`
- 英文 `README.md` 与简体中文 `README_ZH.md` 的首屏引用
- 原 ASCII 拓扑图移除

## Findings

未发现遗留的 P1、P2 或 P3 问题。

图中架构角色保持准确：

- tmux 位于 Host Mac，并明确标记为持续运行的 source of truth。
- Codex、Claude Code 和任意 TUI 位于 tmux pane 内，不由 CtrlX 包装或托管。
- 系统终端直接 attach 同一 tmux，表达原生复用。
- Mac 与 iPhone Viewer 通过 E2EE Relay 访问，iPhone 未被描述为 Host。
- Relay 只配对和转发密文，并明确不能读取终端内容。
- 断开 App、网络和切换 Viewer 后，由 CtrlX 重连，tmux 会话仍继续运行。

## Verification

- `xmllint --noout docs/assets/ctrlx-architecture.svg` 通过。
- 1600×900 原始渲染通过。
- 800×450 README 常见宽度渲染通过。
- 400×225 移动端缩略宽度仍可辨认核心层级，SVG 可继续无损缩放。
- 两份 README 共用同一 SVG，替代文本分别使用英文和简体中文。
- 原 ASCII 拓扑块已移除，未形成重复图示。
- `git diff --check` 通过。

## Assessment

Approved for user acceptance. The diagram is deterministic, maintainable, and keeps the Relay outside the
terminal lifecycle. Do not merge before visual acceptance.
