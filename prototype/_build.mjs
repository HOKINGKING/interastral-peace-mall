import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const OUT = path.dirname(fileURLToPath(import.meta.url));
const money = (n) => '¥' + Number(n).toFixed(2);

// 原创徽记：六边形币章 + 四芒星 + 轨道环（取星际和平公司的企业视觉语言，非官方素材）
const emblem = (cls = '') => `<svg class="emblem ${cls}" viewBox="0 0 64 64" aria-hidden="true">
  <polygon points="32,3.5 56.5,17.5 56.5,46.5 32,60.5 7.5,46.5 7.5,17.5" fill="none" stroke="#c9a24a" stroke-width="2"/>
  <polygon points="32,8.5 52,20.5 52,43.5 32,55.5 12,43.5 12,20.5" fill="none" stroke="#e7c873" stroke-width="1" opacity=".45"/>
  <circle cx="32" cy="32" r="19" fill="none" stroke="#c9a24a" stroke-width="1" opacity=".6"/>
  <ellipse cx="32" cy="32" rx="25" ry="9" fill="none" stroke="#e7c873" stroke-width="1" opacity=".4" transform="rotate(-18 32 32)"/>
  <path d="M32 11.5 L35.6 28.4 L52.5 32 L35.6 35.6 L32 52.5 L28.4 35.6 L11.5 32 L28.4 28.4 Z" fill="#e7c873"/>
  <circle cx="32" cy="32" r="3.4" fill="#0b1020"/>
  <circle cx="32" cy="32" r="1.5" fill="#f6e2a8"/>
</svg>`;

// 品牌标记：使用 assets/ipc-logo-{light|dark}.png（深底用白色版，浅底用黑色版），文件缺失时回退到上面的原创徽记
const mark = (base = '', cls = '', tone = 'light') => `<span class="mark ${cls}">
    <img class="ipc-logo" src="${base}assets/ipc-logo-${tone}.png" alt="星际和平公司标识"
         onload="this.parentElement.classList.add('has-logo')">
    ${emblem('fallback')}
  </span>`;

/* ---------------- 演示数据 ---------------- */
const CATS = [
  { id: 1, name: '家居日用', icon: '🛋️', subs: ['杯壶', '摆件', '香薰'] },
  { id: 2, name: '文创周边', icon: '✒️', subs: ['徽章', '海报', '笔记本'] },
  { id: 3, name: '服饰箱包', icon: '🎒', subs: ['帆布包', 'T恤', '帽子'] },
  { id: 4, name: '模型手办', icon: '🚀', subs: ['舰船模型', '人物手办'] },
  { id: 5, name: '数码配件', icon: '🎧', subs: ['耳机', '鼠标垫', '手机壳'] },
  { id: 6, name: '礼盒套装', icon: '🎁', subs: ['节日礼盒', '办公套装'] },
];

const PRODUCTS = [
  { name: '星际和平水杯 · 500ml', brand: '和平号工作室', price: 59, stock: 128, emoji: '🥤', alt: '', cat: '家居日用', sub: '杯壶', sold: 326 },
  { name: '星轨帆布包 · 加厚版', brand: '轨道制衣', price: 89, stock: 64, emoji: '🎒', alt: 'alt-1', cat: '服饰箱包', sub: '帆布包', sold: 181 },
  { name: '舰桥模型 · 纪念版', brand: '深空模型社', price: 299, stock: 12, emoji: '🛰️', alt: 'alt-4', cat: '模型手办', sub: '舰船模型', sold: 42 },
  { name: '驾驶员徽章套装（6 枚）', brand: '和平号工作室', price: 39, stock: 240, emoji: '🎖️', alt: 'alt-3', cat: '文创周边', sub: '徽章', sold: 517 },
  { name: '深空香薰蜡烛 · 冷杉', brand: '慢燃实验室', price: 129, stock: 36, emoji: '🕯️', alt: 'alt-2', cat: '家居日用', sub: '香薰', sold: 97 },
  { name: '命运星图海报 · A2', brand: '星图印社', price: 49, stock: 88, emoji: '🖼️', alt: 'alt-1', cat: '文创周边', sub: '海报', sold: 203 },
  { name: '联名机械键盘 · 86 键', brand: '轨道外设', price: 399, stock: 21, emoji: '⌨️', alt: 'alt-4', cat: '数码配件', sub: '鼠标垫', sold: 35 },
  { name: '星际和平笔记本 · 方格', brand: '和平号工作室', price: 29, stock: 0, emoji: '📓', alt: 'alt-2', cat: '文创周边', sub: '笔记本', sold: 640 },
  { name: '轨道托盘 · 合金', brand: '慢燃实验室', price: 69, stock: 54, emoji: '🍽️', alt: 'alt-3', cat: '家居日用', sub: '摆件', sold: 76 },
  { name: '星球夜灯 · 暖光', brand: '慢燃实验室', price: 159, stock: 18, emoji: '💡', alt: 'alt-4', cat: '家居日用', sub: '摆件', sold: 58 },
];

const ORDERS = [
  { no: '20261118093012', time: '2026-11-18 09:30', status: 'PAID', total: 388, items: [{ p: PRODUCTS[2], qty: 1 }, { p: PRODUCTS[1], qty: 1 }] },
  { no: '20261117181202', time: '2026-11-17 18:12', status: 'SHIPPED', total: 299, courier: '顺丰速运 · SF1234567890', items: [{ p: PRODUCTS[2], qty: 1 }] },
  { no: '20261116102133', time: '2026-11-16 10:21', status: 'COMPLETED', total: 129, items: [{ p: PRODUCTS[4], qty: 1 }] },
  { no: '20261115143044', time: '2026-11-15 14:30', status: 'PENDING_PAYMENT', total: 59, items: [{ p: PRODUCTS[0], qty: 1 }] },
  { no: '20261114090000', time: '2026-11-14 09:00', status: 'CANCELLED', total: 89, items: [{ p: PRODUCTS[1], qty: 1 }] },
];

const FEEDBACKS = [
  { no: 'FB-1002', type: 'COMPLAINT', order: '20261116102133', category: '商品质量', status: 'REPLIED', time: '2026-11-16 11:02', content: '蜡烛收到时玻璃杯有一道细裂纹，外包装完好，应该是出厂检验没发现。希望换一个或者退部分款。', reply: '已核实为运输前瑕疵，为您补发一件并赠送香薰试用装，预计 48 小时内发出。' },
  { no: 'FB-1001', type: 'FEEDBACK', order: '—', category: '其他', status: 'PENDING', time: '2026-11-17 20:15', content: '希望商品详情页能标出模型的实际尺寸，现在只能靠图片估计，容易买大。' },
];

/* ---------------- 组件 ---------------- */
const badgeOrder = (s) => {
  const map = {
    PENDING_PAYMENT: ['badge-pending', '待支付'],
    PAID: ['badge-paid', '已支付'],
    SHIPPED: ['badge-shipped', '已发货'],
    COMPLETED: ['badge-completed', '已完成'],
    CANCELLED: ['badge-cancelled', '已取消'],
  };
  const m = map[s] || ['', s];
  return `<span class="badge ${m[0]}">${m[1]}</span>`;
};

const badgeFb = (s) => {
  const map = {
    PENDING: ['badge-pending', '待处理'],
    PROCESSING: ['badge-paid', '处理中'],
    REPLIED: ['badge-shipped', '已回复'],
    CLOSED: ['badge-closed', '已关闭'],
  };
  const m = map[s] || ['', s];
  return `<span class="badge ${m[0]}">${m[1]}</span>`;
};

const productCard = (p) => `
<a class="product-card" href="product-detail.html">
  <div class="thumb ph ${p.alt}">${p.emoji}</div>
  <div class="body">
    <div class="name">${p.name}</div>
    <div class="between" style="margin-top:6px">
      <div class="price"><small>¥</small>${p.price}<small>.00</small></div>
      <div class="small muted">已售 ${p.sold}</div>
    </div>
    <div class="meta">
      <span>${p.cat} / ${p.sub}</span>
      ${p.stock === 0 ? '<span class="badge badge-cancelled">已售罄</span>' : `<span class="badge badge-completed">库存 ${p.stock}</span>`}
    </div>
  </div>
</a>`;

const nav = (active) => {
  const items = [
    ['index.html', '首页', 'home'],
    ['category.html', '分类', 'category'],
    ['product-list.html', '全部商品', 'list'],
    ['order-list.html', '我的订单', 'orders'],
    ['feedback-list.html', '投诉与反馈', 'feedback'],
  ];
  return `<nav class="nav">${items.map(([href, label, key]) => `<a href="${href}" class="${active === key ? 'active' : ''}">${label}</a>`).join('')}</nav>`;
};

const pager = (cur = 1, total = 3, label = '24 件商品') => {
  let html = '<div class="pagination">';
  html += `<span class="${cur === 1 ? 'disabled' : ''}">上一页</span>`;
  for (let i = 1; i <= total; i++) html += `<span class="${i === cur ? 'active' : ''}">${i}</span>`;
  html += `<span class="${cur === total ? 'disabled' : ''}">下一页</span>`;
  html += `<span class="small muted" style="border:0;background:none">共 ${total} 页 · ${label}</span></div>`;
  return html;
};

/* ---------------- 页面骨架 ---------------- */
const layout = ({ title, body, admin = false, active = '', crumb = '' }) => {
  const base = admin ? '../' : '';
  const proto = `<div class="proto-bar">
  <div><span class="tag">原型</span> 星际和平商城 · ${admin ? '卖家后台' : '用户端'} · 数据为虚构演示</div>
  <div><a href="${base}sitemap.html">页面清单</a> <span style="opacity:.35">|</span> <a href="${base}index.html">返回前台</a></div>
</div>`;

  if (admin) {
    const menu = [
      ['运营', [['index.html', '📊', '工作台', 'dash']]],
      ['商品', [['category.html', '🗂️', '分类管理', 'cat'], ['product.html', '📦', '商品管理', 'prod'], ['item.html', '🏷️', 'SKU 管理', 'item']]],
      ['交易', [['order.html', '🧾', '订单管理', 'order'], ['feedback.html', '💬', '投诉与反馈', 'fb']]],
      ['用户与日志', [['account.html', '👤', '用户管理', 'acc'], ['log.html', '📜', '操作日志', 'log']]],
    ];
    const side = menu.map(([group, links]) => `
    <div class="group">${group}</div>
    ${links.map(([href, ico, label, key]) => `<a href="${href}" class="${active === key ? 'active' : ''}">${ico} ${label}</a>`).join('')}`).join('');
    return `<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>${title} · 星际和平商城后台</title><link rel="icon" href="${base}assets/ipc-logo-dark.png"><link rel="stylesheet" href="${base}assets/style.css"></head>
<body>
${proto}
<div class="admin">
  <aside class="admin-side">
    <div class="logo-row">${mark('../')}<span>卖家后台</span></div>
    ${side}
  </aside>
  <div class="admin-main">
    <div class="admin-top">
      <div class="crumb">卖家后台 ${crumb ? ' / ' + crumb : ''}</div>
      <div class="admin-user"><span class="avatar">管</span> admin（超级管理员） <a class="btn btn-sm" href="login.html" data-demo="原型演示：已退出后台（不会真的登出）">退出</a></div>
    </div>
    <div class="admin-body">${body}</div>
  </div>
</div>
<script src="${base}assets/app.js"></script>
</body></html>`;
  }

  return `<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>${title} · 星际和平商城</title><link rel="icon" href="${base}assets/ipc-logo-dark.png"><link rel="stylesheet" href="${base}assets/style.css"></head>
<body>
${proto}
<header class="site-header">
  <div class="corp-strip"><div class="container"><span>星际和平公司 · 传统项目部</span><span>以信用点（Credit）结算 · 全星系 48 小时直达</span></div></div>
  <div class="container">
    <div class="brand-row">
      <a class="brand" href="index.html">${mark('', 'brand-mark')}<span class="wm"><b>星际和平商城</b><i>INTERASTRAL PEACE MALL</i></span></a>
      <form class="search" data-ok="原型演示：搜索会跳到商品列表页"><input placeholder="搜索商品，如「水杯」「徽章」"><button type="submit">搜索</button></form>
      <div class="header-actions">
        <a href="user-center.html">你好，星野</a>
        <a class="cart-link" href="cart.html">购物车<span class="count">2</span></a>
        <a href="login.html" data-demo="原型演示：已退出登录（不会真的登出）">退出</a>
      </div>
    </div>
    ${nav(active)}
  </div>
</header>
<main>${body}</main>
<footer class="site-footer">
  <div class="container row" style="justify-content:space-between">
    <div class="footer-corp">
      ${mark('', 'footer-mark')}
      <div>© 2026 星际和平商城 · 星际和平公司传统项目部</div>
    </div>
    <div class="row" style="gap:20px">
      <a href="feedback-form.html">意见反馈</a>
      <a href="admin/index.html">卖家后台</a>
      <a href="sitemap.html">原型页面清单</a>
    </div>
  </div>
</footer>
<script src="${base}assets/app.js"></script>
</body></html>`;
};

/* ---------------- 前台页面 ---------------- */
const pageIndex = () => {
  const tiles = CATS.map((c) => `<a class="cat-tile" href="category.html"><div class="ico">${c.icon}</div><div class="name">${c.name}</div></a>`).join('');
  return `
<section class="page"><div class="container">
  <div class="hero">
    <div class="hero-mark"><img class="hero-emblem" src="assets/ipc-logo-light.png" alt=""></div>
    <span class="seal">IPC · 官方周边直营</span>
    <h1 style="margin-top:14px">把星海搬进日常</h1>
    <p>从舰桥模型到随行水杯，每一件都按飞船标准做质检。今日下单，48 小时内从近地轨道仓发出。</p>
    <p class="small" style="color:#c9b075;letter-spacing:.14em;margin-top:12px">以信用点结算 · 全星系配送 · 传统项目部直营</p>
    <a class="btn btn-primary" href="product-list.html">逛全部商品 →</a>
  </div>

  <div class="mt-24">
    <div class="between mb-16"><h2 style="font-size:18px">逛分类</h2><a class="small muted" href="category.html">全部分类</a></div>
    <div class="cat-grid">${tiles}</div>
  </div>

  <div class="mt-24">
    <div class="between mb-16"><h2 style="font-size:18px">本季推荐</h2><a class="small muted" href="product-list.html">查看全部 →</a></div>
    <div class="grid-products">${PRODUCTS.slice(0, 8).map(productCard).join('')}</div>
  </div>

  <div class="mt-24 grid-3">
    <div class="panel pad"><div class="row"><span style="font-size:22px">🛡️</span><div><div style="font-weight:700">正品保障</div><div class="small muted">官方授权，假一赔十</div></div></div></div>
    <div class="panel pad"><div class="row"><span style="font-size:22px">🚀</span><div><div style="font-weight:700">48 小时发货</div><div class="small muted">现货商品当日出库</div></div></div></div>
    <div class="panel pad"><div class="row"><span style="font-size:22px">↩️</span><div><div style="font-weight:700">7 天无理由</div><div class="small muted">不影响二次销售可退</div></div></div></div>
  </div>
</div></section>`;
};

const pageCategory = () => `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><a href="category.html">家居日用</a><span class="sep">›</span><span>杯壶</span></div>

  <div class="panel pad-sm mb-16">
    <div class="row wrap">
      <span class="small muted">二级分类</span>
      <a class="chip active" href="category.html">全部</a>
      <a class="chip" href="category.html">杯壶</a>
      <a class="chip" href="category.html">摆件</a>
      <a class="chip" href="category.html">香薰</a>
    </div>
  </div>

  <div class="between mb-16">
    <div class="row"><h2 style="font-size:17px">家居日用</h2><span class="small muted">共 ${PRODUCTS.filter((p) => p.cat === '家居日用').length} 件商品</span></div>
    <form data-ok="原型演示：已按所选方式重新排序"><select class="select" style="width:150px;height:34px"><option>默认排序</option><option>价格从低到高</option><option>价格从高到低</option><option>最新上架</option></select></form>
  </div>

  <div class="grid-products">${PRODUCTS.filter((p) => p.cat === '家居日用').map(productCard).join('')}</div>
  ${pager(1, 1, PRODUCTS.filter((p) => p.cat === '家居日用').length + ' 件商品')}
</div></section>`;

const pageList = () => {
  const cats = CATS.map((c) => `
    <a href="category.html" class="active">${c.icon} ${c.name}</a>
    ${c.subs.map((s) => `<a href="product-list.html" class="sub">${s}</a>`).join('')}`).join('');
  return `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><span>全部商品</span></div>
  <div class="row" style="align-items:flex-start;gap:20px">
    <aside class="sidebar panel">
      <div class="title">商品分类</div>
      ${cats}
    </aside>
    <div class="content-main">
      <div class="panel mb-16">
        <div class="filter-bar" style="border-bottom:1px solid var(--line)">
          <span class="small muted">排序</span>
          <a class="chip active" href="product-list.html">综合</a>
          <a class="chip" href="product-list.html">价格 ↑</a>
          <a class="chip" href="product-list.html">价格 ↓</a>
          <span style="flex:1"></span>
          <label class="row small" style="gap:6px"><input type="checkbox" class="checkbox"> 仅看有货</label>
        </div>
        <div class="filter-bar">
          <span class="small muted">已选</span>
          <span class="badge badge-paid">关键词：水杯 ×</span>
          <span class="badge badge-paid">家居日用 ×</span>
          <a class="small muted" href="product-list.html">清空</a>
        </div>
      </div>
      <div class="grid-products">${PRODUCTS.map(productCard).join('')}</div>
      ${pager(1, 3)}
    </div>
  </div>
</div></section>`;
};

const pageDetail = () => `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><a href="category.html">家居日用</a><span class="sep">›</span><a href="product-list.html">杯壶</a><span class="sep">›</span><span>星际和平水杯</span></div>

  <div class="panel pad">
    <div class="row" style="align-items:flex-start;gap:32px">
      <div style="flex:1;min-width:320px">
        <div class="detail-gallery">
          <div class="detail-main-img ph">🥤</div>
          <div class="detail-thumbs">
            <div class="ph">🥤</div><div class="ph alt-1">🥤</div><div class="ph alt-2">🥤</div><div class="ph alt-3">🥤</div><div class="ph alt-4">🥤</div>
          </div>
        </div>
      </div>

      <div style="flex:1;min-width:320px">
        <h1 style="font-size:22px">星际和平水杯 · 500ml<span class="seal detail-seal">IPC 认证正品</span></h1>
        <p class="muted small mt-8">和平号工作室 · 316 不锈钢内胆 · 保温 12 小时</p>
        <div class="mt-16" style="background:#fdf7f7;border:1px solid #f6e3e3;border-radius:10px;padding:14px 16px">
          <div class="price-line"><small>¥</small><span id="skuPrice">59.00</span></div>
          <div class="small muted mt-8">库存 <b id="skuStock">128</b> 件 · 已售 326 件</div>
        </div>

        <div class="mt-24">
          <div class="small muted mb-8">规格</div>
          <div class="sku-list">
            <button class="sku active" data-sku data-price="59.00" data-stock="128">500ml 星白</button>
            <button class="sku" data-sku data-price="59.00" data-stock="46">500ml 深空黑</button>
            <button class="sku is-out" data-sku data-price="69.00" data-stock="0">750ml 星白（缺货）</button>
          </div>
        </div>

        <div class="mt-24 row">
          <div class="qty" data-qty data-min="1" data-max="128">
            <button data-dec>−</button><input value="1" readonly><button data-inc>+</button>
          </div>
          <span class="small muted">单次最多 99 件</span>
        </div>

        <div class="mt-24 row">
          <a class="btn btn-primary" id="addBtn" href="cart.html" style="height:44px;padding:0 28px">加入购物车</a>
          <a class="btn" id="buyBtn" href="checkout.html" style="height:44px;padding:0 28px">立即购买</a>
        </div>

        <div class="mt-24 small muted" style="border-top:1px dashed var(--line);padding-top:14px">
          正品保障 · 48 小时内发货 · 7 天无理由退货 · 支持开具电子发票
        </div>
      </div>
    </div>
  </div>

  <div class="panel pad mt-16">
    <h3 style="font-size:16px;margin-bottom:12px">商品参数</h3>
    <table class="spec-table">
      <tr><th>品牌</th><td>和平号工作室</td><th>容量</th><td>500ml</td></tr>
      <tr><th>材质</th><td>316 不锈钢 + 食品级 PP</td><th>保温</th><td>≥ 12 小时</td></tr>
      <tr><th>重量</th><td>约 320g</td><th>产地</th><td>星际和平港</td></tr>
    </table>
    <h3 style="font-size:16px;margin:20px 0 12px">商品详情</h3>
    <p class="muted">杯身采用哑光喷涂，握感细腻；杯口做圆润处理，直饮不硌嘴。配套硅胶杯垫可拆洗，适合通勤与出差携带。</p>
  </div>

  <div class="mt-24">
    <div class="between mb-16"><h2 style="font-size:18px">看了又看</h2><a class="small muted" href="product-list.html">更多 →</a></div>
    <div class="grid-products">${PRODUCTS.slice(1, 5).map(productCard).join('')}</div>
  </div>
</div></section>`;

const authShell = (title, sub, form, footer) => `
<section class="auth-wrap"><div class="auth-card">
  <h2>${title}</h2>
  <p class="muted small mb-24">${sub}</p>
  ${form}
  <div class="small muted center mt-16">${footer}</div>
</div></section>`;

const captchaField = (label = '图形验证码') => `
<div class="field">
  <label>${label}<span class="req">*</span></label>
  <div class="row" style="gap:10px">
    <input class="input" placeholder="请输入右侧 4 位字符" style="flex:1">
    <div class="captcha-box" data-demo="原型演示：验证码已刷新（真实实现由服务端生成）">K7Q2</div>
  </div>
  <div class="help">不区分大小写，5 分钟内有效，校验后立即失效</div>
</div>`;

const pageLogin = () => authShell('登录', '使用你的星际和平账号继续', `
  <form data-ok="原型演示：登录成功，会跳转到首页">
    <div class="field"><label>用户名<span class="req">*</span></label><input class="input" value="星野" placeholder="请输入用户名"></div>
    <div class="field"><label>密码<span class="req">*</span></label><input class="input" type="password" value="123456" placeholder="请输入密码"></div>
    ${captchaField()}
    <button class="btn btn-primary btn-block" style="height:42px" type="submit">登录</button>
  </form>`, `还没有账号？<a href="register.html" style="color:var(--brand)">立即注册</a> · <a href="index.html">返回首页</a>`);

const pageRegister = () => authShell('注册', '创建账号，开始你的第一次下单', `
  <form data-ok="原型演示：注册成功，会自动登录并跳转首页">
    <div class="field"><label>用户名<span class="req">*</span></label><input class="input" placeholder="4–20 位字母、数字或下划线"><div class="help">登录后不可修改</div></div>
    <div class="field"><label>密码<span class="req">*</span></label><input class="input" type="password" placeholder="6–20 位，需包含字母和数字"></div>
    <div class="field"><label>确认密码<span class="req">*</span></label><input class="input" type="password" placeholder="再次输入密码"></div>
    <div class="field"><label>手机号</label><input class="input" placeholder="选填，用于接收订单通知"></div>
    ${captchaField()}
    <button class="btn btn-primary btn-block" style="height:42px" type="submit">注册</button>
  </form>`, `已有账号？<a href="login.html" style="color:var(--brand)">去登录</a>`);

const userSide = (active) => {
  const items = [
    ['user-center.html', '个人中心', 'center'],
    ['order-list.html', '我的订单', 'orders'],
    ['address-list.html', '收货地址', 'addr'],
    ['feedback-list.html', '我的投诉与反馈', 'fb'],
    ['login.html', '修改密码', 'pwd'],
  ];
  return `<aside class="sidebar panel" style="flex:0 0 200px;width:200px">
    <div class="title">我的账户</div>
    ${items.map(([href, label, key]) => `<a href="${href}" class="${active === key ? 'active' : ''}">${label}</a>`).join('')}
  </aside>`;
};

const pageCenter = () => `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><span>个人中心</span></div>
  <div class="row" style="align-items:flex-start;gap:20px">
    ${userSide('center')}
    <div class="content-main">
      <div class="panel pad">
        <div class="row" style="gap:16px">
          <span class="avatar" style="width:52px;height:52px;font-size:18px">星</span>
          <div>
            <div style="font-size:18px;font-weight:700">星野</div>
            <div class="small muted">手机号 138****6688 · 注册于 2026-09-02 · 上次登录 2026-11-18 09:12</div>
          </div>
          <span style="flex:1"></span>
          <a class="btn btn-sm" href="login.html" data-demo="原型演示：进入修改密码">修改密码</a>
        </div>
      </div>
      <div class="stats mt-16" style="grid-template-columns:repeat(4,1fr)">
        <a class="stat" href="order-list.html"><div class="k">待支付</div><div class="v">1</div></a>
        <a class="stat" href="order-list.html"><div class="k">待收货</div><div class="v">1</div></a>
        <a class="stat" href="order-list.html"><div class="k">已完成</div><div class="v">2</div></a>
        <a class="stat" href="feedback-list.html"><div class="k">投诉/反馈</div><div class="v">2</div></a>
      </div>
      <div class="panel mt-16">
        <div class="pad-sm between" style="border-bottom:1px solid var(--line)"><b>最近订单</b><a class="small muted" href="order-list.html">全部订单 →</a></div>
        <table class="table">
          <thead><tr><th>订单号</th><th>下单时间</th><th>金额</th><th>状态</th><th class="right">操作</th></tr></thead>
          <tbody>
          ${ORDERS.slice(0, 3).map((o) => `<tr><td class="mono">${o.no}</td><td>${o.time}</td><td>${money(o.total)}</td><td>${badgeOrder(o.status)}</td><td class="right"><a class="btn btn-sm" href="order-detail.html">查看</a></td></tr>`).join('')}
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div></section>`;

const pageCart = () => {
  const rows = [
    { p: PRODUCTS[0], qty: 1, spec: '500ml 星白' },
    { p: PRODUCTS[1], qty: 1, spec: '加厚版 · 深空蓝' },
  ].map(({ p, qty, spec }) => `
  <div class="cart-row" data-price="${p.price}">
    <input type="checkbox" class="checkbox" checked>
    <div class="thumb ph ${p.alt}">${p.emoji}</div>
    <div>
      <a href="product-detail.html" style="font-weight:600">${p.name}</a>
      <div class="small muted">规格：${spec}</div>
      <div class="small muted">库存 ${p.stock}</div>
    </div>
    <div>${money(p.price)}</div>
    <div class="qty" data-qty data-min="1" data-max="${p.stock}">
      <button data-dec>−</button><input value="${qty}" readonly><button data-inc>+</button>
    </div>
    <div class="right"><b style="color:var(--danger)" data-subtotal>${money(p.price * qty)}</b></div>
  </div>`).join('');

  return `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><span>购物车</span></div>
  <div data-cart>
  <div class="panel">
    <div class="cart-row" style="background:#fafbfe;color:var(--muted);font-size:12px">
      <input type="checkbox" class="checkbox" data-check-all checked>
      <span style="grid-column:span 2">商品</span><span>单价</span><span>数量</span><span class="right">小计</span>
    </div>
    ${rows}
    <div class="empty hide" id="cartEmpty"><div class="icon">🛒</div>购物车还是空的<a href="product-list.html">去逛逛 →</a></div>
  </div>
  <div class="cart-bar mt-16 panel">
    <div class="row">
      <label class="row small" style="gap:8px"><input type="checkbox" class="checkbox" data-check-all checked> 全选</label>
      <span class="small muted">已选 <b data-count>2</b> 件</span>
    </div>
    <div class="row">
      <span class="small muted">合计（不含运费）</span>
      <span class="total-amount" data-total>¥148.00</span>
      <a class="btn btn-primary" href="checkout.html" style="height:42px;padding:0 26px">去结算</a>
    </div>
  </div>
  </div>
  <p class="small muted mt-8">运费本期固定 ¥0.00；库存变化会在结算时重新校验。</p>
</div></section>`;
};

const pageCheckout = () => `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><a href="cart.html">购物车</a><span class="sep">›</span><span>确认订单</span></div>
  <div class="panel pad-sm mb-16">
    <div class="steps">
      <div class="step done"><div class="n">✓</div>购物车</div>
      <div class="step active"><div class="n">2</div>确认订单</div>
      <div class="step"><div class="n">3</div>支付</div>
      <div class="step"><div class="n">4</div>完成</div>
    </div>
  </div>

  <div class="row" style="align-items:flex-start;gap:20px">
    <div class="content-main">
      <div class="panel pad">
        <div class="between mb-16"><b>收货地址</b><a class="btn btn-sm" href="address-form.html">新增地址</a></div>
        <div class="grid-2">
          <div class="addr-card active">
            <div class="who">星野 · 138****6688 <span class="badge badge-paid" style="margin-left:6px">默认</span></div>
            <div class="small muted">星际和平港 中央区 舰桥大道 1024 号 3 栋 1201</div>
          </div>
          <div class="addr-card">
            <div class="who">星野 · 138****6688</div>
            <div class="small muted">近地轨道仓 A 区 收货点 7 号柜</div>
          </div>
        </div>
      </div>

      <div class="panel mt-16">
        <div class="pad-sm" style="border-bottom:1px solid var(--line)"><b>商品明细</b></div>
        <table class="table">
          <thead><tr><th>商品</th><th>单价</th><th>数量</th><th class="right">小计</th></tr></thead>
          <tbody>
            <tr><td><div class="row"><span class="ph" style="width:44px;height:44px;font-size:18px">🥤</span><div>星际和平水杯 · 500ml<div class="small muted">500ml 星白</div></div></div></td><td>¥59.00</td><td>1</td><td class="right">¥59.00</td></tr>
            <tr><td><div class="row"><span class="ph alt-1" style="width:44px;height:44px;font-size:18px">🎒</span><div>星轨帆布包 · 加厚版<div class="small muted">深空蓝</div></div></div></td><td>¥89.00</td><td>1</td><td class="right">¥89.00</td></tr>
          </tbody>
        </table>
        <div class="pad-sm">
          <label class="small muted">订单备注</label>
          <input class="input mt-8" placeholder="选填，如：工作日送达 / 放前台" maxlength="200">
        </div>
      </div>
    </div>

    <aside class="panel pad" style="width:340px;flex:0 0 340px">
      <b>费用明细</b>
      <div class="divider"></div>
      <div class="between small"><span class="muted">商品总额</span><span>¥148.00</span></div>
      <div class="between small mt-8"><span class="muted">运费</span><span>¥0.00</span></div>
      <div class="divider"></div>
      <div class="between"><span>应付</span><span class="total-amount">¥148.00</span></div>
      <form data-ok="原型演示：订单已提交（真实实现会校验库存并跳转支付页）" class="mt-16">
        <button class="btn btn-primary btn-block" style="height:44px" type="submit">提交订单</button>
      </form>
      <p class="small muted mt-8">提交前会再次校验库存，库存不足会提示并回到购物车。</p>
    </aside>
  </div>
</div></section>`;

const pagePay = () => `
<section class="page"><div class="container">
  <div class="panel pay-box">
    <div style="font-size:40px">💳</div>
    <h2 style="margin-top:8px">等待支付</h2>
    <p class="muted small mt-8">订单号 <span class="mono">20261118093012</span></p>
    <div class="pay-amount mt-16">¥388.00</div>
    <p class="small muted mt-8">请在 <b class="mono" data-countdown="1800">30:00</b> 内完成支付，超时订单会自动取消并释放库存</p>
    <div class="row center mt-24" style="justify-content:center">
      <form data-ok="原型演示：支付成功，订单状态会变为「已支付」"><button class="btn btn-primary" style="height:44px;padding:0 32px" type="submit">模拟支付成功</button></form>
      <form data-ok="原型演示：已取消支付，订单状态会变为「已取消」并回补库存"><button class="btn" style="height:44px;padding:0 32px" type="submit">取消支付</button></form>
    </div>
    <p class="small muted mt-24" style="border-top:1px dashed var(--line);padding-top:14px">本页为模拟支付，不接入真实第三方通道，不会产生任何扣款。</p>
  </div>
</div></section>`;

const orderActions = (o) => {
  const t = {
    PENDING_PAYMENT: `<a class="btn btn-sm btn-primary" href="pay.html">去支付</a><button class="btn btn-sm" data-demo="原型演示：订单已取消，库存回补">取消订单</button>`,
    PAID: `<a class="btn btn-sm btn-primary" href="feedback-form.html">投诉</a><a class="btn btn-sm" href="order-detail.html">查看详情</a>`,
    SHIPPED: `<a class="btn btn-sm btn-primary" href="feedback-form.html">投诉</a><button class="btn btn-sm" data-confirm="原型演示：确认收货后订单变为「已完成」">确认收货</button>`,
    COMPLETED: `<a class="btn btn-sm" href="feedback-form.html">投诉</a><a class="btn btn-sm" href="product-detail.html">再次购买</a>`,
    CANCELLED: `<button class="btn btn-sm" data-confirm="原型演示：已删除该订单（仅终态可删）">删除订单</button>`,
  }[o.status];
  return t;
};

const orderCard = (o) => `
<div class="order-card">
  <div class="order-head">
    <div><span class="no mono">${o.no}</span> <span style="margin-left:12px">${o.time}</span>${o.courier ? ` <span style="margin-left:12px">${o.courier}</span>` : ''}</div>
    <div>${badgeOrder(o.status)}</div>
  </div>
  <div class="order-body">
    <div class="thumb ph ${o.items[0].p.alt}">${o.items[0].p.emoji}</div>
    <div style="flex:1">
      <div style="font-weight:600">${o.items[0].p.name}</div>
      <div class="small muted">${o.items.length > 1 ? `等 ${o.items.length} 件商品` : '共 1 件商品'} · ${o.items[0].p.cat}</div>
    </div>
    <div class="right"><div style="font-weight:700;color:var(--danger)">${money(o.total)}</div><div class="small muted">含运费 ¥0.00</div></div>
  </div>
  <div class="order-foot">
    <div class="small muted">${o.status === 'SHIPPED' ? '卖家已发货，注意查收' : o.status === 'COMPLETED' ? '交易已完成' : o.status === 'PENDING_PAYMENT' ? '等待支付' : ''}</div>
    <div class="row" style="gap:8px">${orderActions(o)}</div>
  </div>
</div>`;

const pageOrderList = () => `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><span>我的订单</span></div>
  <div class="panel mb-16" style="padding:0 8px">
    <div class="tabs">
      <a class="active" href="order-list.html">全部</a><a href="order-list.html">待支付</a><a href="order-list.html">已支付</a><a href="order-list.html">已发货</a><a href="order-list.html">已完成</a><a href="order-list.html">已取消</a>
    </div>
  </div>
  ${ORDERS.map(orderCard).join('')}
  ${pager(1, 1, '5 笔订单')}
  <p class="small muted">提示：只有「已支付 / 已发货 / 已完成」的订单才显示「投诉」入口。</p>
</div></section>`;

const pageOrderDetail = () => `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><a href="order-list.html">我的订单</a><span class="sep">›</span><span class="mono">20261116102133</span></div>

  <div class="panel pad mb-16">
    <div class="between">
      <div>
        <div class="row"><h2 style="font-size:18px">订单 20261116102133</h2>${badgeOrder('COMPLETED')}</div>
        <div class="small muted mt-8">下单时间 2026-11-16 10:21 · 支付时间 2026-11-16 10:22 · 完成时间 2026-11-18 09:05</div>
      </div>
      <div class="row" style="gap:8px">
        <a class="btn btn-sm" href="feedback-form.html">投诉此订单</a>
        <a class="btn btn-sm" href="product-detail.html">再次购买</a>
      </div>
    </div>
  </div>

  <div class="row" style="align-items:flex-start;gap:20px">
    <div class="content-main">
      <div class="panel pad">
        <b>状态时间线</b>
        <div class="timeline mt-16">
          <div class="item done"><div class="t">已提交订单</div><div class="d">2026-11-16 10:21</div></div>
          <div class="item done"><div class="t">支付成功</div><div class="d">2026-11-16 10:22</div></div>
          <div class="item done"><div class="t">卖家已发货 · 顺丰速运 SF1234567890</div><div class="d">2026-11-17 09:40</div></div>
          <div class="item done"><div class="t">确认收货，交易完成</div><div class="d">2026-11-18 09:05</div></div>
        </div>
      </div>

      <div class="panel mt-16">
        <div class="pad-sm" style="border-bottom:1px solid var(--line)"><b>商品明细</b></div>
        <table class="table">
          <thead><tr><th>商品</th><th>单价</th><th>数量</th><th class="right">小计</th></tr></thead>
          <tbody><tr><td><div class="row"><span class="ph alt-2" style="width:44px;height:44px;font-size:18px">🕯️</span><div>深空香薰蜡烛 · 冷杉<div class="small muted">200g</div></div></div></td><td>¥129.00</td><td>1</td><td class="right">¥129.00</td></tr></tbody>
        </table>
      </div>
    </div>

    <aside style="width:320px;flex:0 0 320px">
      <div class="panel pad">
        <b>收货信息</b>
        <div class="divider"></div>
        <div style="font-weight:600">星野 · 138****6688</div>
        <div class="small muted mt-8">星际和平港 中央区 舰桥大道 1024 号 3 栋 1201</div>
      </div>
      <div class="panel pad mt-16">
        <b>金额</b>
        <div class="divider"></div>
        <div class="between small"><span class="muted">商品总额</span><span>¥129.00</span></div>
        <div class="between small mt-8"><span class="muted">运费</span><span>¥0.00</span></div>
        <div class="between mt-8"><span>实付</span><span class="total-amount">¥129.00</span></div>
      </div>
    </aside>
  </div>
</div></section>`;

const pageAddressList = () => `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><span>收货地址</span></div>
  <div class="row" style="align-items:flex-start;gap:20px">
    ${userSide('addr')}
    <div class="content-main">
      <div class="between mb-16"><h2 style="font-size:17px">收货地址（3/20）</h2><a class="btn btn-primary btn-sm" href="address-form.html">+ 新增地址</a></div>
      <div class="grid-2">
        <div class="addr-card">
          <div class="between"><div class="who">星野 · 138****6688</div><span class="badge badge-paid">默认</span></div>
          <div class="small muted mt-8">星际和平港 中央区 舰桥大道 1024 号 3 栋 1201</div>
          <div class="row mt-16" style="gap:8px"><a class="btn btn-sm" href="address-form.html">编辑</a><button class="btn btn-sm" data-demo="原型演示：默认地址已切换">设为默认</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：地址已删除">删除</button></div>
        </div>
        <div class="addr-card">
          <div class="who">星野 · 138****6688</div>
          <div class="small muted mt-8">近地轨道仓 A 区 收货点 7 号柜</div>
          <div class="row mt-16" style="gap:8px"><a class="btn btn-sm" href="address-form.html">编辑</a><button class="btn btn-sm" data-demo="原型演示：默认地址已切换">设为默认</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：地址已删除">删除</button></div>
        </div>
        <div class="addr-card">
          <div class="who">星野（公司）· 139****2233</div>
          <div class="small muted mt-8">深空研发中心 B 座 8 层 前台代收</div>
          <div class="row mt-16" style="gap:8px"><a class="btn btn-sm" href="address-form.html">编辑</a><button class="btn btn-sm" data-demo="原型演示：默认地址已切换">设为默认</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：地址已删除">删除</button></div>
        </div>
      </div>
    </div>
  </div>
</div></section>`;

const pageAddressForm = () => `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><a href="address-list.html">收货地址</a><span class="sep">›</span><span>编辑地址</span></div>
  <div class="row" style="align-items:flex-start;gap:20px">
    ${userSide('addr')}
    <div class="content-main">
      <div class="panel pad" style="max-width:640px">
        <h2 style="font-size:17px;margin-bottom:16px">编辑收货地址</h2>
        <form data-ok="原型演示：地址已保存（真实实现会做归属校验）">
          <div class="grid-2">
            <div class="field"><label>收货人<span class="req">*</span></label><input class="input" value="星野"></div>
            <div class="field"><label>手机号<span class="req">*</span></label><input class="input" value="13800006688"></div>
          </div>
          <div class="grid-3">
            <div class="field"><label>省<span class="req">*</span></label><input class="input" value="星际和平港"></div>
            <div class="field"><label>市<span class="req">*</span></label><input class="input" value="中央区"></div>
            <div class="field"><label>区<span class="req">*</span></label><input class="input" value="舰桥街道"></div>
          </div>
          <div class="field"><label>详细地址<span class="req">*</span></label><textarea class="textarea" style="min-height:80px">舰桥大道 1024 号 3 栋 1201</textarea></div>
          <div class="field"><label class="row" style="gap:8px"><input type="checkbox" class="checkbox" checked> 设为默认地址</label></div>
          <div class="row">
            <button class="btn btn-primary" type="submit">保存</button>
            <a class="btn" href="address-list.html">取消</a>
          </div>
        </form>
      </div>
    </div>
  </div>
</div></section>`;

const fbItem = (f) => `
<div class="fb-item">
  <div class="fb-head">
    <div class="row wrap">
      <span class="badge ${f.type === 'COMPLAINT' ? 'badge-danger' : 'badge-paid'}">${f.type === 'COMPLAINT' ? '投诉' : '反馈'}</span>
      ${f.type === 'COMPLAINT' ? `<span class="small muted mono">订单 ${f.order}</span>` : '<span class="small muted">未关联订单</span>'}
      <span class="small muted">${f.category}</span>
    </div>
    <div class="row">${badgeFb(f.status)}<span class="small muted">${f.time}</span></div>
  </div>
  <div class="content">${f.content}</div>
  ${f.reply ? `<div class="fb-reply"><b>卖家回复</b><div class="mt-8">${f.reply}</div></div>` : ''}
  <div class="row mt-16" style="gap:8px">
    <a class="btn btn-sm" href="feedback-detail.html">查看详情</a>
    ${f.status === 'PENDING' ? `<button class="btn btn-sm" data-confirm="原型演示：已撤销，状态变为「已关闭」">撤销</button>` : ''}
  </div>
</div>`;

const pageFeedbackList = () => `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><span>我的投诉与反馈</span></div>
  <div class="row" style="align-items:flex-start;gap:20px">
    ${userSide('fb')}
    <div class="content-main">
      <div class="between mb-16">
        <h2 style="font-size:17px">我的投诉与反馈（2）</h2>
        <a class="btn btn-primary btn-sm" href="feedback-form.html">+ 提交反馈</a>
      </div>
      <div class="panel mb-16" style="padding:0 8px">
        <div class="tabs"><a class="active" href="feedback-list.html">全部</a><a href="feedback-list.html">投诉</a><a href="feedback-list.html">反馈</a><a href="feedback-list.html">待处理</a><a href="feedback-list.html">已回复</a></div>
      </div>
      ${FEEDBACKS.map(fbItem).join('')}
    </div>
  </div>
</div></section>`;

const pageFeedbackForm = () => `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><a href="feedback-list.html">我的投诉与反馈</a><span class="sep">›</span><span>提交投诉</span></div>
  <div class="panel pad" style="max-width:720px;margin:0 auto">
    <h2 style="font-size:18px">提交投诉</h2>
    <p class="small muted mt-8 mb-24">投诉仅限你本人已购买的订单（已支付、已发货、已完成）。未支付和已取消的订单不能投诉。</p>

    <div class="panel pad-sm mb-16" style="background:#fafbfe">
      <div class="row">
        <span class="ph alt-2" style="width:48px;height:48px;font-size:20px">🕯️</span>
        <div>
          <div style="font-weight:600">深空香薰蜡烛 · 冷杉</div>
          <div class="small muted mono">订单 20261116102133 · 2026-11-16 10:21 · ¥129.00</div>
        </div>
        <span style="flex:1"></span>
        <span class="badge badge-completed">已完成</span>
      </div>
    </div>

    <form data-ok="原型演示：投诉已提交，状态为「待处理」">
      <div class="field">
        <label>类型<span class="req">*</span></label>
        <div class="row"><span class="chip active">订单投诉</span><a class="chip" href="feedback-form.html">普通反馈</a></div>
        <div class="help">切换为「普通反馈」不需要绑定订单</div>
      </div>
      <div class="field">
        <label>问题分类<span class="req">*</span></label>
        <select class="select"><option>商品质量</option><option>物流发货</option><option>服务态度</option><option>其他</option></select>
      </div>
      <div class="field">
        <label>问题描述<span class="req">*</span></label>
        <textarea class="textarea" data-textlen data-min="10" data-max="500" placeholder="请描述遇到的具体问题，例如：收到时杯口有裂纹，外包装完好……">蜡烛收到时玻璃杯有一道细裂纹，外包装完好，应该是出厂检验没发现。</textarea>
        <div class="help"><span data-textlen-out>0 / 500</span> · 10–500 字，请勿填写手机号等敏感信息</div>
      </div>
      <div class="field">
        <label>联系方式</label>
        <input class="input" value="138****6688" placeholder="选填，便于卖家联系你">
        <div class="help">默认使用账号绑定手机号，可修改</div>
      </div>
      <div class="field">
        <label>图片凭证 <span class="badge" style="margin-left:4px">P1</span></label>
        <div class="row wrap">
          <div class="ph" style="width:80px;height:80px;font-size:22px">＋</div>
          <div class="small muted">最多 3 张，单张 ≤ 5MB（加分项，本期可不实现）</div>
        </div>
      </div>
      <div class="row">
        <button class="btn btn-primary" type="submit">提交投诉</button>
        <a class="btn" href="feedback-list.html">取消</a>
      </div>
    </form>
  </div>
</div></section>`;

const pageFeedbackDetail = () => {
  const f = FEEDBACKS[0];
  return `
<section class="page"><div class="container">
  <div class="breadcrumb"><a href="index.html">首页</a><span class="sep">›</span><a href="feedback-list.html">我的投诉与反馈</a><span class="sep">›</span><span class="mono">${f.no}</span></div>
  <div class="row" style="align-items:flex-start;gap:20px">
    ${userSide('fb')}
    <div class="content-main">
      <div class="panel pad">
        <div class="between">
          <div class="row">
            <span class="badge badge-danger">投诉</span>
            ${badgeFb(f.status)}
            <span class="small muted mono">${f.no}</span>
          </div>
          <span class="small muted">提交于 ${f.time}</span>
        </div>
        <div class="divider"></div>
        <div class="small muted">关联订单</div>
        <a class="mono" href="order-detail.html">${f.order}</a> · 深空香薰蜡烛 · 冷杉 · ¥129.00
        <div class="mt-16"><b>问题描述</b><p class="mt-8">${f.content}</p></div>
        <div class="small muted mt-16">分类：${f.category} · 联系方式：138****6688</div>
      </div>

      <div class="panel pad mt-16">
        <b>处理进度</b>
        <div class="timeline mt-16">
          <div class="item done"><div class="t">提交投诉</div><div class="d">2026-11-16 11:02</div></div>
          <div class="item done"><div class="t">卖家已受理</div><div class="d">2026-11-16 12:30</div></div>
          <div class="item done"><div class="t">卖家已回复</div><div class="d">2026-11-16 15:12</div></div>
          <div class="item"><div class="t muted">等待卖家关闭</div><div class="d">已回复不等于办结，卖家关闭后进入终态</div></div>
        </div>
      </div>

      <div class="panel pad mt-16">
        <b>卖家回复</b>
        <div class="fb-reply mt-16">${f.reply}<div class="small muted mt-8">2026-11-16 15:12 · 卖家 admin</div></div>
        <div class="row mt-16" style="gap:8px">
          <button class="btn btn-sm" data-demo="原型演示：已补充说明（真实实现会追加一条沟通记录）">补充说明</button>
          <button class="btn btn-sm" data-demo="原型演示：已标记为已解决">我已解决</button>
        </div>
      </div>
    </div>
  </div>
</div></section>`;
};

/* ---------------- 后台页面 ---------------- */
const adminHead = (title, desc, action = '') => `
<div class="page-title"><div><h1>${title}</h1><p>${desc}</p></div><div class="row">${action}</div></div>`;

const adminIndex = () => `
${adminHead('工作台', '今天先把「待发货」和「待处理投诉」清掉')}
<div class="stats">
  <div class="stat"><div class="k">今日订单</div><div class="v">18 <em>笔</em></div><div class="small muted">较昨日 +12%</div></div>
  <div class="stat"><div class="k">待发货</div><div class="v" style="color:var(--warn)">3 <em>笔</em></div><div class="small muted">已支付未发货</div></div>
  <div class="stat"><div class="k">在售商品</div><div class="v">42 <em>件</em></div><div class="small muted">含 7 个 SPU 下架</div></div>
  <div class="stat"><div class="k">待处理投诉</div><div class="v" style="color:var(--danger)">2 <em>条</em></div><div class="small muted">1 条超过 24 小时</div></div>
</div>

<div class="panel mt-16">
  <div class="pad-sm between" style="border-bottom:1px solid var(--line)"><b>待处理投诉</b><a class="small muted" href="feedback.html">全部 →</a></div>
  <table class="table">
    <thead><tr><th>类型</th><th>用户</th><th>关联订单</th><th>描述</th><th>提交时间</th><th>状态</th><th class="right">操作</th></tr></thead>
    <tbody>
      <tr><td><span class="badge badge-danger">投诉</span></td><td>星野</td><td class="mono">20261116102133</td><td class="muted">蜡烛玻璃杯有细裂纹，希望换货</td><td>11-16 11:02</td><td>${badgeFb('REPLIED')}</td><td class="right"><a class="btn btn-sm" href="feedback-detail.html">处理</a></td></tr>
      <tr><td><span class="badge badge-paid">反馈</span></td><td>星野</td><td>—</td><td class="muted">希望详情页标出模型实际尺寸</td><td>11-17 20:15</td><td>${badgeFb('PENDING')}</td><td class="right"><a class="btn btn-sm btn-primary" href="feedback-detail.html">处理</a></td></tr>
    </tbody>
  </table>
</div>

<div class="panel mt-16">
  <div class="pad-sm between" style="border-bottom:1px solid var(--line)"><b>最近订单</b><a class="small muted" href="order.html">全部 →</a></div>
  <table class="table">
    <thead><tr><th>订单号</th><th>用户</th><th>金额</th><th>状态</th><th>下单时间</th><th class="right">操作</th></tr></thead>
    <tbody>${ORDERS.slice(0, 4).map((o) => `<tr><td class="mono">${o.no}</td><td>星野</td><td>${money(o.total)}</td><td>${badgeOrder(o.status)}</td><td>${o.time}</td><td class="right"><a class="btn btn-sm" href="order-detail.html">详情</a></td></tr>`).join('')}</tbody>
  </table>
</div>`;

const adminCategory = () => `
${adminHead('分类管理', '两级分类：一级用于导航，二级用于商品归属', '<a class="btn btn-primary btn-sm" href="#" data-demo="原型演示：展开新增分类表单">+ 新增分类</a>')}
<div class="panel">
  <table class="table">
    <thead><tr><th>名称</th><th>层级</th><th>描述</th><th>排序</th><th>前台可见</th><th>更新时间</th><th class="right">操作</th></tr></thead>
    <tbody>
      <tr><td><b>家居日用</b></td><td><span class="badge">一级</span></td><td class="muted">杯壶、摆件、香薰</td><td>1</td><td><label class="switch"><input type="checkbox" checked><span></span></label></td><td>2026-11-12 10:00</td><td class="right"><div class="actions"><button class="btn btn-sm" data-demo="原型演示：编辑分类">编辑</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：存在子分类，删除被拒绝（2004）">删除</button></div></td></tr>
      <tr><td class="muted" style="padding-left:32px">— 杯壶</td><td><span class="badge">二级</span></td><td class="muted">水杯、保温杯</td><td>1</td><td><label class="switch"><input type="checkbox" checked><span></span></label></td><td>2026-11-12 10:00</td><td class="right"><div class="actions"><button class="btn btn-sm" data-demo="原型演示：编辑分类">编辑</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：分类下有商品，删除被拒绝（2004）">删除</button></div></td></tr>
      <tr><td class="muted" style="padding-left:32px">— 摆件</td><td><span class="badge">二级</span></td><td class="muted">桌面摆件、模型</td><td>2</td><td><label class="switch"><input type="checkbox" checked><span></span></label></td><td>2026-11-12 10:00</td><td class="right"><div class="actions"><button class="btn btn-sm" data-demo="原型演示：编辑分类">编辑</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：已逻辑删除">删除</button></div></td></tr>
      <tr><td><b>文创周边</b></td><td><span class="badge">一级</span></td><td class="muted">徽章、海报、笔记本</td><td>2</td><td><label class="switch"><input type="checkbox" checked><span></span></label></td><td>2026-11-10 15:20</td><td class="right"><div class="actions"><button class="btn btn-sm" data-demo="原型演示：编辑分类">编辑</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：存在子分类，删除被拒绝（2004）">删除</button></div></td></tr>
      <tr><td><b>礼盒套装</b></td><td><span class="badge">一级</span></td><td class="muted">暂未上架商品</td><td>6</td><td><label class="switch"><input type="checkbox"><span></span></label></td><td>2026-11-08 09:30</td><td class="right"><div class="actions"><button class="btn btn-sm" data-demo="原型演示：编辑分类">编辑</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：已逻辑删除">删除</button></div></td></tr>
    </tbody>
  </table>
</div>`;

const adminProduct = () => `
${adminHead('商品管理', 'SPU 维度管理，上下架影响前台可见性', '<a class="btn btn-primary btn-sm" href="#" data-demo="原型演示：打开新增商品表单（含 SKU 子表）">+ 新增商品</a>')}
<div class="panel pad-sm mb-16">
  <form class="row wrap" data-ok="原型演示：已按条件筛选">
    <input class="input" style="width:200px" placeholder="商品名称">
    <select class="select" style="width:150px"><option>全部分类</option><option>家居日用</option><option>文创周边</option></select>
    <select class="select" style="width:130px"><option>全部状态</option><option>上架中</option><option>已下架</option></select>
    <button class="btn" type="submit">查询</button>
  </form>
</div>
<div class="panel">
  <table class="table">
    <thead><tr><th>商品</th><th>分类</th><th>最低价</th><th>SKU</th><th>总库存</th><th>上架</th><th>更新时间</th><th class="right">操作</th></tr></thead>
    <tbody>
      ${PRODUCTS.slice(0, 6).map((p, i) => `<tr>
        <td><div class="row"><span class="ph ${p.alt}" style="width:38px;height:38px;font-size:16px">${p.emoji}</span><div>${p.name}<div class="small muted mono">SPU-${1001 + i}</div></div></div></td>
        <td class="muted">${p.cat} / ${p.sub}</td>
        <td>${money(p.price)}</td>
        <td>${2 + (i % 3)}</td>
        <td>${p.stock}</td>
        <td><label class="switch"><input type="checkbox" ${i === 5 ? '' : 'checked'}><span></span></label></td>
        <td class="muted">2026-11-1${(i % 8) + 1} 10:00</td>
        <td class="right"><div class="actions"><a class="btn btn-sm" href="item.html">SKU</a><button class="btn btn-sm" data-demo="原型演示：编辑商品">编辑</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：该商品存在未完结订单，删除被拒绝（2005）">删除</button></div></td>
      </tr>`).join('')}
    </tbody>
  </table>
</div>`;

const adminItem = () => `
${adminHead('SKU 管理', 'SPU-1001 · 星际和平水杯 · 500ml', '<button class="btn btn-primary btn-sm" data-demo="原型演示：打开新增 SKU 表单">+ 新增 SKU</button>')}
<div class="panel pad mb-16">
  <div class="row"><span class="ph" style="width:56px;height:56px;font-size:24px">🥤</span>
    <div><div style="font-weight:700">星际和平水杯 · 500ml</div><div class="small muted">家居日用 / 杯壶 · 上架中 · 3 个 SKU · 总库存 174</div></div>
    <span style="flex:1"></span><a class="btn btn-sm" href="product.html" data-demo="原型演示：返回商品列表">返回商品列表</a>
  </div>
</div>
<div class="panel">
  <table class="table">
    <thead><tr><th>规格</th><th>价格</th><th>库存</th><th>版本</th><th>状态</th><th>更新时间</th><th class="right">操作</th></tr></thead>
    <tbody>
      <tr><td>500ml 星白</td><td>¥59.00</td><td>128</td><td class="mono">7</td><td><span class="badge badge-completed">在售</span></td><td class="muted">2026-11-17 09:12</td><td class="right"><div class="actions"><button class="btn btn-sm" data-demo="原型演示：编辑 SKU">编辑</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：该 SKU 存在未完结订单，删除被拒绝">删除</button></div></td></tr>
      <tr><td>500ml 深空黑</td><td>¥59.00</td><td>46</td><td class="mono">3</td><td><span class="badge badge-completed">在售</span></td><td class="muted">2026-11-15 16:40</td><td class="right"><div class="actions"><button class="btn btn-sm" data-demo="原型演示：编辑 SKU">编辑</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：已逻辑删除">删除</button></div></td></tr>
      <tr><td>750ml 星白</td><td>¥69.00</td><td>0</td><td class="mono">9</td><td><span class="badge badge-cancelled">售罄</span></td><td class="muted">2026-11-10 11:05</td><td class="right"><div class="actions"><button class="btn btn-sm" data-demo="原型演示：编辑 SKU">编辑</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：已逻辑删除">删除</button></div></td></tr>
    </tbody>
  </table>
</div>
<p class="small muted mt-8">库存扣减走乐观锁（version + stock >= qty 条件更新），后台改库存会校验 version 是否一致。</p>`;

const adminAccount = () => `
${adminHead('用户管理', '前台注册用户，可禁用或重置密码')}
<div class="panel pad-sm mb-16">
  <form class="row wrap" data-ok="原型演示：已按条件筛选">
    <input class="input" style="width:200px" placeholder="用户名 / 手机号">
    <select class="select" style="width:130px"><option>全部状态</option><option>启用</option><option>禁用</option></select>
    <button class="btn" type="submit">查询</button>
  </form>
</div>
<div class="panel">
  <table class="table">
    <thead><tr><th>ID</th><th>用户名</th><th>昵称</th><th>手机号</th><th>状态</th><th>订单数</th><th>注册时间</th><th class="right">操作</th></tr></thead>
    <tbody>
      <tr><td class="mono">1274</td><td>星野</td><td>星野</td><td>138****6688</td><td><span class="badge badge-completed">启用</span></td><td>5</td><td>2026-09-02</td><td class="right"><div class="actions"><button class="btn btn-sm" data-demo="原型演示：编辑用户信息">编辑</button><button class="btn btn-sm" data-confirm="原型演示：已重置为随机密码，需通知用户">重置密码</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：已禁用，该用户无法登录前台（1004）">禁用</button></div></td></tr>
      <tr><td class="mono">1273</td><td>orbit_x</td><td>轨道</td><td>139****2233</td><td><span class="badge badge-completed">启用</span></td><td>2</td><td>2026-10-11</td><td class="right"><div class="actions"><button class="btn btn-sm" data-demo="原型演示：编辑用户信息">编辑</button><button class="btn btn-sm" data-confirm="原型演示：已重置密码">重置密码</button><button class="btn btn-sm btn-danger" data-confirm="原型演示：已禁用">禁用</button></div></td></tr>
      <tr><td class="mono">1272</td><td>blacksun</td><td>黑日</td><td>137****9911</td><td><span class="badge badge-cancelled">禁用</span></td><td>0</td><td>2026-10-02</td><td class="right"><div class="actions"><button class="btn btn-sm" data-demo="原型演示：编辑用户信息">编辑</button><button class="btn btn-sm" data-confirm="原型演示：已重置密码">重置密码</button><button class="btn btn-sm" data-confirm="原型演示：已启用">启用</button></div></td></tr>
    </tbody>
  </table>
</div>`;

const adminOrder = () => `
${adminHead('订单管理', '筛选、查看详情、发货；已取消/已完成才可删除')}
<div class="panel pad-sm mb-16">
  <form class="row wrap" data-ok="原型演示：已按条件筛选">
    <input class="input" style="width:200px" placeholder="订单号">
    <input class="input" style="width:160px" placeholder="用户名">
    <select class="select" style="width:140px"><option>全部状态</option><option>待支付</option><option>已支付</option><option>已发货</option><option>已完成</option><option>已取消</option></select>
    <input class="input" style="width:180px" type="date">
    <button class="btn" type="submit">查询</button>
  </form>
</div>
<div class="panel">
  <table class="table">
    <thead><tr><th>订单号</th><th>用户</th><th>金额</th><th>状态</th><th>下单时间</th><th>物流</th><th class="right">操作</th></tr></thead>
    <tbody>${ORDERS.map((o) => `<tr>
      <td class="mono">${o.no}</td><td>星野</td><td>${money(o.total)}</td><td>${badgeOrder(o.status)}</td><td class="muted">${o.time}</td>
      <td class="small muted">${o.courier || '—'}</td>
      <td class="right"><div class="actions">
        <a class="btn btn-sm" href="order-detail.html">详情</a>
        ${o.status === 'PAID' ? '<a class="btn btn-sm btn-primary" href="order-detail.html">发货</a>' : ''}
        ${o.status === 'CANCELLED' || o.status === 'COMPLETED' ? '<button class="btn btn-sm btn-danger" data-confirm="原型演示：已逻辑删除">删除</button>' : '<button class="btn btn-sm btn-danger is-disabled" data-demo="原型演示：仅终态订单可删除（3002）">删除</button>'}
      </div></td>
    </tr>`).join('')}</tbody>
  </table>
</div>`;

const adminOrderDetail = () => {
  const o = ORDERS[0];
  return `
${adminHead('订单详情', `订单 ${o.no} · 卖家视角`, '<a class="btn btn-sm" href="order.html">返回列表</a>')}
<div class="row" style="align-items:flex-start;gap:18px">
  <div class="content-main">
    <div class="panel pad">
      <div class="between mb-16"><b>订单信息</b>${badgeOrder(o.status)}</div>
      <div class="grid-3 small">
        <div><span class="muted">订单号</span><div class="mono">${o.no}</div></div>
        <div><span class="muted">下单时间</span><div>${o.time}</div></div>
        <div><span class="muted">支付时间</span><div>2026-11-18 09:31</div></div>
        <div><span class="muted">用户</span><div>星野（ID 1274）</div></div>
        <div><span class="muted">应付</span><div style="font-weight:700;color:var(--danger)">${money(o.total)}</div></div>
        <div><span class="muted">运费</span><div>¥0.00</div></div>
      </div>
    </div>

    <div class="panel mt-16">
      <div class="pad-sm" style="border-bottom:1px solid var(--line)"><b>商品明细</b></div>
      <table class="table">
        <thead><tr><th>商品</th><th>规格</th><th>单价</th><th>数量</th><th class="right">小计</th></tr></thead>
        <tbody>
          <tr><td>舰桥模型 · 纪念版</td><td class="muted">1:2200 合金</td><td>¥299.00</td><td>1</td><td class="right">¥299.00</td></tr>
          <tr><td>星轨帆布包 · 加厚版</td><td class="muted">深空蓝</td><td>¥89.00</td><td>1</td><td class="right">¥89.00</td></tr>
        </tbody>
      </table>
      <div class="pad-sm small muted">订单明细为快照，卖家不可修改金额与商品。</div>
    </div>

    <div class="panel mt-16 pad">
      <b>收货信息（可编辑）</b>
      <div class="grid-2 mt-16">
        <div class="field"><label>收货人</label><input class="input" value="星野"></div>
        <div class="field"><label>手机号</label><input class="input" value="13800006688"></div>
      </div>
      <div class="field"><label>地址</label><input class="input" value="星际和平港 中央区 舰桥大道 1024 号 3 栋 1201"></div>
      <button class="btn" data-demo="原型演示：收货信息已保存">保存收货信息</button>
    </div>
  </div>

  <aside style="width:340px;flex:0 0 340px">
    <div class="panel pad">
      <b>发货</b>
      <p class="small muted mt-8">仅「已支付」订单可发货，发货后状态变为「已发货」。</p>
      <form class="mt-16" data-ok="原型演示：已发货，写入物流单号与发货时间">
        <div class="field"><label>物流公司<span class="req">*</span></label><select class="select"><option>顺丰速运</option><option>京东物流</option><option>中通快递</option></select></div>
        <div class="field"><label>物流单号<span class="req">*</span></label><input class="input" placeholder="如 SF1234567890"></div>
        <button class="btn btn-primary btn-block" type="submit">确认发货</button>
      </form>
    </div>
    <div class="panel pad mt-16">
      <b>状态时间线</b>
      <div class="timeline mt-16">
        <div class="item done"><div class="t">用户提交订单</div><div class="d">2026-11-18 09:30</div></div>
        <div class="item done"><div class="t">支付成功</div><div class="d">2026-11-18 09:31</div></div>
        <div class="item"><div class="t muted">等待卖家发货</div><div class="d">—</div></div>
      </div>
    </div>
  </aside>
</div>`;
};

const adminFeedback = () => `
${adminHead('投诉与反馈', '待处理优先；回复后仍需关闭才算办结')}
<div class="panel pad-sm mb-16">
  <form class="row wrap" data-ok="原型演示：已按条件筛选">
    <select class="select" style="width:130px"><option>全部类型</option><option>投诉</option><option>反馈</option></select>
    <select class="select" style="width:140px"><option>全部状态</option><option>待处理</option><option>处理中</option><option>已回复</option><option>已关闭</option></select>
    <input class="input" style="width:180px" placeholder="订单号">
    <button class="btn" type="submit">查询</button>
  </form>
</div>
<div class="panel">
  <table class="table">
    <thead><tr><th>编号</th><th>类型</th><th>用户</th><th>关联订单</th><th>分类</th><th>描述</th><th>状态</th><th>提交时间</th><th class="right">操作</th></tr></thead>
    <tbody>
      ${FEEDBACKS.map((f) => `<tr>
        <td class="mono">${f.no}</td>
        <td><span class="badge ${f.type === 'COMPLAINT' ? 'badge-danger' : 'badge-paid'}">${f.type === 'COMPLAINT' ? '投诉' : '反馈'}</span></td>
        <td>星野</td>
        <td class="mono small">${f.order}</td>
        <td class="muted">${f.category}</td>
        <td class="muted" style="max-width:260px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${f.content}</td>
        <td>${badgeFb(f.status)}</td>
        <td class="muted">${f.time}</td>
        <td class="right"><a class="btn btn-sm ${f.status === 'PENDING' ? 'btn-primary' : ''}" href="feedback-detail.html">处理</a></td>
      </tr>`).join('')}
      <tr>
        <td class="mono">FB-0998</td><td><span class="badge badge-danger">投诉</span></td><td>orbit_x</td><td class="mono small">20261114090000</td><td class="muted">物流发货</td>
        <td class="muted" style="max-width:260px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">下单三天还没揽收，页面显示已发货但查不到物流</td><td>${badgeFb('PROCESSING')}</td><td class="muted">11-15 09:02</td><td class="right"><a class="btn btn-sm" href="feedback-detail.html">处理</a></td>
      </tr>
    </tbody>
  </table>
</div>`;

const adminFeedbackDetail = () => {
  const f = FEEDBACKS[1];
  return `
${adminHead('处理投诉 / 反馈', `${f.no} · ${f.type === 'COMPLAINT' ? '订单投诉' : '普通反馈'}`, '<a class="btn btn-sm" href="feedback.html">返回列表</a>')}
<div class="row" style="align-items:flex-start;gap:18px">
  <div class="content-main">
    <div class="panel pad">
      <div class="between"><div class="row"><span class="badge ${f.type === 'COMPLAINT' ? 'badge-danger' : 'badge-paid'}">${f.type === 'COMPLAINT' ? '投诉' : '反馈'}</span>${badgeFb(f.status)}</div><span class="small muted">${f.time}</span></div>
      <div class="divider"></div>
      <p>${f.content}</p>
      <div class="small muted mt-16">提交人：星野（ID 1274） · 分类：${f.category} · 联系方式：138****6688</div>
      ${f.type === 'COMPLAINT' ? `<div class="mt-16 small muted">关联订单：<a class="mono" href="order-detail.html">${f.order}</a></div>` : ''}
    </div>

    <div class="panel pad mt-16">
      <b>回复用户</b>
      <form class="mt-16" data-ok="原型演示：回复已发送，状态变为「已回复」">
        <div class="field">
          <textarea class="textarea" placeholder="回复内容 1–500 字，会展示给用户"></textarea>
          <div class="help">建议说明处理结论与时限，避免承诺无法兑现的补偿</div>
        </div>
        <div class="row">
          <button class="btn btn-primary" type="submit">发送回复</button>
          <button class="btn" data-confirm="原型演示：已关闭，状态变为「已关闭」（终态）">直接关闭</button>
        </div>
      </form>
    </div>
  </div>

  <aside style="width:320px;flex:0 0 320px">
    <div class="panel pad">
      <b>状态流转</b>
      <div class="timeline mt-16">
        <div class="item done"><div class="t">用户提交</div><div class="d">${f.time}</div></div>
        <div class="item"><div class="t muted">待受理</div><div class="d">受理后变为「处理中」</div></div>
        <div class="item"><div class="t muted">待回复</div><div class="d">回复后变为「已回复」</div></div>
        <div class="item"><div class="t muted">待关闭</div><div class="d">关闭后进入终态</div></div>
      </div>
      <div class="divider"></div>
      <button class="btn btn-block" data-demo="原型演示：已受理，状态变为「处理中」">受理</button>
      <p class="small muted mt-8">用户仅能在「待处理」状态撤销。</p>
    </div>
  </aside>
</div>`;
};

const adminLog = () => `
${adminHead('操作日志', '后台写操作与登录都会落表，密码等参数已脱敏')}
<div class="panel pad-sm mb-16">
  <form class="row wrap" data-ok="原型演示：已按条件筛选">
    <select class="select" style="width:150px"><option>全部模块</option><option>订单管理</option><option>售后管理</option><option>商品管理</option><option>用户管理</option></select>
    <input class="input" style="width:160px" placeholder="操作人">
    <input class="input" style="width:180px" type="date">
    <button class="btn" type="submit">查询</button>
  </form>
</div>
<div class="panel">
  <table class="table">
    <thead><tr><th>时间</th><th>操作人</th><th>模块</th><th>动作</th><th>目标</th><th>参数</th><th>耗时</th><th>结果</th></tr></thead>
    <tbody>
      <tr><td class="muted">11-18 09:35</td><td>admin</td><td>售后管理</td><td>回复投诉</td><td class="mono">FB-1002</td><td class="small muted">{"reply":"已补发…"}</td><td>42ms</td><td><span class="badge badge-completed">成功</span></td></tr>
      <tr><td class="muted">11-18 09:30</td><td>admin</td><td>售后管理</td><td>受理投诉</td><td class="mono">FB-1002</td><td class="small muted">—</td><td>18ms</td><td><span class="badge badge-completed">成功</span></td></tr>
      <tr><td class="muted">11-18 09:12</td><td>admin</td><td>订单管理</td><td>发货</td><td class="mono">20261118093012</td><td class="small muted">{"courierCompany":"顺丰速运","trackingNo":"SF…"}</td><td>63ms</td><td><span class="badge badge-completed">成功</span></td></tr>
      <tr><td class="muted">11-17 18:40</td><td>admin</td><td>商品管理</td><td>下架</td><td class="mono">SPU-1006</td><td class="small muted">{"status":0}</td><td>27ms</td><td><span class="badge badge-completed">成功</span></td></tr>
      <tr><td class="muted">11-17 18:02</td><td>admin</td><td>用户管理</td><td>重置密码</td><td class="mono">1273</td><td class="small muted">{"password":"***"}</td><td>31ms</td><td><span class="badge badge-completed">成功</span></td></tr>
      <tr><td class="muted">11-17 09:00</td><td>admin</td><td>登录</td><td>后台登录</td><td class="mono">—</td><td class="small muted">{"username":"admin"}</td><td>12ms</td><td><span class="badge badge-danger">失败</span></td></tr>
    </tbody>
  </table>
</div>
<p class="small muted mt-8">失败记录也会保留（如登录失败、状态机拒绝），便于追溯。</p>`;

const adminLogin = () => `<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>后台登录 · 星际和平商城</title>
<link rel="icon" href="../assets/ipc-logo-dark.png">
<link rel="stylesheet" href="../assets/style.css"></head>
<body>
<div class="proto-bar">
  <div><span class="tag">原型</span> 星际和平商城 · 卖家后台 · 数据为虚构演示</div>
  <div><a href="../sitemap.html">页面清单</a> <span style="opacity:.35">|</span> <a href="../index.html">返回前台</a></div>
</div>
<div class="auth-wrap" style="min-height:calc(100vh - 34px);background:radial-gradient(900px 400px at 20% 0%, #1e2a5a, transparent 60%), #0b1020">
  <div class="auth-card">
    <div class="row" style="gap:10px;margin-bottom:18px">${mark('../', 'auth-mark', 'dark')}<div><b>星际和平商城 · 卖家后台</b><div class="small muted" style="letter-spacing:.1em;font-size:10px">INTERASTRAL PEACE CORPORATION · 传统项目部</div></div></div>
    <form data-ok="原型演示：登录成功，会跳转到工作台">
      <div class="field"><label>账号<span class="req">*</span></label><input class="input" value="admin"></div>
      <div class="field"><label>密码<span class="req">*</span></label><input class="input" type="password" value="123456"></div>
      ${captchaField()}
      <button class="btn btn-primary btn-block" style="height:42px" type="submit">登录后台</button>
    </form>
    <p class="small muted center mt-16">演示账号 admin / 123456 · <a href="../index.html" style="color:var(--brand)">返回前台</a></p>
  </div>
</div>
<script src="../assets/app.js"></script>
</body></html>`;

/* ---------------- 页面清单 ---------------- */
const sitemap = () => {
  const front = [
    ['index.html', '首页', '§5.1 分类导航、推荐商品'],
    ['category.html', '分类页', '§5.2 一级/二级分类落地'],
    ['product-list.html', '商品列表', '§5.3 筛选 / 排序 / 搜索 / 分页'],
    ['product-detail.html', '商品详情', '§5.4 SKU 切换、加购、立即购买'],
    ['login.html', '登录', '§5.6 用户名 + 密码 + 验证码'],
    ['register.html', '注册', '§5.5 用户名唯一、密码强度'],
    ['user-center.html', '个人中心', '§5.8 账号信息、订单入口'],
    ['cart.html', '购物车', '§5.9 改数量、勾选、合计'],
    ['checkout.html', '确认订单', '§5.11 地址、明细、运费、提交'],
    ['pay.html', '模拟支付', '§5.12 支付成功 / 取消'],
    ['order-list.html', '订单列表', '§5.13 状态 Tab、操作按钮'],
    ['order-detail.html', '订单详情', '§5.13 时间线、明细、投诉入口'],
    ['address-list.html', '收货地址', '§5.10 默认地址、增删改'],
    ['address-form.html', '地址编辑', '§5.10 字段校验'],
    ['feedback-list.html', '我的投诉与反馈', '§5.15 类型/状态筛选'],
    ['feedback-form.html', '提交投诉', '§5.15 仅限本人已购买订单'],
    ['feedback-detail.html', '反馈详情', '§8.4 状态时间线、卖家回复'],
  ];
  const admin = [
    ['admin/login.html', '后台登录', '§6.1 独立会话'],
    ['admin/index.html', '工作台', '§6.1 概览卡片（P1）'],
    ['admin/category.html', '分类管理', '§6.2 两级分类、删除拦截'],
    ['admin/product.html', '商品管理', '§6.3 SPU 上下架'],
    ['admin/item.html', 'SKU 管理', '§6.3 规格、价格、库存'],
    ['admin/account.html', '用户管理', '§6.4 禁用、重置密码'],
    ['admin/order.html', '订单管理', '§6.5 筛选、发货、删除'],
    ['admin/order-detail.html', '订单详情（后台）', '§6.5 发货、改收货信息'],
    ['admin/feedback.html', '投诉与反馈列表', '§6.6 待处理优先'],
    ['admin/feedback-detail.html', '投诉处理', '§6.6 受理/回复/关闭'],
    ['admin/log.html', '操作日志', '§6.7 审计、参数脱敏'],
  ];
  const table = (rows) => `<div class="panel"><table class="table"><thead><tr><th>页面</th><th>原型文件</th><th>对应 PRD</th></tr></thead><tbody>${rows
    .map(([href, name, note]) => `<tr><td><a href="${href}" style="color:var(--brand);font-weight:600">${name}</a></td><td class="mono small">${href}</td><td class="muted small">${note}</td></tr>`)
    .join('')}</tbody></table></div>`;
  return `
<section class="page"><div class="container">
  <div class="page-title"><div><h1>原型页面清单</h1><p>共 ${front.length + admin.length} 个页面（不含本清单页），对应 PRD §9 的页面与路由；点击任意一行可预览</p></div></div>
  <h2 style="font-size:16px;margin-bottom:12px">用户端（${front.length}）</h2>
  ${table(front)}
  <h2 style="font-size:16px;margin:24px 0 12px">卖家后台（${admin.length}）</h2>
  ${table(admin)}
  <p class="small muted mt-16">原型说明：所有数据为虚构；按钮点击只弹提示，不写库；表单提交被拦截。用于确认信息层级与交互路径，不代替接口联调。</p>
</div></section>`;
};

/* ---------------- 输出 ---------------- */
const pages = [
  ['index.html', layout({ title: '首页', body: pageIndex(), active: 'home' })],
  ['category.html', layout({ title: '分类', body: pageCategory(), active: 'category' })],
  ['product-list.html', layout({ title: '全部商品', body: pageList(), active: 'list' })],
  ['product-detail.html', layout({ title: '商品详情', body: pageDetail(), active: 'list' })],
  ['login.html', layout({ title: '登录', body: pageLogin() })],
  ['register.html', layout({ title: '注册', body: pageRegister() })],
  ['user-center.html', layout({ title: '个人中心', body: pageCenter() })],
  ['cart.html', layout({ title: '购物车', body: pageCart() })],
  ['checkout.html', layout({ title: '确认订单', body: pageCheckout() })],
  ['pay.html', layout({ title: '支付', body: pagePay() })],
  ['order-list.html', layout({ title: '我的订单', body: pageOrderList(), active: 'orders' })],
  ['order-detail.html', layout({ title: '订单详情', body: pageOrderDetail(), active: 'orders' })],
  ['address-list.html', layout({ title: '收货地址', body: pageAddressList() })],
  ['address-form.html', layout({ title: '编辑地址', body: pageAddressForm() })],
  ['feedback-list.html', layout({ title: '我的投诉与反馈', body: pageFeedbackList(), active: 'feedback' })],
  ['feedback-form.html', layout({ title: '提交投诉', body: pageFeedbackForm(), active: 'feedback' })],
  ['feedback-detail.html', layout({ title: '反馈详情', body: pageFeedbackDetail(), active: 'feedback' })],
  ['sitemap.html', layout({ title: '页面清单', body: sitemap() })],
  ['admin/login.html', adminLogin()],
  ['admin/index.html', layout({ title: '工作台', body: adminIndex(), admin: true, active: 'dash', crumb: '工作台' })],
  ['admin/category.html', layout({ title: '分类管理', body: adminCategory(), admin: true, active: 'cat', crumb: '分类管理' })],
  ['admin/product.html', layout({ title: '商品管理', body: adminProduct(), admin: true, active: 'prod', crumb: '商品管理' })],
  ['admin/item.html', layout({ title: 'SKU 管理', body: adminItem(), admin: true, active: 'item', crumb: '商品管理 / SKU' })],
  ['admin/account.html', layout({ title: '用户管理', body: adminAccount(), admin: true, active: 'acc', crumb: '用户管理' })],
  ['admin/order.html', layout({ title: '订单管理', body: adminOrder(), admin: true, active: 'order', crumb: '订单管理' })],
  ['admin/order-detail.html', layout({ title: '订单详情', body: adminOrderDetail(), admin: true, active: 'order', crumb: '订单管理 / 详情' })],
  ['admin/feedback.html', layout({ title: '投诉与反馈', body: adminFeedback(), admin: true, active: 'fb', crumb: '投诉与反馈' })],
  ['admin/feedback-detail.html', layout({ title: '投诉处理', body: adminFeedbackDetail(), admin: true, active: 'fb', crumb: '投诉与反馈 / 处理' })],
  ['admin/log.html', layout({ title: '操作日志', body: adminLog(), admin: true, active: 'log', crumb: '操作日志' })],
];

for (const [file, html] of pages) {
  const target = path.join(OUT, file);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, html, 'utf8');
}
console.log('已生成', pages.length, '个页面 ->', OUT);
