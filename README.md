# 登科喵备考计划

面向微信小程序的备考计划与学习执行工具。使用 uni-app / Vue 3、Spring Boot 3.5、MySQL 8；核心计划由规则引擎生成，不依赖 AI。

## 现在可以体验什么

- 蓝色首页、考试倒计时、首页分享入口。
- 已按用户提供的UI设计调整：猫咪渐变横幅、彩色快捷入口、考试网格、生成完成页、计划总览与阶段切换、图形底部导航。
- 考研、公务员、教资、四六级、自定义考试；科目、权重、日期、学习预算可编辑。
- 基础/强化/冲刺阶段路线，按科目权重分配每日分钟数。
- 任务完成/撤销、七日学习图表、科目占比、连续打卡统计。
- 多计划创建与切换。
- 完整方案价格预览；支付尚未开放，不扣费，也不创建虚假已支付订单。

模板目前是通用学习路线，不含完整考试大纲、章节题库或实际考试日期库。教资等考试存在科目差异，创建时请修改为自己实际报考的科目。用户录入的日期是唯一考试日期来源。

## 直接导入微信开发者工具

当前工作区已生成构建目录：`D:\code\exam-assistant\frontend\dist\build\mp-weixin`。

1. 微信开发者工具 → 导入项目。
2. 目录选择上述 **mp-weixin 编译目录**，不要选仓库根目录或 `frontend/src`。
3. AppID：`wxbee1bab19364f057`（用户提供的本项目账号）。
4. 使用普通小程序模式；本项目后端使用 Spring Boot，不要求启用云开发。
5. 默认是本地体验，数据只保存在当前设备，不调用网络服务。
6. 首页 → 创建备考计划 → 填写日期/科目 → 生成 → 任务页打卡。

也可以导入仓库根目录 `D:\code\exam-assistant`：根目录 `project.config.json` 已通过 `miniprogramRoot` 指向 `frontend/dist/build/mp-weixin/`。需要先执行构建；仅点击开发者工具的“编译”不会把 Vue 源码转换为小程序代码。若提示找不到 `app.json`，执行 `scripts/dev.ps1 build` 后重新打开项目。

构建目录不提交 Git。其他电脑拉取仓库后需执行下面的构建命令。

## 前端开发与构建

本机快捷命令（在仓库根目录的 PowerShell 执行）：

```powershell
.\scripts\dev.ps1 miniapp   # 自动编译，导入 frontend/dist/dev/mp-weixin
.\scripts\dev.ps1 preview   # 浏览器预览
.\scripts\dev.ps1 backend   # 启动后端，并处理本机JDK临时路径问题
.\scripts\dev.ps1 build     # 构建微信和H5
.\scripts\dev.ps1 test      # 前后端测试
```

需要 Node.js 22 和 npm。

```powershell
cd frontend
npm ci
npm test
npm run build:mp-weixin
npm run dev:mp-weixin
```

`build:mp-weixin` 输出 `dist/build/mp-weixin`，`dev:mp-weixin` 输出 `dist/dev/mp-weixin`；后者适合开发中自动编译。H5 预览：`npm run dev:h5`，地址 `http://127.0.0.1:5173`；`npm run build:h5` 生成 H5 静态文件。

如系统 npm 异常，本工作区已准备独立 Node 22，PowerShell 中执行：

```powershell
$env:PATH='D:\code\exam-assistant\.tools\node-v22.16.0-win-x64;'+$env:PATH
cd D:\code\exam-assistant\frontend
npm run build:mp-weixin
```

## 后端本地运行

需要 JDK 17+、Maven 3.6.3+。本地 profile 使用 H2 文件数据库，便于无 MySQL 体验；生产 profile 使用 MySQL。数据库结构由 `backend/src/main/resources/schema.sql` 初始化，考试模板与商品在第一次启动时写入数据库。

```powershell
cd backend
mvn verify
mvn spring-boot:run
```

本工作区未改系统环境变量，独立 JDK/Maven 位于 `.tools`。可在仓库根执行：

```powershell
$env:JAVA_HOME='D:\code\exam-assistant\.tools\java\jdk-17.0.20.1+1'
$env:PATH=$env:JAVA_HOME+'\bin;'+$env:PATH
& .\.tools\apache-maven-3.9.9\bin\mvn.cmd -f backend/pom.xml spring-boot:run
```

如果 Windows JDK 报 `Unable to establish loopback connection`，使用上面的 `scripts/dev.ps1 backend`，它通过 `jdk.net.unixdomain.tmpdir` 指定短临时目录。普通启动命令在其他环境仍然适用。

服务地址 `http://localhost:8080/api`；本地数据库在运行目录的 `data/`，已排除 Git。local profile 开放隔离的体验账号登录；生产默认关闭体验登录。真实微信登录需在后端环境变量设置 `WECHAT_APPSECRET`，密钥不应写入源码或聊天。

## 联调后端

复制 `frontend/.env.example` 为 `frontend/.env.local`：

```dotenv
VITE_API_MODE=server
VITE_API_BASE_URL=http://localhost:8080/api
```

重启前端并重新编译。开发者工具连接本机后端时，仅在本地调试设置中允许未配置的 localhost 地址；上线时必须使用 HTTPS 和已配置的 request 合法域名，不能沿用 localhost。

本地模式与服务端模式的数据相互独立，不会自动迁移。服务端 token 有效期7天，过期后需要重新登录；体验登录产生新账号，正式微信登录按 openid 找回同一账号。

## Docker / MySQL

微信云托管通过 GitHub 部署时，构建目录使用仓库根目录 `/`，Dockerfile 使用根目录 `Dockerfile`，端口 `8080`。根目录镜像默认启用 `prod`，部署环境变量必须设置 `DB_URL`、`DB_USER`、`DB_PASSWORD`。微信登录另外配置 `WECHAT_APPSECRET`，且保持 `DEMO_LOGIN=false`。密钥不放入仓库或 Docker 构建参数。

原来的 `backend/Dockerfile` 仍供 Compose 使用，其构建目录是 `backend/`；不要把该 Dockerfile 与仓库根目录构建上下文混用。

```powershell
Copy-Item .env.example .env
# 修改 .env 的数据库密码与微信配置
docker compose up -d --build
```

后端绑定本机 `127.0.0.1:8080`；生产对外访问需配置 HTTPS 反向代理。MySQL 不暴露宿主端口，使用命名卷保存数据。Redis 为可选基础设施：`docker compose --profile cache up -d`。当前会话与模板使用数据库，暂未依赖 Redis 缓存。

## 项目结构

```text
frontend/src/pages/     首页、创建、计划、任务、记录、我的、方案
frontend/src/api/       本地/服务端适配
frontend/src/utils/     本地规则引擎
backend/src/main/java/  API、登录、存储、规则引擎
backend/src/main/resources/schema.sql  数据库结构
docs/API.md            接口与认证说明
docs/DEVELOPMENT.md    已验证范围与上线待办
assets/brand/          品牌图标
```

微信认证、备案、真机分享、生产域名、真实微信登录与虚拟支付需要平台配置及验证。当前是可运行的开发预览，尚不能作为已接入支付的正式上线版本。
