# Q&A 问题记录

> 本文档记录项目开发过程中的问题及解答

---

## 问题1：数据录入时设备和检测项目的绑定机制

### 问题描述

在数据录入时，是先输入被检测设备的信息和检测项目，再对检测项目进行数据录入。请问：
1. 被检测设备的信息和检测项目是如何进行绑定的？
2. 这个绑定在代码里又是如何实现的？

### 回答

设备和检测项目的绑定通过 **数据库外键关联** 和 **统一的数据传输对象** 实现。

---

### 1. 数据结构绑定

#### 前端数据传输

```javascript
// index.html - 数据录入表单结构
formData = {
    deviceInfo: {           // 设备基本信息
        productNo: "ELEC-2024-0001",
        productName: "电能表0.5S级",
        manufacturer: "华东电力",
        testDate: "2024-01-05"
    },
    projects: [              // 检测项目数组
        {
            projectName: "PT1",
            rating: "1",
            phaseResults: { ao: {...}, bo: {...}, co: {...} }
        },
        {
            projectName: "PT2",
            rating: "1",
            phaseResults: { ao: {...}, bo: {...}, co: {...} }
        }
    ]
}
```

**关键点**：设备信息和检测项目在同一个 `formData` 对象中，通过 JavaScript 对象引用自然绑定。

---

### 2. 数据库绑定

#### 表结构关系

```
┌─────────────────────────────────┐
│       test_records (主表)        │
│  id: 1                          │
│  product_no: "ELEC-2024-0001"   │
└───────────────┬─────────────────┘
                │
                │  record_id (外键)
                │
    ┌──────────┴──────────┐
    │                         │
    ▼                         ▼
┌───────────────────┐  ┌───────────────────┐
│test_input_details │  │test_phase_results│
│  record_id: 1    │  │  record_id: 1   │
│  item_name: "PT1" │  │  item_name: "PT1"│
│  rating: "1"      │  │  phase: "ao"     │
└───────────────────┘  └───────────────────┘
```

#### 绑定字段说明

| 字段 | 说明 | 绑定方式 |
|------|------|----------|
| `record_id` | 关联主表ID | 外键约束 |
| `item_name` | 项目名称 | 字符串匹配 |

---

### 3. 代码实现

#### 后端保存逻辑 (TestRecordServiceImpl.java)

```java
public Long save(RecordDTO dto) {
    // 1. 保存主表，获得自增ID
    TestRecord record = new TestRecord();
    record.setProductNo(dto.getDeviceInfo().getProductNo());
    // ... 设置其他设备信息
    testRecordMapper.addRecord(record);
    Long recordId = record.getId();  // 获取生成的ID

    // 2. 保存检测项目（关联 record_id）
    for (ProjectItem item : dto.getProjects()) {
        TestInputDetail detail = new TestInputDetail();
        detail.setRecordId(recordId);           // 关键：绑定到主表
        detail.setItemName(item.projectName);   // 项目名称
        detail.setRating(item.rating);
        // ...
        testRecordMapper.addInputDetail(detail);

        // 3. 保存三相数据（关联 record_id + item_name）
        for (Map.Entry<String, PhaseData> entry : item.phaseResults.entrySet()) {
            TestPhaseResult phase = new TestPhaseResult();
            phase.setRecordId(recordId);         // 关联主表
            phase.setItemName(item.projectName); // 关联具体项目
            phase.setPhase(entry.getKey());     // ao/bo/co
            // ...
            testRecordMapper.addPhaseResult(phase);
        }
    }
}
```

---

### 4. 查询时的绑定恢复

#### 查询详情 (getDetail方法)

```java
public RecordDTO getDetail(Long id) {
    // 1. 查询主表
    TestRecord record = testRecordMapper.getRecordById(id);

    // 2. 查询所有检测项目（通过 record_id 关联）
    List<TestInputDetail> inputList = testRecordMapper.getInputDetailsByRecordId(id);

    // 3. 查询所有三相数据（通过 record_id + item_name 关联）
    List<TestPhaseResult> phaseList = testRecordMapper.getPhaseResultsByRecordId(id);

    // 4. 组装 DTO（恢复绑定关系）
    dto.setDeviceInfo(deviceInfo);
    dto.setProjects(projectsList);  // projects 数组与 inputList 对应
}
```

---

### 5. 绑定流程图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         数据录入流程                                  │
└─────────────────────────────────────────────────────────────────────┘

【步骤1：用户输入】
┌─────────────────┐    ┌─────────────────┐
│ 设备基本信息     │    │ 选择检测项目     │
│ - 产品编号      │    │ □ PT1          │
│ - 产品名称      │    │ □ PT2          │
│ - 制造商       │    │ □ CT1          │
└────────┬────────┘    └────────┬────────┘
         │                      │
         └──────────┬───────────┘
                    ▼
         ┌─────────────────┐
         │  RecordDTO      │
         │  deviceInfo     │
         │  projects[]    │  ← 在同一个对象中自然绑定
         └────────┬────────┘
                  │
                  ▼
【步骤2：保存到数据库】
         ┌────────┴────────┐
         ▼                 ▼
┌─────────────────┐  ┌─────────────────┐
│  test_records   │  │ record_id: 1    │ ← 主表获得自增ID
│  id: 1          │  │ (外键)          │
└────────┬────────┘  └─────────────────┘
         │
         │ record_id = 1
         ▼
┌─────────────────────────────────────────────────┐
│  test_input_details                             │
│  record_id | item_name | rating | ...          │
│  ──────────┼───────────┼─────────┼──           │
│  1         | PT1       | 1        |           │ ← 绑定到主表
│  1         | PT2       | 1        |           │
└────────┬────────────────────────────────────────┘
         │
         │ record_id + item_name
         ▼
┌─────────────────────────────────────────────────┐
│  test_phase_results                              │
│  record_id | item_name | phase | f_percent |... │
│  ──────────┼───────────┼───────┼────────────── │
│  1         | PT1       | ao     | 0.012        │ ← 绑定到项目和主表
│  1         | PT1       | bo     | 0.015        │
│  1         | PT1       | co     | 0.018        │
│  1         | PT2       | ao     | 0.025        │
│  1         | PT2       | bo     | 0.028        │
│  1         | PT2       | co     | 0.031        │
└─────────────────────────────────────────────────┘
```

---

### 总结

| 绑定层级 | 实现方式 | 代码位置 |
|---------|---------|---------|
| 设备 ↔ 项目 | 同一 DTO 对象 | index.html formData |
| 主表 ↔ 子表 | `record_id` 外键 | TestRecordServiceImpl.save() |
| 项目 ↔ 三相数据 | `record_id` + `item_name` | TestRecordServiceImpl.save() |
| 查询恢复 | 关联查询 | TestRecordMapper + getDetail() |

---

## 问题2：如何演示登录并获取Token

### 问题描述

我如何演示：登录 → 获取 Token

### 回答

有多种方式可以演示登录并获取Token，以下是几种常用的方法：

---

### 方法1：通过 Swagger UI（推荐）

1. 确保应用已启动（`http://localhost:8080`）
2. 打开浏览器访问：**http://localhost:8080/swagger-ui.html**
3. 展开 `认证管理` 部分
4. 点击 `POST /api/auth/login`
5. 点击 **Try it out** 按钮
6. 在请求体中输入：
```json
{
  "username": "admin",
  "password": "password"
}
```
7. 点击 **Execute** 执行
8. 在 Response body 中查看返回的 Token：
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "nickname": "系统管理员",
  "role": "ADMIN",
  "expiresIn": 86400000,
  "code": 200,
  "message": "登录成功"
}
```

---

### 方法2：通过 curl 命令

在终端执行以下命令：

```bash
# 登录获取Token
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

**响应示例：**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTcwOTQ4MjAwMCwiZXhwIjoxNzA5NTY4NDAwfQ.xxx",
  "username": "admin",
  "nickname": "系统管理员",
  "role": "ADMIN",
  "expiresIn": 86400000,
  "code": 200,
  "message": "登录成功"
}
```

---

### 方法3：通过前端登录页面

1. 访问 **http://localhost:8080/**
2. 自动跳转到登录页面
3. 输入用户名和密码
4. 点击登录
5. 登录成功后自动跳转到主页
6. 打开浏览器开发者工具（F12）
7. 在 **Application** → **Local Storage** 中查看存储的 Token：
   - `token`: JWT令牌
   - `username`: 用户名
   - `nickname`: 昵称
   - `role`: 角色

---

### 方法4：通过 Postman

1. 新建请求，选择 **POST** 方法
2. URL: `http://localhost:8080/api/auth/login`
3. 在 **Headers** 中添加：
   - Key: `Content-Type`
   - Value: `application/json`
4. 在 **Body** 中选择 **raw**，输入：
```json
{
  "username": "admin",
  "password": "password"
}
```
5. 点击 **Send** 发送请求
6. 在响应中查看 Token

---

### Token 结构解析

返回的 Token 是一个 JWT 字符串，由三部分组成：

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsIm
                  │                    │
        Header (Base64)      Payload/Claims (Base64)
```

**Payload 解码后内容：**
```json
{
  "sub": "admin",           // 用户名
  "role": "ADMIN",           // 角色
  "iat": 1709482000,        // 签发时间（Unix时间戳）
  "exp": 1709568400         // 过期时间（Unix时间戳）
}
```

---

### 演示：使用Token访问受保护接口

获取 Token 后，可以携带 Token 访问需要认证的接口：

```bash
# 使用Token查询检测记录列表
curl -X GET "http://localhost:8080/api/records" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

如果 Token 无效或过期，会返回 401 错误：
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

---

### Token 有效期

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `expiresIn` | 86400000 | 24小时（毫秒） |
| `exp` | 当前时间 + 24小时 | Token 过期时间 |

---

### 常见问题

| 问题 | 解决方案 |
|------|----------|
| 登录失败"用户不存在" | 检查用户名是否正确 |
| 登录失败"密码错误" | 密码应为 `password` |
| Token 无效 | Token 过期或被篡改 |
| 401  Unauthorized | 检查请求头是否正确携带 Token |

---

## 问题3：如何演示带Token访问接口

### 问题描述

如何演示：带 Token 访问接口

### 回答

带 Token 访问接口需要先登录获取 Token，然后在请求头中携带 Token 进行访问。

---

### 完整演示流程

#### 第一步：登录获取 Token

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

**响应：**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTcwOTQ4MjAwMCwiZXhwIjoxNzA5NTY4NDAwfQ.xxx",
  "username": "admin",
  "code": 200,
  "message": "登录成功"
}
```

复制返回的 Token 值（注意：实际 Token 很长，需要完整复制）。

---

#### 第二步：携带 Token 访问接口

##### 方式1：查询检测记录列表

```bash
curl -X GET "http://localhost:8080/api/records" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTcwOTQ4MjAwMCwiZXhwIjoxNzA5NTY4NDAwfQ.xxx"
```

**响应：**
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "productNo": "ELEC-2024-0001",
      "productName": "电能表0.5S级",
      ...
    }
  ],
  "message": "查询成功"
}
```

##### 方式2：查询单条记录详情

```bash
curl -X GET "http://localhost:8080/api/records/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTcwOTQ4MjAwMCwiZXhwIjoxNzA5NTY4NDAwfQ.xxx"
```

##### 方式3：获取统计数据

```bash
curl -X GET "http://localhost:8080/api/stats" \
  -H "Authorization: Bearer <你的Token>"
```

---

### 请求头格式说明

```
Authorization: Bearer <Token>
     │              │
     │              └── 替换为实际的 Token 值
     └── 固定格式：Bearer + 空格 + Token
```

**正确格式示例：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTcwOTQ4MjAwMCwiZXhwIjoxNzA5NTY4NDAwfQ.xxx
```

---

### Swagger UI 中演示

1. 先登录获取 Token（在问题2中已说明）
2. 点击页面右上角的 **Authorize** 按钮
3. 在弹窗中粘贴 Token 值
4. 点击 **Authorize** 确认
5. 之后所有接口请求都会自动携带 Token

---

### Postman 中演示

1. **新建请求**，选择 GET 方法，URL: `http://localhost:8080/api/records`
2. **添加请求头**：
   - Key: `Authorization`
   - Value: `Bearer eyJhbGciOiJIUzI1NiJ9...`（替换为实际 Token）
3. 点击 **Send** 发送请求

---

### 浏览器中演示（前端）

打开浏览器访问主页后：

1. 按 **F12** 打开开发者工具
2. 切换到 **Network** 标签
3. 点击任意功能（如查询记录）
4. 查看请求，在 **Request Headers** 中可以看到：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

### 演示：不带 Token 或 Token 无效

#### 不带 Token

```bash
curl -X GET "http://localhost:8080/api/records"
```

**响应：** 仍然可以访问（因为当前配置为 permitAll）

#### Token 无效或过期

```bash
curl -X GET "http://localhost:8080/api/records" \
  -H "Authorization: Bearer 无效的Token"
```

**响应：** 会返回 401 错误
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

---

### Token 验证流程图

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Token 验证完整流程                                │
└─────────────────────────────────────────────────────────────────────┘

客户端请求
     │
     │ GET /api/records
     │ Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     ▼
┌─────────────────────────────────────┐
│    JwtAuthenticationFilter            │
│                                      │
│  1. 提取 Token                       │
│     authHeader = "Bearer xxx"        │
│     token = "xxx"                    │
│                                      │
│  2. 验证 Token                       │
│     jwtUtils.validateToken(token)     │
│     ├── 签名验证                      │
│     ├── 过期检查                      │
│     └── 提取用户信息                  │
│                                      │
└──────────────┬────────────────────────┘
              │
       ┌─────┴─────┐
       ▼           ▼
   验证通过      验证失败
       │           │
       ▼           ▼
┌──────────┐  ┌──────────────┐
│ Controller│  │  401 错误  │
│ 处理请求  │  │ Unauthorized │
└──────────┘  └──────────────┘
```

---

### 注意事项

| 注意事项 | 说明 |
|---------|------|
| Token 要完整 | Token 字符串很长，必须完整复制 |
| Bearer 格式 | 必须包含 `Bearer ` 前缀和空格 |
| Token 有时效 | 默认24小时过期，过期需重新登录 |
| 敏感信息 | 不要在公开场合暴露 Token |

---

## 问题4：使用Apifox进行测试演示

### 问题描述

我使用 Apifox 这个软件进行测试演示

### 回答

Apifox 是一款优秀的 API 测试工具，以下是在 Apifox 中测试本系统接口的详细步骤。

---

### Apifox 配置

#### 1. 新建项目

1. 打开 Apifox，点击 **新建项目**
2. 项目名称：`二次压降检测系统`
3. 保存

#### 2. 添加环境变量（推荐）

1. 进入 **项目设置** → **环境变量**
2. 添加环境：
   - 环境名称：`本地开发`
   - 变量名：`baseUrl`
   - 变量值：`http://localhost:8080`

---

### 接口配置

#### 接口1：用户登录

| 配置项 | 值 |
|--------|-----|
| 请求方法 | `POST` |
| 请求路径 | `/api/auth/login` |
| 请求 URL | `{{baseUrl}}/api/auth/login` |

**Headers 配置：**
| Key | Value |
|-----|-------|
| Content-Type | application/json |

**Body 配置（raw/JSON）：**
```json
{
  "username": "admin",
  "password": "password"
}
```

**操作步骤：**
1. 点击 **发送** 按钮
2. 查看响应，复制 `token` 字段的值

**响应示例：**
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "admin",
    "nickname": "系统管理员",
    "role": "ADMIN",
    "expiresIn": 86400000
  },
  "message": "登录成功"
}
```

---

#### 接口2：查询检测记录列表

| 配置项 | 值 |
|--------|-----|
| 请求方法 | `GET` |
| 请求路径 | `/api/records` |
| 请求 URL | `{{baseUrl}}/api/records` |

**Headers 配置：**
| Key | Value |
|-----|-------|
| Authorization | Bearer {{token}} |

**注意：** 先在环境变量中添加 `token` 变量，值为登录接口返回的 Token 值。

---

#### 接口3：查询单条记录详情

| 配置项 | 值 |
|--------|-----|
| 请求方法 | `GET` |
| 请求路径 | `/api/records/{id}` |
| 请求 URL | `{{baseUrl}}/api/records/1` |

**Headers 配置：**
| Key | Value |
|-----|-------|
| Authorization | Bearer {{token}} |

---

#### 接口4：获取统计数据

| 配置项 | 值 |
|--------|-----|
| 请求方法 | `GET` |
| 请求路径 | `/api/stats` |
| 请求 URL | `{{baseUrl}}/api/stats` |

**Headers 配置：**
| Key | Value |
|-----|-------|
| Authorization | Bearer {{token}} |

---

### Apifox 环境变量配置示例

```
┌─────────────────────────────────────────────────────────────┐
│ 环境：本地开发                                            │
├─────────────────────────────────────────────────────────────┤
│ 变量名          │ 当前值                                 │
├─────────────────┼────────────────────────────────────────┤
│ baseUrl         │ http://localhost:8080                  │
│ token           │ eyJhbGciOiJIUzI1NiJ9...               │
└─────────────────┴────────────────────────────────────────┘
```

---

### 完整测试流程（Apifox）

#### 步骤1：登录获取 Token

1. 打开登录接口
2. 请求体：
```json
{
  "username": "admin",
  "password": "password"
}
```
3. 点击 **发送**
4. 响应成功，复制返回的 `token` 值

#### 步骤2：设置 Token 环境变量

1. 进入 **环境变量**
2. 找到 `token` 变量
3. 粘贴刚才复制的 Token 值
4. 保存

#### 步骤3：测试需要认证的接口

1. 打开任意需要认证的接口
2. Headers 添加：
```
Authorization: Bearer {{token}}
```
3. 点击 **发送**
4. 验证响应

---

### 批量测试

#### 测试场景：登录 → 查询 → 详情

1. 新建 **测试场景**
2. 添加步骤：
   - 步骤1：登录接口
   - 步骤2：提取 Token（使用后置操作提取）
   - 步骤3：查询列表（自动使用提取的 Token）
   - 步骤4：查询详情

#### 后置操作提取 Token

```javascript
// 后置脚本
var response = pm.response.json();
if (response.data && response.data.token) {
    pm.environment.set("token", response.data.token);
}
```

---

### Apifox 测试效果

```
┌─────────────────────────────────────────────────────────────┐
│ Apifox - 二次压降检测系统                                 │
├─────────────────────────────────────────────────────────────┤
│ [POST] /api/auth/login                        ✓ 200 OK    │
│ [GET]  /api/records                           ✓ 200 OK    │
│ [GET]  /api/records/1                        ✓ 200 OK    │
│ [GET]  /api/stats                             ✓ 200 OK    │
└─────────────────────────────────────────────────────────────┘
```

---

### 常见问题

| 问题 | 解决方案 |
|------|----------|
| 401 Unauthorized | 检查 Authorization 头是否正确设置 |
| Token 为空 | 确认登录接口返回了 token 字段 |
| 环境变量不生效 | 检查是否选择了正确的环境 |

---

### 优点

使用 Apifox 的好处：
- ✅ 可视化界面，操作简单
- ✅ 支持环境变量，方便切换
- ✅ 自动保存历史记录
- ✅ 支持测试场景批量执行
- ✅ 团队协作功能

---

## 问题5：部分接口不需要Token也能访问

### 问题描述

经过 API 测试发现，部分接口（如 GET /api/records）不需要携带 Token 也可以访问成功。

### 问题原因

这是因为在开发调试阶段，为了方便测试，我将 SecurityConfig 配置为：
```java
.authorizeHttpRequests(auth -> auth
    .anyRequest().permitAll()  // 所有请求都放行
)
```

这种配置意味着**所有请求都不需要认证**，所以即使不携带 Token 也能正常访问。

### 解决方案

我已经修改了 SecurityConfig.java，现在需要认证的接口必须携带 Token：

```java
.authorizeHttpRequests(auth -> auth
    // 放行的路径（不需要Token）
    .requestMatchers("/api/auth/**").permitAll()                    // 认证接口
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swagger文档
    .requestMatchers("/login.html", "/").permitAll()                   // 登录页
    .requestMatchers("/static/**", "/css/**", "/js/**").permitAll()   // 静态资源

    // 其他所有请求都需要认证
    .anyRequest().authenticated()
)
```

### 现在的接口访问规则

| 接口 | 是否需要 Token | 说明 |
|------|---------------|------|
| POST /api/auth/login | ❌ 不需要 | 登录接口 |
| POST /api/auth/register | ❌ 不需要 | 注册接口 |
| GET /api/records | ✅ 需要 | 检测记录列表 |
| GET /api/records/{id} | ✅ 需要 | 检测记录详情 |
| POST /api/records | ✅ 需要 | 新增检测记录 |
| DELETE /api/records/{id} | ✅ 需要 | 删除检测记录 |
| GET /api/records/{id}/pdf | ✅ 需要 | 导出PDF |
| GET /api/stats | ✅ 需要 | 统计数据 |
| GET /swagger-ui.html | ❌ 不需要 | API文档 |

### 重新测试验证

修改配置后，重启应用，再次测试：

#### 不带 Token 访问（应该失败）

```bash
curl -X GET "http://localhost:8080/api/records"
```

**预期响应：** 401 Unauthorized

#### 携带 Token 访问（应该成功）

```bash
curl -X GET "http://localhost:8080/api/records" \
  -H "Authorization: Bearer <Token>"
```

**预期响应：** 200 OK + 数据

### 重启应用

配置修改后必须重启应用才能生效：
```bash
# 停止当前应用，然后重新启动
mvn spring-boot:run
```

---

## 问题6：登录成功后跳转显示403错误

### 问题描述

登录成功后，页面跳转显示：
```
拒绝访问 localhost
你没有查看此页面的用户权限。
HTTP ERROR 403
```

### 问题原因

这是因为在 SecurityConfig 中没有正确配置 `/index.html` 路径的放行规则，导致该路径被拦截，需要认证才能访问。

### 已完成的修复

我已经在 SecurityConfig.java 中添加了 `/index.html` 的放行配置：

```java
.authorizeHttpRequests(auth -> auth
    // 认证相关接口（不需要Token）
    .requestMatchers("/api/auth/**").permitAll()

    // Swagger文档（不需要Token）
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

    // 登录页和主页（不需要Token）
    .requestMatchers("/login.html", "/", "/index.html").permitAll()

    // 静态资源（不需要Token）
    .requestMatchers("/static/**", "/css/**", "/js/**", "/favicon.ico", "/images/**").permitAll()

    // 其他所有请求都需要认证
    .anyRequest().authenticated()
)
```

### 重启应用验证

修改配置后必须重启应用才能生效：
```bash
mvn spring-boot:run
```

### 现在的访问规则

| 路径 | 是否需要 Token | 说明 |
|------|---------------|------|
| `/login.html` | ❌ 不需要 | 登录页 |
| `/index.html` | ❌ 不需要 | 主页面 |
| `/` | ❌ 不需要 | 首页 |
| `/api/auth/login` | ❌ 不需要 | 登录接口 |
| `/api/records/*` | ✅ 需要 | 检测记录接口 |
| `/swagger-ui.html` | ❌ 不需要 | API文档 |

---

## 问题7：登录成功跳转后显示无法连接到服务器

### 问题描述

登录成功后跳转了，但是显示"无法连接到服务器，请检查后端服务是否已启动 (localhost:8080)"，而后端已经启动。

### 问题原因

前端使用 axios 发送 API 请求，但是 axios 请求**没有携带 Token**。

之前的代码中，只有手动封装的 `apiRequest` 函数会携带 Token，但是 `fetchStats()`、`fetchRecords()` 等函数直接使用 axios 调用，没有携带 Authorization 头，导致请求被 Spring Security 拦截。

### 已完成的修复

我在 index.html 中添加了 axios 的**请求拦截器**和**响应拦截器**：

```javascript
// Axios 请求拦截器：自动携带 Token
axios.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = 'Bearer ' + token;
        }
        return config;
    },
    error => Promise.reject(error)
);

// Axios 响应拦截器：处理 401/403 错误
axios.interceptors.response.use(
    response => response,
    error => {
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            // Token 无效或过期，清除本地存储并跳转到登录页
            localStorage.removeItem('token');
            window.location.href = '/login.html';
        }
        return Promise.reject(error);
    }
);
```

### 请求拦截器的作用

1. **自动携带 Token**：所有 axios 请求自动添加 `Authorization: Bearer <token>` 头
2. **无需修改现有代码**：所有使用 axios 的请求都会自动生效

### 响应拦截器的作用

1. **处理认证失败**：如果返回 401/403 错误，自动清除本地存储并跳转到登录页
2. **提升用户体验**：Token 过期时自动引导用户重新登录

### 现在的请求流程

```
用户登录成功
     ↓
Token 存储到 localStorage
     ↓
访问主页 → axios 请求
     ↓
请求拦截器自动添加 Token
     ↓
后端验证 Token → 通过
     ↓
返回数据 → 显示页面
```

---

## 问题8：Swagger 展示接口列表与鉴权配置

### 问题描述

如何打开 Swagger，展示接口列表与鉴权配置

### 回答

Swagger UI 是 API 文档可视化工具，可以直接查看和测试所有接口。

### 访问地址

重启应用后，打开浏览器访问：
```
http://localhost:8080/swagger-ui.html
```

### Swagger 界面说明

```
┌─────────────────────────────────────────────────────────────────────┐
│ 二次压降检测系统 API                                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│ ▼ 认证管理                                                        │
│   ├── POST /api/auth/login    登录                                 │
│   └── POST /api/auth/register 注册                                 │
│                                                                     │
│ ▼ 检测记录管理                                                     │
│   ├── GET  /api/records       查询列表                            │
│   ├── GET  /api/records/{id}  查询详情                            │
│   ├── POST /api/records       新增记录                            │
│   ├── DELETE /api/records/{id} 删除记录                            │
│   └── GET  /api/records/{id}/pdf 导出PDF                          │
│                                                                     │
│ ▼ 统计数据                                                        │
│   └── GET  /api/stats          获取统计                           │
│                                                                     │
│ Authorize 🔓  ← 点击这里输入 Token                                │
└─────────────────────────────────────────────────────────────────────┘
```

### 使用 Swagger 测试接口

#### 1. 测试登录接口（不需要 Token）

1. 展开 **认证管理**
2. 点击 `POST /api/auth/login`
3. 点击 **Try it out**
4. 输入：
```json
{
  "username": "admin",
  "password": "password"
}
```
5. 点击 **Execute**
6. 在 Response 中复制 `token` 值

#### 2. 配置全局鉴权（需要 Token）

1. 点击页面右上角的 **Authorize** 按钮 🔓
2. 在输入框中粘贴 Token（格式：`Bearer eyJhbGciOiJIUzI1NiJ9...`）
3. 点击 **Authorize**
4. 关闭弹窗
5. 之后所有接口请求都会自动携带 Token

#### 3. 测试需要认证的接口

1. 展开 **检测记录管理**
2. 点击 `GET /api/records`
3. 点击 **Try it out**
4. 点击 **Execute**
5. 查看 Response 结果

### Swagger 中的鉴权标识

| 标识 | 含义 |
|------|------|
| 🔓 Open | 全局未配置 Token |
| 🔐 Closed | 全局已配置 Token |
| 🔒 锁图标 | 该接口需要 Token |

### 接口鉴权分组

```
认证相关接口（无需 Token）：

┌─────────────────────────────────────────┐
│ 🔓 认证管理 /auth                         │
│   ├── POST /login     ❌ 不需要 Token    │
│   └── POST /register ❌ 不需要 Token    │
└─────────────────────────────────────────┘

业务相关接口（需要 Token）：

┌─────────────────────────────────────────┐
│ 🔒 检测记录管理 /records                   │
│   ├── GET  /records      ✅ 需要 Token    │
│   ├── POST /records      ✅ 需要 Token    │
│   └── DELETE /records/{id} ✅ 需要 Token  │
└─────────────────────────────────────────┘
```

### 配置说明

Swagger 的鉴权配置在 `SwaggerConfig.java` 中定义：

```java
// 安全方案
.components(new Components()
    .addSecuritySchemes("bearerAuth",
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
    )
)

// 全局应用
.addSecurityItem(new SecurityRequirement()
    .addList("bearerAuth"))
```

### 注意事项

1. **重启应用**：配置修改后需要重启应用才能生效
2. **Token 有效期**：默认 24 小时，过期需重新登录获取
3. **Bearer 格式**：Token 前需要加 `Bearer ` 前缀

### 完整测试流程

```
1. 访问 Swagger：http://localhost:8080/swagger-ui.html
2. 测试登录：POST /api/auth/login → 获取 Token
3. 配置鉴权：点击 Authorize → 粘贴 Token
4. 测试业务接口：GET /api/records
5. 验证结果：查看 Response 是否返回数据
```

---

## 问题9：（待补充）

