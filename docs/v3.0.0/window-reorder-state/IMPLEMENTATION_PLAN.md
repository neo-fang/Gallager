# CtrlX 3.0.0：纯终端 Window 拖拽修复

## 问题

本地 session 只有 tmux Window、尚未打开文件或浏览器标签时，
`SessionFileTabsState` 可能不存在。拖拽 UI 接收事件后使用可选写入，
而排序回调又在状态缺失时直接返回，最终没有执行 tmux Window 重排。

现有 Tab Reorder E2E 在拖拽前创建 Browser 标签，意外初始化了该状态，
因此没有覆盖真实的纯终端场景。

## 实施范围

1. 选择本地 session 时立即创建唯一的 `SessionFileTabsState`。
2. 本地 `WindowTabBar` 要求非空状态，删除拖拽链路中的可选写入。
3. 将本地 E2E 的首次拖拽放到 Browser/File 标签创建之前。
4. 保持 tmux 重排算法、远程协议和 split 行为不变。

## 验收标准

- 纯终端 session 的 Window 可拖到指定位置，tmux 实际顺序同步更新。
- 新建 Browser/File 标签后拖拽行为不回归。
- Window 重排单元测试和相关构建通过。

