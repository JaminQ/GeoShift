# GeoShift — Android Mock Location APP

一个基于 Android Mock Location API 的位置模拟工具，通过地图选点来修改设备的 GPS 位置信息。

## 功能

- 🗺️ **OSM 离线/在线地图** — 基于 OSMDroid，无需 API Key
- 📌 **地图选点** — 点击地图或拖拽标记点选择位置
- 📋 **坐标复制/粘贴** — 一键复制或粘贴经纬度
- 🌍 **逆地理编码** — 自动显示选中位置的地址
- ◎ **Mock Location 模拟** — 点击"开始模拟"后将设备 GPS 定位到选定位置
- 🔔 **前台通知** — 模拟运行时显示通知栏状态
- 💾 **深色科技风 UI** — Material 3 设计

## 使用方法

### 1. 环境准备

- Android Studio ( Arctic Fox 或更新版本)
- Android SDK API 34
- Gradle 8.2+

### 2. 导入项目

1. 打开 Android Studio → `Open` → 选择 `geoshift-android` 目录
2. 等待 Gradle 同步完成（首次可能需要下载依赖）
3. 连接 Android 手机（开启 USB 调试）

### 3. 编译安装

```
./gradlew assembleDebug
```

或直接在 Android Studio 中点击 Run。

### 4. 启用 Mock Location

**关键步骤 — 必须先完成：**

1. 手机进入 **设置 → 关于手机 → 版本号**，连续点击 7 次开启**开发者选项**
2. 进入 **设置 → 系统和更新 → 开发人员选项**
3. 找到 **"选择模拟位置应用"**（部分机型叫"允许模拟位置"）
4. 选择 **GeoShift**

### 5. 使用

1. 打开 GeoShift
2. 在地图上点击选择目标位置
3. 点击 **"开始模拟"** 按钮
4. 授权位置权限
5. 完成 — 所有 APP 现在都会认为你在选定的位置

> ⚠️ 注意：部分 APP（如 Google Play 服务、银行类 APP）会检测 mock location 并拒绝服务。

## 技术说明

- **最低 Android 版本：** Android 7.0 (API 24)
- **目标版本：** Android 14 (API 34)
- **地图引擎：** OSMDroid (OpenStreetMap)
- **定位服务：** Google Play Services FusedLocationProvider
- **权限模型：** Android 12+ 使用 `FusedLocationProviderClient.setMockLocation()`

## 项目结构

```
geoshift-android/
├── app/
│   ├── build.gradle              # 应用级构建配置
│   └── src/main/
│       ├── AndroidManifest.xml   # 清单文件
│       ├── java/com/geoshift/mockloc/
│       │   ├── MainActivity.java          # 主界面
│       │   └── MockLocationService.java   # 模拟位置后台服务
│       └── res/
│           ├── layout/activity_main.xml   # 主界面布局
│           ├── values/                    # 字符串/主题
│           └── drawable/                  # 图标资源
├── build.gradle                  # 项目级构建配置
└── settings.gradle               # 项目设置
```

## 注意事项

- 部分国产 ROM（小米、华为、OPPO 等）对 Mock Location 有额外限制，可能需要在开发者选项中手动允许
- Android 12+ 的定位权限更加严格，首次使用需要手动授予精确位置权限
- 该工具仅用于 APP 定位功能测试等正当用途
