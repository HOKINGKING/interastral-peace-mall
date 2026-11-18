# 星际和平商城 · 产品需求文档（PRD）

> Interastral Peace Mall — Product Requirements Document
> 版本：v1.0（评审稿）
> 日期：2026-11-17
> 范围：用户端商城（前台）+ 后台管理系统（卖家端）
> 关联文档：`README.md`（技术栈与目录）、`db/init.sql`（建表脚本）、`docs/api.md`（接口表，可由本文 §10 导出）
> 读者：开发（前端/后端）、测试、答辩评委

---

## 0. 文档说明

### 0.1 本文覆盖什么

本文是商城项目的需求基准，覆盖两端全部功能：用户端商城（商品、用户、购物车与订单）与后台管理系统（分类与商品、用户、订单、操作日志）。需求按「能开发、能测试、能验收」的颗粒度编写——每个功能有明确的规则与边界，每条验收标准都能独立执行。

章节读法：

| 章节 | 内容 | 什么时候看 |
|---|---|---|
| §1–§4 | 背景、范围、角色、总体设计 | 立项与开发前，先建立全局 |
| §5–§6 | 功能需求（前台 / 后台） | 分模块开发时逐条对照 |
| §7–§8 | 数据模型、订单状态机 | 建表与写 Service 前必须对齐 |
| §9–§11 | 页面路由、接口与错误码、非功能需求 | 前后端联调、联调排错 |
| §12–§14 | 验收标准、里程碑、风险与待评审项 | 提测、答辩、迭代 |
| 附录 | 关键设计决策、答辩脚本、变更记录 | 被问「为什么这么设计」时 |

### 0.2 标记与维护

| 标记 | 含义 |
|---|---|
| 无标记 | 已确定，按此实现 |
| 【P1】 | 加分项，时间够再做，不阻塞验收 |
| 【待评审】 | 需要团队评审后拍板，见 §14.3 |

本文是唯一需求基准：代码与本文不一致时，先评审、再同步（改文档或改代码），不允许两边各说一套。修改流程：评审通过 → 更新附录 C 变更记录 → 同步 `docs/api.md` 与 `db/init.sql`。

### 0.3 术语

| 术语 | 说明 |
|---|---|
| SPU / 商品 | `product` 表，前台展示单位（如「星际和平水杯」） |
| SKU / 单品 | `item` 表，可售单位（如「水杯 · 500ml 白」），库存与价格挂在 SKU |
| 商品类型 | `category`（分类树）+ `product`（SPU）的合称，后台维护的对象 |
| PRG | Post/Redirect/Get，表单提交后重定向，防刷新重复提交 |
| 乐观锁 | `version` 字段 + MyBatis-Plus `@Version`，防库存并发超卖 |

---

## 1. 项目背景与目标

**背景**：SSM 框架综合实践课程设计。仓库已具备 Spring Boot 3.2.12 + Spring MVC + MyBatis-Plus 3.5.5 骨架（统一返回、全局异常、分页与乐观锁插件、Thymeleaf、文件上传配置）。需要在此基础上完成一个**可运行的网上商城**，覆盖「浏览 → 注册登录 → 加购 → 下单 → 支付 → 发货 → 收货」全流程，并配一套卖家后台。

**目标**：

1. **业务闭环**：顾客能走完购物全流程；卖家能维护商品、管理用户、处理并发出订单。
2. **工程规范**：分层清晰（controller / service / mapper / entity），前后台分包隔离，统一返回与统一异常，关键操作有日志。
3. **可讲清楚**：答辩时能讲明白「一个请求怎么走完」「库存为什么不会超卖」「后台和前台为什么不串号」。

**非目标（Out of Scope）**：真实支付、真实物流对接、短信/邮件、优惠券营销、评价体系、多店铺/卖家入驻、财务对账、数据分析报表、多端（小程序/App）。

---

## 2. 范围与优先级

### 2.1 本期交付（P0，进验收）

| 域 | 功能 |
|---|---|
| 前台 · 商品 | 首页、分类页、商品列表（筛选/排序/搜索/分页）、商品详情与 SKU 切换、加购、立即购买 |
| 前台 · 用户 | 注册、登录、图形验证码、登出、个人中心、修改密码 |
| 前台 · 交易 | 购物车（加购/改量/删除/勾选）、收货地址（增删改查/设默认）、结算下单、模拟支付、订单列表/详情/取消/确认收货 |
| 前台 · 售后 | 订单投诉（仅限本人已购买订单）、意见反馈、我的投诉与反馈 |
| 后台 · 商品 | 分类管理（增删改查）、商品管理（增删改查、上下架、主图上传） |
| 后台 · 用户 | 用户列表/详情/修改/禁用/重置密码 |
| 后台 · 订单 | 列表/详情/筛选/修改收货信息/发货（填物流单号）/删除 |
| 后台 · 售后 | 投诉/反馈列表、详情、受理、回复、关闭 |
| 公共 | 前后台登录拦截、会话隔离、统一返回、全局异常、操作日志、参数校验 |

### 2.2 加分项（P1）

1. 列表价格排序（综合 / 价格升 / 价格降）。
2. 超时未支付自动关单（定时任务，默认 30 分钟）。
3. 后台工作台概览（今日订单数、待发货数、在售商品数）。
4. 订单状态时间线可视化。
5. 投诉/反馈支持图片附件（≤3 张，复用图片上传）。
6. 后台工作台显示待处理投诉数。

### 2.3 明确不做（P2）

优惠券、积分、评价、收藏、发票、退款/退货、在线客服/即时聊天、多规格多图相册、批量导入导出、卖家注册与权限分级（后台只有一种角色）。

---

## 3. 用户角色与权限模型

| 角色 | 身份来源 | 权限 |
|---|---|---|
| 游客 | 未登录 | 浏览首页/列表/详情、搜索、注册、登录、取验证码 |
| 注册用户 | 前台 `account`（`status=1`） | 游客全部 + 购物车、地址、下单、支付、订单管理、投诉与反馈、个人中心、登出 |
| 卖家/管理员 | 后台 `admin`（`status=1`） | 后台登录、分类/商品/用户/订单管理、发货、处理投诉与反馈、查看操作日志 |
| 系统 | 定时任务 | 超时关单【P1】、回补库存 |

### 3.1 权限落地

| 项 | 约定 |
|---|---|
| 前台拦截 | `LoginInterceptor` 拦截 `/cart/**`、`/order/**`、`/address/**`、`/feedback/**`、`/user/center`、`/user/password`；放行 `/`、`/product/**`、`/category/**`、`/search`、`/login`、`/register`、`/user/login`、`/user/register`、`/captcha`、`/uploads/**` 及静态资源（`/css/**`、`/js/**`、`/img/**`、`/favicon.ico`） |
| 后台拦截 | `AdminLoginInterceptor` 拦截 `/admin/**`；放行 `/admin/login`、`/admin/captcha`、静态资源 |
| 未登录处理 | **页面请求** → 302 跳登录页并带 `redirect` 参数；**AJAX 请求**（`X-Requested-With: XMLHttpRequest` 或 `Accept: application/json`）→ 返回 `code=401` JSON |
| 会话键 | 前台 `session.LOGIN_ACCOUNT`（存 accountId、username）；后台 `session.LOGIN_ADMIN`（存 adminId、username） |
| 会话隔离 | 两者共用同一个 `JSESSIONID`（Spring 默认一个 HttpSession），**靠不同 key + 不同拦截器路径实现隔离**；同一浏览器同时登录前后台，也互不顶替 |
| 禁用联动 | 登录时校验 `status=1`，禁用账号返回 1004；已在会话中的用户，拦截器中每 5 分钟或每次登录态校验时回查一次状态，失效即清会话【P1 可简化为只在登录时校验】 |
| 越权防护 | 订单/地址/购物车/投诉等用户态操作一律以**会话中的 accountId** 过滤，禁止信任前端传入的 accountId；后台删除/重置密码等高危操作二次确认 |

---

## 4. 系统总体设计

### 4.1 请求链路

```
浏览器
  │  GET /product/list?page=1
  ▼
DispatcherServlet
  ├─ LoginInterceptor（按路径判断是否放行）
  ▼
Controller（front / admin 分包）
  ├─ @Valid 入参校验 → 失败抛 MethodArgumentNotValidException → 全局异常处理
  ▼
Service（事务边界 @Transactional）
  ▼
Mapper（MyBatis-Plus；复杂 SQL 走 resources/mapper/*.xml）
  ▼
MySQL 8.0
  │
  ├─ 页面请求 → 返回视图（Thymeleaf：front/** 或 admin/**）
  └─ 数据请求 → @ResponseBody → Result<T>
```

### 4.2 代码分包

```
com.ipc.mall
├── controller/front   FrontHomeController, ProductController, AccountController,
│                       CartController, AddressController, OrderController, FeedbackController
├── controller/admin   AdminLoginController, AdminCategoryController, AdminProductController,
│                       AdminItemController, AdminAccountController, AdminOrderController,
│                       AdminFeedbackController
├── interceptor        LoginInterceptor, AdminLoginInterceptor
├── aspect             OperLogAspect（@OperLog 注解切面）
├── common/result      Result, ResultCode
├── common/exception   BizException, GlobalExceptionHandler
└── config             MybatisPlusConfig, WebMvcConfig, SchedulingConfig（P1）, UploadConfig
```

### 4.3 统一返回

```json
{ "code": 200, "message": "操作成功", "data": {}, "timestamp": 1763471400000 }
```

- 页面请求不返回此结构，而是视图名或 302；表单成功后 PRG 重定向，失败回填错误信息到 `Model`。
- 数据接口统一 `Result<T>`；分页数据为 `{ records: [], total: 0, current: 1, size: 12 }`。

### 4.4 统一异常与校验

| 异常 | 处理 |
|---|---|
| `BizException(code, message)` | 业务可预期错误，转 `Result` |
| `MethodArgumentNotValidException` | 取第一条字段错误信息，`code=400` |
| `Exception` | 记录 `error` 日志 + 堆栈，返回 `code=500`，不回显堆栈 |

---

## 5. 功能需求 · 前台

### 5.1 首页

| 区块 | 内容 | 数据来源 |
|---|---|---|
| 顶部 | Logo、搜索框、登录/注册入口（已登录显示昵称+下拉）、购物车入口（带数量角标） | 会话 |
| 导航 | 一级分类（最多 8 个，按 `sort` 升序），点击进入分类页 | `category`（`parent_id=0 AND visible=1`） |
| 内容 | 轮播/推荐位（静态可接受）、分类入口、推荐商品（`product.status=1` 按更新时间取 8 条） | `product` + `item` |
| 底部 | 版权、帮助链接、意见反馈入口（静态入口，跳 `/feedback/form`） | — |

> 商品卡片统一展示：主图、名称、最低价、库存状态（有货/售罄）。最低价 = 该 SPU 下所有 SKU 的 `price` 最小值。

### 5.2 分类页

- 路由 `/category/{id}`（`id` 可为一级或二级分类），模板 `front/category/index`；入口为首页分类导航、列表页左侧分类栏、商品详情面包屑。
- 展示：
  - 落在一级分类：顶部列出该分类下全部二级分类入口（按 `sort` 升序），下方为该一级分类**含全部子分类**的商品；
  - 落在二级分类：展示同级二级分类切换 + 当前分类商品，面包屑为 `首页 › 一级 › 二级`；
  - 商品区复用商品卡（主图、名称、最低价、库存状态），分页 `page`/`size`（默认 12）。
- 规则：
  - `id` 不存在或已逻辑删除 → 404 页面；
  - `category.visible=0` → 302 到 `/product/list` 并提示「该分类已下架」；
  - 分类下无商品 → 空状态「该分类暂无商品」+ 推荐其他分类入口，不报错；
  - 分类页只读，不提供加购（加购在详情页完成）。

### 5.3 商品列表

- 入口：分类页「查看全部」、首页「查看更多」、搜索框、面包屑。
- 筛选：`categoryId`（含其子分类）、`keyword`（商品名/描述模糊匹配）。
- 搜索：关键词匹配 `product.name`、`product.descn`；空关键词回到全量；关键词 trim + 长度 ≤ 50。`/search` 与 `/product/list` 共用同一视图与同一套筛选逻辑。
- 分页：`page` 默认 1，`size` 默认 12，上限 60；返回总数。
- 排序：默认 `product.update_time DESC`；【P1】支持 `priceAsc`/`priceDesc`（按最低价）。
- 展示：主图、名称、最低价、库存状态、分类名。
- 规则：仅展示 `product.status=1` 且所属 `category.visible=1`；无 SKU 的 SPU 不展示。

### 5.4 商品详情与 SKU 切换

- 展示：主图、名称、品牌、短描述、详情（可为空）、规格列表、价格（随规格变化）、库存、分类面包屑（`首页 › 一级 › 二级`，各级可点击；分类失效时退化为 `首页 › 商品列表`）。
- SKU 切换：
  1. 页面一次性下发该 SPU 的 SKU 列表，切换规格时通过 `GET /product/{id}/skus` 局部刷新价格/库存/按钮状态，**不整页刷新、URL 不变**；
  2. 默认选中第一个 `stock>0` 的 SKU；全部售罄时选中第一个并禁用购买；
  3. 已选 SKU 售罄或下架时保留选中项并标红提示，不允许加购；
  4. 数量上限随所选 SKU 变化：`1 ~ min(stock, 99)`。
- 交互：
  1. 「加入购物车」→ 未登录跳登录（带 redirect）；
  2. 「立即购买」→ 直接进入确认订单页（不落购物车）。
- 规则：`product.status=0` 或全部 SKU `stock=0` → 显示「已下架 / 已售罄」，禁用加购与购买；**下架商品详情页仍可访问**（给出提示），避免用户收藏链接 404。

### 5.5 注册

| 字段 | 校验 |
|---|---|
| 用户名 | 必填，4–20 位字母/数字/下划线，唯一 |
| 密码 | 必填，6–20 位，须含字母与数字 |
| 确认密码 | 与密码一致 |
| 手机号 | 选填，11 位，格式校验 |
| 图形验证码 | 必填，4 位，忽略大小写，5 分钟有效，校验后立即失效 |

- 密码 BCrypt 加密（`BCryptPasswordEncoder`，默认强度 10）后入库。
- 用户名重复返回 `1001`；成功自动登录并跳首页。
- 注册入口在登录页与首页顶部。

### 5.6 登录 / 登出

- 输入：用户名 + 密码 + 图形验证码。
- 失败原因需**区分**：验证码错误（1003）/ 用户不存在或密码错误（1002，不区分两者，防用户名枚举）/ 账号被禁用（1004）。
- 成功：写 `session.LOGIN_ACCOUNT`，更新 `account.last_login_time`，跳转 `redirect` 参数目标或首页。
- 登出：`POST /user/logout`（GET 不做状态变更），清会话，重定向首页。

### 5.7 验证码

- 接口：`GET /captcha`（前台）、`GET /admin/captcha`（后台），返回 `image/png`，响应头 `Cache-Control: no-store`。
- 实现：Java2D `BufferedImage` 手写（**不新增依赖**），4 位字符（去除易混淆的 `0O1lI`），干扰线 3 条、噪点 30 个。
- 存储：`session.CAPTCHA_CODE` / `session.CAPTCHA_EXPIRE_AT`（后台用 `ADMIN_` 前缀独立 key，避免同浏览器前后台互相覆盖）。
- 规则：5 分钟有效；校验**不区分大小写**；**校验一次即失效**（无论成败），失败后必须点图刷新；前端点击图片刷新（加时间戳参数防缓存）。

> 为什么不引 Hutool/Kaptcha：`pom.xml` 已固定依赖，手写 40 行即可，答辩时能讲清"验证码怎么生成、怎么校验、为什么一次性"。若要引入，只需在 pom 加 `hutool-captcha`，接口契约不变。

### 5.8 个人中心

- 展示：用户名、昵称、手机号（中间 4 位打码）、注册时间、最后登录时间。
- 修改密码：原密码 + 新密码 + 确认新密码；成功后清会话强制重登（安全默认）。
- 【P1】修改昵称/手机号。
- 不上传头像（非目标）。

### 5.9 购物车

- 仅登录可用；入口顶部角标显示勾选/全部数量。
- 操作：加入（SKU + 数量）、修改数量（1–99 且 ≤ 库存）、删除（逻辑删除）、单选/全选、进入结算。
- 加购规则：
  - 同 SKU 已存在（`deleted=0`）→ 数量累加，上限 99 与库存；
  - **已逻辑删除的同 SKU 记录 → 复活该行**（`deleted=0`），不新增行（避免 `(account_id,item_id)` 唯一索引冲突，见 §7.5）；
  - `product.status=0` 或库存不足 → 拒绝加购并提示。
- 展示：主图、名称、规格、单价、数量、小计、库存状态；失效商品（下架/删除）置灰且不参与结算。
- 合计：仅统计勾选项。

### 5.10 收货地址

- 字段：收货人（≤20）、手机号（11 位）、省/市/区（三级文本或下拉）、详细地址（≤100）、是否默认。
- 操作：新增、编辑、删除（逻辑删除）、设为默认。
- 规则：首个地址自动为默认；设默认时同事务内把该用户其他地址 `is_default=0`；删除默认地址后不自动补选；地址数量上限 20。
- 校验：所有操作以 `account_id` 限定，越权返回 `3004`。

### 5.11 结算下单

1. 入口：购物车勾选 → 结算；或详情页「立即购买」。
2. 确认页展示：商品明细（图、名、规格、单价、数量、小计）、收货地址（可切换/新增）、运费（本期固定 0，字段落库 `freight_amount`）、应付总额。
3. 提交规则（**按顺序校验，任一失败即中止**）：

| # | 校验 | 失败码 |
|---|---|---|
| 1 | 已选商品非空 | 3005 |
| 2 | 地址存在且属于当前用户 | 3004 |
| 3 | 每个 SKU 仍 `status=1` 且 `stock ≥ quantity` | 2001 / 2002 |
| 4 | 单价以**数据库当前价**为准，忽略页面传值 | — |
| 5 | 防重复提交（见下） | 3006 |

4. 落库（单事务）：
   - 生成 `orders`（状态 `PENDING_PAYMENT`，收货信息**快照**，`order_no` 规则见 §7.3）；
   - 生成 `order_item`（名称/规格/单价/数量/图片**快照**）；
   - 扣减库存：`UPDATE item SET stock = stock - #{qty}, version = version + 1 WHERE id = ? AND stock >= #{qty} AND version = #{version}`，受影响行数为 0 即失败回滚；
   - 清空购物车中已下单的勾选项（逻辑删除）。
5. 成功后重定向到支付页 `/order/{id}/pay`。

**防重复提交**：确认页进入时生成一次性 `submitToken`（UUID）存 Session 并放入隐藏域；提交时比对并立即移除；重复提交返回 3006 并跳订单列表。同时 `order_no` 建唯一索引兜底。

**并发与重试**：乐观锁冲突（受影响行数 0）时**不自动重试**，直接返回「库存不足/已被抢购」，保证同一 SKU 不超卖；库存在下单时扣减（而非支付时），超时关单再回补。

### 5.12 模拟支付

- 页面：订单号、应付金额、倒计时（可选）、「模拟支付成功」「取消支付」两个按钮。
- 支付成功：`POST /order/{id}/pay` → 校验 `status=PENDING_PAYMENT` 且订单属于当前用户 → 置 `PAID`、写 `pay_time`。
- 取消：`POST /order/{id}/cancel` → 状态置 `CANCELLED`、写 `cancel_time`、**回补库存**。
- 幂等：重复点击支付，若已是 `PAID` 则直接跳成功页（不重复写时间戳）。

### 5.13 订单管理

| 功能 | 规则 |
|---|---|
| 订单列表 | 按状态 Tab（全部/待支付/已支付/已发货/已完成/已取消）筛选；分页；展示订单号、下单时间、首件商品图+名称、件数、总额、状态、操作按钮 |
| 订单详情 | 商品明细、收货信息、金额（商品总额 + 运费 = 应付）、状态时间线（下单/支付/发货/完成/取消，各自取对应时间戳）、物流公司 + 单号（已发货后显示） |
| 去支付 | 仅 `PENDING_PAYMENT` |
| 取消订单 | 仅 `PENDING_PAYMENT`；二次确认；回补库存 |
| 确认收货 | 仅 `SHIPPED`；二次确认；置 `COMPLETED`、写 `finish_time` |
| 投诉 | 仅 `PAID`/`SHIPPED`/`COMPLETED` 显示入口，跳 `/feedback/form?orderId={id}`；其余状态不显示，接口侧同样校验（§5.15） |
| 越权 | 所有操作校验 `account_id`，不匹配返回 3003 |

### 5.14 超时关单（系统行为）【P1】

- 定时任务每分钟扫描 `status=PENDING_PAYMENT AND create_time < now() - 30min`。
- 逐单处理：条件更新 `UPDATE orders SET status='CANCELLED' WHERE id=? AND status='PENDING_PAYMENT'`，受影响行数为 1 才回补库存（**幂等**，防与用户手动取消并发重复回补）。
### 5.15 投诉与反馈

| 类型 | 入口 | 是否绑订单 |
|---|---|---|
| 投诉 | 订单详情页「投诉」按钮（仅 `PAID` / `SHIPPED` / `COMPLETED` 订单显示） | 必须绑定该订单 |
| 反馈 | 个人中心「意见反馈」、页脚「意见反馈」 | 不绑订单 |

- 两者共用一张 `feedback` 表与一套状态机（§8.4），用 `type` 区分；同一张表单，`orderId` 有值即为投诉。
- **投诉只能投诉自己的订单**：以会话中的 `accountId` 校验订单归属，禁止信任前端传入的订单号。

**提交规则**

| 项 | 投诉 | 反馈 |
|---|---|---|
| 登录 | 必须 | 必须 |
| 订单校验 | 必须是本人订单，且状态 ∈ {`PAID`,`SHIPPED`,`COMPLETED`}；否则 3007 | — |
| 重复限制 | 同一订单只允许一条未关闭的投诉（`PENDING`/`PROCESSING`/`REPLIED`），否则 3008 | 同一用户 1 小时内最多 5 条，超限 3009 |
| 分类 | 必填：商品质量 / 物流发货 / 服务态度 / 其他 | 选填，枚举同上 |
| 内容 | 10–500 字，纯文本 | 5–500 字，纯文本 |
| 联系方式 | 选填，≤50 字 | 选填 |
| 图片 | 【P1】≤3 张 | 【P1】≤3 张 |

**提交与查看**

- 提交成功 → PRG 重定向到「我的反馈」，提示「已提交，卖家会尽快处理」。
- 我的反馈（`/feedback/list`）：Tab（全部 / 投诉 / 反馈）+ 状态筛选；展示类型、关联订单号、内容摘要（前 30 字）、状态、提交时间、是否已回复。
- 详情（`/feedback/{id}`）：完整内容、订单摘要（投诉时，可跳订单详情）、状态时间线、卖家回复及时间。
- 越权：非本人记录一律返回 404，不暴露记录是否存在。
- **撤销**：仅 `PENDING` 可撤销 → `CLOSED`，`close_reason='用户撤销'`；卖家受理后不可撤销。

---

## 6. 功能需求 · 后台

### 6.1 登录与工作台

- 登录：`admin` 表账号（系统内置，不开放注册），用户名 + 密码 + 验证码；成功写 `session.LOGIN_ADMIN`。
- 密码 BCrypt；初始化账号由 `init.sql` 插入（`admin / 123456` 的 BCrypt 密文），首次登录后建议改密码【P1】。
- 工作台：左侧菜单（商品/用户/订单/日志）+ 顶部（当前卖家、退出）+ 【P1】概览卡片（今日订单、待发货、在售商品、注册用户）。

### 6.2 分类管理（Category）

- 两级结构：`parent_id=0` 为一级分类，其余为二级分类（两级足以支撑导航与筛选，见附录 A · D-1）。
- 列表：树形或分组展示，支持按名称搜索。
- 新增/修改：名称（唯一，≤30）、描述、排序、是否前台可见（`visible`）。
- 删除：逻辑删除；**存在子分类或下属商品时禁止删除**（返回 2004）。
- 一级分类删除前须先清空二级分类与商品。

### 6.3 商品管理（Product / Item）

商品由「SPU + 若干 SKU」组成，后台两个页面：SPU 列表、SKU 编辑。

**SPU（`product`）**

| 功能 | 规则 |
|---|---|
| 列表 | 分页；按名称/所属分类/上下架状态筛选；展示主图、名称、分类、最低价、SKU 数、总库存、状态、更新时间 |
| 新增/修改 | 所属分类（须为二级分类）、名称（≤60）、品牌（≤30）、短描述（≤200）、详情（富文本/纯文本均可）、主图、状态 |
| 上下架 | 切换 `status`（1 上架 / 0 下架）。下架后前台列表不再出现、详情页显示已下架、不可加购 |
| 删除 | 逻辑删除；**存在未完结订单（PENDING_PAYMENT/PAID/SHIPPED）引用其 SKU 时禁止删除**（返回 2005），防止订单详情丢失商品信息（订单已存快照，此为业务保守策略） |
| 主图上传 | `POST /admin/upload/image`，jpg/png/webp，≤5MB，UUID 重命名，返回可访问 URL（`/uploads/**`） |

**SKU（`item`）**

| 功能 | 规则 |
|---|---|
| 列表 | 按 SPU 查看；展示规格、价格、库存、版本 |
| 新增/修改 | 规格（≤50，同 SPU 内唯一）、价格（0.01–99999.99）、库存（0–999999）、图片（可选，缺省用 SPU 主图） |
| 删除 | 逻辑删除；**不允许删掉 SPU 下的最后一条 SKU**（每个在售 SPU 至少保留 1 个可售 SKU） |
| 改库存 | 允许直接改（正向补货）；后台改库存不走乐观锁重试，但需校验 `version` 一致，冲突提示刷新 |

> 上架状态只放 SPU 一层（附录 A · D-2）；SKU 无 `status`，售罄用 `stock=0` 表达。成本价 `cost` 不做（财务对账为非目标）。

### 6.4 用户管理（Account）

| 功能 | 规则 |
|---|---|
| 列表 | 分页；按用户名/手机号搜索；展示 ID、用户名、昵称、手机号（打码）、状态、注册时间 |
| 详情 | 完整信息 + 订单数；手机号中间 4 位打码 |
| 修改 | 昵称、手机号、状态（启用/禁用） |
| 禁用 | `status=0` 后该用户**无法登录前台**，已登录会话在下次校验时失效；已产生的订单不受影响 |
| 重置密码 | 卖家输入新密码（或生成随机 8 位）→ BCrypt 入库；成功后该用户全部会话不强制失效（课程项目从简），但旧密码不可再登录 |
| 越权 | 后台不做数据行级隔离（只有一种角色） |

### 6.5 订单管理与发货

| 功能 | 规则 |
|---|---|
| 列表 | 分页；按订单号/用户名/状态/下单时间区间筛选；展示订单号、用户、金额、状态、下单时间 |
| 详情 | 订单信息 + 商品明细 + 收货信息 + 状态时间线 + 操作按钮 |
| 修改 | 仅允许修改**收货信息**（收货人/电话/地址）与备注；订单金额与商品明细**锁定快照，不可改**（附录 A · D-4） |
| 发货 | 仅 `PAID` 可发货：填写物流公司（≤30）+ 物流单号（≤40）→ 状态置 `SHIPPED`、写 `ship_time`、存 `courier_company`/`tracking_no`；重复发货返回 3002 |
| 删除 | 逻辑删除；**仅 `CANCELLED` / `COMPLETED` 可删**（附录 A · D-5）；二次确认；删除不回补库存（已取消订单早已回补） |
| 越权 | 无行级权限，但所有变更记操作日志 |

### 6.6 投诉与反馈管理

| 功能 | 规则 |
|---|---|
| 列表 | 分页；按类型/状态/时间区间/订单号/用户名筛选；排序：`PENDING` 优先，其次按提交时间倒序 |
| 详情 | 内容、联系方式、关联订单摘要（可跳订单详情）、状态时间线 |
| 受理 | `PENDING` → `PROCESSING`；重复受理返回 3002 |
| 回复 | `PENDING`/`PROCESSING` 可回复，回复内容 1–500 字必填 → `REPLIED`，写 `reply`、`reply_admin_id`、`reply_time` |
| 关闭 | 未关闭状态均可关闭，填原因（≤100）→ `CLOSED`；关闭后用户可对同一订单重新投诉 |
| 权限 | 单一角色，不做行级隔离；受理/回复/关闭均记操作日志（模块「售后管理」） |
| 【P1】 | 工作台「待处理投诉」计数；超过 24 小时未受理标红 |

### 6.7 操作日志（`sys_oper_log`）

- 实现：`@OperLog(module="订单管理", action="发货")` 注解 + AOP 切面，拦截 Controller 方法。
- 记录：`admin_id`、`admin_name`、`module`、`action`、`target_id`、`request_uri`、`request_method`、`request_params`（脱敏：password 替换为 `***`）、`ip`、`success`、`error_msg`、`cost_ms`、`create_time`。
- 覆盖范围：后台所有写操作（新增/修改/删除/上下架/发货/重置密码/禁用）+ 前后台登录（`success=false` 也记）。
- 查询：后台「日志」页分页展示，按模块/操作人/时间筛选。
- 异常不阻断主流程：切面内 try-catch，日志失败只打 `warn`。

---

## 7. 数据模型

### 7.1 通用约定

| 约定 | 说明 |
|---|---|
| 主键 | `id BIGINT AUTO_INCREMENT`，MP `id-type: auto` |
| 公共字段 | `deleted TINYINT DEFAULT 0`（逻辑删除，MP 全局配置）、`create_time`、`update_time`（`MyMetaObjectHandler` 自动填充） |
| 并发字段 | `version INT DEFAULT 0`，仅 `item` 使用 `@Version` |
| 金额 | `DECIMAL(10,2)`，禁止 `double`/`float` |
| 命名 | 表名 snake_case 单数（`orders` 保留复数，避 MySQL 关键字）；字段 snake_case；Java 驼峰 |
| 字符集 | `utf8mb4` / `utf8mb4_general_ci`，InnoDB |
| 时间 | `DATETIME`，`serverTimezone=Asia/Shanghai` |
| 逻辑删除与唯一索引 | 唯一索引一律带上 `deleted` 或改用「含已删除记录的应用层查重」，见 §7.5 |

### 7.2 表清单（10 张业务表 + 2 张辅助表）

| # | 表名 | 说明 | 归属 |
|---|---|---|---|
| 1 | `account` | 前台用户 | 前台 |
| 2 | `category` | 商品分类（两级，`parent_id` 自关联） | 公共 |
| 3 | `product` | 商品 SPU | 公共 |
| 4 | `item` | 商品 SKU（价格/库存/乐观锁） | 公共 |
| 5 | `cart` | 购物车 | 前台 |
| 6 | `orders` | 订单主表（含收货/物流快照） | 公共 |
| 7 | `order_item` | 订单明细（商品快照） | 公共 |
| 8 | `address` | 收货地址 | 前台 |
| 9 | `admin` | 后台卖家账号 | 后台 |
| 10 | `feedback` | 投诉与反馈（`type` 区分，投诉绑订单） | 公共 |
| 11 | `sys_oper_log` | 操作日志（审计） | 公共 |
| 12 | `sys_check` | 环境自检（已存在） | 辅助 |

> 与 README「10 张业务表」一致（不含 `sys_oper_log`、`sys_check`）。建表顺序：`account`、`admin`、`category` → `product` → `item` → `cart`、`address` → `orders` → `order_item` → `feedback` → `sys_oper_log`。

### 7.3 字段字典

**account（前台用户）**

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | — |
| username | VARCHAR(20) | NOT NULL, UNIQUE(`username`,`deleted`) | 登录名 |
| password | VARCHAR(60) | NOT NULL | BCrypt 密文（定长 60） |
| nickname | VARCHAR(20) | NULL | 昵称，缺省取 username |
| phone | VARCHAR(11) | NULL | 手机号 |
| status | TINYINT | NOT NULL DEFAULT 1 | 1 启用 / 0 禁用 |
| last_login_time | DATETIME | NULL | — |
| deleted / create_time / update_time | — | 见 §7.1 | — |

**admin（后台账号）**

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id / username / password | — | 同 account（username 唯一） | — |
| nickname | VARCHAR(20) | NULL | — |
| status | TINYINT | DEFAULT 1 | 1 启用 |
| last_login_time | DATETIME | NULL | — |
| deleted / create_time / update_time | — | — | — |

**category（分类）**

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | — |
| parent_id | BIGINT | NOT NULL DEFAULT 0 | 0=一级；否则指向一级 id |
| name | VARCHAR(30) | NOT NULL | 同级内唯一（应用层校验） |
| descn | VARCHAR(200) | NULL | 描述 |
| sort | INT | DEFAULT 0 | 升序排列 |
| visible | TINYINT | DEFAULT 1 | 1 前台可见 |
| deleted / create_time / update_time | — | — | — |

**product（SPU）**

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | — |
| category_id | BIGINT | NOT NULL, INDEX | 二级分类 |
| name | VARCHAR(60) | NOT NULL, INDEX | 商品名（搜索用） |
| brand | VARCHAR(30) | NULL | 品牌 |
| descn | VARCHAR(200) | NULL | 短描述（列表/详情顶部） |
| detail | TEXT | NULL | 详情（可空） |
| main_image | VARCHAR(255) | NULL | 主图 URL |
| status | TINYINT | DEFAULT 1 | 1 上架 / 0 下架 |
| deleted / create_time / update_time | — | — | `update_time` 用于默认排序 |

**item（SKU）**

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | — |
| product_id | BIGINT | NOT NULL, INDEX | 所属 SPU |
| spec | VARCHAR(50) | NOT NULL | 规格名，如「500ml 白」；同 SPU 内唯一 |
| price | DECIMAL(10,2) | NOT NULL | 售价 |
| stock | INT | NOT NULL DEFAULT 0 | 库存，`CHECK (stock >= 0)`（应用层保证） |
| image | VARCHAR(255) | NULL | 规格图，空则用 SPU 主图 |
| version | INT | NOT NULL DEFAULT 0 | 乐观锁 |
| deleted / create_time / update_time | — | — | — |

**cart（购物车）**

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | — |
| account_id | BIGINT | NOT NULL, INDEX | — |
| item_id | BIGINT | NOT NULL | — |
| quantity | INT | NOT NULL DEFAULT 1 | 1–99 |
| deleted / create_time / update_time | — | — | 逻辑删除即「移出购物车」 |

**address（收货地址）**

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | — |
| account_id | BIGINT | NOT NULL, INDEX | — |
| receiver | VARCHAR(20) | NOT NULL | 收货人 |
| phone | VARCHAR(11) | NOT NULL | — |
| province / city / district | VARCHAR(20) | NULL | 省/市/区 |
| detail | VARCHAR(100) | NOT NULL | 详细地址 |
| is_default | TINYINT | DEFAULT 0 | 1 默认 |
| deleted / create_time / update_time | — | — | — |

**orders（订单）**

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | — |
| order_no | VARCHAR(32) | NOT NULL, UNIQUE | `yyyyMMddHHmmss` + 4 位随机数，冲突重试 3 次 |
| account_id | BIGINT | NOT NULL, INDEX | 下单用户 |
| total_amount | DECIMAL(10,2) | NOT NULL | 应付 = 商品总额 + 运费 |
| freight_amount | DECIMAL(10,2) | DEFAULT 0.00 | 运费，本期固定 0 |
| status | VARCHAR(20) | NOT NULL, INDEX | 见 §8（存枚举名，不存数字） |
| receiver_name | VARCHAR(20) | NOT NULL | 收货快照 |
| receiver_phone | VARCHAR(11) | NOT NULL | 快照 |
| receiver_province / receiver_city / receiver_district | VARCHAR(20) | NULL | 快照 |
| receiver_detail | VARCHAR(100) | NOT NULL | 快照 |
| courier_company | VARCHAR(30) | NULL | 物流公司（发货写入） |
| tracking_no | VARCHAR(40) | NULL | 物流单号 |
| remark | VARCHAR(200) | NULL | 订单备注/后台备注 |
| pay_time / ship_time / finish_time / cancel_time | DATETIME | NULL | 状态时间线 |
| deleted / create_time / update_time | — | — | — |

**order_item（订单明细，全快照）**

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | — |
| order_id | BIGINT | NOT NULL, INDEX | — |
| item_id | BIGINT | NOT NULL | 原 SKU id（仅追溯，不参与展示逻辑） |
| product_name | VARCHAR(60) | NOT NULL | 快照 |
| spec | VARCHAR(50) | NOT NULL | 快照 |
| price | DECIMAL(10,2) | NOT NULL | 下单时单价快照 |
| quantity | INT | NOT NULL | — |
| image | VARCHAR(255) | NULL | 快照，订单列表展示用 |
| create_time | DATETIME | NOT NULL | 无 update/delete（明细随主单） |

**feedback（投诉与反馈）**

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | — |
| type | VARCHAR(20) | NOT NULL | `COMPLAINT` 投诉 / `FEEDBACK` 反馈 |
| account_id | BIGINT | NOT NULL, INDEX | 提交人 |
| order_id | BIGINT | NULL, INDEX | 投诉必填，反馈为 `NULL` |
| category | VARCHAR(20) | NULL | `QUALITY` / `LOGISTICS` / `SERVICE` / `OTHER` |
| content | VARCHAR(500) | NOT NULL | 正文，纯文本 |
| images | VARCHAR(500) | NULL | 【P1】逗号分隔 URL，≤3 张 |
| contact | VARCHAR(50) | NULL | 联系方式（选填） |
| status | VARCHAR(20) | NOT NULL, INDEX | 见 §8.4 |
| reply | VARCHAR(500) | NULL | 卖家回复 |
| reply_admin_id | BIGINT | NULL | 回复人 |
| reply_time | DATETIME | NULL | 回复时间 |
| close_reason | VARCHAR(100) | NULL | 用户撤销 / 卖家关闭原因 |
| deleted / create_time / update_time | — | — | — |

**sys_oper_log（操作日志）**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | PK |
| admin_id / admin_name | BIGINT / VARCHAR(20) | 操作人（前台登录日志时 admin_id=0，admin_name=用户名） |
| module / action | VARCHAR(30) / VARCHAR(30) | 如「订单管理 / 发货」 |
| target_id | BIGINT | 目标主键 |
| request_uri / request_method | VARCHAR(200) / VARCHAR(10) | — |
| request_params | TEXT | JSON，password 等脱敏 |
| ip | VARCHAR(45) | 支持 IPv6 |
| success | TINYINT | 1/0 |
| error_msg | VARCHAR(500) | 失败原因 |
| cost_ms | INT | 耗时 |
| create_time | DATETIME | 无 `update_time`/`deleted` |

### 7.4 索引

| 表 | 索引 | 目的 |
|---|---|---|
| account | UNIQUE(`username`,`deleted`) | 登录唯一（逻辑删除下允许同名重建，见 §7.5） |
| admin | UNIQUE(`username`,`deleted`) | 同上 |
| product | (`category_id`,`status`)、(`update_time`) | 列表筛选 + 默认排序 |
| product | FULLTEXT 或普通 INDEX(`name`) | 关键词搜索（数据量小用 `LIKE '%kw%'` 即可） |
| item | (`product_id`) | 详情页取 SKU |
| cart | (`account_id`,`item_id`) | 加购查重（非唯一，见 §7.5） |
| orders | UNIQUE(`order_no`)、(`account_id`,`status`,`create_time`) | 幂等 + 我的订单筛选 |
| order_item | (`order_id`) | 详情/列表聚合 |
| feedback | (`account_id`,`create_time`)、(`order_id`)、(`type`,`status`) | 我的反馈 / 按订单查重 / 后台筛选 |
| sys_oper_log | (`admin_id`,`create_time`)、(`module`) | 后台日志页 |

### 7.5 逻辑删除与唯一性的两个坑

1. **唯一索引 + 逻辑删除**：若 `(username)` 单列唯一，用户注销后无法再注册同名账号。方案：唯一索引建在 `(username, deleted)`；但逻辑删除多次会重复 `deleted=1` 冲突 → 约定**同一用户名只允许存在 1 条 `deleted=1` 记录**，删除时若已有同名已删记录则物理删除旧记录（或把 `deleted` 设为该行 id 而非 1）。本项目取**最简方案**：`deleted` 只存 0/1，删除前先清理同名历史已删记录。
2. **购物车复活**：`cart` 不建唯一索引，加购时按 `(account_id, item_id)` 查**含已删除**的记录：存在则 `deleted=0` 并累加数量，避免重复行导致合计错误。

---

## 8. 订单状态机（唯一权威定义）

> 订单必须经过卖家发货这一环：若省掉 `SHIPPED`，发货动作与物流单号将无处落地，前台也无从展示物流信息。

### 8.1 状态枚举

| 状态 | 代码值（存库） | 说明 | 终态 |
|---|---|---|---|
| 待支付 | `PENDING_PAYMENT` | 下单成功，未支付 | 否 |
| 已支付 | `PAID` | 支付成功，待卖家发货 | 否 |
| 已发货 | `SHIPPED` | 卖家已填物流单号 | 否 |
| 已完成 | `COMPLETED` | 用户确认收货 | 是 |
| 已取消 | `CANCELLED` | 用户取消 / 超时未付 | 是 |

### 8.2 转移表

```
待支付 ──支付成功──▶ 已支付 ──卖家发货──▶ 已发货 ──确认收货──▶ 已完成
   │
   └──用户取消 / 超时──▶ 已取消（终态）
```

| # | 转移 | 触发 | 角色 | 前置校验 | 副作用 |
|---|---|---|---|---|---|
| T1 | — → `PENDING_PAYMENT` | 提交订单 | 用户 | 库存充足、地址归属本人、Token 未复用 | 扣库存、写明细快照 |
| T2 | `PENDING_PAYMENT` → `PAID` | 模拟支付成功 | 用户 | 订单归属本人 | 写 `pay_time` |
| T3 | `PENDING_PAYMENT` → `CANCELLED` | 取消支付 / 用户取消 | 用户 | 归属本人 | 回补库存、写 `cancel_time` |
| T4 | `PENDING_PAYMENT` → `CANCELLED` | 超时关单【P1】 | 系统 | 超 30 分钟 | 条件更新成功才回补库存 |
| T5 | `PAID` → `SHIPPED` | 卖家发货 | 卖家 | 已填物流公司+单号 | 写 `ship_time`、`courier_company`、`tracking_no` |
| T6 | `SHIPPED` → `COMPLETED` | 确认收货 | 用户 | 归属本人 | 写 `finish_time` |
| T7 | `CANCELLED`/`COMPLETED` → 逻辑删除 | 后台删除 | 卖家 | 状态为终态 | 写 `deleted=1`（不回补库存） |

### 8.3 约束

1. 状态只能沿上图方向流转，**禁止跳级**（如 `PAID` 直接确认收货 → 3002）。
2. `CANCELLED` 只能从 `PENDING_PAYMENT` 进入；`COMPLETED` 只能从 `SHIPPED` 进入。
3. 所有流转由 `OrderStatus` 枚举 + `canTransitTo(target)` 校验方法统一控制，**禁止直接 set 状态字符串**，禁止在 SQL 里随意 `UPDATE status`（除 T4 的条件更新）。
4. 每次流转用**条件更新**保证幂等：`UPDATE orders SET status=#{to} WHERE id=#{id} AND status=#{from}`，受影响行数 0 则返回 3002。
5. 取消/关单必须回补库存，且**只回补一次**（依赖条件更新成功与否判断）。

### 8.4 投诉与反馈状态机

| 状态 | 代码值 | 说明 | 终态 |
|---|---|---|---|
| 待处理 | `PENDING` | 用户已提交，卖家未受理 | 否 |
| 处理中 | `PROCESSING` | 卖家已受理 | 否 |
| 已回复 | `REPLIED` | 卖家已回复，等用户确认 | 否 |
| 已关闭 | `CLOSED` | 用户撤销 / 卖家关闭 | 是 |

```
待处理 ──卖家受理──▶ 处理中 ──卖家回复──▶ 已回复 ──卖家关闭──▶ 已关闭（终态）
   │                    │                    │
   └──用户撤销──────────┴────卖家关闭────────┴──▶ 已关闭（终态）
```

| # | 转移 | 触发 | 角色 | 前置 | 副作用 |
|---|---|---|---|---|---|
| C1 | — → `PENDING` | 提交投诉/反馈 | 用户 | 见 §5.15 提交规则 | 写 `create_time` |
| C2 | `PENDING` → `PROCESSING` | 受理 | 卖家 | — | 记录受理人 |
| C3 | `PENDING`/`PROCESSING` → `REPLIED` | 回复 | 卖家 | 回复内容非空 | 写 `reply`、`reply_admin_id`、`reply_time` |
| C4 | `PENDING` → `CLOSED` | 撤销 | 用户 | 仅 `PENDING` | 写 `close_reason='用户撤销'` |
| C5 | 未关闭 → `CLOSED` | 关闭 | 卖家 | 填关闭原因 | 写 `close_reason` |

约束：`CLOSED` 为终态不可回退；所有流转用条件更新（`WHERE id=? AND status=?`），影响行数为 0 返回 3002；`REPLIED` 不等于办结，仍需卖家关闭才进入终态（保证后台有明确的收口动作）。

---

## 9. 页面与路由

### 9.1 前台（`templates/front/**`，共享 header + nav + footer）

| 页面 | 模板 | 路由 | 登录 |
|---|---|---|---|
| 首页 | `front/index` | `/` | 否 |
| 分类页 | `front/category/index` | `/category/{id}` | 否 |
| 商品列表 | `front/product/list` | `/product/list` | 否 |
| 商品详情 | `front/product/detail` | `/product/{id}` | 否 |
| 搜索结果 | 复用列表 | `/search?keyword=` | 否 |
| 登录 | `front/login` | `/login` | 否 |
| 注册 | `front/register` | `/register` | 否 |
| 个人中心 | `front/user/center` | `/user/center` | 是 |
| 修改密码 | `front/user/password` | `/user/password` | 是 |
| 购物车 | `front/cart/list` | `/cart` | 是 |
| 确认订单 | `front/order/checkout` | `/order/checkout` | 是 |
| 支付页 | `front/order/pay` | `/order/{id}/pay` | 是 |
| 订单列表 | `front/order/list` | `/order/list` | 是 |
| 订单详情 | `front/order/detail` | `/order/{id}` | 是 |
| 我的反馈 | `front/feedback/list` | `/feedback/list` | 是 |
| 投诉/反馈表单 | `front/feedback/form` | `/feedback/form`（`orderId` 有值为投诉） | 是 |
| 反馈详情 | `front/feedback/detail` | `/feedback/{id}` | 是 |
| 地址列表 | `front/address/list` | `/address/list` | 是 |
| 地址编辑 | `front/address/form` | `/address/form`（新增）/`/address/form/{id}`（编辑） | 是 |

### 9.2 后台（`templates/admin/**`，独立侧边栏布局）

| 页面 | 模板 | 路由 | 登录 |
|---|---|---|---|
| 后台登录 | `admin/login` | `/admin/login` | 否 |
| 工作台 | `admin/index` | `/admin` | 是 |
| 分类列表/编辑 | `admin/category/list`、`admin/category/form` | `/admin/category` | 是 |
| SPU 列表/编辑 | `admin/product/list`、`admin/product/form` | `/admin/product` | 是 |
| SKU 列表/编辑 | `admin/item/list`、`admin/item/form` | `/admin/item` | 是 |
| 用户列表/编辑 | `admin/account/list`、`admin/account/form` | `/admin/account` | 是 |
| 订单列表/详情 | `admin/order/list`、`admin/order/detail` | `/admin/order` | 是 |
| 投诉列表/详情 | `admin/feedback/list`、`admin/feedback/detail` | `/admin/feedback` | 是 |
| 操作日志 | `admin/log/list` | `/admin/log` | 是 |

> `templates/index.html` 占位页在 M3 迁移为 `front/index`，根路径 `/` 指向新首页。

---

## 10. 接口约定

### 10.1 统一规范

- 路径：前台无前缀，后台统一 `/admin/**`；REST 风格，查询 `GET`、新增 `POST`、修改 `PUT`、删除 `DELETE`（表单场景用 `POST` + `_method` 隐藏域兼容）。
- 返回：`Result<T>`；分页返回 `data.records/total/current/size`。
- 参数：路径参数为 id，查询参数分页统一 `page`/`size`；日期 `yyyy-MM-dd HH:mm:ss`。
- 鉴权：见 §3.1；未登录 JSON 请求返回 401，页面请求 302。
- 幂等：写操作要求 POST/PUT/DELETE；GET 不做状态变更。

### 10.2 前台接口

| # | 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|---|
| F1 | GET | `/` | 首页（分类 + 推荐） | 否 |
| F2 | GET | `/product/list` | 列表（`categoryId`、`keyword`、`page`、`size`、`sort`） | 否 |
| F3 | GET | `/category/{id}` | 分类页（一级/二级分类落地，含子分类商品，分页） | 否 |
| F4 | GET | `/product/{id}` | 详情（含 SKU 列表） | 否 |
| F5 | GET | `/product/{id}/skus` | 刷新规格价格库存（JSON） | 否 |
| F6 | GET | `/search` | 搜索（复用列表） | 否 |
| F7 | GET | `/captcha` | 图形验证码（`image/png`） | 否 |
| F8 | GET | `/register` | 注册页 | 否 |
| F9 | POST | `/user/register` | 提交注册（PRG） | 否 |
| F10 | GET | `/login` | 登录页（`redirect` 参） | 否 |
| F11 | POST | `/user/login` | 登录（PRG） | 否 |
| F12 | POST | `/user/logout` | 登出 | 是 |
| F13 | GET | `/user/center` | 个人中心 | 是 |
| F14 | POST | `/user/password` | 修改密码 | 是 |
| F15 | GET | `/cart` | 购物车页 | 是 |
| F16 | POST | `/cart/add` | 加购（`itemId`、`quantity`） | 是 |
| F17 | PUT | `/cart/{id}` | 改数量（`quantity`） | 是 |
| F18 | DELETE | `/cart/{id}` | 删除 | 是 |
| F19 | GET | `/address/list` | 地址列表 | 是 |
| F20 | GET | `/address/form[/{id}]` | 地址表单 | 是 |
| F21 | POST | `/address/save` | 新增/保存（`id` 有值为更新） | 是 |
| F22 | DELETE | `/address/{id}` | 删除 | 是 |
| F23 | POST | `/address/{id}/default` | 设为默认 | 是 |
| F24 | GET | `/order/checkout` | 确认订单（`itemId`+`quantity` 或 `cartIds`） | 是 |
| F25 | POST | `/order/submit` | 提交订单（`submitToken`）→ 302 支付页 | 是 |
| F26 | GET | `/order/{id}/pay` | 支付页 | 是 |
| F27 | POST | `/order/{id}/pay` | 模拟支付成功 | 是 |
| F28 | POST | `/order/{id}/cancel` | 取消订单 | 是 |
| F29 | GET | `/order/list` | 订单列表（`status`、`page`） | 是 |
| F30 | GET | `/order/{id}` | 订单详情 | 是 |
| F31 | POST | `/order/{id}/receive` | 确认收货 | 是 |
| F32 | GET | `/feedback/form` | 投诉/反馈表单（`orderId` 有值为投诉） | 是 |
| F33 | POST | `/feedback/submit` | 提交（`type`、`orderId`、`category`、`content`、`contact`） | 是 |
| F34 | GET | `/feedback/list` | 我的投诉与反馈（`type`、`status`、`page`） | 是 |
| F35 | GET | `/feedback/{id}` | 详情（含卖家回复） | 是 |
| F36 | POST | `/feedback/{id}/cancel` | 撤销（仅 `PENDING`） | 是 |

### 10.3 后台接口

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| A1 | GET | `/admin/captcha` | 验证码 |
| A2 | GET | `/admin/login` / POST `/admin/login` | 登录页 / 提交 |
| A3 | POST | `/admin/logout` | 退出 |
| A4 | GET | `/admin` | 工作台 |
| A5 | GET | `/admin/category` | 分类列表（`keyword`、`page`） |
| A6 | GET | `/admin/category/form[/{id}]` | 分类表单 |
| A7 | POST | `/admin/category/save` | 新增/修改 |
| A8 | DELETE | `/admin/category/{id}` | 删除（有下级则 2004） |
| A9 | GET | `/admin/product` | SPU 列表（`keyword`、`categoryId`、`status`、`page`） |
| A10 | GET | `/admin/product/form[/{id}]` | SPU 表单（含 SKU 子表） |
| A11 | POST | `/admin/product/save` | 新增/修改 SPU + SKU 列表 |
| A12 | POST | `/admin/product/{id}/status` | 上下架（`status`） |
| A13 | DELETE | `/admin/product/{id}` | 删除（有未完结订单则 2005） |
| A14 | GET | `/admin/item` | SKU 列表（`productId`） |
| A15 | POST | `/admin/item/save` | 新增/修改 SKU |
| A16 | DELETE | `/admin/item/{id}` | 删除 SKU（最后一个禁删） |
| A17 | POST | `/admin/upload/image` | 图片上传，返回 URL |
| A18 | GET | `/admin/account` | 用户列表（`keyword`、`status`、`page`） |
| A19 | GET | `/admin/account/{id}` | 用户详情 |
| A20 | POST | `/admin/account/save` | 修改昵称/手机号/状态 |
| A21 | POST | `/admin/account/{id}/reset-password` | 重置密码 |
| A22 | GET | `/admin/order` | 订单列表（`orderNo`、`username`、`status`、`dateFrom`、`dateTo`、`page`） |
| A23 | GET | `/admin/order/{id}` | 订单详情 |
| A24 | POST | `/admin/order/{id}/ship` | 发货（`courierCompany`、`trackingNo`） |
| A25 | POST | `/admin/order/{id}/receiver` | 修改收货信息 |
| A26 | DELETE | `/admin/order/{id}` | 删除（限终态） |
| A27 | GET | `/admin/log` | 操作日志（`module`、`adminName`、`dateFrom`、`dateTo`） |
| A28 | GET | `/admin/feedback` | 投诉/反馈列表（`type`、`status`、`orderNo`、`username`、`dateFrom`、`dateTo`、`page`） |
| A29 | GET | `/admin/feedback/{id}` | 详情（含关联订单摘要） |
| A30 | POST | `/admin/feedback/{id}/accept` | 受理 → `PROCESSING` |
| A31 | POST | `/admin/feedback/{id}/reply` | 回复（`reply`）→ `REPLIED` |
| A32 | POST | `/admin/feedback/{id}/close` | 关闭（`closeReason`）→ `CLOSED` |

### 10.4 错误码

| 码 | 含义 | 场景 |
|---|---|---|
| 200 | 成功 | — |
| 400 | 参数校验失败 | `@Valid` 不通过 |
| 401 | 未登录/会话过期 | 拦截器 JSON 分支 |
| 403 | 无权限 | 越权访问他人资源（也可统一为 3003/3004） |
| 404 | 资源不存在 | — |
| 500 | 系统异常 | 兜底 |
| 1001 | 用户名已存在 | 注册 |
| 1002 | 用户名或密码错误 | 登录 |
| 1003 | 验证码错误或已过期 | 登录/注册/后台登录 |
| 1004 | 账号已被禁用 | 登录 |
| 1005 | 原密码不正确 | 修改密码 |
| 2001 | 商品不存在或已下架 | 加购/下单/详情 |
| 2002 | 库存不足 | 加购/下单 |
| 2003 | 规格不存在 | 加购 |
| 2004 | 存在子分类或商品，无法删除 | 分类删除 |
| 2005 | 存在未完结订单，无法删除 | SPU 删除 |
| 3001 | 订单不存在 | 详情/操作 |
| 3002 | 当前状态不允许该操作 | 订单/投诉状态机拒绝 |
| 3003 | 无权操作该订单 | 越权 |
| 3004 | 收货地址不存在或不属于当前用户 | 地址/下单 |
| 3005 | 未选择结算商品 | 结算 |
| 3006 | 重复提交，请勿刷新重试 | 下单 Token 复用 |
| 3007 | 投诉仅限本人已购买的订单 | 非本人订单 / 未支付 / 已取消 |
| 3008 | 该订单已有未关闭的投诉 | 重复投诉 |
| 3009 | 提交过于频繁，请稍后再试 | 同一用户 1 小时内超过 5 条反馈 |
| 4001 | 文件类型不支持或超过大小限制 | 上传 |
| 5001 | 后台账号或密码错误 | 后台登录 |

---

## 11. 非功能需求

### 11.1 安全

| 项 | 要求 |
|---|---|
| 密码 | BCrypt（强度 10）存储；注册/改密校验强度（6–20，字母+数字）；日志与接口不回显密码 |
| 会话 | 超时 30 分钟（`application.yml` 已配）；改密后清会话；后台与前台 key 隔离 |
| 越权 | 所有用户态资源以会话 accountId 过滤；后台管理接口仅后台会话可进 |
| SQL 注入 | 全部走 MyBatis 参数占位 `#{}`；排序字段用白名单枚举映射，禁止拼接 |
| XSS | Thymeleaf `th:text` 默认转义；`detail` 如需富文本，白名单过滤标签；投诉/反馈正文按纯文本渲染，不解析 HTML |
| CSRF | **未引入 Spring Security，无 CSRF Token**，属已知限制（§14.2）；缓解：写操作全部 POST/PUT/DELETE + 自定义一次性 Token（下单） |
| 上传 | 白名单 `jpg/jpeg/png/webp`，魔数校验 + 大小 ≤5MB，UUID 重命名，存 `./uploads/`，禁止执行权限；`/uploads/**` 不经拦截器 |
| 验证码 | 一次性、5 分钟有效、忽略大小写；防暴力破解靠图形码 + 失败重试（无锁定，已知限制） |
| 日志 | 请求参数脱敏（password/token → `***`） |

### 11.2 并发与一致性

| 场景 | 方案 |
|---|---|
| 库存扣减 | `item.version` 乐观锁 + `stock >= qty` 条件更新，失败即返回 2002（不超卖） |
| 重复下单 | 一次性 `submitToken` + `order_no` 唯一索引 |
| 重复取消/关单 | 条件更新 `WHERE status='PENDING_PAYMENT'`，只有成功者回补库存 |
| 地址默认互斥 | 同事务内先 `UPDATE ... SET is_default=0 WHERE account_id=?` 再置 1 |
| 后台改库存 | 带 `version` 校验，冲突提示刷新 |
| 事务 | 下单、取消、发货、重置密码、设默认地址均 `@Transactional(rollbackFor=Exception.class)` |

### 11.3 性能

- 商品列表分页，前台单页 ≤ 60（默认 12），后台 ≤ 100（默认 10）；禁止全表返回。
- 列表页避免 N+1：用 `LEFT JOIN` 或一次 `IN` 查 SKU 聚合出最低价与总库存。
- 订单列表按 `(account_id, status, create_time)` 索引；首页推荐限制 8 条。
- 图片 ≤5MB，`<img loading="lazy">`；不做 CDN（本地静态目录）。
- 单机目标：并发 50 请求下 P95 < 500ms（课程演示规模，不做压测报告）。

### 11.4 可用性与规范

- 统一 `Result`、统一异常、统一分页、统一命名（controller/service/mapper/entity/dto/vo）。
- 所有页面表单有错误回显；危险操作二次确认；空状态页有引导（如空购物车跳商品列表）。
- 兼容 Chrome/Edge 最新版；分辨率 ≥1366×768；不做移动端适配（非目标）。

### 11.5 日志

- 业务日志：`com.ipc.mall` 为 `debug`（开发期打印 SQL），生产改 `info`。
- 操作日志：`sys_oper_log` 表（后台写操作 + 登录）。
- 异常日志：`GlobalExceptionHandler` 打印 `error` + 堆栈，返回友好信息。

---

## 12. 验收标准

> 每条可独立执行；「GWT」= 前置条件 / 操作 / 预期结果。

### 12.1 前台

| 编号 | 验收标准 |
|---|---|
| AC-01 | G：以游客直接访问 `/product/list?categoryId={一级id}`；W：翻页并切换排序；T：仅显示该分类（含子分类）且 `status=1` 的商品，分页与排序生效 |
| AC-02 | G：搜索框输入商品名片段；W：搜索；T：结果命中名称或描述，关键词为空时回到全量列表 |
| AC-03 | G：进入含多 SKU 的商品详情；W：切换规格；T：价格、库存、加购按钮状态随之变化，切换到的 SKU 售罄时按钮禁用 |
| AC-04 | G：游客在详情页点「加入购物车」；W：点击；T：跳登录页且 `redirect` 指向原商品，登录后回到该商品 |
| AC-05 | G：游客从首页点一级分类；W：进入分类页并点某个二级分类；T：前者 URL 为 `/category/{一级id}`、顶部列出该一级下全部二级分类、商品涵盖所有子分类；后者只显示该二级分类商品，面包屑为 `首页 › 一级 › 二级` |
| AC-06 | G：已有 `visible=0` 的分类、空分类、不存在的分类 id；W：分别访问 `/category/{id}`；T：下架分类 302 到 `/product/list` 并提示，空分类显示「该分类暂无商品」，非法 id 返回 404 |
| AC-07 | G：注册页；W：输入已存在用户名 + 正确验证码；T：提示 1001，不创建账号 |
| AC-08 | G：登录页；W：验证码错误提交；T：提示 1003，且验证码图片已刷新（旧码不能再用） |
| AC-09 | G：已用账号 A 登录；W：退出后用错误密码登录 3 次；T：均提示 1002（不透露用户是否存在） |
| AC-10 | G：后台禁用用户 B；W：B 尝试登录；T：提示 1004，无法进入前台 |
| AC-11 | G：购物车已有 SKU X 数量 2；W：再次加购同 SKU 数量 1；T：数量变为 3，不产生两行记录 |
| AC-12 | G：购物车删除 SKU X 后再次加购；W：加购；T：原记录复活为一行，数量正确，不出现重复行 |
| AC-13 | G：勾选 2 件商品；W：提交订单；T：生成 `PENDING_PAYMENT` 订单，明细与收货信息为快照，购物车对应项消失，库存已扣减 |
| AC-14 | G：商品库存 1；W：两个会话同时提交该商品各 1 件；T：仅一单成功，另一单提示 2002，库存不为负 |
| AC-15 | G：在确认页点提交后按 F5 刷新重提；W：刷新；T：不产生第二笔订单（Token 失效/3006），跳转订单列表 |
| AC-16 | G：待支付订单；W：取消；T：状态 `CANCELLED`、写 `cancel_time`、库存回补 |
| AC-17 | G：待支付订单；W：模拟支付成功；T：状态 `PAID`、写 `pay_time`；重复点击不重复写时间 |
| AC-18 | G：`PAID` 订单；W：前台点确认收货；T：拒绝（3002），必须等卖家发货 |
| AC-19 | G：用户 A 登录；W：直接访问用户 B 的订单详情 URL；T：返回 3003/404，不泄露内容 |
| AC-20 | G：未登录；W：访问 `/cart`、`/order/list`、`/address/list`；T：302 到登录页；AJAX 请求返回 401 JSON |
| AC-21 | G：已发货订单；W：用户确认收货；T：状态 `COMPLETED`、写 `finish_time`，时间线展示完整四步 |

### 12.2 后台

| 编号 | 验收标准 |
|---|---|
| AC-22 | G：未登录；W：访问 `/admin/product`；T：302 到 `/admin/login`；登录后回到目标页 |
| AC-23 | G：一级分类下有二级分类；W：删除该一级分类；T：提示 2004，数据未删 |
| AC-24 | G：无子分类无商品；W：删除分类；T：逻辑删除，前台导航不再出现 |
| AC-25 | G：新建 SPU 并添加 2 个 SKU；W：前后台分别查看；T：前台详情展示 2 个规格，最低价正确，总库存为 2 个 SKU 之和 |
| AC-26 | G：SPU 上架中；W：后台下架；T：前台列表消失、详情显示「已下架」、不可加购 |
| AC-27 | G：SPU 存在 `PAID` 订单；W：后台删除该 SPU；T：提示 2005，删除被拒 |
| AC-28 | G：上传 `.exe` 或 6MB 图片；W：上传；T：提示 4001，文件未落盘 |
| AC-29 | G：用户 C 已登录前台；W：后台禁用 C；T：C 刷新受保护页面后会话失效（或下次登录被拒），订单数据仍可查 |
| AC-30 | G：后台重置用户 D 密码为 `abc123456`；W：D 用旧密码登录、再用新密码登录；T：旧密码失败，新密码成功 |
| AC-31 | G：`PAID` 订单；W：发货填物流公司 + 单号；T：状态 `SHIPPED`、写 `ship_time`，前台详情显示单号 |
| AC-32 | G：`PAID` 订单；W：再次发货；T：提示 3002，物流信息不被覆盖 |
| AC-33 | G：`SHIPPED` 订单；W：后台删除；T：拒绝（仅终态可删）；`CANCELLED` 订单可删且列表不再出现 |
| AC-34 | G：后台执行发货与重置密码；W：查看 `/admin/log`；T：两条日志含操作人、模块、动作、目标 id、耗时，参数中密码为 `***` |
| AC-35 | G：`sys_oper_log` 查询；W：按模块筛选；T：仅显示该模块日志，分页正确 |

### 12.3 售后（投诉与反馈）

| 编号 | 验收标准 |
|---|---|
| AC-36 | G：用户 A 有 `PENDING_PAYMENT` 与 `CANCELLED` 各一笔订单；W：查看订单列表与详情并尝试投诉；T：两处均无投诉入口；直接 POST `/feedback/submit` 带该订单 id 返回 3007 |
| AC-37 | G：用户 B 的 `COMPLETED` 订单；W：用户 A 用该订单 id 提交投诉；T：返回 3007，`feedback` 表无新增记录 |
| AC-38 | G：用户 A 的 `PAID` 订单已有一条 `PENDING` 投诉；W：再次提交同订单投诉；T：返回 3008；改用该用户另一笔 `SHIPPED` 订单提交则成功 |
| AC-39 | G：已登录用户；W：不选订单提交反馈 → 打开「我的反馈」；T：列表区分投诉与反馈，状态为待处理，详情显示完整内容与状态时间线 |
| AC-40 | G：一条 `PENDING` 投诉；W：用户撤销；T：状态 `CLOSED`、`close_reason=用户撤销`，该订单可重新投诉；对 `PROCESSING` 投诉撤销返回 3002 |
| AC-41 | G：一条 `PENDING` 投诉；W：后台受理 → 回复 → 前台查看详情；T：状态 `PROCESSING` → `REPLIED`，前台显示回复内容与回复时间，操作日志新增两条（受理/回复） |
| AC-42 | G：一条 `REPLIED` 投诉；W：后台关闭并填原因；T：状态 `CLOSED`，前台不可再撤销，用户可对同一订单重新投诉；对已关闭记录再次关闭返回 3002 |

---

## 13. 里程碑

| 阶段 | 内容 | 完成标志 |
|---|---|---|
| M0 ✅ | 骨架：pom、application.yml、统一返回、全局异常、MP 分页/乐观锁、`/ping`、`init.sql` 建库 | `/ping` 通 |
| M1 | 数据层：10 张业务表 DDL + 测试数据、`BaseEntity` + 自动填充、实体/Mapper | 表建好，MP 能查通 |
| M2 | 公共层：`LoginInterceptor`、`AdminLoginInterceptor`、验证码、上传、`@OperLog` 切面 | 未登录被拦、验证码出图、日志落表 |
| M3 | 前台商品：首页、分类页、列表（筛选/排序/搜索）、详情与 SKU 切换、加购入口 | AC-01～AC-06 |
| M4 | 前台用户：注册、登录、登出、个人中心、改密 | AC-07～AC-10 |
| M5 | 前台交易：购物车、地址、下单（乐观锁 + Token）、模拟支付、订单管理、状态机 | AC-11～AC-21 |
| M6 | 后台：登录、分类、SPU/SKU、用户、订单、发货、日志 | AC-22～AC-35 |
| M7 | 售后：投诉与反馈（前台提交、我的反馈、撤销 + 后台受理、回复、关闭） | AC-36～AC-42 |
| M8 | 联调 + 演示数据 + 答辩材料（时序图、超卖演示脚本） | 全流程走通，见附录 B |

> 依赖：M1 是所有模块的前提；M6 的「发货」依赖 M5 的状态机；M7 依赖 M5 的订单状态（投诉要先有已支付订单）；M8 的演示脚本依赖 M3～M7 全部完成。
> 【P1】超时关单在工作量允许时插入 M5 末尾，不影响验收。

---

## 14. 风险、已知限制与待定项

### 14.1 风险

| 风险 | 影响 | 应对 |
|---|---|---|
| 前后台模板风格割裂 | 演示观感差 | M1 先定两套公共片段（front/admin 各一份） |
| 商品三级结构理解偏差 | 表设计返工 | 本文 §7.3 已定：两级分类 + SPU + SKU，开发前对齐 |
| 乐观锁导致演示时「抢不到」 | 现场翻车 | 演示前把库存调大；超卖演示用单独低库存商品 |
| 图片存本地目录 | 换机器后图片丢失 | `uploads/` 加入 `.gitignore`，演示前重新上传；文档注明 |
| 定时任务与手动取消并发 | 重复回补库存 | 条件更新 + 受影响行数判断（§5.14） |

### 14.2 已知限制（答辩时主动说明，比被问出来好）

1. 无 CSRF Token（未引入 Spring Security）。
2. 登录失败无锁定/限流，仅图形验证码。
3. 后台只有单一角色，无行级权限，只有操作日志。
4. 无退款/售后，取消仅限待支付。
5. 图片存本地磁盘，未接对象存储。
6. 无接口文档自动化（Swagger），`docs/api.md` 手工维护。
7. 未做性能压测，指标为演示规模估计。
8. 投诉/反馈无附件上传（P1）、无站内信或邮件通知，用户需主动回到「我的反馈」查看回复。

### 14.3 待评审项

| # | 待定内容 | 建议 |
|---|---|---|
| Q1 | 商品排序是否加价格排序 | 时间够就做（P1），SQL 用最低价子查询 |
| Q2 | 超时关单是否本期做 | 建议做：逻辑简单，答辩加分（P1） |
| Q3 | 后台是否显示工作台统计 | 建议只做 4 个数字卡片，不做图表 |
| Q4 | 订单备注是否开放给用户 | 建议只做后台备注，用户端不做（减少字段暴露） |
| Q5 | 是否允许后台修改订单金额 | 不允许（快照锁定），与本文一致 |
| Q6 | 验证码是否区分大小写 | 不区分，降低演示失败率 |
| Q7 | 投诉是否限制时间窗（如订单完成后 30 天内） | 本期不限；若加，只需在提交校验里加一个时间条件 |

---

## 附录 A · 关键设计决策

> 下面这些选择影响后续所有模块，改动成本高。记录在此，代码评审和答辩时可直接对着讲。

| # | 决策 | 备选（未采用） | 理由 |
|---|---|---|---|
| D-1 | 商品用「两级分类 + SPU + SKU」表达 | 大类 / 小类 / 单品三级命名 | 分类树本身已经表达了大类与小类，再叠一层命名会让「商品类型」和「商品」的边界变模糊；SPU 负责展示、SKU 负责售卖，职责单一 |
| D-2 | 上架状态只放在 SPU（`product.status`） | 每个 SKU 各带 status | 前台展示单位是 SPU，一处开关就能控制可见性；SKU 停售用 `stock=0` 表达，避免两级状态联动出错 |
| D-3 | 订单设 5 个状态（含 `SHIPPED`） | 4 个状态，支付后直接确认收货 | 有卖家发货就必然有「在途」阶段；省掉它，物流单号无处存放，也说不清「已支付但未收到」的订单归谁处理 |
| D-4 | 订单金额与商品明细锁定快照 | 允许后台改价、改明细 | 订单是交易凭证，改价会让支付金额与订单不一致；改价属财务能力，本期非目标 |
| D-5 | 仅终态订单可删（`CANCELLED` / `COMPLETED`） | 任意状态可删 | 删除进行中的订单会让用户端订单凭空消失，库存与支付状态也无从追溯 |
| D-6 | 操作日志落库（`sys_oper_log`） | 只打应用日志 | 后台改动直接影响前台数据，出问题要能回答「谁在什么时候改了什么」；AOP 切面实现成本低 |
| D-7 | 库存下单时扣减，取消/关单回补 | 支付成功时才扣 | 下单即锁定库存，避免「先卖后扣」导致超卖；回补靠状态条件更新，保证只补一次 |
| D-8 | 前后台会话用同一 `JSESSIONID` + 不同 key | 两套独立 Session / 独立端口 | Spring 默认单 Session，key + 拦截器已能完全隔离，独立 Session 属于额外成本 |
| D-9 | 禁用账号联动前台登录 | 后台只做标记，不影响登录 | 不联动则「禁用」按钮没有实际效果，功能自相矛盾 |
| D-10 | 验证码手写 Java2D 生成 | 引入 Hutool / Kaptcha | `pom.xml` 依赖已固定，40 行代码即可，且能讲清生成与校验过程；换实现只需加依赖，接口契约不变 |
| D-11 | 运费固定 0，但保留 `freight_amount` 字段 | 不建该字段 | 金额结构（商品总额 + 运费 = 应付）现在就要定死，以后加运费规则不必改表、不必改订单快照 |
| D-12 | 分类字段取 `parent_id` + `visible` + `descn` + `sort` | 只保留 `parent_id` | 导航可见性、排序、描述是三个独立诉求，混在一起以后要加字段做兼容 |
| D-13 | 投诉只能绑定本人已购买的订单（`PAID`/`SHIPPED`/`COMPLETED`） | 允许对任意订单投诉，或不绑订单直接投诉 | 投诉是交易纠纷，先有交易才谈得上纠纷；未支付、已取消的订单没有履约事实，投诉无从核实，也挡住恶意刷投诉 |
| D-14 | 投诉与反馈共用 `feedback` 一张表 | 拆成两张表 | 两者字段八成重合（内容、状态、回复、关闭原因），分表会让后台列表、回复、日志各写两套；用 `type` 区分即可 |

## 附录 B · 答辩演示脚本（5 分钟）

1. **0:00–0:40 首页、分类页与搜索**：首页分类导航 → 分类页（一级分类下的二级入口、商品区）→ 列表筛选与翻页 → 搜索「水杯」。
2. **0:40–1:20 商品详情**：切换两个规格，价格与库存联动；游客加购 → 被拦 → 登录后回到该页。
3. **1:20–2:00 注册/登录**：故意输错验证码（展示一次性失效），再正确登录。
4. **2:00–3:00 下单**：购物车勾选 → 确认页（地址、运费）→ 提交 → 演示刷新不重复下单 → 模拟支付。
5. **3:00–3:40 超卖**：开两个浏览器，库存 1 的商品同时下单，只有一单成功（提前把 `log-impl` 打开，现场展示乐观锁 SQL）。
6. **3:40–4:20 后台**：登录 → SPU 列表 → 下架某商品（前台刷新已消失）→ 订单发货（填单号）→ 处理一条投诉（受理 + 回复）→ 操作日志里看到刚才四条动作。
7. **4:20–5:00 收尾**：前台确认收货 → 订单完成时间线；回到文档讲「状态机 + 乐观锁 + 会话隔离」三个设计点。

## 附录 C · 变更记录

| 版本 | 日期 | 变更 | 作者 |
|---|---|---|---|
| v1.0 | 2026-11-17 | 初稿：范围与角色、商品/用户/交易/后台四域功能、数据模型、订单状态机、接口与错误码、验收标准、里程碑 | — |
