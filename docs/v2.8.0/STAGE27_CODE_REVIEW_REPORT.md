# Stage 27 Code Review Report

## Scope

- iOS session 内窗口选择、切换与 session 缺失判断。
- Host 断线、重连和密集全量状态刷新时的 SwiftUI task 生命周期。
- App 内主动关闭 session 后的导航行为。

## Findings

### Critical / High / Medium

- **High（已修复）**：第一版只处理了 `WindowLayoutView` 的空快照路径，遗漏了
  `LiveTerminalView` 在任意 pane `streamEnd` 时调用父级 `dismiss()`。真机切换窗口仍可
  返回 session 列表。修正版删除该导航副作用，并删除父页面基于固定延时的缺失推断。

### Low

- 外部关闭 session 后不再自动返回列表，需要用户使用系统返回按钮。空快照没有可依赖的
  延迟上界，保留页面比基于定时器误退出更可靠；App 内主动关闭成功后仍立即返回。

## Correctness checks

- 初次进入且尚无状态时不会把空列表误判成 session 消失。
- 当前 window 短暂缺失时优先切换到 Host 标记的 active window，否则选择排序后的首个 window。
- 窗口候选变化通过稳定 task identity 重新协调选择；空集合保持当前导航。
- 空快照和 pane `streamEnd` 都不得改变 session 导航层级。
- 未修改 relay 协议、tmux 命令、键盘输入或 terminal stream。
- 意外 `streamEnd` 只允许一次自动 replacement recovery；再次结束时留在页面提供显式重连，
  不形成无界命令循环。
- 10 项聚焦测试、1659 项完整测试和 iOS generic device build 通过。

## Assessment

Approved. 修正版收回了子 pane 的导航所有权并删除基于时间的 session 缺失推断；状态更少、
责任边界更清楚，未增加协议分支或第二套导航状态机。真机验证新建和切换 window 均未再异常
返回 session 列表。
