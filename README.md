# Interastral Peace Mall ｜ 星际和平商城

> 基于 Spring Boot + Spring MVC + MyBatis-Plus 的商城项目，包含**用户端商城**与**后台管理系统**。
> 课程实验：SSM 框架综合实践 · Git 版本控制 · AI Agent 辅助开发

## 一、技术栈

| 层面 | 选型 | 版本 |
|---|---|---|
| JDK | Java | 17（兼容 21） |
| 框架 | Spring Boot | 3.2.12 |
| Web | Spring MVC + Thymeleaf | — |
| 持久层 | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0 |
| 数据库工具 | Navicat | 16 |
| 密码加密 | spring-security-crypto（BCrypt） | 由 Boot 管理 |
| 构建 | Maven | 3.9 |
| 前端 | Thymeleaf + Bootstrap 5 + 少量 axios | — |

## 二、目录结构

```
interastral-peace-mall
├── pom.xml
├── db/init.sql                          # 建库脚本（业务表后续补充）
└── src/main
    ├── java/com/ipc/mall
    │   ├── MallApplication.java          # 启动类
    │   ├── common/result                # 统一返回 Result / ResultCode
    │   ├── common/exception             # BizException / 全局异常处理
    │   ├── config/MybatisPlusConfig     # 分页 + 乐观锁插件
    │   ├── controller                   # 控制层（后续分 front / admin 两个子包）
    │   ├── service + service/impl       # 业务层
    │   ├── mapper                       # MyBatis-Plus Mapper 接口
    │   ├── entity / dto / vo            # 实体、入参、出参
    │   ├── interceptor                  # 登录拦截器
    │   ├── aspect                       # AOP 切面（日志、耗时）
    │   ├── annotation                   # 自定义注解
    │   └── handler                      # 字段自动填充
    └── resources
        ├── application.yml
        ├── templates/index.html         # 首页占位
        ├── static/{css,js,img}
        └── mapper/                      # 复杂 SQL 的 XML
```

## 三、快速启动

1. **建库**：打开 Navicat → 连上本地 MySQL → 新建查询 → 执行 `db/init.sql`
2. **改配置**：编辑 `src/main/resources/application.yml` 的 `spring.datasource.password`
3. **启动**：IDEA 打开项目目录，等待 Maven 依赖下载完成，运行 `MallApplication`
4. **验证**：
   - 浏览器访问 <http://localhost:8080/> → 看到「星际和平商城」页面
   - 访问 <http://localhost:8080/ping> → 返回 `{"code":200,"message":"操作成功","data":"pong",...}`

## 四、已包含内容

- [x] Maven 骨架 + Spring Boot 3.2.12 依赖齐全（Web/Thymeleaf/Validation/MP/MySQL/BCrypt/Lombok/DevTools）
- [x] 统一返回 `Result` + `ResultCode`
- [x] 全局异常处理（业务异常、参数校验、兜底 500）
- [x] MyBatis-Plus 分页 + 乐观锁插件（`MybatisPlusConfig`）
- [x] `application.yml`（数据源、MyBatis-Plus、上传、日志）
- [x] `.gitignore`（target / .idea / uploads / application-local.yml）
- [x] 首页占位模板 + `/ping` 自检接口
- [x] `db/init.sql` 建库脚本

## 五、下一步（第 0 阶段剩余）

- [ ] 补充 10 张业务表 DDL 与测试数据
- [ ] 建立实体类 `BaseEntity` + 字段自动填充 `MyMetaObjectHandler`
- [ ] 登录拦截器 `LoginInterceptor` + `WebMvcConfig`
- [ ] AOP：`@OperLog` 操作日志切面、耗时监控切面
- [ ] 建立 GitHub 仓库并推送（见下）

## 六、Git 提交

```bash
git init
git add .
git status        # 确认没有 target/ 和 .idea/
git commit -m "chore: 初始化项目骨架"
git remote add origin https://github.com/<你的用户名>/interastral-peace-mall.git
git branch -M main
git push -u origin main
git checkout -b dev
git push -u origin dev
```

**提交信息规范**：`feat:` 新功能 / `fix:` 修复 / `sql:` 脚本 / `docs:` 文档 / `chore:` 杂项

## 七、分支约定

| 分支 | 用途 |
|---|---|
| `main` | 可交付版本（答辩时用） |
| `dev` | 集成开发分支 |
| `feature/xxx` | 个人功能分支，完成后合入 dev |
