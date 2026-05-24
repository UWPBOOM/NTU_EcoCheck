# NTU电费查询

南通大学宿舍电费查询 Android 应用。一键查询宿舍剩余电量，简洁高效。

## 功能

- **统一身份认证登录** — 学号 + 密码，自动管理会话
- **三级联动选择** — 校区 → 楼栋 → 房间，数据来自 3 个校区 / 74 栋楼 / 11607 间宿舍
- **一键查询** — 后台模拟桌面浏览器完成全流程，秒出结果
- **查询历史** — 自动记录每次查询，按时间倒序展示
- **记住配置** — 学号、密码、房间选择本地持久化，下次打开自动填充

## 技术栈

| 层级 | 技术 |
|------|------|
| UI | Jetpack Compose + Material Design 3 |
| 架构 | MVVM (ViewModel + StateFlow) |
| 网络 | OkHttp 4.12 (Cookie 管理 + 桌面 UA 伪装) |
| 解析 | Jsoup 1.18 (HTML) + kotlinx.serialization (JSON) |
| 持久化 | Room 2.6 (SQLite) |
| 并发 | Kotlin Coroutines + Flow |

## 构建

1. 用 Android Studio 打开项目根目录
2. 等待 Gradle 同步完成
3. 连接设备或启动模拟器，点击 Run

**环境要求：**
- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17+
- Android SDK 34

> 首次同步需要下载依赖，请确保网络通畅。如遇 `gradle-*-src.zip` 下载超时的警告可安全忽略（仅影响 IDE 代码提示，不影响编译）。

## 项目结构

```
app/src/main/
├── AndroidManifest.xml
├── assets/
│   └── ntu_electricity_rooms.json    # 宿舍数据 (3 校区 74 楼栋 11607 房间)
├── java/com/ntu/electricity/
│   ├── MainActivity.kt               # 入口 + Navigation
│   ├── EcoCheckApplication.kt        # Application 初始化
│   ├── data/
│   │   ├── local/                    # Room 数据库、DAO、Entity
│   │   └── model/                    # JSON 反序列化模型
│   ├── network/
│   │   ├── NtuHttpClient.kt          # OkHttp 封装 (Cookie + UA)
│   │   ├── HtmlParser.kt             # Jsoup HTML 解析
│   │   ├── ElectricityQuerier.kt     # 完整查询流程 (7 步)
│   │   └── RoomDataLoader.kt         # 房间数据加载
│   ├── repository/
│   │   └── EcoRepository.kt          # 数据仓库
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── HomeScreen.kt         # 首页 (表单 + 结果)
│   │   │   └── HistoryScreen.kt      # 历史记录
│   │   └── theme/                    # MD3 主题 (颜色 / 字体)
│   └── viewmodel/
│       ├── HomeViewModel.kt          # 首页状态管理
│       └── HistoryViewModel.kt       # 历史状态管理
└── res/
    ├── drawable/                     # 自适应图标 (闪电 ⚡)
    └── values/                       # 字符串、主题
```

## 图标设计

扁平化闪电符号，NTU 蓝 (#0052D9) 底色 + 白色闪电 + 柔和光晕点缀。矢量 XML 绘制，适配所有分辨率。

## License

仅供南通大学校内使用。
