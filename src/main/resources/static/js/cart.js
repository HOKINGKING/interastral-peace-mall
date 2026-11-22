/*
 * 购物车交互（PRD §5.9）：
 * 1) 详情页「加入购物车」→ fetch POST /cart/add，toast 提示并刷新顶部角标；
 * 2) 购物车页：勾选 / 全选实时重算合计、改数量（PUT）、删除（DELETE）；
 * 3) 角标数字通过 GET /cart/count 回填，首屏由服务端渲染。
 * 依赖 front.js 提供的 window.showToast 与 [data-qty] 步进器。
 */
(function () {
  'use strict';

  var JSON_HEADERS = {'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json'};

  function toast(message) {
    if (typeof window.showToast === 'function') {
      window.showToast(message);
    }
  }

  function money(value) {
    return '¥' + (Number(value) || 0).toFixed(2);
  }

  /**
   * 统一发请求。业务失败也是 HTTP 200 + code != 200（GlobalExceptionHandler 的约定），
   * 所以这里只看 Result.code，不回落到 HTTP 状态码上判断。
   */
  function request(method, url, params) {
    var options = {method: method, headers: JSON_HEADERS, credentials: 'same-origin', cache: 'no-store'};
    if (params) {
      var body = new URLSearchParams();
      Object.keys(params).forEach(function (key) {
        body.append(key, params[key]);
      });
      options.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
      options.body = body.toString();
    }
    return fetch(url, options).then(function (response) {
      if (response.status === 401) {
        window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
        return null;
      }
      return response.text().then(function (text) {
        if (!text) {
          return {code: response.status, message: '请求失败，请稍后再试'};
        }
        try {
          return JSON.parse(text);
        } catch (e) {
          return {code: response.status, message: '服务返回异常，请稍后再试'};
        }
      });
    });
  }

  // ------------------------------------------------------------------ 顶部角标

  function applyBadge(count) {
    var link = document.querySelector('.cart-link');
    if (!link) {
      return;
    }
    var badge = link.querySelector('.count');
    if (count <= 0) {
      if (badge) {
        badge.parentNode.removeChild(badge);
      }
      return;
    }
    if (!badge) {
      badge = document.createElement('span');
      badge.className = 'count';
      link.appendChild(badge);
    }
    badge.textContent = count > 99 ? '99+' : String(count);
  }

  function refreshBadge() {
    return request('GET', '/cart/count').then(function (result) {
      if (result && result.code === 200) {
        applyBadge(Number(result.data) || 0);
      }
    }).catch(function () {
      // 角标刷新失败不影响主流程
    });
  }

  // ------------------------------------------------------------------ 详情页加购

  var addForm = document.querySelector('form[action$="/cart/add"]');
  if (addForm) {
    addForm.addEventListener('submit', function (event) {
      // 同一表单里的「立即购买」按钮带 formaction="/order/checkout"，交回浏览器直接 GET 到结算页
      if (event.submitter && event.submitter.getAttribute('formaction')) {
        return;
      }
      event.preventDefault();
      var submit = addForm.querySelector('button[type="submit"]');
      if (!submit || submit.disabled) {
        return;
      }
      var payload = {};
      new FormData(addForm).forEach(function (value, key) {
        payload[key] = value;
      });
      submit.disabled = true;
      request('POST', '/cart/add', payload).then(function (result) {
        if (result && result.code === 200) {
          toast(result.message || '已加入购物车');
          refreshBadge();
        } else {
          toast((result && result.message) || '加入购物车失败');
        }
      }).catch(function () {
        toast('网络异常，请稍后再试');
      }).then(function () {
        submit.disabled = false;
      });
    });
  }

  // ------------------------------------------------------------------ 购物车页

  var cartRoot = document.querySelector('[data-cart]');
  if (!cartRoot) {
    return;
  }

  var items = Array.prototype.slice.call(cartRoot.querySelectorAll('[data-cart-id]')).map(function (row) {
    var input = row.querySelector('.qty input');
    return {
      row: row,
      id: Number(row.getAttribute('data-cart-id')),
      price: Number(row.getAttribute('data-price') || 0),
      available: row.getAttribute('data-available') !== 'false',
      checkbox: row.querySelector('[data-check]'),
      input: input,
      subtotalEl: row.querySelector('[data-subtotal]'),
      lastQty: Number(input && input.value) || 1,
      timer: null
    };
  });
  var checkAlls = Array.prototype.slice.call(document.querySelectorAll('[data-check-all]'));
  var countEl = document.querySelector('[data-count]');
  var totalEl = document.querySelector('[data-total]');

  /** 合计只统计勾选项（PRD §5.9），勾选是纯前端状态，不落库 */
  function recompute() {
    var selected = 0;
    var total = 0;
    items.forEach(function (item) {
      var qty = Number(item.input.value) || 0;
      var subtotal = item.price * qty;
      if (item.subtotalEl) {
        item.subtotalEl.textContent = money(subtotal);
      }
      if (item.available && item.checkbox && item.checkbox.checked) {
        selected += qty;
        total += subtotal;
      }
    });
    if (countEl) {
      countEl.textContent = String(selected);
    }
    if (totalEl) {
      totalEl.textContent = money(total);
    }
    var selectable = items.filter(function (item) {
      return item.available && item.checkbox;
    });
    var checked = selectable.filter(function (item) {
      return item.checkbox.checked;
    });
    checkAlls.forEach(function (box) {
      box.checked = selectable.length > 0 && checked.length === selectable.length;
      box.indeterminate = checked.length > 0 && checked.length < selectable.length;
    });
  }

  function saveQuantity(item) {
    var qty = Number(item.input.value) || 0;
    if (qty < 1 || qty === item.lastQty) {
      return;
    }
    request('PUT', '/cart/' + item.id, {quantity: qty}).then(function (result) {
      if (result && result.code === 200) {
        item.lastQty = qty;
        recompute();
        refreshBadge();
        return;
      }
      item.input.value = item.lastQty;
      toast((result && result.message) || '修改数量失败');
      recompute();
    }).catch(function () {
      item.input.value = item.lastQty;
      toast('网络异常，请稍后再试');
      recompute();
    });
  }

  /** 连点时只发最后一次，避免 1→2→3 的乱序覆盖 */
  function scheduleSave(item) {
    clearTimeout(item.timer);
    item.timer = setTimeout(function () {
      saveQuantity(item);
    }, 250);
  }

  function removeRow(item) {
    if (!window.confirm('确定要从购物车移除该商品吗？')) {
      return;
    }
    request('DELETE', '/cart/' + item.id).then(function (result) {
      if (result && result.code === 200) {
        if (item.row.parentNode) {
          item.row.parentNode.removeChild(item.row);
        }
        items = items.filter(function (other) {
          return other !== item;
        });
        if (items.length === 0) {
          window.location.reload();
          return;
        }
        toast(result.message || '已从购物车移除');
        recompute();
        refreshBadge();
        return;
      }
      toast((result && result.message) || '删除失败');
    }).catch(function () {
      toast('网络异常，请稍后再试');
    });
  }

  items.forEach(function (item) {
    if (item.checkbox) {
      item.checkbox.addEventListener('change', recompute);
    }
    var inc = item.row.querySelector('[data-inc]');
    var dec = item.row.querySelector('[data-dec]');
    [inc, dec].forEach(function (button) {
      if (!button) {
        return;
      }
      button.addEventListener('click', function () {
        // front.js 的 [data-qty] 步进器先改 input.value，这里等它跑完再读最终值
        setTimeout(function () {
          recompute();
          scheduleSave(item);
        }, 0);
      });
    });
    var remove = item.row.querySelector('[data-remove]');
    if (remove) {
      remove.addEventListener('click', function () {
        removeRow(item);
      });
    }
  });

  checkAlls.forEach(function (box) {
    box.addEventListener('change', function () {
      items.forEach(function (item) {
        if (item.available && item.checkbox) {
          item.checkbox.checked = box.checked;
        }
      });
      recompute();
    });
  });

  recompute();

  // 去结算：勾选状态只在前端，这里把勾选行拼成 cartIds 再跳确认页（F24）
  var checkoutLink = document.querySelector('[data-checkout]');
  if (checkoutLink) {
    checkoutLink.addEventListener('click', function (event) {
      event.preventDefault();
      var ids = items.filter(function (item) {
        return item.available && item.checkbox && item.checkbox.checked;
      }).map(function (item) {
        return item.id;
      });
      if (ids.length === 0) {
        toast('请先选择要结算的商品');
        return;
      }
      window.location.href = '/order/checkout?cartIds=' + ids.join(',');
    });
  }
})();
