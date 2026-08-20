# CtrlX 3.0.0 Stage 6 Code Review Report

## Review scope

本次审查覆盖 `feature/ctrlx-distribution` 相对基础提交
`919c7772928531d4d0bb266bdf275691d361901e` 的独立发行版改动，重点检查用户品牌、
Apple 技术身份、本地状态、E2EE/Relay 边界、构建发布脚本、对应源码披露与运维配置。

## Findings and disposition

### High

无未解决项。

### Medium

1. `scripts/deploy.sh` 将环境文件中的 host/user/path 直接拼入 SSH 命令。现已限制为明确的
   DNS/IPv4、用户名和绝对路径字符集，拒绝 shell 元字符。
2. 本地打包脚本在存在多个 Apple Development 证书时选择第一张证书，可能与 provisioning
   profile 或 xcconfig 的 Team ID 不一致。现已按对应 Team ID 精确选择证书。
3. 监控配置仍含上游 metric、Grafana folder、Hetzner instance 与部署目录。现已切换为
   CtrlX 通用自托管命名，并纳入技术身份边界检查。
4. Xcode 工程的辅助与 Release target 仍含上游 Development Team。现已全部改为从未提交的
   CtrlX 本地 xcconfig 读取，并把上游 Team ID 加入技术身份边界检查。

### Low

1. `sbin/auto-env.sh` 使用 Bash 不接受的下划线数字字面量，已改为普通整数；生成的
   `.env.local` 权限收紧为 `0600`。
2. 产品源码 URL 曾接受短 commit，无法保证不可变链接。现只把完整 40/64 位 revision
   视为精确源码，并增加测试。

## Verification

- 用户品牌与技术身份边界脚本通过。
- Swift 全量测试通过：1715 tests / 247 suites。
- macOS 与 iOS Simulator 无签名 Xcode build 通过。
- Linux Relay Docker release build 通过，运行容器四个公开元数据/健康端点通过。
- Sidecar Python tests 通过：132 tests。
- website production build、plist、shell、Python compile、Compose config 与 `git diff --check` 通过。

## Residual risk and approval

**结论：有条件批准源码合入，不批准正式发布。**

源码层面未发现 Critical/High/Medium 遗留问题。正式 release 仍必须等待专用 Apple/APNs、
Notary、Sparkle、DNS/Relay 资源，并完成 CtrlX 与 Gallager 双应用同时安装的真机隔离验收。
这些是明确的外部门禁，不能用占位配置或旧 Gallager 基础设施绕过。
