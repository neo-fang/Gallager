# CtrlX 3.0.0 Stage 1 TODO：仓库与合规基线

## Stage Status

- **Status**：✅ Completed
- **Progress**：6/6 tasks
- **Dependencies**：`develop/v2.9.0@919c777` ✅

## Tasks

- [x] 创建 `feature/ctrlx-distribution` 隔离 worktree。
- [x] 新增 `NOTICE.md`，说明 CtrlX 来源、非官方关系和 AGPL 许可。
- [x] 新增 `MODIFICATIONS.md`，记录基础提交、日期、维护者和主要修改。
- [x] 更新 README 的 CtrlX 产品、来源、构建和源码提供说明。
- [x] 确认 `LICENSE` 与 `THIRD_PARTY_LICENSES.md` 保持完整。
- [x] 执行敏感配置扫描、文档文件检查和 `git diff --check`。

## Acceptance

- [x] 来源、基础提交、日期与维护者可从仓库直接审计。
- [x] CtrlX 不暗示获得 Gallager 官方认可。
- [x] AGPL 网络源码提供义务有显著说明。
- [x] 无真实签名、APNs、Relay、DNS 或支付凭据进入提交。

## Blockers

- `jicezeng/CtrlX` 尚未确认创建，因此本 Stage 不更改 remote，也不 push。
