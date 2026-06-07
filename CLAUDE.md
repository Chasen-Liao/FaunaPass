# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概况

- **课程设计题目**：基于 Spring Boot 3 的校园流浪动物图鉴与动态打卡系统设计与实现
- **学号 / 作者**：23251102132 廖朝正
- **课程**：综合课程设计 II（计算机科学与技术 / 软件工程专业集中实践环节）
- **设计要求全文**：[docs/设计要求.md](docs/设计要求.md)
- **当前状态**：仓库初始化完成，已选定纯净 Spring Boot 3 自建方案，后端代码尚未生成。

## 技术栈（已与用户确认）

| 维度 | 选型 | 备注 |
|------|------|------|
| 基础框架 | Spring Boot 3.x | 从 start.spring.io 起一个干净工程，不依赖任何二开脚手架 |
| JDK | 17（最终以 `pom.xml` 中 `java.version` 为准） | |
| 持久层 | MyBatis-Plus | 直接用 `Page<T>` 做分页 |
| 数据库 | MySQL 8.x | 编码 utf8mb4 |
| 鉴权 | Spring Security + JWT（**轻量版**） | 仅做账号密码登录返回 token；不做注册、不做角色管理 CRUD |
| 构建工具 | Maven | 单模块 |
| 架构 | 前后端分离，RESTful API | 前端形态待定（最简 HTML+原生 JS 或 Vue 3 SPA） |
| 分层 | Controller / Service / Mapper（标准三层） | |
| 接口文档 | springdoc-openapi（Swagger UI） | |

## 项目方针（重要）

**本项目不基于任何二开脚手架（如 RuoYi）。** 之前评估过 RuoYi 基础版（Shiro + Thymeleaf 单体）与 RuoYi-Vue（Spring Security + JWT + Vue），均不适合本课设：

- 业务非常窄（2 张业务表、~5 个核心接口），脚手架自带的用户/角色/菜单/字典/代码生成器都是无关代码
- 报告需要解释"为什么写了这么多与题目无关的模块"，反而模糊了对 Spring Boot 三层架构的考核
- 纯净自建总代码量约 1200 行 Java + 400 行前端，每一行都属于自己

**鉴权为什么用"轻量版"：** 设计要求未明确鉴权细节，但报告参考格式 4.1 是"系统登录"，老师期望有登录功能。最小实现 = 账号密码登录 → 返回 JWT → 后续接口校验 token + 注解区分管理员/学生。**不做注册、不做用户/角色管理界面、user 表预置 2 个种子账号即可。**

## 仓库目录约定

```
.
├── .gitignore                # Java/Maven/Spring Boot 通用
├── CLAUDE.md                 # 本文件
├── docs/
│   ├── 设计要求.md           # 课程设计原始要求
│   └── superpowers/specs/    # 设计文档（spec），由 brainstorming 流程生成
├── project/                  # 后端 Spring Boot 工程（Maven，待初始化）
│   ├── pom.xml
│   ├── src/main/java/...
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── mapper/
│   ├── sql/                  # 建表脚本 + 种子数据
│   └── uploads/              # 本地文件上传目录（被 .gitignore 排除）
├── frontend/                 # 前端工程（形态待定，可能为静态 HTML 或 Vite + Vue 3）
└── 23251102132廖朝正综合课程设计II课程设计报告A.doc
```

## 常用命令（待后端代码就位后填充）

- 构建：`mvn -f project/pom.xml clean package`
- 本地运行：`mvn -f project/pom.xml spring-boot:run`
- 运行单测：`mvn -f project/pom.xml test -Dtest=ClassName#methodName`
- 数据库初始化：`mysql -u root -p < project/sql/init.sql`
- 接口文档：`http://localhost:8080/swagger-ui.html`（启动后）

## 业务核心概念

- **图鉴 Animal**：校园流浪动物档案（名字、类型猫/狗、常驻区域、封面照片）。管理员维护。
- **动态打卡 CheckIn**：学生偶遇某只动物时发布的文字打卡（animal_id + user_id + content + created_at）
- **用户 User**：学生 / 管理员两类角色，使用本地自建 `user` 表（不复用任何脚手架的 `sys_user`）
- **一对多关系**：`animal (1) → check_in (N)`，是课程设计**核心考察点**
- **多表关联**：详情页时间轴需要 `check_in JOIN user JOIN animal`

## 编码纪律（来自用户全局 CLAUDE.md）

- 先想清楚再写：明确假设、暴露权衡、不确定先问
- 简单优先：最小代码解决当前问题，不做投机性功能
- 精准改动：只碰必须碰的，不"顺手优化"相邻代码
- 以结果验证为导向：多步骤任务公开计划，每步可独立验证

## 后续待办（提示未来 Claude）

- [ ] 在 `docs/superpowers/specs/` 下产出设计文档（brainstorming → writing-plans）
- [ ] 在 `project/` 下初始化 Spring Boot 3 工程（Maven）
- [ ] 设计 `user` / `animal` / `check_in` 三张业务表 DDL，编写种子数据脚本
- [ ] 实现核心 6 个接口（登录 + 图鉴 CRUD + 打卡发布 + 时间轴）
- [ ] 决定前端形态（最简 HTML 或 Vue 3 SPA）并实现最小可演示页面
- [ ] 编写课程设计报告（`docs/课程设计报告.md` 或更新现有 .doc）
- [ ] 补全 CLAUDE.md 中"常用命令"小节为可执行的真实命令
