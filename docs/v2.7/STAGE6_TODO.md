# Stage 6 TODO

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 8/8 tasks
- **Dependencies**: Stage 5 ✅

## Tasks

- [x] 建立终端主题调色板单一数据源并新增 Anysphere Dark。
- [x] 本地、远程渲染及富文本复制共用主题调色板。
- [x] Default Dark 背景与外层深色窗口背景对齐。
- [x] 单 pane 移除焦点框，多 pane 保留原有焦点高亮和分隔线。
- [x] 增加调色板及富文本复制回归测试，完成 SwiftPM/Xcode 构建并安装本机版本。
- [x] 终端工作区外壳共用当前主题背景，并使用中性的标签选中样式。
- [x] 增加侧栏会话高亮设置，本地与远端会话共用且默认关闭。
- [x] 真机视觉验收主题切换、工作区一体性、侧栏选项及 pane 焦点提示。

## Decisions

- 不覆盖 Default Dark；Anysphere Dark 是第五个可选主题。
- 不删除焦点状态或 pane 聚焦同步，仅让单 pane 隐藏焦点框。
- 不增加第二套本地/远程主题代码。
- 工作区外壳复用终端主题调色板，不另建一套颜色配置。
