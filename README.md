# 二次压降检测系统

基于 Spring Boot 的二次压降检测数据管理系统，支持检测记录的增删改查、OCR 图片识别自动录入、PDF 检测报告生成和统计概览。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.4.12 |
| 安全框架 | Spring Security + JWT (jjwt 0.12.6) |
| ORM | MyBatis 3.0.5 |
| 数据库 | MySQL 8.0 |
| 前端 | Vue.js 3 + Bootstrap 5 + Axios + ECharts (CDN) |
| PDF 生成 | iTextPDF 5.5.13.3 |
| OCR 识别 | 百度 OCR API |
| API 文档 | Swagger / OpenAPI (springdoc 2.8.4) |
| Java 版本 | JDK 21 |

## 功能模块

- **用户认证** — JWT 无状态 Token 登录/注册，BCrypt 密码加密
- **数据管理** — 检测记录增删改查，支持按产品编号/制造商模糊搜索
- **数据录入** — 三步向导式录入（基础信息 → OCR 识别 → 确认提交），支持多检测项目
- **OCR 识别** — 上传检测设备截图，自动提取计量点编号、环境参数和三相误差数据
- **PDF 报告** — 一键生成规范的中文检测报告，支持在线预览和下载打印
- **统计概览** — 本月检测数/合格率、历史总量、近6月趋势图、合格率饼图

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+

### 数据库初始化

1. 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS testdata DEFAULT CHARACTER SET utf8mb4;
```

2. 执行初始化脚本（建表 + 测试数据）：

```bash
mysql -u root -p testdata < sql/init_database.sql
```

### 修改配置

编辑 `src/main/resources/application.properties`，修改数据库连接信息：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/testdata
spring.datasource.username=root
spring.datasource.password=你的密码
```

### 启动应用

```bash
mvn spring-boot:run
```

启动后访问：
- 应用页面：http://localhost:8080
- Swagger API 文档：http://localhost:8080/swagger-ui.html

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| test | admin123 | 普通用户 |

## 项目结构

```
src/main/java/com/hspyx/
├── config/          # Spring 配置（Security、Swagger、WebMvc）
├── controller/      # REST 控制器（Auth、TestRecord、OCR）
├── dto/             # 数据传输对象（LoginRequest、AuthResponse 等）
├── entity/          # 数据库实体类
├── filter/          # JWT 认证过滤器
├── mapper/          # MyBatis Mapper 接口
├── pojo/            # 业务模型（RecordDTO、TestRecord 等）
├── service/         # 服务接口与实现
└── utils/           # 工具类（JwtUtils）

src/main/resources/
├── static/          # 前端静态页面（login.html、index.html）
└── application.properties

sql/
└── init_database.sql   # 数据库初始化脚本
```

## API 接口

### 认证接口（无需 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/register` | 用户注册 |

### 业务接口（需要 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/records` | 查询记录列表（支持 keyword 模糊搜索）|
| GET | `/api/records/{id}` | 获取记录详情 |
| POST | `/api/records` | 新增检测记录 |
| DELETE | `/api/records/{id}` | 删除检测记录 |
| GET | `/api/records/{id}/pdf` | 下载 PDF 检测报告 |
| GET | `/api/stats` | 获取统计概览数据 |
| POST | `/api/ocr/recognize` | OCR 图片识别（文件上传）|
| POST | `/api/ocr/auto-recognize` | 自动识别（Base64 或远程 URL）|

### 认证方式

在请求头中携带 Token：

```
Authorization: Bearer <token>
```

PDF 下载等特殊场景支持 URL 参数传递：`?token=<token>`

## 数据库表结构

| 表名 | 说明 |
|------|------|
| `users` | 用户表（账号、密码、角色、状态）|
| `test_records` | 检测记录主表（产品信息、环境参数、结论）|
| `test_input_details` | 检测项目详情表（档位、上下限、实测值）|
| `test_phase_results` | 三相误差结果表（ao/bo/co 各相 f%、d、dU%、Upt:U、Uyb:U）|

表关系：`test_records` 1:N `test_input_details` 1:N `test_phase_results`（级联删除）。
