/*
 * 收货地址交互（PRD §5.10）：
 * 1) 表单保存 → fetch POST /address/save，成功后回列表页；
 * 2) 列表页：设为默认、删除（二次确认），均只发 JSON 请求，不整页刷新。
 * 依赖 front.js 提供的 window.showToast。
 */
(function () {
  'use strict';

  var JSON_HEADERS = {'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json'};

  function toast(message) {
    if (typeof window.showToast === 'function') {
      window.showToast(message);
    }
  }

  /** 业务失败也是 HTTP 200 + code != 200，这里只看 Result.code */
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

  // ------------------------------------------------------------------ 表单保存

  var form = document.querySelector('[data-address-form]');
  if (form) {
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      var submit = form.querySelector('button[type="submit"]');
      if (submit && submit.disabled) {
        return;
      }
      var payload = {};
      new FormData(form).forEach(function (value, key) {
        payload[key] = value;
      });
      // 未勾选默认时 FormData 里不会有 isDefault，补一个空串让后端归一化成 0
      if (payload.isDefault === undefined) {
        payload.isDefault = '0';
      }
      if (submit) {
        submit.disabled = true;
      }
      request('POST', '/address/save', payload).then(function (result) {
        if (result && result.code === 200) {
          toast(result.message || '收货地址已保存');
          // 从确认订单页进来时带 returnTo，保存后回到原结算页；否则回地址列表
          var returnTo = form.querySelector('input[name="returnTo"]');
          var target = returnTo && returnTo.value ? returnTo.value : '/address/list';
          setTimeout(function () {
            window.location.href = target;
          }, 500);
          return;
        }
        if (submit) {
          submit.disabled = false;
        }
        toast((result && result.message) || '保存失败，请检查填写内容');
      }).catch(function () {
        if (submit) {
          submit.disabled = false;
        }
        toast('网络异常，请稍后再试');
      });
    });
  }

  // ------------------------------------------------------------------ 列表操作

  function markDefaultCard(card) {
    document.querySelectorAll('[data-address-id]').forEach(function (other) {
      other.classList.remove('active');
      var badge = other.querySelector('.between .badge');
      if (badge) {
        badge.parentNode.removeChild(badge);
      }
      var button = other.querySelector('[data-set-default]');
      if (button) {
        button.disabled = false;
      }
    });
    card.classList.add('active');
    var button = card.querySelector('[data-set-default]');
    if (button) {
      button.disabled = true;
    }
    var head = card.querySelector('.between');
    if (head) {
      var badge = document.createElement('span');
      badge.className = 'badge badge-paid';
      badge.textContent = '默认';
      head.appendChild(badge);
    }
  }

  document.querySelectorAll('[data-set-default]').forEach(function (button) {
    button.addEventListener('click', function () {
      var card = button.closest('[data-address-id]');
      if (!card) {
        return;
      }
      button.disabled = true;
      request('POST', '/address/' + card.getAttribute('data-address-id') + '/default').then(function (result) {
        if (result && result.code === 200) {
          markDefaultCard(card);
          toast(result.message || '已设为默认地址');
          return;
        }
        button.disabled = false;
        toast((result && result.message) || '设置失败');
      }).catch(function () {
        button.disabled = false;
        toast('网络异常，请稍后再试');
      });
    });
  });

  document.querySelectorAll('[data-remove]').forEach(function (button) {
    button.addEventListener('click', function () {
      var card = button.closest('[data-address-id]');
      if (!card || !window.confirm('确定要删除这条收货地址吗？')) {
        return;
      }
      request('DELETE', '/address/' + card.getAttribute('data-address-id')).then(function (result) {
        if (result && result.code === 200) {
          if (card.parentNode) {
            card.parentNode.removeChild(card);
          }
          toast(result.message || '收货地址已删除');
          if (!document.querySelector('[data-address-id]')) {
            window.location.reload();
          }
          return;
        }
        toast((result && result.message) || '删除失败');
      }).catch(function () {
        toast('网络异常，请稍后再试');
      });
    });
  });
})();
