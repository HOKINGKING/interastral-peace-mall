(function () {
  var $ = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
  var timer;

  window.toast = function (msg) {
    var el = $('#toast');
    if (!el) { el = document.createElement('div'); el.id = 'toast'; el.className = 'toast'; document.body.appendChild(el); }
    el.textContent = msg;
    requestAnimationFrame(function () { el.classList.add('show'); });
    clearTimeout(timer);
    timer = setTimeout(function () { el.classList.remove('show'); }, 2400);
  };

  // 所有原型按钮：只提示，不跳转
  document.addEventListener('click', function (e) {
    var demo = e.target.closest('[data-demo]');
    if (demo) {
      e.preventDefault();
      toast(demo.getAttribute('data-demo') || '原型演示：这个操作不会真的发生');
    }
    var cf = e.target.closest('[data-confirm]');
    if (cf) {
      e.preventDefault();
      toast(cf.getAttribute('data-confirm'));
    }
  });

  $$('form').forEach(function (f) {
    f.addEventListener('submit', function (e) {
      e.preventDefault();
      toast(f.getAttribute('data-ok') || '原型演示：表单已拦截，没有提交到服务器');
    });
  });

  // 数量步进
  $$('[data-qty]').forEach(function (box) {
    var input = $('input', box);
    function set(v) {
      var min = Number(box.getAttribute('data-min') || 1);
      var max = Number(box.getAttribute('data-max') || 99);
      input.value = Math.max(min, Math.min(max, v || min));
      box.dispatchEvent(new CustomEvent('qty:change', { bubbles: true }));
    }
    $$('button', box).forEach(function (b) {
      b.addEventListener('click', function () {
        set(Number(input.value) + (b.hasAttribute('data-inc') ? 1 : -1));
      });
    });
  });

  // 规格切换
  var skus = $$('[data-sku]');
  if (skus.length) {
    skus.forEach(function (s) {
      s.addEventListener('click', function () {
        if (s.classList.contains('is-out')) { toast('该规格暂时缺货'); return; }
        skus.forEach(function (x) { x.classList.remove('active'); });
        s.classList.add('active');
        var price = $('#skuPrice'), stock = $('#skuStock'), buy = $('#buyBtn'), add = $('#addBtn');
        var p = s.getAttribute('data-price'), st = Number(s.getAttribute('data-stock'));
        if (price) price.textContent = p;
        if (stock) stock.textContent = st;
        var qty = $('.qty');
        if (qty) {
          qty.setAttribute('data-max', st);
          var i = $('input', qty);
          if (Number(i.value) > st) i.value = st;
        }
        [buy, add].forEach(function (b) {
          if (!b) return;
          b.classList.toggle('is-disabled', st === 0);
        });
      });
    });
  }

  // 购物车合计
  var cart = $('[data-cart]');
  if (cart) {
    function recalc() {
      var total = 0, count = 0;
      $$('.cart-row[data-price]', cart).forEach(function (row) {
        var cb = $('.checkbox', row);
        var qtyInput = $('.qty input', row);
        var qty = qtyInput ? Number(qtyInput.value || 0) : 0;
        var price = Number(row.getAttribute('data-price') || 0);
        var sub = $('[data-subtotal]', row);
        if (sub) sub.textContent = '¥' + (price * qty).toFixed(2);
        if (cb && cb.checked) { total += price * qty; count += qty; }
      });
      var t = $('[data-total]'); if (t) t.textContent = '¥' + total.toFixed(2);
      var c = $('[data-count]'); if (c) c.textContent = count;
    }
    cart.addEventListener('click', function (e) {
      if (e.target.matches('[data-check-all]')) {
        $$('.checkbox', cart).forEach(function (c) { c.checked = e.target.checked; });
      }
      recalc();
    });
    cart.addEventListener('input', recalc);
    recalc();
  }

  // 支付倒计时
  var cd = $('[data-countdown]');
  if (cd) {
    var left = Number(cd.getAttribute('data-countdown') || 1800);
    setInterval(function () {
      if (left <= 0) { cd.textContent = '已超时'; return; }
      left--;
      cd.textContent = String(Math.floor(left / 60)).padStart(2, '0') + ':' + String(left % 60).padStart(2, '0');
    }, 1000);
  }

  // 反馈/投诉字数统计
  var ta = $('[data-textlen]');
  if (ta) {
    var out = $('[data-textlen-out]');
    var min = Number(ta.getAttribute('data-min') || 0);
    var max = Number(ta.getAttribute('data-max') || 500);
    function sync() {
      var n = ta.value.length;
      if (out) out.textContent = n + ' / ' + max + (n && n < min ? '（至少 ' + min + ' 字）' : '');
      if (out) out.style.color = n > max || (n && n < min) ? '#dc2626' : '';
    }
    ta.addEventListener('input', sync);
    sync();
  }
})();
