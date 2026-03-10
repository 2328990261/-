# 轻小说推荐系统

基于 **Vue 3 + Spring Boot** 的轻小说阅读与推荐系统，支持书籍浏览、阅读、个人书架、推荐算法及后台管理（轮播图、标签、书籍上架等）。

---

## 一、技术栈与版本要求

| 类别     | 技术/工具   | 版本要求 | 说明 |
|----------|-------------|----------|------|
| 前端     | Node.js     | **^20.19.0** 或 **>=22.12.0** | 见 `vue/package.json` 的 `engines` |
| 前端     | Vue         | ^3.5.x   | Vue 3 组合式 API |
| 前端     | Vite        | ^7.3.x   | 构建与开发服务器 |
| 前端     | Element Plus| ^2.13.x  | UI 组件库 |
| 后端     | Java        | **21**   | JDK 21（与 Spring Boot 3 匹配） |
| 后端     | Spring Boot | 3.2.5    | 见 `springboot/pom.xml` |
| 后端     | Maven       | 3.6+     | 构建与依赖管理 |
| 数据库   | MySQL       | 8.0+     | 推荐 8.0 或以上，字符集 utf8mb4 |

请先在本机安装并确认版本：

- **Node**：`node -v` 应在 20.19+ 或 22.12+
- **Java**：`java -version` 应为 21
- **Maven**：`mvn -v` 建议 3.6+
- **MySQL**：服务运行在 3306，并已创建好数据库（见下文）

---

## 二、项目结构

```
项目根目录/
├── vue/                    # 前端（Vue 3 + Vite）
│   ├── src/
│   │   ├── api/            # 接口封装
│   │   ├── views/          # 页面
│   │   ├── components/     # 公共组件
│   │   └── utils/          # 工具（如 request.js）
│   ├── .env.development    # 开发环境变量（API 地址等）
│   ├── vite.config.js      # Vite 配置（端口、代理）
│   └── package.json
├── springboot/             # 后端（Spring Boot）
│   ├── src/main/
│   │   ├── java/           # 业务代码
│   │   └── resources/
│   │       ├── application.yml   # 主配置文件
│   │       └── mapper/           # MyBatis XML
│   └── pom.xml
├── novel_db.sql            # 数据库结构与初始数据（必执行）
├── admin_module_db.sql     # 后台相关表（若单独提供）
├── 行为功能相关表结构.sql   # 用户行为等表
├── 其他 *.sql              # 按需执行的补充脚本
└── README.md               # 本说明
```

---

## 三、数据库配置与初始化

### 3.1 创建数据库

在 MySQL 中执行：

```sql
CREATE DATABASE IF NOT EXISTS novel_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

### 3.2 执行 SQL 脚本（按顺序）

1. **主库结构与数据**（必做）  
   在 `novel_db` 库下执行项目根目录的：
   - `novel_db.sql`  
   包含：小说主表、分卷、用户、轮播图、标签、评论、阅读历史等表及初始数据。

2. **按需执行**（若项目中有且你的库尚未包含）：  
   - `admin_module_db.sql` — 后台管理相关  
   - `行为功能相关表结构.sql` — 用户行为相关  
   - `user_preference_tag.sql` / `update_user_preference_tag.sql` — 用户偏好标签  
   - `banner_carousel.sql` / `fix_banner_carousel.sql` — 轮播图  

执行方式示例（命令行）：

```bash
mysql -u root -p novel_db < novel_db.sql
```

或在 Navicat / DBeaver 等工具中打开并执行对应 SQL 文件。

### 3.3 数据库账号

确保本机 MySQL 存在一个可访问 `novel_db` 的账号。默认配置使用：

- 主机：`localhost:3306`
- 库名：`novel_db`
- 用户名：`root`
- 密码：`123456`

若你的账号/密码不同，需修改后端配置（见下一节）。

---

## 四、后端配置与启动

### 4.1 配置文件位置

- **主配置**：`springboot/src/main/resources/application.yml`

### 4.2 主要配置项说明

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/novel_db
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB

server:
  port: 8081

upload:
  covers-dir: ${user.dir}/uploads/covers

mybatis:
  mapper-locations: classpath:mapper/*.xml, classpath:com/wangrui/springboot/mapper/*.xml
  type-aliases-package: com.wangrui.springboot.pojo
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

| 配置项 | 说明 | 可修改为 |
|--------|------|----------|
| `spring.datasource.url` | 数据库连接 | 你的 MySQL 地址/库名 |
| `spring.datasource.username` / `password` | 数据库账号密码 | 你的账号密码 |
| `server.port` | 后端服务端口 | 保持 **8081**（前端代理指向此端口） |
| `upload.covers-dir` | 封面上传目录 | 默认为项目运行目录下的 `uploads/covers` |
| `mybatis.log-impl` | SQL 日志 | 生产可改为 `org.apache.ibatis.logging.slf4j.Slf4jImpl` 或关闭 |

**注意**：本地敏感配置可放在 `application-local.yml`（已被 .gitignore 忽略），避免提交到仓库。

### 4.3 启动后端

在项目根目录下执行：

```bash
cd springboot
mvn spring-boot:run
```

或先打包再运行：

```bash
mvn clean package -DskipTests
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

看到类似 “Started SpringbootApplication” 且无报错，表示后端已在 **http://localhost:8081** 运行。  
可选：访问 http://localhost:8081/swagger-ui.html 查看接口文档（若已集成 SpringDoc）。

---

## 五、前端配置与启动

### 5.1 环境变量

- **开发环境**：`vue/.env.development`  
  若仓库中无此文件（已被 .gitignore 忽略），可复制 `vue/.env.example` 为 `vue/.env.development` 再按需修改。

内容示例：

```env
VITE_API_BASE_URL=http://localhost:8081
```

用于开发时某些直接写死 baseURL 的请求（如部分管理端接口）。若全部走 Vite 代理，则前端请求写 `/api` 即可，见下文。

### 5.2 代理与端口（vite.config.js）

前端开发服务器端口：**5173**。  
接口代理规则（在 `vue/vite.config.js` 中）：

- 请求 `/api` → 转发到 `http://localhost:8081`
- 部分路径会去掉 `/api` 前缀再转发到后端（如 `/api/novel` → 后端 `/novel`）
- `/api/user/behavior`、`/api/recommend`、`/api/admin` 保留 `/api` 前缀转发
- `/cover` 直接转发到 8081（封面等静态资源）

因此后端必须运行在 **8081**，否则需同时改 `vite.config.js` 的 `proxy.target` 和 `.env.development` 中的 `VITE_API_BASE_URL`。

### 5.3 安装依赖并启动

在项目根目录下执行：

```bash
cd vue
npm install
npm run dev
```

浏览器访问：**http://localhost:5173**。

### 5.4 其他脚本

- 生产构建：`npm run build`（产物在 `vue/dist`）
- 预览构建结果：`npm run preview`
- 代码检查：`npm run lint`

---

## 六、启动顺序与访问方式

1. **启动 MySQL**，并确认已执行 `novel_db.sql` 等脚本。  
2. **启动后端**：在 `springboot` 目录执行 `mvn spring-boot:run`，确保 8081 正常。  
3. **启动前端**：在 `vue` 目录执行 `npm install` 与 `npm run dev`。  
4. **访问**：浏览器打开 **http://localhost:5173**。

若只部署前端静态资源，可将 `vue/dist` 放到 Nginx 等，并配置反向代理将 `/api`、`/cover` 转到 8081 后端。

---

## 七、配置汇总（给他人复现用）

| 项目     | 配置/位置 | 默认值/说明 |
|----------|-----------|-------------|
| Node     | 版本      | ^20.19.0 或 >=22.12.0 |
| Java     | 版本      | 21 |
| MySQL    | 库名      | novel_db |
| MySQL    | 账号/密码 | root / 123456（可改 application.yml） |
| 后端端口 | application.yml `server.port` | 8081 |
| 前端端口 | vite.config.js `server.port` | 5173 |
| API 代理 | vite.config.js `server.proxy['/api'].target` | http://localhost:8081 |
| 封面上传目录 | application.yml `upload.covers-dir` | ${user.dir}/uploads/covers |

---

## 八、常见问题

- **前端请求 404 或跨域**：确认后端已启动在 8081，且前端用 `npm run dev` 启动（走 Vite 代理）。  
- **数据库连接失败**：检查 MySQL 已启动、库名/账号/密码与 `application.yml` 一致、已执行 `novel_db.sql`。  
- **Node 版本不符**：用 nvm 或官网安装符合 `engines` 的 Node 版本。  
- **封面上传失败**：看 `upload.covers-dir` 路径是否有写权限，或改为绝对路径。

---

## 九、文档与补充说明

- 书籍上传（EPUB）：见项目根目录 **EPUB上传功能说明.md**。  
- 其他业务或表结构说明，可参考项目内对应 SQL 与 Markdown 文件。

如有问题，请根据上述版本与配置逐项核对后再排查。
