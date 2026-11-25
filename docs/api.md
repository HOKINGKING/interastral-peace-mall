# 星际和平商城 · 接口约定（API Contract）

> 依据 `docs/PRD.md` §10 导出，是前后端联调的唯一接口基准。
> 版本：v1.2 ｜ 日期：2026-11-25 ｜ 手工维护（未引入 Swagger，见 PRD §14.2 已知限制 #6）。
> 修改接口时须同步更新本文，并回写 PRD 附录 C 变更记录。

---

## 1. 统一规范

| 项 | 约定 |
|---|---|
| 基础路径 | 前台无前缀；后台统一 `/admin/**` |
| 返回结构 | 数据接口统一 `Result<T>`；页面请求返回视图名或 302，不返回 JSON |
| 分页 | 查询参数统一 `page` / `size`；返回 `data.records / total / current / size`；前台 `size` 默认 12、上限 60；后台 `size` 默认 10、上限 100 |
| 鉴权 | 见 PRD §3.1：页面请求未登录 → 302 跳登录页并带 `redirect`；AJAX 请求（`X-Requested-With: XMLHttpRequest` 或 `Accept: application/json`）→ `code=401` JSON |
| HTTP 方法 | 查询 `GET`、新增 `POST`、修改 `PUT`、删除 `DELETE`；表单场景用 `POST` + `_method` 隐藏域兼容 |
| 幂等 | 写操作一律 POST/PUT/DELETE；GET 不做状态变更；登出用 POST |
| PRG | 表单提交成功后 302 重定向（防刷新重复提交），失败回填错误到 Model |
| 日期 | `yyyy-MM-dd HH:mm:ss`（`DATETIME`，`Asia/Shanghai`） |

### 1.1 统一返回结构

```json
{ "code": 200, "message": "操作成功", "data": {}, "timestamp": 1763471400000 }
```

- `code`：见 §5 错误码；`200` 成功。
- `message`：提示语，直接可展示。
- `data`：业务数据；分页时为 `{ "records": [], "total": 0, "current": 1, "size": 12 }`。

### 1.2 会话键

| 端 | 会话键 | 内容 |
|---|---|---|
| 前台 | `session.LOGIN_ACCOUNT` | accountId、username、displayName |
| 后台 | `session.LOGIN_ADMIN` | adminId、username、displayName |

---

## 2. 前台接口

> 鉴权「是」= 需登录（`LoginInterceptor` 拦截）；「否」= 游客可访问。

### 2.1 商品与搜索（F1–F6）

| 编号 | 方法 | 路径 | 鉴权 | 请求参数 | 返回 |
|---|---|---|---|---|---|
| F1 | GET | `/` | 否 | — | 首页视图：一级分类导航（≤8 个）、推荐商品（`status=1` 按更新时间取 8 条） |
| F2 | GET | `/product/list` | 否 | `categoryId`（含子分类）、`keyword`（≤50，trim）、`onlyInStock`（0/1）、`page`、`size`、`sort`（`default`/`priceAsc`/`priceDesc`/`newest`） | 列表视图；仅 `status=1` 且分类可见、有 SKU 的 SPU；每件展示主图/名称/最低价/已售件数/库存状态 |
| F3 | GET | `/category/{id}` | 否 | `id`（一级或二级）、`page`、`size`、`sort`、`onlyInStock` | 分类视图：一级→二级入口 + 全部子分类商品；二级→同级切换 + 当前分类商品 |
| F4 | GET | `/product/{id}` | 否 | `id` | 详情视图：主图与相册（≤5 张）/名称/品牌/参数表/描述/详情/规格列表/价格/库存/已售件数/面包屑 |
| F5 | GET | `/product/{id}/skus` | 否 | `id` | `Result<List<ItemVO>>`：`{id, spec, price, stock, image, soldOut}`，用于局部刷新价格/库存 |
| F6 | GET | `/search` | 否 | `keyword`、`page`、`size` | 与 `/product/list` 同视图、同筛选逻辑 |

### 2.2 验证码与认证（F7–F14）

| 编号 | 方法 | 路径 | 鉴权 | 请求参数 | 返回 |
|---|---|---|---|---|---|
| F7 | GET | `/captcha` | 否 | — | `image/png`，`Cache-Control: no-store`；4 位字符，5 分钟有效，一次性 |
| F8 | GET | `/register` | 否 | — | 注册页视图 |
| F9 | POST | `/user/register` | 否 | `username`、`password`、`confirmPassword`、`phone`（选填）、`captcha` | 成功 302 首页并自动登录；失败回填错误（1001 重名 / 1003 验证码） |
| F10 | GET | `/login` | 否 | `redirect`（登录后回跳） | 登录页视图 |
| F11 | POST | `/user/login` | 否 | `username`、`password`、`captcha`、`redirect` | 成功写会话、更新 `last_login_time`、302 到 `redirect` 或首页；失败区分 1002/1003/1004 |
| F12 | POST | `/user/logout` | 是 | — | 清会话，302 首页 |
| F13 | GET | `/user/center` | 是 | — | 个人中心视图：用户名/昵称/手机号打码/注册时间/最后登录 |
| F14 | POST | `/user/password` | 是 | `oldPassword`、`newPassword`、`confirmPassword` | 成功清会话强制重登；失败 1005 原密码不正确 |

### 2.3 购物车（F15–F18）

| 编号 | 方法 | 路径 | 鉴权 | 请求参数 | 返回 |
|---|---|---|---|---|---|
| F15 | GET | `/cart` | 是 | — | 购物车视图：勾选项合计、失效商品置灰 |
| F16 | POST | `/cart/add` | 是 | `itemId`、`quantity`（1–99） | `Result<Void>`；同 SKU 已存在则累加；已逻辑删除则复活；`status=0`/库存不足返回 2001/2002 |
| F17 | PUT | `/cart/{id}` | 是 | `quantity`（1–99 且 ≤ 库存） | `Result<Void>` |
| F18 | DELETE | `/cart/{id}` | 是 | — | `Result<Void>`（逻辑删除） |

### 2.4 收货地址（F19–F23）

| 编号 | 方法 | 路径 | 鉴权 | 请求参数 | 返回 |
|---|---|---|---|---|---|
| F19 | GET | `/address/list` | 是 | — | 地址列表视图（数量上限 20） |
| F20 | GET | `/address/form[/{id}]` | 是 | `id`（编辑时） | 地址表单视图 |
| F21 | POST | `/address/save` | 是 | `id`（有值=更新）、`receiver`、`phone`、`province`、`city`、`district`、`detail`、`isDefault` | `Result<Void>`；首个地址自动设默认 |
| F22 | DELETE | `/address/{id}` | 是 | `id` | `Result<Void>`（逻辑删除；非本人返回 3004） |
| F23 | POST | `/address/{id}/default` | 是 | `id` | `Result<Void>`；同事务内清其他地址 `is_default` |

### 2.5 订单（F24–F31）

| 编号 | 方法 | 路径 | 鉴权 | 请求参数 | 返回 |
|---|---|---|---|---|---|
| F24 | GET | `/order/checkout` | 是 | `itemId`+`quantity`（立即购买）或 `cartIds`（购物车结算，逗号分隔） | 确认订单视图：商品明细/地址/订单备注输入/运费/应付；生成 `submitToken` 存 Session 并下发 |
| F25 | POST | `/order/submit` | 是 | `submitToken`、`addressId`、`cartIds` 或 `itemId`+`quantity`、`remark`（≤200） | 成功 302 到 `/order/{id}/pay`；失败回填（3004 地址 / 2001 下架 / 2002 库存 / 3005 空购物车 / 3006 重复提交 / 400 备注超长） |
| F26 | GET | `/order/{id}/pay` | 是 | `id` | 支付页视图：订单号/应付/倒计时 |
| F27 | POST | `/order/{id}/pay` | 是 | `id` | 成功 302 到订单详情；仅 `PENDING_PAYMENT`；置 `PAID`、写 `pay_time`、累加 `product.sales`、写一条 `payment_record`；幂等：已 `PAID` 直接返回，不重复写流水 |
| F28 | POST | `/order/{id}/cancel` | 是 | `id` | 302；置 `CANCELLED`、写 `cancel_time`、回补库存；仅 `PENDING_PAYMENT` |
| F29 | GET | `/order/list` | 是 | `status`（Tab）、`page` | 订单列表视图：订单号/首件图+名/件数/总额/状态/操作按钮（终态订单带「删除订单」） |
| F30 | GET | `/order/{id}` | 是 | `id` | 订单详情视图：明细/收货/备注/金额/状态时间线/物流/支付流水（流水号、方式、金额、时间）；非本人返回 3003/404 |
| F31 | POST | `/order/{id}/receive` | 是 | `id` | 302；置 `COMPLETED`、写 `finish_time`；仅 `SHIPPED` |
| F37 | DELETE | `/order/{id}` | 是 | `id` | `Result<Void>`；仅 `CANCELLED`/`COMPLETED` 可删（逻辑删除，不回补库存），其余 3002 |

> 支付流水 `payment_record`：`order_id`、`order_no`、`account_id`、`amount`、`pay_method`（`MOCK`）、`status`（`SUCCESS`）、`transaction_no`（`PAY + orderNo`）、`pay_time`；`order_id` 与 `transaction_no` 均唯一，随支付成功与订单状态同一事务写入。

### 2.6 投诉与反馈（F32–F36）

| 编号 | 方法 | 路径 | 鉴权 | 请求参数 | 返回 |
|---|---|---|---|---|---|
| F32 | GET | `/feedback/form` | 是 | `orderId`（有值=投诉，展示订单快照） | 表单视图 |
| F33 | POST | `/feedback/submit` | 是 | `type`（COMPLAINT/FEEDBACK）、`orderId`、`category`（QUALITY/LOGISTICS/SERVICE/OTHER）、`content`、`contact`、`images`（P1，≤3） | `Result<Void>`；投诉须本人 `PAID/SHIPPED/COMPLETED` 订单（3007），同订单仅一条未关闭投诉（3008），反馈 1 小时 ≤5 条（3009） |
| F34 | GET | `/feedback/list` | 是 | `type`、`status`、`page` | 我的投诉与反馈视图 |
| F35 | GET | `/feedback/{id}` | 是 | `id` | 详情视图：内容/用户补充说明/订单摘要/状态时间线/卖家回复；非本人 404 |
| F36 | POST | `/feedback/{id}/cancel` | 是 | `id` | `Result<Void>`；仅 `PENDING` 可撤销 → `CLOSED`，`close_reason='用户撤销'` |
| F38 | POST | `/feedback/{id}/note` | 是 | `id`、`note`（≤200） | `Result<Void>`；把说明追加到 `user_note`（带时间前缀，总长 ≤1000），不改状态；`CLOSED` 后 3002 |
| F39 | POST | `/feedback/{id}/resolve` | 是 | `id` | `Result<Void>`；仅 `REPLIED` → `CLOSED`，`close_reason='用户确认已解决'` |

---

## 3. 后台接口

> 全部需 `AdminLoginInterceptor` 鉴权，除 A1/A2（登录）外。

### 3.1 登录与工作台（A1–A4）

| 编号 | 方法 | 路径 | 请求参数 | 返回 |
|---|---|---|---|---|
| A1 | GET | `/admin/captcha` | — | `image/png`，独立 `ADMIN_` 前缀会话 key |
| A2 | GET / POST | `/admin/login` | `username`、`password`、`captcha` | 登录页视图 / 成功 302 工作台，失败 5001 |
| A3 | POST | `/admin/logout` | — | 302 后台登录页 |
| A4 | GET | `/admin` | — | 工作台视图（P1：今日订单/待发货/在售商品/注册用户卡片） |

### 3.2 分类管理（A5–A8）

| 编号 | 方法 | 路径 | 请求参数 | 返回 |
|---|---|---|---|---|
| A5 | GET | `/admin/category` | `keyword`、`page` | 分类列表视图（树形/分组） |
| A6 | GET | `/admin/category/form[/{id}]` | `id` | 分类表单视图 |
| A7 | POST | `/admin/category/save` | `id`、`parentId`、`name`（≤30，同级唯一）、`descn`、`sort`、`visible` | `Result<Void>` |
| A8 | DELETE | `/admin/category/{id}` | `id` | `Result<Void>`；有子分类或商品返回 2004 |

### 3.3 商品管理 / SPU（A9–A13）

| 编号 | 方法 | 路径 | 请求参数 | 返回 |
|---|---|---|---|---|
| A9 | GET | `/admin/product` | `keyword`、`categoryId`、`status`、`page` | SPU 列表视图：主图/名称/分类/最低价/销量（`sales`）/SKU 数/总库存/状态 |
| A10 | GET | `/admin/product/form[/{id}]` | `id` | SPU 表单视图（基本信息、主图与相册、参数表、SKU 子表） |
| A11 | POST | `/admin/product/save` | `id`、`categoryId`（二级）、`name`（≤60）、`brand`、`descn`、`detail`、`mainImage`、`images`（≤5 张逗号分隔）、`specs`（JSON ≤8 组）、`status`、`skus[]` | `Result<Void>`；SPU + SKU 列表一并保存 |
| A12 | POST | `/admin/product/{id}/status` | `id`、`status`（1/0） | `Result<Void>`（上下架） |
| A13 | DELETE | `/admin/product/{id}` | `id` | `Result<Void>`；存在未完结订单引用其 SKU 返回 2005 |

### 3.4 SKU 管理（A14–A16）

| 编号 | 方法 | 路径 | 请求参数 | 返回 |
|---|---|---|---|---|
| A14 | GET | `/admin/item` | `productId` | SKU 列表视图：规格/价格/库存/版本 |
| A15 | POST | `/admin/item/save` | `id`、`productId`、`spec`（同 SPU 内唯一）、`price`（0.01–99999.99）、`stock`（0–999999）、`image` | `Result<Void>`；改库存带 `version` 校验 |
| A16 | DELETE | `/admin/item/{id}` | `id` | `Result<Void>`；SPU 下最后一条 SKU 禁删 |

### 3.5 图片上传（A17）

| 编号 | 方法 | 路径 | 请求参数 | 返回 |
|---|---|---|---|---|
| A17 | POST | `/admin/upload/image` | `multipart/form-data`：`file`（jpg/png/webp，≤5MB） | `Result<String>`：可访问 URL（`/uploads/**`）；不合法返回 4001 |

### 3.6 用户管理（A18–A21）

| 编号 | 方法 | 路径 | 请求参数 | 返回 |
|---|---|---|---|---|
| A18 | GET | `/admin/account` | `keyword`（用户名/手机号）、`status`、`page` | 用户列表视图：ID/用户名/昵称/手机号打码/状态/注册时间 |
| A19 | GET | `/admin/account/{id}` | `id` | 用户详情视图（含订单数） |
| A20 | POST | `/admin/account/save` | `id`、`nickname`、`phone`、`status` | `Result<Void>` |
| A21 | POST | `/admin/account/{id}/reset-password` | `id`、`password` | `Result<Void>`；BCrypt 入库，旧密码失效 |

### 3.7 订单管理与发货（A22–A26）

| 编号 | 方法 | 路径 | 请求参数 | 返回 |
|---|---|---|---|---|
| A22 | GET | `/admin/order` | `orderNo`、`username`、`status`、`dateFrom`、`dateTo`、`page` | 订单列表视图 |
| A23 | GET | `/admin/order/{id}` | `id` | 订单详情视图：订单信息/明细/收货/时间线/发货表单 |
| A24 | POST | `/admin/order/{id}/ship` | `id`、`courierCompany`（≤30）、`trackingNo`（≤40） | `Result<Void>`；仅 `PAID`，重复发货返回 3002 |
| A25 | POST | `/admin/order/{id}/receiver` | `id`、`receiverName`、`receiverPhone`、`receiverDetail` | `Result<Void>`；仅改收货信息，金额/明细锁定 |
| A26 | DELETE | `/admin/order/{id}` | `id` | `Result<Void>`；仅 `CANCELLED`/`COMPLETED` 可删，不回补库存 |

### 3.8 操作日志（A27）

| 编号 | 方法 | 路径 | 请求参数 | 返回 |
|---|---|---|---|---|
| A27 | GET | `/admin/log` | `module`、`adminName`、`dateFrom`、`dateTo`、`page` | 日志列表视图：时间/操作人/模块/动作/目标/参数（脱敏）/耗时/结果 |

### 3.9 投诉与反馈管理（A28–A32）

| 编号 | 方法 | 路径 | 请求参数 | 返回 |
|---|---|---|---|---|
| A28 | GET | `/admin/feedback` | `type`、`status`、`orderNo`、`username`、`dateFrom`、`dateTo`、`page` | 列表视图；`PENDING` 优先排序 |
| A29 | GET | `/admin/feedback/{id}` | `id` | 详情视图（内容、用户补充说明、关联订单摘要） |
| A30 | POST | `/admin/feedback/{id}/accept` | `id` | `Result<Void>`；`PENDING` → `PROCESSING`，重复返回 3002 |
| A31 | POST | `/admin/feedback/{id}/reply` | `id`、`reply`（1–500 必填） | `Result<Void>`；→ `REPLIED`，写 `reply`/`reply_admin_id`/`reply_time` |
| A32 | POST | `/admin/feedback/{id}/close` | `id`、`closeReason`（≤100） | `Result<Void>`；未关闭状态 → `CLOSED` |

---

## 4. 错误码

| 码 | 含义 | 场景 |
|---|---|---|
| 200 | 成功 | — |
| 400 | 参数校验失败 | `@Valid` 不通过 |
| 401 | 未登录/会话过期 | 拦截器 JSON 分支 |
| 403 | 无权限 | 越权访问他人资源（也可用 3003/3004） |
| 404 | 资源不存在 | — |
| 500 | 系统异常 | 兜底，不回显堆栈 |
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
| 3007 | 投诉仅限本人已购买的订单 | 非本人 / 未支付 / 已取消 |
| 3008 | 该订单已有未关闭的投诉 | 重复投诉 |
| 3009 | 提交过于频繁，请稍后再试 | 1 小时内超 5 条反馈 |
| 4001 | 文件类型不支持或超过大小限制 | 上传 |
| 5001 | 后台账号或密码错误 | 后台登录 |

---

## 5. 附：订单与售后状态机（与 `OrderStatus` / `FeedbackStatus` 枚举一致）

### 5.1 订单状态机（PRD §8）

```
待支付 ──支付成功──▶ 已支付 ──卖家发货──▶ 已发货 ──确认收货──▶ 已完成
   │
   └──用户取消 / 超时──▶ 已取消（终态）
```

| 状态 | 代码值（存库） | 终态 |
|---|---|---|
| 待支付 | `PENDING_PAYMENT` | 否 |
| 已支付 | `PAID` | 否 |
| 已发货 | `SHIPPED` | 否 |
| 已完成 | `COMPLETED` | 是 |
| 已取消 | `CANCELLED` | 是 |

> 转移合法性由 `OrderStatus#canTransitTo` 统一控制；每次流转用条件更新 `UPDATE orders SET status=#{to} WHERE id=#{id} AND status=#{from}` 保证幂等；取消/关单必须回补库存且只回补一次。
> 销量 `product.sales` 仅在 T2（支付成功）按 `order_item.product_id` 汇总累加，取消/关单不回退（PRD §7.6，AC-45）。

### 5.2 投诉与反馈状态机（PRD §8.4）

```
待处理 ──受理──▶ 处理中 ──回复──▶ 已回复 ──关闭──▶ 已关闭（终态）
   │                │                └──用户确认已解决──▶ 已关闭（终态）
   └──用户撤销──────┴────卖家关闭──────────────────────▶ 已关闭（终态）
```

| 状态 | 代码值 | 终态 |
|---|---|---|
| 待处理 | `PENDING` | 否 |
| 处理中 | `PROCESSING` | 否 |
| 已回复 | `REPLIED` | 否（仍需卖家关闭或用户确认已解决） |
| 已关闭 | `CLOSED` | 是 |

> 「补充说明」只追加 `user_note`、**不改变状态**；「我已解决」是 `REPLIED → CLOSED` 的条件更新，与撤销同样幂等（影响行数 0 返回 3002）。
