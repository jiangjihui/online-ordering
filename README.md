# 在线点餐系统 Online Ordering System

餐厅在线点餐系统，支持顾客扫码点餐、后厨看板、管理后台。

## 技术栈

- **后端**: Spring Boot 3.2 + Java 17 + MyBatis Plus + SQLite + Spring Security + WebSocket
- **前端**: Vue 3 + TypeScript + Pinia + Element Plus + STOMP WebSocket

## 功能

### 顾客端
- 扫码进入点餐页面（按桌号）
- 浏览菜品、搜索、按标签筛选
- 加入购物车、下单、加菜
- 查看订单状态实时更新
- 呼叫服务员

### 后厨端
- 实时查看新订单
- 标记单品制作状态（待做 → 制作中 → 已完成）
- 整单完成
- 查看服务员呼叫

### 管理后台
- 仪表盘：营收统计、热门菜品、今日订单
- 菜品管理：增删改查、售罄开关、上架状态、图片上传
- 分类管理、套餐管理
- 订单查看、清台操作
- 餐桌管理、QR码生成
- 日报导出（打印菜单、导出营收）

## 快速开始

### 前置要求
- Java 17+
- Node.js 18+
- Maven 3.8+

### 后端启动

```bash
cd backend
# 确保使用 Java 17
export JAVA_HOME=/path/to/jdk-17
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`，首次启动自动执行 Flyway 数据库迁移和种子数据初始化。

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:3000`。

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | ADMIN |
| kitchen | kitchen123 | KITCHEN |

## 项目结构

```
online-ordering/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/ordering/
│   │   ├── config/             # 配置（Security、CORS、WebSocket、MyBatisPlus）
│   │   ├── controller/         # REST API 控制器
│   │   ├── service/            # 业务逻辑层
│   │   ├── mapper/             # MyBatis 数据访问层
│   │   ├── entity/             # 数据实体
│   │   ├── dto/                # 请求/响应对象
│   │   ├── common/             # 统一响应、异常处理
│   │   ├── websocket/          # WebSocket 处理器
│   ├── src/main/resources/
│   │   ├── application.yml     # 配置文件
│   │   ├── db/migration/       # Flyway 数据库迁移脚本
│   │   ├── mapper/             # MyBatis XML 映射
├── frontend/                   # Vue 3 前端（子模块）
│   ├── src/
│   │   ├── api/                # API 调用层（axios + WebSocket）
│   │   ├── stores/             # Pinia 状态管理
│   │   ├── views/              # 页面组件
│   │   │   ├── customer/       # 顾客端页面
│   │   │   ├── kitchen/        # 后厨端页面
│   │   │   ├── admin/          # 管理端页面
│   │   ├── components/         # 通用组件
│   │   ├── types/              # TypeScript 类型定义
│   │   ├── utils/              # 工具函数
├── LICENSE
├── .gitignore
```

## API 概览

| 模块 | 公开接口 | 认证接口 |
|------|---------|---------|
| 菜品 | GET categories/dishes/combos | POST/PUT/DELETE (ADMIN) |
| 订单 | GET/POST orders | PUT item status (ADMIN/KITCHEN) |
| 餐桌 | GET tables | POST/PUT/DELETE (ADMIN) |
| 呼叫 | POST waiter-calls | GET/PUT (ADMIN/KITCHEN) |
| 统计 | — | GET stats (ADMIN/KITCHEN) |

## 环境配置

前端使用 Vite 环境变量配置 API 地址：

- `.env.development` — 开发环境（localhost:8080）
- `.env.production` — 生产环境（需修改为实际部署地址）

## License

[MIT](LICENSE)
