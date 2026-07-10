# API 文档

## 认证

### JWT 认证
所有 API 请求需要在 Header 中携带 JWT Token：
```
Authorization: Bearer {token}
```

### API Key 认证
Open API 使用 API Key 认证（路径 `/api/**`）：
```
X-API-Key: {your-api-key}
```

## 缺陷管理 API

### 查询缺陷列表
```
GET /system/defect/list
```

参数：
- `defectName`: 缺陷名称（可选）
- `projectId`: 项目ID（可选）
- `status`: 状态（可选）
- `pageNum`: 页码
- `pageSize`: 每页数量

响应：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 100,
    "rows": [...]
  }
}
```

### 创建缺陷
```
POST /system/defect
```

请求体：
```json
{
  "defectName": "登录按钮无响应",
  "projectId": 1,
  "defectType": "功能缺陷",
  "priority": "高",
  "description": "详细描述..."
}
```

### 更新缺陷
```
PUT /system/defect
```

### 删除缺陷
```
DELETE /system/defect/{defectId}
```

## 项目管理 API

### 查询项目列表
```
GET /system/project/list
```

### 创建项目
```
POST /system/project
```

### 更新项目
```
PUT /system/project
```

### 删除项目
```
DELETE /system/project/{projectId}
```

## 用户管理 API

### 查询用户列表
```
GET /system/user/list
```

### 创建用户
```
POST /system/user
```

### 更新用户
```
PUT /system/user
```

## 响应格式

所有 API 返回统一格式：
```json
{
  "code": 200,
  "msg": "success",
  "data": {...}
}
```

错误响应：
```json
{
  "code": 500,
  "msg": "错误信息"
}
```

## 状态码

- `200`: 成功
- `401`: 未认证
- `403`: 无权限
- `404`: 资源不存在
- `500`: 服务器错误
