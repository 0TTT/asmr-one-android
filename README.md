# AsmrOne 特供版（Android）

一个专为 Android 定制的 **精简 / 纯净版** [asmr.one](https://asmr.one/) 客户端，用于浏览、搜索、在线播放和离线缓存 ASMR 音声作品。

> ⚠️ 本项目仅供学习交流使用，请遵守当地法律法规与站点使用条款。所有作品版权归原作者及 DLsite 所有，请勿用于商业用途。

## 技术栈

| 组件 | 选型 |
| --- | --- |
| 语言 | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| 网络 | Retrofit + OkHttp + kotlinx.serialization |
| 播放 | Media3 (ExoPlayer) + MediaSessionService |
| 本地存储 | Room（收藏 / 历史 / 下载记录） |
| 依赖注入 | Hilt |
| 图片加载 | Coil |
| 后台下载 | WorkManager |

## 功能

- **浏览作品**：首页最新 / 下载最多 / 评分最高排序，双列瀑布流
- **搜索**：按作品名 / 社团 / 声优 / RJ 号搜索
- **作品详情**：封面、社团、声优、标签、评分、音轨列表
- **在线播放**：单曲播放 + 整作连播，后台播放 + 通知栏控制
- **离线下载**：音轨下载到本地缓存
- **收藏 / 历史**：本地收藏夹、播放历史

## 项目结构

```
app/src/main/java/com/asmrone/app/
├── data/
│   ├── model/          # 数据模型（Work / Track / Tag ...）
│   ├── remote/         # Retrofit API 接口
│   ├── local/          # Room 数据库（Entity / Dao / Database）
│   └── repository/     # 仓库层
├── di/                 # Hilt 依赖注入模块
├── service/            # 播放服务 / 下载 Worker
└── ui/
    ├── screen/         # 各页面 + ViewModel
    ├── component/      # 可复用组件
    └── theme/          # 主题
```

## API 说明

后端接口基地址：`https://api.asmr.one/api`

| 接口 | 说明 |
| --- | --- |
| `GET /works?page=&order=&sort=&subtitle=` | 作品列表 |
| `GET /search/{keyword}?page=&order=&sort=&includeTranslationWorks=` | 搜索 |
| `GET /tracks/{workId}?v=1` | 作品音轨/文件树 |

作品 JSON 关键字段：`id`、`title`、`circle.name`、`vas[].name`、`tags[].i18n`、`rate_average_2dp`（0-5 浮点评分，如 4.75）、`duration`（秒）、封面字段为驼峰 `mainCoverUrl` / `samCoverUrl` / `thumbnailCoverUrl`。

音轨节点关键字段：`type`（folder/file 递归树）、`title`、`children`、`media_stream_url`（在线播放）、`media_download_url`（下载）、`size`（int64）。媒体 URL 若为相对路径，代码已通过 `resolvedStreamUrl` / `resolvedDownloadUrl` 自动补全 `https://api.asmr.one` 前缀。

## 构建

### 环境要求

- JDK 17+
- Android Studio（含 Android SDK Platform 35）
- Android Gradle Plugin 8.5.2

### 步骤

```bash
# 1. 生成 gradle wrapper（首次，需本机已装 gradle 或直接用 Android Studio 打开）
gradle wrapper --gradle-version 8.9

# 2. 命令行构建 debug APK
./gradlew :app:assembleDebug

# 3. 产物位于
app/build/outputs/apk/debug/app-debug.apk
```

> 最简单的方式：直接用 **Android Studio** 打开项目根目录 `asmr-one-android/`，等待 Gradle 同步完成后点击 Run 即可。

### local.properties

若本机未配置 `ANDROID_HOME`，需在项目根目录创建 `local.properties`：

```
sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
```

## 注意事项

1. **网络环境**：部分网络环境下 `api.asmr.one` 可能被干扰，播放器在真机上走的是设备自身网络。
2. **后台播放**：Android 13+ 首次使用会请求通知权限，请授予以正常显示播放通知。
3. **下载存储**：离线文件保存在应用私有目录 `filesDir/downloads`，卸载应用会一并清除。

## 后续可扩展

- 标签筛选（tag / circle / va / duration / rate 过滤）
- 播放队列管理
- 字幕显示（`has_subtitle` 作品）
- 推荐 / 热门接口
- 设置页（主题、默认排序、下载目录）
