# Stage 22 TODO：iOS 终端复制格式与多行粘贴

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 1/6 tasks
- **Dependencies**: Stage 11 ✅, Stage 20 ✅

## Tasks

- [x] 定位复制快照与系统粘贴的完整输入链路。
- [ ] 使用 SwiftTerm 逻辑行导出替代逐行拼接。
- [ ] 保留 bracketed-paste 起止序列及多行输入顺序。
- [ ] 增加快照格式和输入解析聚焦测试。
- [ ] 完成完整测试与 iOS device 构建。
- [ ] 完成 iPhone 真机复制、粘贴验收。

## Decisions

- 使用 SwiftTerm 已有逻辑行选择算法，不读取或暴露其内部 `isWrapped` 状态。
- 只识别标准 `CSI 200~` / `CSI 201~`，其它未知 CSI 继续丢弃。
- 复用现有 `SendKeystroke` FIFO，不新增 Relay 或 Host 命令协议。
- 复制页保持静态快照、只读和系统原生选择，不恢复终端键盘焦点。

## Blockers

- None.

## Verification

- 源码审计确认 SwiftTerm 粘贴会发送 bracketed-paste 起止序列；Gallager 当前将 200/201
  当作未知扩展键丢弃，正文中的 LF/CR 随后被解析为多个 `.enter`。
