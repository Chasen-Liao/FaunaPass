# 设计文档：校园流浪动物图鉴与动态打卡系统

> **作者**：23251102132 廖朝正
> **课程**：综合课程设计 II
> **日期**：2026-06-07
> **状态**：设计已批准，待生成实施计划

---

## 1. 项目概述

### 1.1 项目目标

实现一个面向校园场景的"流浪动物图鉴 + 偶遇打卡"Web 系统，覆盖课程设计要求中的全部考核点：

- Spring Boot 3 + MyBatis-Plus + MySQL 标准三层架构
- RESTful API 与前后端分离
- 一对多（animal 1 : N check_in）业务建模与多表关联查询
- 文件上传、分页、模糊查询
- JWT 鉴权与基于角色的接口权限

### 1.2 非目标（YAGNI）

以下功能**明确不做**，避免脚手架化和过度设计：

- 不基于 RuoYi 等二开脚手架
- 不做用户/角色/菜单/字典等通用后台管理模块
- 不做密码找回、邮箱验证、第三方登录、token 刷新
- 不做权限粒度到按钮级别的 RBAC（角色只有 `ADMIN` / `STUDENT` 两种，靠 `@PreAuthorize` 注解区分）
- 不做软删动物档案的"恢复"功能
- 不做评论、点赞、关注等社交功能
- 不引入 Redis、消息队列、ES 等中间件

### 1.3 用户角色

| 角色 | 标识 | 可做的操作 |
|------|------|-----------|
| 管理员 | `ADMIN` | 登录、动物图鉴 CRUD、浏览图鉴与时间轴 |
| 学生 | `STUDENT` | 注册、登录、浏览图鉴、查看时间轴、发布打卡 |

---

## 2. 技术栈

| 维度 | 选型 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.x（latest stable） |
| 持久层 | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.x |
| 鉴权 | Spring Security + JJWT | 6.x / 0.12.x |
| 接口文档 | springdoc-openapi | 2.x |
| 构建 | Maven | 3.9+ |
| 前端 | 原生 HTML + JS + CSS | 无框架，无构建步骤 |
| 静态资源 | Spring Boot 内嵌（`src/main/resources/static/`） | |

---

## 3. 总体架构

### 3.1 系统部署架构

```
┌────────────────────────────────────────────────────────────────┐
│                       浏览器（学生 / 管理员）                    │
│   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│   │ login.   │  │ register.│  │ index.   │  │ admin.   │       │
│   │ html     │  │ html     │  │ html     │  │ html     │       │
│   └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
│         │            │            │            │                │
│         └────────────┴───── fetch + Bearer JWT ──────┐          │
└──────────────────────────────────────────────────────┼──────────┘
                                                       │
                                                       ▼
┌────────────────────────────────────────────────────────────────┐
│            Spring Boot 3 应用（localhost:8080）                 │
│   ┌────────────────────────────────────────────────────────┐   │
│   │  Spring Security 过滤链（JwtAuthenticationFilter）      │   │
│   └────────────────────────────────────────────────────────┘   │
│   ┌────────────────┐ ┌────────────────┐ ┌────────────────┐    │
│   │ AuthController │ │AnimalController│ │CheckInController│   │
│   └────────┬───────┘ └────────┬───────┘ └────────┬───────┘    │
│   ┌────────▼───────┐ ┌────────▼───────┐ ┌────────▼───────┐    │
│   │  AuthService   │ │ AnimalService  │ │ CheckInService │    │
│   └────────┬───────┘ └────────┬───────┘ └────────┬───────┘    │
│   ┌────────▼───────┐ ┌────────▼───────┐ ┌────────▼───────┐    │
│   │  UserMapper    │ │ AnimalMapper   │ │ CheckInMapper  │    │
│   └────────┬───────┘ └────────┬───────┘ └────────┬───────┘    │
└────────────┼──────────────────┼──────────────────┼────────────┘
             │                  │                  │
             ▼                  ▼                  ▼
┌────────────────────────────────────────────────────────────────┐
│                       MySQL 8.x                                 │
│            user 表    animal 表    check_in 表                  │
└────────────────────────────────────────────────────────────────┘
            ┌────────────────────────────────────┐
            │  本地文件系统：project/uploads/    │
            │  /animal/yyyy/MM/<uuid>.<ext>      │
            └────────────────────────────────────┘
```

### 3.2 分层职责

| 层 | 职责 | 不允许的事 |
|----|------|-----------|
| Controller | 参数接收/校验、调用 Service、组装响应 | 不写业务逻辑、不直接访问 Mapper |
| Service | 业务逻辑、事务边界 | 不依赖 HttpServletRequest |
| Mapper | 数据访问 | 不写业务逻辑 |
| DTO/VO | 接口入参/出参 | 不与数据库实体混用 |
| Entity | MyBatis-Plus 实体，对应数据库表 | 不直接作为接口出参 |

### 3.3 后端目录结构

```
project/
├── pom.xml
├── sql/
│   ├── init.sql              # 建表 + 索引 + 外键
│   └── seed.sql              # 种子用户、示例动物、示例打卡
├── uploads/                  # .gitignore 排除
│   └── animal/yyyy/MM/...
└── src/main/
    ├── java/com/cazoo/animal/
    │   ├── AnimalApplication.java
    │   ├── common/
    │   │   ├── Result.java                # 统一响应封装
    │   │   ├── PageResult.java            # 分页响应封装
    │   │   ├── ResultCode.java            # 业务错误码枚举
    │   │   ├── BusinessException.java
    │   │   └── GlobalExceptionHandler.java
    │   ├── config/
    │   │   ├── MybatisPlusConfig.java     # 分页插件
    │   │   ├── SecurityConfig.java        # Spring Security
    │   │   └── WebMvcConfig.java          # 静态资源映射 + uploads 路径
    │   ├── security/
    │   │   ├── JwtUtil.java
    │   │   ├── JwtAuthenticationFilter.java
    │   │   └── CustomUserDetailsService.java
    │   ├── module/
    │   │   ├── auth/
    │   │   │   ├── AuthController.java
    │   │   │   ├── AuthService.java
    │   │   │   ├── dto/LoginRequest.java
    │   │   │   ├── dto/RegisterRequest.java
    │   │   │   └── vo/LoginResponse.java
    │   │   ├── user/
    │   │   │   ├── User.java              # 含 UserRole 内部枚举
    │   │   │   ├── UserService.java       # 按 username 查 / 注册 / 重名校验
    │   │   │   └── UserMapper.java
    │   │   ├── animal/
    │   │   │   ├── AnimalController.java
    │   │   │   ├── AnimalService.java
    │   │   │   ├── AnimalMapper.java
    │   │   │   ├── Animal.java            # 含 AnimalType 内部枚举
    │   │   │   ├── dto/AnimalCreateRequest.java
    │   │   │   └── dto/AnimalQueryRequest.java
    │   │   ├── checkin/
    │   │   │   ├── CheckInController.java
    │   │   │   ├── CheckInService.java
    │   │   │   ├── CheckInMapper.java     # 时间轴 SQL 用 @Select 注解
    │   │   │   ├── CheckIn.java
    │   │   │   └── dto/CheckInCreateRequest.java
    │   │   └── upload/
    │   │       └── FileStorageService.java
    └── resources/
        ├── application.yml
        └── static/                        # 前端 4 个 HTML + js/css
            ├── login.html
            ├── register.html
            ├── index.html                 # 学生端：图鉴 + 时间轴 + 打卡
            ├── admin.html                 # 管理员端：图鉴 CRUD
            ├── css/style.css
            └── js/
                ├── api.js                 # fetch 封装 + token 注入
                ├── auth.js                # 登录/注册/登出/token 持久化
                ├── index.js
                └── admin.js
```

---

## 4. 数据库设计

### 4.1 表结构

#### `user` 表

```sql
CREATE TABLE user (
  id         BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
  username   VARCHAR(50)  NOT NULL                 COMMENT '登录名',
  password   VARCHAR(100) NOT NULL                 COMMENT 'BCrypt 哈希',
  nickname   VARCHAR(50)  NOT NULL                 COMMENT '昵称（时间轴显示）',
  role       VARCHAR(20)  NOT NULL                 COMMENT 'ADMIN / STUDENT',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统账号';
```

**字段说明**：

- `username` 全局唯一，注册时校验
- `password` 使用 BCrypt（10 轮）哈希，**绝不明文存储**
- `nickname` 用于打卡时间轴显示发布人名字，与 username 解耦
- `role` 字符串枚举，应用层用 `UserRole` enum 映射
- 不存邮箱/手机号（设计要求未涉及，YAGNI）

#### `animal` 表

```sql
CREATE TABLE animal (
  id           BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
  name         VARCHAR(50)  NOT NULL                 COMMENT '暂定名字',
  type         VARCHAR(10)  NOT NULL                 COMMENT 'CAT / DOG',
  area         VARCHAR(100) NOT NULL                 COMMENT '常驻区域',
  cover_image  VARCHAR(255) NULL                     COMMENT '封面图相对路径',
  deleted      TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '0=正常 1=已删除',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_name (name),
  KEY idx_type_deleted (type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动物图鉴档案';
```

**字段说明**：

- `name` 普通索引，支持 `LIKE 'xxx%'` 前缀匹配（**仅前缀模糊**；全模糊 `LIKE '%xxx%'` 不走索引）。本课设数据量小，也可接受全模糊，按需取舍
- `type` 字符串枚举，取值固定为 `CAT`（猫）、`DOG`（狗），与应用层 `AnimalType` enum 双向映射（设计要求 §3.1 字面要求"猫 / 狗"）
- `area` 自由文本，不预设字典（设计要求未要求）
- `cover_image` 存相对路径如 `/uploads/animal/2026/06/abc-def.jpg`；可空（允许先建档后补图）
- `deleted` **软删标记**，所有查询带 `WHERE deleted = 0`
- `idx_type_deleted` 复合索引：图鉴列表的核心查询条件就是 type + deleted

**软删 vs 硬删的决策依据**：设计要求 3.1 明确写"建议保留打卡记录"。软删一次性解决"保留历史 + 解除关联"两个需求，是最优解。

#### `check_in` 表

```sql
CREATE TABLE check_in (
  id         BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
  animal_id  BIGINT       NOT NULL                 COMMENT '关联动物 id',
  user_id    BIGINT       NOT NULL                 COMMENT '发布人 id',
  content    VARCHAR(500) NOT NULL                 COMMENT '打卡文字',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_animal_created (animal_id, created_at),
  KEY idx_user (user_id),
  CONSTRAINT fk_checkin_animal FOREIGN KEY (animal_id) REFERENCES animal(id) ON DELETE RESTRICT,
  CONSTRAINT fk_checkin_user   FOREIGN KEY (user_id)   REFERENCES user(id)   ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='偶遇打卡';
```

**字段说明**：

- `idx_animal_created` 复合索引：时间轴的核心查询 `WHERE animal_id = ? ORDER BY created_at DESC` 直接命中
- 外键 `ON DELETE RESTRICT`：阻止硬删动物与用户。配合 animal 的软删，业务上不会触发这个限制
- `content` 500 字符上限：500 字对一条打卡足够，避免 TEXT 字段的查询开销

### 4.2 ER 图

```
┌───────────────┐                                         ┌──────────────────┐
│     user      │                                         │      animal      │
├───────────────┤                                         ├──────────────────┤
│ id (PK)       │                                         │ id (PK)          │
│ username      │                                         │ name             │
│ password      │                                         │ type             │
│ nickname      │                                         │ area             │
│ role          │                                         │ cover_image      │
│ created_at    │                                         │ deleted          │
└───────┬───────┘                                         │ created_at       │
        │                                                 │ updated_at       │
        │ 1                                               └────────┬─────────┘
        │                                                          │ 1
        │                                                          │
        │                ┌──────────────────┐                      │
        │                │     check_in     │                      │
        │                ├──────────────────┤                      │
        │   N            │ id (PK)          │              N       │
        └───────────────▶│ user_id (FK)     │◀─────────────────────┘
                         │ animal_id (FK)   │
                         │ content          │
                         │ created_at       │
                         └──────────────────┘

         user  1 ── N  check_in  ── N  1  animal
   （一个用户发布多条打卡；一个动物拥有多条打卡 ← 课设核心一对多）
```

### 4.3 核心查询 SQL

**时间轴查询（三表 JOIN，写到报告里）**：

```sql
SELECT c.id, c.content, c.created_at,
       u.id AS user_id, u.nickname AS user_nickname,
       a.id AS animal_id, a.name AS animal_name
FROM check_in c
INNER JOIN user   u ON c.user_id   = u.id
INNER JOIN animal a ON c.animal_id = a.id
WHERE c.animal_id = #{animalId}
  AND a.deleted = 0
ORDER BY c.created_at DESC;
```

**图鉴分页查询（MyBatis-Plus）**：

```java
// AnimalService
LambdaQueryWrapper<Animal> qw = new LambdaQueryWrapper<Animal>()
    .eq(Animal::getDeleted, 0)
    // .likeRight = LIKE 'xxx%'（前缀模糊，走 idx_name 索引）
    // 若必须全模糊，把 likeRight 改成 like 即可，但会全表扫描
    .likeRight(StringUtils.hasText(req.getName()), Animal::getName, req.getName())
    .eq(req.getType() != null, Animal::getType, req.getType())
    .orderByDesc(Animal::getCreatedAt);
return animalMapper.selectPage(new Page<>(req.getPage(), req.getSize()), qw);
```

> **可选产品决策**：管理员后台列表也可以不要 name 模糊,只按 type 精确筛,这样性能最优;模糊查询放在前端搜索框可做可不做。

### 4.4 种子数据

`seed.sql` 预置：

- 3 个用户：`admin/admin123`（ADMIN）、`student01/123456`（张三）、`student02/123456`（李四）
- 4 只动物：奶牛猫@图书馆草坪、橘猫@西区食堂、黑狗@东门、白猫@教学楼后
- 每只动物 2-3 条打卡，便于演示时间轴

---

## 5. 接口设计

### 5.1 统一响应格式

```json
{
  "code": 0,
  "msg": "success",
  "data": { ... }
}
```

`code = 0` 成功；非 0 为业务错误码（如 1001 用户名已存在、1002 密码错误、2001 动物不存在）。

分页返回 `data`：

```json
{
  "records": [ ... ],
  "total": 42,
  "page": 1,
  "size": 10
}
```

### 5.2 接口列表

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 1 | POST | `/api/auth/register` | 公开 | 学生注册（role 固定为 STUDENT） |
| 2 | POST | `/api/auth/login` | 公开 | 登录，返回 JWT |
| 3 | GET | `/api/auth/me` | 已登录 | 返回当前用户信息（前端判断角色用） |
| 4 | GET | `/api/animals` | 已登录 | 图鉴列表：`?name=&type=&page=&size=` |
| 5 | GET | `/api/animals/{id}` | 已登录 | 图鉴详情 |
| 6 | POST | `/api/animals` | ADMIN | 录入动物（multipart：name/type/area + cover 文件） |
| 7 | PUT | `/api/animals/{id}` | ADMIN | 修改动物（同上，cover 可选） |
| 8 | DELETE | `/api/animals/{id}` | ADMIN | 软删动物 |
| 9 | POST | `/api/check-ins` | STUDENT | 发布打卡 |
| 10 | GET | `/api/animals/{id}/check-ins` | 已登录 | 时间轴 |

> 设计要求列出 5 个核心接口；上表多出的 `/register`、`/login`、`/me`、`/animals/{id}`、`/animals/{id} PUT` 都是支撑性接口，每一个都有明确用途，不属于过度设计。

### 5.3 接口示例

**POST `/api/auth/login`**

```http
POST /api/auth/login
Content-Type: application/json

{ "username": "student01", "password": "123456" }
```

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 2,
    "username": "student01",
    "nickname": "张三",
    "role": "STUDENT"
  }
}
```

**POST `/api/animals`（multipart）**

```
POST /api/animals
Authorization: Bearer <token>
Content-Type: multipart/form-data; boundary=...

--...
Content-Disposition: form-data; name="name"
奶牛猫
--...
Content-Disposition: form-data; name="type"
CAT
--...
Content-Disposition: form-data; name="area"
图书馆草坪
--...
Content-Disposition: form-data; name="cover"; filename="cat.jpg"
Content-Type: image/jpeg

<binary>
--...--
```

**GET `/api/animals/1/check-ins`**

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 12,
      "content": "今天在教学楼下看到它了，喂了根猫条，精神状态良好",
      "createdAt": "2026-06-05 14:32:18",
      "userId": 2,
      "userNickname": "张三",
      "animalId": 1,
      "animalName": "奶牛猫"
    },
    { ... }
  ]
}
```

---

## 6. 鉴权设计（轻量版）

### 6.1 流程

```
浏览器                       后端
  │                            │
  │ POST /api/auth/login       │
  ├───────────────────────────▶│
  │                            │ ① 用 BCrypt 校验密码
  │                            │ ② 生成 JWT(sub=userId, role, exp=24h)
  │ { token, role, ... }       │
  │◀───────────────────────────│
  │                            │
  │ 存 localStorage["token"]   │
  │                            │
  │ GET /api/animals           │
  │ Authorization: Bearer xxx  │
  ├───────────────────────────▶│
  │                            │ JwtAuthenticationFilter
  │                            │  ├─ 解析 token
  │                            │  ├─ 校验签名 + 过期
  │                            │  └─ 注入 SecurityContext
  │                            │ Controller @PreAuthorize 检查角色
  │ { ...data }                │
  │◀───────────────────────────│
```

### 6.2 关键决策

- **算法**：HS256（对称签名），key 写在 `application.yml`，足够本课设
- **过期**：24 小时，不做 refresh token
- **存储**：浏览器端 `localStorage["token"]`（简单；本课设不考虑 XSS 防护强度）
- **退出**：前端清 localStorage 即可，服务端无 session
- **权限注解**：`@PreAuthorize("hasRole('ADMIN')")` 放在 Controller 方法上
- **⚠️ authorities 必须含 `ROLE_` 前缀**（`hasRole` 内部会自动加 `ROLE_`，所以 `UserDetails` 返回的是 `new SimpleGrantedAuthority("ROLE_" + user.getRole())`，**不能**直接传 `"ADMIN"`）。这是 Spring Security 6 最常见的踩坑——少了 `ROLE_` 前缀，ADMIN 鉴权会 403 但代码看着没毛病

### 6.3 Spring Security 配置要点

```java
http
  .csrf(csrf -> csrf.disable())                              // 前后端分离不需要 CSRF
  .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))// JWT 无状态
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
      .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/uploads/**").permitAll()
      .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
      .anyRequest().authenticated())
  // ⚠️ AccessDeniedException 在过滤器链里抛,不会进 @RestControllerAdvice
  // 必须在 SecurityConfig 里显式写 JSON 响应,否则 STUDENT 调 ADMIN 接口会拿到 Spring 默认 403 页面
  .exceptionHandling(e -> e
      .accessDeniedHandler((req, res, ex) -> {
        res.setStatus(403);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"code\":4003,\"msg\":\"无权限访问\"}");
      })
      .authenticationEntryPoint((req, res, ex) -> {
        res.setStatus(401);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"code\":4001,\"msg\":\"未登录或 token 失效\"}");
      }))
  .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

---

## 7. 文件上传设计

### 7.1 存储策略

- **物理路径**：`project/uploads/animal/yyyy/MM/<uuid>.<ext>`（**绝对路径**，从 yml 注入，**不要**用 `file:./uploads/` 相对路径——IDEA 启动与 `java -jar` 启动的工作目录不同，Windows 下会找不到文件）
- **数据库存**：`/uploads/animal/2026/06/abc-def.jpg`（**相对路径**，便于迁移）
- **`application.yml` 配置**：
  ```yaml
  app:
    upload-dir: ${user.dir}/uploads
  ```
- **静态映射**（`WebMvcConfig`，从 `@Value("${app.upload-dir}")` 注入）：
  ```java
  @Value("${app.upload-dir}")
  private String uploadDir;

  // 启动时确保目录存在
  @Override public void afterPropertiesSet() {
      Files.createDirectories(Paths.get(uploadDir, "animal"));
  }

  // 静态资源映射
  registry.addResourceHandler("/uploads/**")
          .addResourceLocations("file:" + uploadDir + "/");
  ```

### 7.2 限制

- 仅 `jpg / jpeg / png / gif / webp`
- 单文件 ≤ 5MB（`application.yml` `spring.servlet.multipart.max-file-size: 5MB`）
- 文件名重命名为 UUID，避免中文路径与同名覆盖

### 7.3 `FileStorageService` 接口

```java
public interface FileStorageService {
    /** 保存图片，返回相对路径（如 /uploads/animal/2026/06/uuid.jpg） */
    String saveAnimalCover(MultipartFile file);
}
```

---

## 8. 前端设计

### 8.1 页面与职责

| 页面 | 角色 | 职责 |
|------|------|------|
| `login.html` | 公开 | 账号密码登录，成功后按角色跳转 |
| `register.html` | 公开 | 学生注册（role 固定 STUDENT） |
| `index.html` | STUDENT | 浏览图鉴（列表 + 详情 + 时间轴）、发布打卡 |
| `admin.html` | ADMIN | 浏览图鉴 + 录入/修改/软删动物 |

### 8.2 JS 组织

- `api.js`：单一 `request(method, url, body, isFormData)` 函数，自动注入 `Authorization: Bearer <token>`，统一处理 401（跳登录页）
- `auth.js`：`login() / register() / logout() / getCurrentUser()`，封装 token 读写
- `index.js` / `admin.js`：页面级逻辑，调用 `api.js`

### 8.3 不做的事

- 不用 Vue / React / jQuery
- 不做组件化，每个页面一个 HTML 文件
- 不做路由（页面跳转用 `<a href>` 与 `location.href`）
- 样式用一个 `style.css`，不引 UI 框架（如要美观可引 CDN 形式的 Pico.css 一个文件，但不必须）

---

## 9. 错误处理

### 9.1 业务异常

```java
public class BusinessException extends RuntimeException {
    private final ResultCode code;
    // ...
}
```

`ResultCode` 枚举集中管理错误码与默认消息：

```java
public enum ResultCode {
    SUCCESS(0, "success"),
    USERNAME_EXISTS(1001, "用户名已存在"),
    INVALID_CREDENTIALS(1002, "用户名或密码错误"),
    ANIMAL_NOT_FOUND(2001, "动物档案不存在"),
    FILE_TYPE_INVALID(3001, "不支持的图片格式"),
    FILE_TOO_LARGE(3002, "图片大小超过 5MB"),
    INTERNAL_ERROR(9999, "服务器内部错误");
    // ...
}
```

### 9.2 全局异常处理器

`GlobalExceptionHandler` 用 `@RestControllerAdvice`：

- `BusinessException` → 返回对应 code/msg，HTTP 200
- `MethodArgumentNotValidException` → 校验失败，code = 4001
- **不处理** `AccessDeniedException` / `AuthenticationException`——它们在 Spring Security 过滤器链里抛出，由 `SecurityConfig` 的 `exceptionHandling` 配 JSON 响应（见 §6.3）
- 其他 `Exception` → 兜底 9999，日志记录堆栈

---

## 10. 测试策略

**最小可信测试集**（避免为了测试而测试，预算 ~0.5 人天）：

- **集成测试**：用 `@SpringBootTest` + `MockMvc`，只测核心 3 个用例
  - `AuthControllerTest`：登录成功 + 密码错误 / 注册重名（一个测试类覆盖）
  - `AnimalControllerTest`：分页查询（含 type 筛选 + name 模糊）
  - `CheckInControllerTest`：时间轴按时间倒序 + 软删动物的旧打卡不显示
- **不做**：单独的 Service 单元测试（业务逻辑薄，集成测覆盖足够）；不做前端测试；CheckIn 业务用前端手测 + Swagger-like curl 验证即可
- **建议**：测试运行用 H2 内存数据库 + `application-test.yml`，避免污染 dev 数据

---

## 11. 实施顺序（推荐）

1. **脚手架**：Maven 工程、application.yml、HikariCP 数据源、MyBatis-Plus 配置（注册分页插件）、WebMvcConfig 静态映射
2. **数据库**：写 `init.sql` 与 `seed.sql`，本地建库导入
3. **最小 Spring Security**：先配 `permitAll()` 跑通 `/api/hello`，确认过滤器链能启动
4. **User + 鉴权**：User 实体（内含 UserRole 枚举）、UserService、UserDetailsService、JwtUtil、JwtAuthenticationFilter、SecurityConfig 接入 JWT、AuthController
5. **Animal CRUD**：Entity（内含 AnimalType 枚举）、Mapper、Service、Controller、FileStorageService
6. **CheckIn**：Entity、Mapper（用 `@Select` 注解写时间轴 JOIN）、Service、Controller
7. **集成测试**：Auth 2 个用例 + Animal 分页 1 个用例
8. **前端**：login → register → index → admin，每写一页就在浏览器手测
9. **联调**：前后端打通，截图
10. **报告（与第 8 步并行启动）**：边做前端边写"系统登录""图鉴管理"章节，避免压到最后

每一步完成后跑测试 + 启动 + 浏览器验证。

---

## 12. 风险与缓解

| 风险 | 缓解 |
|------|------|
| Spring Security 6.x API 变化大，配置容易踩坑 | 第一步先把"放行 + JWT 过滤器"跑通再写业务 |
| MyBatis-Plus 分页插件未生效，导致 SQL 没 LIMIT | 启动后调一次分页接口，看日志确认 SQL |
| 文件上传中文文件名乱码 | 一律 UUID 重命名，不依赖原文件名 |
| `localStorage` 跨页面共享但跨标签页清不掉 | 前端 401 时主动清 localStorage + 跳转登录页 |

---

## 13. 报告章节映射

| 报告章节 | 本 spec 对应位置 |
|---------|-----------------|
| 一、实验目的 | §1.1 + §1.2 |
| 二、需求分析 | §1.3 + §5.2（接口即功能点） |
| 三、总体设计 / 架构 | §3 |
| 三、数据库设计 | §4 |
| 三、接口设计 | §5 |
| 四、系统登录 | §6 + 前端 login.html 截图 |
| 四、图鉴管理 | §5.2 接口 4-8 + §7 文件上传 |
| 四、打卡功能 | §5.2 接口 9 |
| 四、时间轴聚合 | §4.3 SQL + §5.2 接口 10 |
| 五、总结 | 自由发挥 |
