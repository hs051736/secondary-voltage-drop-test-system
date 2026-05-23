# 二次压降检测系统 API 文档

## 目录
- [1. 概述](#1-概述)
- [2. 认证接口](#2-认证接口)
- [3. 检测记录接口](#3-检测记录接口)
- [4. 统计数据接口](#4-统计数据接口)
- [5. OCR识别接口](#5-ocr识别接口)
- [6. 数据模型](#6-数据模型)
- [7. 错误码说明](#7-错误码说明)

---

## 1. 概述

### 1.1 基本信息

| 项目 | 说明 |
|------|------|
| 基础URL | `http://localhost:8080/api` |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| API文档 | `/swagger-ui.html` |

### 1.2 认证方式

本系统采用 **JWT (JSON Web Token)** 无状态认证机制。

**请求头格式：**

```
Authorization: Bearer <token>
```

**示例：**

```http
GET /api/records HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTcwOTQ4MjAwMCwiZXhwIjoxNzA5NTY4NDAwfQ.xxxxx
```

### 1.3 公共请求头

所有请求都需要包含以下请求头：

| 请求头 | 值 | 说明 |
|--------|-----|------|
| Content-Type | application/json | 请求内容类型 |
| Authorization | Bearer {token} | JWT认证令牌（除认证接口外） |

---

## 2. 认证接口

### 2.1 用户登录

**接口地址：** `POST /api/auth/login`

**功能说明：** 使用用户名和密码登录系统，验证成功后返回JWT Token。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**请求示例：**
```json
{
    "username": "admin",
    "password": "password"
}
```

**成功响应 (200 OK)：**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTcwOTQ4MjAwMCwiZXhwIjoxNzA5NTY4NDAwfQ.xxxxx",
    "username": "admin",
    "nickname": "系统管理员",
    "role": "ADMIN",
    "expiresIn": 86400000,
    "code": 200,
    "message": "登录成功"
}
```

**失败响应 (401 Unauthorized)：**
```json
{
    "token": null,
    "username": null,
    "nickname": null,
    "role": null,
    "expiresIn": null,
    "code": 401,
    "message": "密码错误"
}
```

**错误消息：**
| 消息 | 说明 |
|------|------|
| 用户不存在 | 用户名不存在 |
| 密码错误 | 密码不正确 |
| 账号已被禁用 | 用户状态为禁用 |

---

### 2.2 用户注册

**接口地址：** `POST /api/auth/register`

**功能说明：** 注册新用户，注册成功后自动登录并返回JWT Token。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名（唯一） |
| password | String | 是 | 密码（至少6位） |
| nickname | String | 否 | 昵称 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |

**请求示例：**
```json
{
    "username": "newuser",
    "password": "123456",
    "nickname": "新用户",
    "email": "newuser@example.com"
}
```

**成功响应 (200 OK)：**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuZXd1c2VyIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3MDk0ODIwMDAsImV4cCI6MTcwOTU2ODQwMH0.xxxxx",
    "username": "newuser",
    "nickname": "新用户",
    "role": "USER",
    "expiresIn": 86400000,
    "code": 200,
    "message": "登录成功"
}
```

**失败响应 (400 Bad Request)：**
```json
{
    "token": null,
    "code": 401,
    "message": "用户名已存在"
}
```

---

## 3. 检测记录接口

### 3.1 查询检测记录列表

**接口地址：** `GET /api/records`

**功能说明：** 查询检测记录列表，支持关键字模糊搜索。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 否 | 搜索关键字（匹配产品编号、产品名称、制造商） |

**请求示例：**
```http
GET /api/records?keyword=电能表 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**成功响应 (200 OK)：**

```json
{
    "code": 200,
    "data": [
        {
            "id": 1,
            "productNo": "ELEC-2024-0001",
            "productName": "电能表0.5S级",
            "manufacturer": "华东电力设备有限公司",
            "origin": "上海",
            "testDate": "2024-01-05",
            "secondaryVoltage": "100V",
            "temperature": "22°C",
            "humidity": "55%",
            "resultStatus": "合格",
            "createTime": "2024-01-05T10:30:00"
        },
        {
            "id": 2,
            "productNo": "ELEC-2024-0002",
            "productName": "电能表0.2S级",
            "manufacturer": "华北电力科技公司",
            "origin": "北京",
            "testDate": "2024-01-08",
            "secondaryVoltage": "100V",
            "temperature": "20°C",
            "humidity": "48%",
            "resultStatus": "合格",
            "createTime": "2024-01-08T14:20:00"
        }
    ],
    "message": "查询成功"
}
```

---

### 3.2 查询检测记录详情

**接口地址：** `GET /api/records/{id}`

**功能说明：** 根据ID查询单条检测记录的完整信息，包括设备信息、检测项目和所有三相数据。

**路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 检测记录ID |

**请求示例：**
```http
GET /api/records/1 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**成功响应 (200 OK)：**
```json
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "id": 1,
        "productNo": "ELEC-2024-0001",
        "productName": "电能表0.5S级",
        "manufacturer": "华东电力设备有限公司",
        "origin": "上海",
        "testDate": "2024-01-05",
        "deviceInfo": {
            "productNo": "ELEC-2024-0001",
            "productName": "电能表0.5S级",
            "manufacturer": "华东电力设备有限公司",
            "origin": "上海",
            "testDate": "2024-01-05"
        },
        "testResult": {
            "secondaryVoltage": "100V",
            "temperature": "22°C",
            "humidity": "55%",
            "resultStatus": "合格",
            "phaseResults": {
                "ao": {
                    "f": "0.012",
                    "d": "1.5",
                    "dU": "0.018",
                    "uptU": "1.00012",
                    "uybU": "1.00008"
                },
                "bo": {
                    "f": "0.015",
                    "d": "2.0",
                    "dU": "0.022",
                    "uptU": "1.00015",
                    "uybU": "1.0001"
                },
                "co": {
                    "f": "0.018",
                    "d": "2.3",
                    "dU": "0.025",
                    "uptU": "1.00018",
                    "uybU": "1.00012"
                }
            }
        },
        "projects": [
            {
                "projectName": "PT1",
                "rating": "1",
                "percentage": "100%",
                "min": "-0.05",
                "max": "+0.05",
                "measured": "0.023",
                "secondaryVoltage": "100V",
                "temperature": "22°C",
                "humidity": "55%",
                "resultStatus": "合格",
                "phaseResults": {
                    "ao": {"f": "0.012", "d": "1.5", "dU": "0.018", "uptU": "1.00012", "uybU": "1.00008"},
                    "bo": {"f": "0.015", "d": "2.0", "dU": "0.022", "uptU": "1.00015", "uybU": "1.0001"},
                    "co": {"f": "0.018", "d": "2.3", "dU": "0.025", "uptU": "1.00018", "uybU": "1.00012"}
                },
                "verified": true
            },
            {
                "projectName": "PT2",
                "rating": "1",
                "percentage": "100%",
                "min": "-0.05",
                "max": "+0.05",
                "measured": "0.031",
                "secondaryVoltage": "100V",
                "temperature": "22°C",
                "humidity": "55%",
                "resultStatus": "合格",
                "phaseResults": {
                    "ao": {"f": "0.025", "d": "3.1", "dU": "0.035", "uptU": "1.00025", "uybU": "1.00018"},
                    "bo": {"f": "0.028", "d": "3.5", "dU": "0.04", "uptU": "1.00028", "uybU": "1.0002"},
                    "co": {"f": "0.031", "d": "3.8", "dU": "0.044", "uptU": "1.00031", "uybU": "1.00022"}
                },
                "verified": true
            }
        ]
    }
}
```

---

### 3.3 新增检测记录

**接口地址：** `POST /api/records`

**功能说明：** 创建新的检测记录，包括设备信息和多个检测项目。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deviceInfo | Object | 是 | 设备基本信息 |
| projects | Array | 是 | 检测项目列表 |

**请求示例：**
```json
{
    "deviceInfo": {
        "productNo": "ELEC-2024-0100",
        "productName": "电能表0.5S级",
        "manufacturer": "测试厂商",
        "origin": "深圳",
        "testDate": "2024-03-30"
    },
    "projects": [
        {
            "projectName": "PT1",
            "rating": "1",
            "percentage": "100%",
            "min": "-0.05",
            "max": "+0.05",
            "measured": "0.015",
            "secondaryVoltage": "100V",
            "temperature": "25°C",
            "humidity": "60%",
            "resultStatus": "合格",
            "phaseResults": {
                "ao": {"f": "0.008", "d": "1.0", "dU": "0.012", "uptU": "1.00008", "uybU": "1.00005"},
                "bo": {"f": "0.010", "d": "1.3", "dU": "0.015", "uptU": "1.00010", "uybU": "1.00007"},
                "co": {"f": "0.012", "d": "1.6", "dU": "0.018", "uptU": "1.00012", "uybU": "1.00008"}
            }
        },
        {
            "projectName": "PT2",
            "rating": "1",
            "percentage": "100%",
            "min": "-0.05",
            "max": "+0.05",
            "measured": "0.020",
            "secondaryVoltage": "100V",
            "temperature": "25°C",
            "humidity": "60%",
            "resultStatus": "合格",
            "phaseResults": {
                "ao": {"f": "0.010", "d": "1.2", "dU": "0.015", "uptU": "1.00010", "uybU": "1.00007"},
                "bo": {"f": "0.012", "d": "1.5", "dU": "0.018", "uptU": "1.00012", "uybU": "1.00008"},
                "co": {"f": "0.015", "d": "1.9", "dU": "0.022", "uptU": "1.00015", "uybU": "1.00010"}
            }
        }
    ]
}
```

**成功响应 (200 OK)：**
```json
{
    "code": 200,
    "message": "保存成功",
    "data": 15
}
```

**失败响应 (400 Bad Request)：**
```json
{
    "code": 400,
    "message": "保存失败：产品编号不能为空"
}
```

---

### 3.4 删除检测记录

**接口地址：** `DELETE /api/records/{id}`

**功能说明：** 根据ID删除检测记录及其关联的检测项目和相位数据。

**路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 检测记录ID |

**请求示例：**
```http
DELETE /api/records/1 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**成功响应 (200 OK)：**
```json
{
    "code": 200,
    "message": "删除成功"
}
```

**失败响应 (404 Not Found)：**
```json
{
    "code": 404,
    "message": "记录不存在"
}
```

---

### 3.5 导出PDF报告

**接口地址：** `GET /api/records/{id}/pdf`

**功能说明：** 根据检测记录ID导出PDF格式的检测报告。

**路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 检测记录ID |

**请求示例：**
```http
GET /api/records/1/pdf HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**成功响应：** PDF文件流

```
Content-Type: application/pdf
Content-Disposition: attachment; filename="检测报告_ELEC-2024-0001.pdf"
```

---

## 4. 统计数据接口

### 4.1 获取统计数据

**接口地址：** `GET /api/stats`

**功能说明：** 获取本月和总体的检测统计数据，包括检测数量和合格率。

**请求示例：**
```http
GET /api/stats HTTP/1.1
Host: localhost:8080
```

**成功响应 (200 OK)：**
```json
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "totalRecords": 20,
        "monthRecords": 5,
        "totalQualified": 18,
        "totalUnqualified": 2,
        "totalQualifiedRate": 90.0,
        "monthQualified": 5,
        "monthUnqualified": 0,
        "monthQualifiedRate": 100.0
    }
}
```

**响应字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| totalRecords | Integer | 总检测记录数 |
| monthRecords | Integer | 本月检测记录数 |
| totalQualified | Integer | 总合格数 |
| totalUnqualified | Integer | 总不合格数 |
| totalQualifiedRate | Double | 总合格率（%） |
| monthQualified | Integer | 本月合格数 |
| monthUnqualified | Integer | 本月不合格数 |
| monthQualifiedRate | Double | 本月合格率（%） |

---

## 5. OCR识别接口

### 5.1 识别图片数据

**接口地址：** `POST /api/ocr/recognize`

**功能说明：** 上传检测设备截图，通过OCR识别提取检测数据。

**请求格式：** `multipart/form-data`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| image | File | 是 | 图片文件（支持JPG、PNG） |

**请求示例：**
```http
POST /api/ocr/recognize HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="image"; filename="test.jpg"
Content-Type: image/jpeg

[图片二进制数据]
------WebKitFormBoundary--
```

**成功响应 (200 OK)：**
```json
{
    "code": 200,
    "message": "识别成功",
    "data": {
        "secondaryVoltage": "100V",
        "temperature": "25°C",
        "humidity": "60%",
        "phaseResults": {
            "ao": {
                "f": "0.012",
                "d": "1.5",
                "dU": "0.018",
                "uptU": "1.00012",
                "uybU": "1.00008"
            },
            "bo": {
                "f": "0.015",
                "d": "2.0",
                "dU": "0.022",
                "uptU": "1.00015",
                "uybU": "1.0001"
            },
            "co": {
                "f": "0.018",
                "d": "2.3",
                "dU": "0.025",
                "uptU": "1.00018",
                "uybU": "1.00012"
            }
        }
    }
}
```

**失败响应 (400 Bad Request)：**
```json
{
    "code": 400,
    "message": "识别失败：无法从图片中提取数据"
}
```

---

## 6. 数据模型

### 6.1 设备信息 (DeviceInfo)

```json
{
    "productNo": "ELEC-2024-0001",
    "productName": "电能表0.5S级",
    "manufacturer": "华东电力设备有限公司",
    "origin": "上海",
    "testDate": "2024-01-05"
}
```

### 6.2 项目信息 (ProjectItem)

```json
{
    "projectName": "PT1",
    "rating": "1",
    "percentage": "100%",
    "min": "-0.05",
    "max": "+0.05",
    "measured": "0.023",
    "secondaryVoltage": "100V",
    "temperature": "25°C",
    "humidity": "60%",
    "resultStatus": "合格",
    "phaseResults": {
        "ao": {...},
        "bo": {...},
        "co": {...}
    }
}
```

### 6.3 相位数据 (PhaseData)

```json
{
    "f": "0.012",
    "d": "1.5",
    "dU": "0.018",
    "uptU": "1.00012",
    "uybU": "1.00008"
}
```

**字段说明：**

| 字段 | 说明 |
|------|------|
| f | 比差 (%) |
| d | 角差 (分) |
| dU | 压降误差 (%) |
| uptU | Upt:U 比值 |
| uybU | Uyb:U 比值 |

### 6.4 认证响应 (AuthResponse)

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

## 7. 错误码说明

### 7.1 HTTP状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（登录失败、Token无效） |
| 403 | 禁止访问（权限不足） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 7.2 业务错误码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 认证失败 |
| 404 | 记录不存在 |
| 500 | 服务器内部错误 |

### 7.3 常见错误消息

| 消息 | 说明 |
|------|------|
| 用户不存在 | 登录时用户名不存在 |
| 密码错误 | 登录时密码不正确 |
| 账号已被禁用 | 用户状态为禁用 |
| 用户名已存在 | 注册时用户名已被占用 |
| 记录不存在 | 查询或删除的记录ID不存在 |
| 保存失败 | 数据保存失败 |

---

## 8. 接口调用示例

### 8.1 使用 cURL

**登录：**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

**查询记录列表：**
```bash
curl -X GET http://localhost:8080/api/records \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**创建检测记录：**
```bash
curl -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "deviceInfo": {
      "productNo": "ELEC-2024-0100",
      "productName": "电能表0.5S级"
    },
    "projects": [...]
  }'
```

### 8.2 使用 JavaScript (Fetch API)

```javascript
// 登录
const loginRes = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'password' })
});
const loginData = await loginRes.json();
const token = loginData.token;

// 查询记录
const recordsRes = await fetch('/api/records', {
    headers: { 'Authorization': `Bearer ${token}` }
});
const recordsData = await recordsRes.json();
console.log(recordsData.data);
```

### 8.3 使用 Java (OkHttp)

```java
// 登录
MediaType JSON = MediaType.parse("application/json; charset=utf-8");
String json = "{\"username\":\"admin\",\"password\":\"password\"}";

Request request = new Request.Builder()
    .url("http://localhost:8080/api/auth/login")
    .post(RequestBody.create(json, JSON))
    .build();

try (Response response = client.newCall(request).execute()) {
    String responseBody = response.body().string();
    System.out.println(responseBody);
}
```

---

## 9. 附录

### 9.1 数据库表结构

**users 表**
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'USER',
    status INT DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME
);
```

**test_records 表**
```sql
CREATE TABLE test_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_no VARCHAR(100) NOT NULL,
    product_name VARCHAR(200),
    manufacturer VARCHAR(200),
    origin VARCHAR(200),
    test_date DATE,
    secondary_voltage VARCHAR(50),
    temperature VARCHAR(20),
    humidity VARCHAR(20),
    result_status VARCHAR(20),
    create_time DATETIME,
    update_time DATETIME
);
```

**test_input_details 表**
```sql
CREATE TABLE test_input_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    item_name VARCHAR(50) NOT NULL,
    rating VARCHAR(50),
    percentage VARCHAR(20),
    min_limit VARCHAR(50),
    max_limit VARCHAR(50),
    measured_val VARCHAR(50),
    secondary_voltage VARCHAR(50),
    temperature VARCHAR(20),
    humidity VARCHAR(20),
    result_status VARCHAR(20),
    create_time DATETIME
);
```

**test_phase_results 表**
```sql
CREATE TABLE test_phase_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    item_name VARCHAR(50) NOT NULL,
    phase VARCHAR(10) NOT NULL,
    f_percent DECIMAL(15,8),
    d_percent DECIMAL(15,8),
    du_percent DECIMAL(15,8),
    uptu_percent DECIMAL(15,8),
    uybu_percent DECIMAL(15,8),
    create_time DATETIME
);
```


**API基础URL：** http://localhost:8080/api
**Swagger文档：** http://localhost:8080/swagger-ui.html