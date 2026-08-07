# Stage 18 TODO：macOS 本地终端输入延迟

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 4/6 tasks
- **Dependencies**: Stage 17 ✅

## Tasks

- [x] 固定本地输入场景并记录逐次 tmux 客户端调用基线。
- [x] 为持久连接命令编码、命中、回退和 FIFO 顺序增加聚焦测试。
- [x] 复用现有 control-mode 连接发送本地交互按键。
- [x] 保留且验证连接不存在、编码不支持或写入前断开时的进程回退。
- [ ] 完成聚焦测试、完整测试和 macOS Release 构建。
- [ ] 使用 Release 产物完成本机 local session 验收并记录结果。

## Decisions

- 第一阶段只消除本地交互输入的逐批次 tmux 子进程，不修改输出渲染链路。
- 不替换插件、remote host 或非交互命令使用的 `TmuxService` 进程路径。
- 持久路径必须安全编码 literal 文本，不接受未经编码的任意 control-mode 命令。
- 只有确定尚未写入 control client 时才允许回退；写入后的不确定失败不得重放。
- `KeystrokeCoalescer` 的普通按键快速路径是条件性第二阶段，没有测量证据不实施。

## Blockers

- 无。

## Verification

- 待完成。
