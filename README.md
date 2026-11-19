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
        ├── templates/front/**           # 前台页面与公共片段
        ├── templates/admin/**           # 后台页面
        ├── static/{css,js,img}          # 从 prototype/assets 复制的样式与图标
        └── mapper/                      # 复杂 SQL 的 XML
```

## 三、快速启动

1. **建库**：打开 Navicat → 连上本地 MySQL → 依次执行 `db/schema.sql`、`db/data.sql`
   （`db/init.sql` 是旧版仅建库自检脚本，不再作为开发依据）
2. **改配置**：编辑 `src/main/resources/application.yml` 的 `spring.datasource.username/password`
3. **启动**：IDEA 打开项目目录，等待 Maven 依赖下载完成，运行 `MallApplication`
4. **验证**：
   - 前台首页：<http://localhost:8080/>，登录：<http://localhost:8080/login>，注册：<http://localhost:8080/register>
   - 后台登录：<http://localhost:8080/admin/login>，工作台：<http://localhost:8080/admin>
   - 自检：<http://localhost:8080/ping> 返回 `{"code":200,"message":"操作成功","data":"pong",...}`

### 演示账号

| 端 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 前台 | `xingye` | `PetStore123!` | 正常用户 |
| 前台 | `bob` | `PetStore123!` | 正常用户 |
| 前台 | `disabled` | `PetStore123!` | 禁用账号，登录返回 1004 |
| 后台 | `admin` | `123456` | 管理员 |

## 四、已包含内容

- [x] Maven 骨架 + Spring Boot 3.2.12 依赖齐全（Web/Thymeleaf/Validation/MP/MySQL/BCrypt/Lombok/DevTools）
- [x] 统一返回 `Result` + `ResultCode`
- [x] 全局异常处理（业务异常、参数校验、兜底 500）
- [x] MyBatis-Plus 分页 + 乐观锁插件（`MybatisPlusConfig`）
- [x] `application.yml`（数据源、MyBatis-Plus、上传、日志）
- [x] 12 张表结构、实体与 Mapper（`db/schema.sql` / `db/data.sql`）
- [x] 前台注册、登录、登出、图形验证码、个人中心、修改密码
- [x] 后台登录、退出、`/admin` 工作台占位页
- [x] 前后台登录拦截器、5 分钟账号状态回查、会话隔离
- [x] 认证单元测试（`src/test/java/com/ipc/mall/auth`）

## 五、下一步

- [ ] 前台商品：首页、分类页、列表、详情与 SKU 切换
- [ ] 购物车、收货地址、下单、模拟支付、订单状态机
- [ ] 后台分类、SPU/SKU、用户、订单、发货、操作日志
- [ ] 投诉与反馈（前台提交 + 后台受理/回复/关闭）

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

## 八、素材与版权说明

本项目为 SSM 框架综合实践的**小组课程作业，非商业项目**，不上线售卖、不对外提供服务。

- 界面中的星际和平公司标识（`prototype/assets/ipc-logo-*.png`）由组员提供，版权归米哈游 /《崩坏：星穹铁道》所有；本项目不声称拥有该标识，仅用于课程作业的界面演示。
- 除标识外，插画位、渐变底与图标均为自制或 emoji 占位，未使用其他官方美术资源；项目内不涉及真实商品、交易与资金往来。
- 若仓库公开，建议保留本说明，或将标识文件加入 `.gitignore`，只随作业包提交；对外展示前请替换为自有标识（`prototype/assets/emblem.svg` 为原创兜底徽记）。

细节见 [`prototype/README.md`](prototype/README.md) 的「版权与素材声明」一节。页面本身不显示这些文字，以免影响观感。
