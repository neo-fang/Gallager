# CtrlX README 重写实施计划

## 目标

将 README 从内部组件说明改为面向使用者的 CtrlX 发行版首页，突出两个不会随 Agent
provider 变化的核心能力：tmux 原生复用，以及多主机、全端访问。

## 定位边界

1. 产品体验称为“多主机、全端访问（Any-device access）”，不误称为 P2P。
2. 明确 Mac 同时可以是 tmux Host 和 Viewer；iPhone 当前只作为 Viewer。
3. 明确 Relay 负责配对和密文路由，不宣称设备间建立直接网络连接。
4. tmux 是终端状态的唯一事实来源；CtrlX 退出、断线或切换 Viewer 不结束 tmux 会话。
5. Agent 识别、通知和辅助操作是增强能力，不把 CtrlX 描述成特定 Agent CLI 的包装器。

## 实施内容

1. 重写首屏定位、核心价值和连接模型。
2. 增加端能力矩阵、典型使用场景和零参数 macOS 安装入口。
3. 保留构建、对应源码、上游归属及 AGPL 义务。
4. 校验 README 内部链接、公开 URL、Markdown 格式和事实边界。

## 验收标准

- 首屏同时表达 tmux-native、多主机和 Mac/iPhone 访问能力。
- 不出现 P2P、账号漫游或 iPhone Host 等不准确表述。
- 安装、自托管、构建和许可证入口可用。
- `git diff --check` 通过。
