# uwuSettingsExt

## 原 uwuAOSP 项目说明

`uwuSettingsExt` 是 uwuAOSP 的扩展设置中心，集中管理 uwuQS、后台策略、传感器访问、Moment/MomentArc、智能建议、应用跳转权限、认证和状态栏歌词等功能。

源项目：[uwuAOSP/platform_packages_apps_uwuSettingsExt](https://github.com/uwuAOSP/platform_packages_apps_uwuSettingsExt)

## UotanOS 集成

- 保留 `uwuSettingsExt`、`org.uwuaosp.settingsext`、uwu 资源/API/设置键与 `uwu-sdk/uwuCompose`。
- 用户可见的平台名称显示为 UotanOS，`uwuAI Core` 和 `uwuQS` 等组件名保持不变。

独立 Gradle 开发说明见 [README.gradle.md](./README.gradle.md)。
