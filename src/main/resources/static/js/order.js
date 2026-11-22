/*
 * 交易页面交互（PRD §5.11–§5.13）：
 * 确认页提交防连点；支付页倒计时；订单列表/详情的二次确认与 fetch 删除。
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

  function request(method, url) {
    return fetch(url, {method: method, headers: JSON_HEADERS, credentials: 'same-origin', cache: 'no-store'})
      .then(function (response) {
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

  // ------------------------------------------------------------------ 确认页

  var submitForm = document.querySelector('form[action$="/order/submit"]');
  if (submitForm) {
    var submitButton = submitForm.querySelector('button[type="submit"]');
    submitForm.addEventListener('submit', function (event) {
      var addressSelected = submitForm.querySelector('input[name="addressId"]:checked');
      if (!addressSelected) {
        event.preventDefault();
        toast('请选择收货地址');
        return;
      }
      // 等浏览器完成原生校验后再禁用，避免校验失败按钮一直灰着
      setTimeout(function () {
        if (submitButton) {
          submitButton.disabled = true;
          submitButton.textContent = '提交中…';
        }
      }, 0);
    });
    window.addEventListener('pageshow', function () {
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = '提交订单';
      }
    });
  }

  // ------------------------------------------------------------------ 支付页倒计时

  var countdown = document.querySelector('[data-countdown]');
  if (countdown) {
    var remaining = Number(countdown.getAttribute('data-countdown')) || 0;
    var tick = function () {
      if (remaining <= 0) {
        countdown.textContent = '00:00';
        return;
      }
      var minutes = Math.floor(remaining / 60);
      var seconds = remaining % 60;
      countdown.textContent = (minutes < 10 ? '0' : '') + minutes + ':' + (seconds < 10 ? '0' : '') + seconds;
      remaining -= 1;
      setTimeout(tick, 1000);
    };
    tick();
  }

  // ------------------------------------------------------------------ 二次确认（取消/收货/删除）

  document.querySelectorAll('[data-confirm]').forEach(function (element) {
    element.addEventListener('click', function (event) {
      var message = element.getAttribute('data-confirm');
      if (message && !window.confirm(message)) {
        event.preventDefault();
      }
    });
  });

  // ------------------------------------------------------------------ 删除订单（F37，fetch）

  document.querySelectorAll('[data-delete-order]').forEach(function (button) {
    button.addEventListener('click', function () {
      var orderId = button.getAttribute('data-delete-order');
      if (!window.confirm('删除后订单不再显示，且不可恢复，确定删除吗？')) {
        return;
      }
      request('DELETE', '/order/' + orderId).then(function (result) {
        if (result && result.code === 200) {
          toast(result.message || '订单已删除');
          var card = button.closest('[data-order-id]');
          if (card) {
            card.parentNode.removeChild(card);
          }
          setTimeout(function () {
            window.location.reload();
          }, 500);
          return;
        }
        toast((result && result.message) || '删除失败');
      }).catch(function () {
        toast('网络异常，请稍后再试');
      });
    });
  });
})();
