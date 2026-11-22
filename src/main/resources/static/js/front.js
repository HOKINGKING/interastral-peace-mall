/* 前台交互：轻提示、待接入模块、数量步进、SKU 切换、相册切图 */
(function () {
  'use strict';

  var toastTimer;

  function toast(message) {
    var el = document.getElementById('toast');
    if (!el) {
      el = document.createElement('div');
      el.id = 'toast';
      el.className = 'toast';
      document.body.appendChild(el);
    }
    el.textContent = message;
    requestAnimationFrame(function () {
      el.classList.add('show');
    });
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      el.classList.remove('show');
    }, 2400);
  }

  window.showToast = toast;

  /**
   * 待接入模块（购物车 / 订单 / 售后）：
   * 目标接口要到 M5/M7 才落地，这里统一只弹提示，避免点进 404。
   * 详情页游客的「加入购物车」是显式登录链接（AC-04），不经过本拦截。
   */
  document.addEventListener('click', function (event) {
    var target = event.target.closest('[data-pending]');
    if (!target) {
      return;
    }
    event.preventDefault();
    toast(target.getAttribute('data-pending') || '该模块尚未接入');
  });

  document.addEventListener('submit', function (event) {
    var form = event.target.closest('[data-pending]');
    if (!form) {
      return;
    }
    event.preventDefault();
    toast(form.getAttribute('data-pending') || '该模块尚未接入');
  });

  // 筛选条件变化后自动提交（仅看有货、排序下拉）
  document.querySelectorAll('[data-auto-submit]').forEach(function (el) {
    el.addEventListener('change', function () {
      if (el.form) {
        el.form.submit();
      }
    });
  });

  // 数量步进：值与隐藏字段 js-qty-target 同步（加购 / 立即购买各有一个）
  document.querySelectorAll('[data-qty]').forEach(function (box) {
    var input = box.querySelector('input');
    if (!input) {
      return;
    }

    function bounds() {
      var min = Number(box.dataset.min || 1);
      var max = Number(box.dataset.max || 99);
      if (max < min) {
        max = min;
      }
      return {min: min, max: max};
    }

    function sync(value) {
      var range = bounds();
      var next = Math.max(range.min, Math.min(range.max, Number(value) || range.min));
      input.value = next;
      document.querySelectorAll('.js-qty-target').forEach(function (target) {
        target.value = next;
      });
    }

    box.querySelectorAll('[data-inc]').forEach(function (button) {
      button.addEventListener('click', function () {
        sync(Number(input.value) + 1);
      });
    });
    box.querySelectorAll('[data-dec]').forEach(function (button) {
      button.addEventListener('click', function () {
        sync(Number(input.value) - 1);
      });
    });
    sync(input.value);
  });

  // SKU 切换：价格 / 库存 / 数量上限 / 加购按钮联动，不整页刷新（PRD §5.4）
  var skuButtons = document.querySelectorAll('[data-sku]');
  if (skuButtons.length > 0) {
    var priceEl = document.getElementById('skuPrice');
    var stockEl = document.getElementById('skuStock');
    var cartItemEl = document.getElementById('cartItemId');
    var qtyBox = document.getElementById('qtyBox');
    var submitButtons = document.querySelectorAll('form[action$="/cart/add"] button[type="submit"]');

    function selectSku(button) {
      var stock = Number(button.dataset.stock || 0);
      var price = button.dataset.price;
      skuButtons.forEach(function (item) {
        item.classList.remove('active');
      });
      button.classList.add('active');

      if (priceEl && price) {
        priceEl.textContent = price;
      }
      if (stockEl) {
        stockEl.textContent = stock;
      }
      if (cartItemEl) {
        cartItemEl.value = button.dataset.id;
      }
      if (qtyBox) {
        var max = Math.min(stock, 99);
        qtyBox.dataset.max = max;
        var input = qtyBox.querySelector('input');
        if (input && Number(input.value) > max) {
          input.value = Math.max(1, max);
          document.querySelectorAll('.js-qty-target').forEach(function (target) {
            target.value = input.value;
          });
        }
      }
      submitButtons.forEach(function (submit) {
        submit.disabled = stock <= 0;
      });
    }

    skuButtons.forEach(function (button) {
      button.addEventListener('click', function () {
        if (button.classList.contains('is-out')) {
          toast('该规格暂时缺货');
          return;
        }
        selectSku(button);
      });
    });
  }

  // 相册：点击缩略图切换主图（URL 不变）
  var gallery = document.querySelector('[data-gallery]');
  var mainImage = document.getElementById('mainImage');
  if (gallery && mainImage) {
    gallery.querySelectorAll('[data-thumb]').forEach(function (thumb) {
      thumb.addEventListener('click', function () {
        if (!thumb.dataset.full) {
          return;
        }
        mainImage.src = thumb.dataset.full;
        gallery.querySelectorAll('[data-thumb]').forEach(function (item) {
          item.classList.remove('active');
        });
        thumb.classList.add('active');
      });
    });
  }
})();
