# Stage 29 Code Review Report

## Scope

- macOS DMG 与 iOS IPA 的本地 Xcode 打包命令。
- Swift package 锁文件使用方式与构建 provenance。

## Findings

### Critical / High / Medium

- **High（已修复）**：本地打包允许 Xcode 查询满足版本范围的更新，可能使同一 Git revision
  使用未提交的依赖图，并让构建受远端网络状态影响。

### Low

- None.

## Correctness checks

- 两条打包命令均显式使用已提交的 `Package.resolved`。
- 未改变缓存、签名、版本、build stamp、source revision 或产物路径。
- 不接受命令行覆盖，原有零参数调用保持不变。
- 锁定模式依赖解析、shell 语法和 `git diff --check` 通过。

## Assessment

Approved. 修改仅增加 Xcode 官方锁定开关，没有引入第二套依赖解析逻辑或额外配置面。
