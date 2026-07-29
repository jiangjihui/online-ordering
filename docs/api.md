# API 文档

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: HTTP Basic Authentication
- **响应格式**: 统一 JSON 包装

### 统一响应格式

```json
{
  "code": 0,
  "data": {},
  "message": "success"
}
```

- `code`: 0 表示成功，1 表示失败
- `data`: 业务数据
- `message`: 描述信息

### 认证

管理端和后厨端接口需要 HTTP Basic Auth，请求头格式：

```
Authorization: Basic base64(username:password)
```

默认账号：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | ADMIN |
| kitchen | kitchen123 | KITCHEN |

---

## 分类（Categories）

### GET /api/categories

获取所有分类列表（按 sort_order 排序）。

**权限**: 公开

**响应示例**:

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "name": "热菜",
      "nameEn": "Hot Dishes",
      "sortOrder": 1,
      "icon": "🔥"
    }
  ]
}
```

### POST /api/categories

创建分类。

**权限**: ADMIN

**请求体**:

```json
{
  "name": "热菜",
  "nameEn": "Hot Dishes",
  "sortOrder": 1,
  "icon": "🔥"
}
```

### PUT /api/categories/{id}

更新分类。

**权限**: ADMIN

**请求体**: 同创建分类

### DELETE /api/categories/{id}

删除分类。

**权限**: ADMIN

---

## 菜品（Dishes）

### GET /api/dishes

获取菜品列表，支持筛选参数。

**权限**: 公开

**查询参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| category_id | Long | 按分类筛选 |
| status | String | 按状态筛选（active/inactive） |
| search | String | 搜索菜名 |
| sold_out | Integer | 按售罄状态（0=在售，1=售罄） |

**响应示例**:

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "name": "红烧肉",
      "nameEn": "Braised Pork",
      "categoryId": 1,
      "price": 38.0,
      "description": "经典红烧肉",
      "descriptionEn": "Classic braised pork",
      "image": "abc123.jpg",
      "status": "active",
      "soldOut": 0,
      "isSpicy": 0,
      "labels": ["推荐", "招牌"],
      "sortOrder": 1
    }
  ]
}
```

### GET /api/dishes/{id}

获取单个菜品详情。

**权限**: 公开

### POST /api/dishes

创建菜品。

**权限**: ADMIN

**请求体**:

```json
{
  "name": "红烧肉",
  "nameEn": "Braised Pork",
  "categoryId": 1,
  "price": 38.0,
  "description": "经典红烧肉",
  "status": "active",
  "soldOut": 0,
  "isSpicy": 0,
  "labels": ["推荐", "招牌"],
  "sortOrder": 1
}
```

### PUT /api/dishes/{id}

更新菜品（含售罄开关、上架状态）。

**权限**: ADMIN

### DELETE /api/dishes/{id}

删除菜品。

**权限**: ADMIN

### POST /api/dishes/{id}/image

上传菜品图片。

**权限**: ADMIN

**请求**: `multipart/form-data`，字段名 `file`

**响应**:

```json
{
  "code": 0,
  "data": "abc123.jpg",
  "message": "success"
}
```

`data` 为图片文件名，前端拼接为 `/images/abc123.jpg` 访问。

---

## 套餐（Combos）

### GET /api/combos

获取套餐列表。

**权限**: 公开

**查询参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| status | String | 按状态筛选（active/inactive） |

**响应示例**:

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "name": "4人超值套餐",
      "nameEn": "4-Person Value Combo",
      "price": 128.0,
      "description": "含4道菜品",
      "status": "active",
      "items": [
        {
          "id": 1,
          "comboId": 1,
          "dishId": 1,
          "dishName": "红烧肉",
          "quantity": 1
        }
      ]
    }
  ]
}
```

### POST /api/combos

创建套餐（含 combo_items）。

**权限**: ADMIN

**请求体**:

```json
{
  "name": "4人超值套餐",
  "price": 128.0,
  "description": "含4道菜品",
  "status": "active",
  "items": [
    { "dishId": 1, "dishName": "红烧肉", "quantity": 1 },
    { "dishId": 3, "dishName": "鱼香肉丝", "quantity": 1 }
  ]
}
```

### PUT /api/combos/{id}

更新套餐（含 combo_items）。

**权限**: ADMIN

### DELETE /api/combos/{id}

删除套餐。

**权限**: ADMIN

---

## 订单（Orders）

### GET /api/orders

获取订单列表，支持筛选。

**权限**: 公开

**查询参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| table_id | Long | 按桌号筛选 |
| status | String | 按状态筛选（pending/preparing/completed/closed） |
| start_date | String | 开始日期（yyyy-MM-dd） |
| end_date | String | 结束日期（yyyy-MM-dd） |

**响应示例**:

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "tableId": 1,
      "orderType": "scan",
      "status": "pending",
      "totalAmount": 76.0,
      "remark": "",
      "createdAt": "2026-07-29 10:00:00",
      "startedAt": null,
      "completedAt": null,
      "items": [
        {
          "id": 1,
          "orderId": 1,
          "dishId": 1,
          "dishName": "红烧肉",
          "quantity": 1,
          "remark": "",
          "status": "pending",
          "startedAt": null,
          "completedAt": null
        }
      ]
    }
  ]
}
```

### GET /api/orders/{id}

获取单个订单详情（含 items）。

**权限**: 公开

### POST /api/orders

创建订单（顾客下单）。

**权限**: 公开

**请求体**:

```json
{
  "tableId": 1,
  "orderType": "scan",
  "totalAmount": 76.0,
  "remark": "不要辣",
  "items": [
    { "dishId": 1, "dishName": "红烧肉", "quantity": 1, "remark": "" },
    { "dishId": 3, "dishName": "鱼香肉丝", "quantity": 1, "remark": "多放辣" }
  ]
}
```

> 下单后自动将对应餐桌状态变为"用餐中"。

### PUT /api/orders/{id}/items/{itemId}/status

更新单品制作状态。

**权限**: ADMIN / KITCHEN

**请求体**:

```json
{
  "status": "preparing"
}
```

状态流转：`pending` → `preparing` → `completed`

> 自动级联：
> - 任一菜品变为 preparing，订单自动变为 preparing（并记录 startedAt）
> - 所有菜品变为 completed，订单自动变为 completed（并记录 completedAt）

### PUT /api/orders/{id}/complete-all

整单全部完成。

**权限**: ADMIN / KITCHEN

将所有未完成的菜品一次性标记为 completed，订单状态变为 completed。

### GET /api/orders/stats

获取仪表盘统计数据。

**权限**: ADMIN / KITCHEN

**响应示例**:

```json
{
  "code": 0,
  "data": {
    "diningTableCount": 3,
    "pendingOrderCount": 2,
    "preparingOrderCount": 1,
    "todayRevenue": 256.0,
    "activeDishCount": 13,
    "topDishes": [
      {
        "dishId": 1,
        "dishName": "红烧肉",
        "totalQuantity": 5,
        "totalRevenue": 190.0
      }
    ],
    "recentCompletedOrders": [...]
  }
}
```

---

## 餐桌（Tables）

### GET /api/tables

获取所有餐桌列表。

**权限**: 公开

**响应示例**:

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "number": "A1",
      "area": "大厅",
      "capacity": 4,
      "status": "idle"
    }
  ]
}
```

餐桌状态：`idle`（空闲）、`dining`（用餐中）、`reserved`（预留）

### GET /api/tables/{id}

按 ID 获取餐桌。

**权限**: 公开

### GET /api/tables/number/{number}

按桌号获取餐桌（顾客扫码用）。

**权限**: 公开

**示例**: `GET /api/tables/number/A1`

### POST /api/tables

创建餐桌。

**权限**: ADMIN

**请求体**:

```json
{
  "number": "D1",
  "area": "包间",
  "capacity": 8,
  "status": "idle"
}
```

### PUT /api/tables/{id}

更新餐桌。

**权限**: ADMIN

### DELETE /api/tables/{id}

删除餐桌。

**权限**: ADMIN

---

## 服务员呼叫（Waiter Calls）

### POST /api/waiter-calls

顾客呼叫服务员。

**权限**: 公开

**请求体**:

```json
{
  "tableId": 1,
  "tableNumber": "A1"
}
```

### GET /api/waiter-calls

获取呼叫列表。

**权限**: ADMIN / KITCHEN

**查询参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| status | String | 按状态筛选（pending/handled） |

### PUT /api/waiter-calls/{id}/handle

处理呼叫。

**权限**: ADMIN / KITCHEN

将呼叫状态变为"已处理"。

---

## 权限汇总

| 接口 | GET | POST | PUT | DELETE |
|------|-----|------|-----|--------|
| /api/categories | 公开 | ADMIN | ADMIN | ADMIN |
| /api/dishes | 公开 | ADMIN | ADMIN | ADMIN |
| /api/dishes/{id}/image | — | ADMIN | — | — |
| /api/combos | 公开 | ADMIN | ADMIN | ADMIN |
| /api/orders | 公开 | 公开 | ADMIN/KITCHEN | — |
| /api/orders/stats | ADMIN/KITCHEN | — | — | — |
| /api/tables | 公开 | ADMIN | ADMIN | ADMIN |
| /api/waiter-calls | ADMIN/KITCHEN | 公开 | ADMIN/KITCHEN | — |

---

## WebSocket

连接地址: `http://localhost:8080/ws`（STOMP 协议，通过 SockJS）

### 频道

| 频道 | 说明 |
|------|------|
| `/topic/admin` | 后厨和管理端订阅 |
| `/topic/table/{tableId}` | 顾客端按桌号订阅 |

### 事件列表

| 事件 | Payload | 推送频道 |
|------|---------|----------|
| `order.created` | Order 对象 | `/topic/admin` |
| `order.item-status-updated` | `{ orderId, item: OrderItem }` | `/topic/admin`, `/topic/table/{tableId}` |
| `order.completed` | `{ orderId }` | `/topic/admin`, `/topic/table/{tableId}` |
| `waiter-call.created` | WaiterCall 对象 | `/topic/admin` |
| `waiter-call.handled` | `{ callId }` | `/topic/admin` |
