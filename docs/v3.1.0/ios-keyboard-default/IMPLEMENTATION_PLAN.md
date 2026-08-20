# CtrlX 3.1.0 Stage 3：iOS 默认键盘状态

## 问题

iOS 进入终端会话时始终默认隐藏软键盘。频繁输入的用户需要每次手动
点击键盘控件，但默认弹出又不适合以查看为主的现有用户。

## 设计

1. Settings 的 Terminal 区域增加 `Show Keyboard on Entry` 开关。
2. 配置通过现有 `IOSSettings` / `PreferencesService` 持久化，缺省为 `false`。
3. `WindowLayoutView` 创建时仅读取一次配置，作为该次会话页面的初始
   键盘状态。页面内用户后续的显示/隐藏操作仍是唯一实时状态源。
4. 修改设置后从下一次进入会话开始生效，不在已打开的终端中突然抢占
   键盘焦点。

## 非目标

- 不按 Host、session、window 或 pane 分别记忆键盘状态。
- 不改变键盘按钮位置、快捷键栏、输入代理或 Relay 协议。
- 不在 window 切换、pane 切换或 stream 重连时重复强制弹出键盘。

## 验收标准

- 新安装和未配置用户进入会话时仍默认隐藏键盘。
- 打开设置后，下一次进入会话会自动激活当前 pane 并弹出键盘。
- 关闭设置后，下一次进入会话恢复默认隐藏。
- 页面内的 Show/Hide Keyboard、复制页恢复和 blocking form 逻辑保持不变。
- iPhoneOS 构建通过，相关纯逻辑测试通过，并完成 iPhone 真机验收。
