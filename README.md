# Three Orbs · 今日三件事

![Three Orbs icon](app/src/main/res/mipmap-anydpi/ic_spatial_launcher.png)

Three Orbs 是一个基于 PICO Spatial SDK 0.13.3、Kotlin 与 Jetpack Compose SpatialUI 构建的轻量空间任务应用。每天固定三个任务槽，每件事对应一颗等权重的 3D 发光球，帮助用户只关注当天最重要的三件事。

## 核心功能

- 首次启动输入三件事，生成三颗等边三角排列的 3D 任务球
- 短捏编辑任务；长捏 0.8 秒完成任务，并提供 2 秒撤销入口
- 通过 3D 抓取环整体移动球组，三球相对位置与尺寸保持不变
- 支持任务编辑、替换、完成、归档，以及未完成任务延续到明天
- 三项全部完成后，日期墙生成小型星座
- DataStore 纯本地持久化，仅保留最近 14 天历史
- 同时使用 Spatial Pointer 交互路径适配手柄射线与手部追踪
- 无提醒、日历同步、排行榜、重力或刚体物理

## 技术栈

- Kotlin
- Jetpack Compose + PICO SpatialUI
- PICO Spatial SDK 0.13.3
- Spatial ECS：Entity、ModelComponent、TransformComponent、CollisionComponent、InteractableComponent
- Preferences DataStore

## 构建

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Debug APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 真机安装

```powershell
pico-cli device list --format json
pico-cli app install --device <device-id> app\build\outputs\apk\debug\app-debug.apk
pico-cli app launch --device <device-id> com.example.threeorbs --activity .platform.LaunchActivity
```

## 数据与隐私

所有任务与最近 14 天历史均保存在设备本地。项目不申请日历同步、排行榜或云端任务存储能力。
