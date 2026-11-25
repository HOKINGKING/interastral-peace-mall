/*
 * 后台交互：轻提示、fetch 表单提交、删除二次确认。
 * 后台页面不加载 front.js，所以这里自带一份 toast。
 */
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

  var JSON_HEADERS = {'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json'};

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
        window.location.href = '/admin/login';
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

  // 通用二次确认
  document.querySelectorAll('[data-confirm]').forEach(function (element) {
    element.addEventListener('click', function (event) {
      var message = element.getAttribute('data-confirm');
      if (message && !window.confirm(message)) {
        event.preventDefault();
      }
    });
  });

  // 后台表单：fetch 提交，成功后按 data-redirect 回列表
  var form = document.querySelector('[data-admin-form]');
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
      // checkbox 未勾选时 FormData 里没有该字段，补 0 让后端归一化成隐藏
      if (payload.visible === undefined) {
        payload.visible = '0';
      }
      if (submit) {
        submit.disabled = true;
      }
      request('POST', form.getAttribute('action'), payload).then(function (result) {
        if (result && result.code === 200) {
          toast(result.message || '保存成功');
          setTimeout(function () {
            window.location.href = form.getAttribute('data-redirect') || '/admin';
          }, 500);
          return;
        }
        if (submit) {
          submit.disabled = false;
        }
        toast((result && result.message) || '保存失败');
      }).catch(function () {
        if (submit) {
          submit.disabled = false;
        }
        toast('网络异常，请稍后再试');
      });
    });
  }

  // 删除分类（存在子分类或商品时后端返回 2004）
  document.querySelectorAll('[data-delete-category]').forEach(function (button) {
    button.addEventListener('click', function () {
      var id = button.getAttribute('data-delete-category');
      var name = button.getAttribute('data-category-name') || '该分类';
      if (!window.confirm('确定删除「' + name + '」吗？存在子分类或商品时会被拒绝。')) {
        return;
      }
      request('DELETE', '/admin/category/' + id).then(function (result) {
        if (result && result.code === 200) {
          toast(result.message || '分类已删除');
          var row = button.closest('[data-category-id]');
          if (row) {
            row.parentNode.removeChild(row);
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
