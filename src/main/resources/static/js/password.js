/*
 * 修改密码异步交互（PRD §5.8）：
 * - 提交走 fetch，失败只刷新提示文字，不整页刷新；
 * - 新密码 / 确认新密码出错时，只清空这两栏，原密码保留；
 * - 输入过程中实时校验格式与两次输入是否一致。
 */
(function () {
  'use strict';

  var form = document.querySelector('[data-password-form]');
  if (!form) {
    return;
  }

  var oldInput = document.getElementById('oldPassword');
  var newInput = document.getElementById('newPassword');
  var confirmInput = document.getElementById('confirmPassword');
  var submitBtn = form.querySelector('[data-password-submit]');
  var PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{6,20}$/;

  function container(field) {
    return document.querySelector('[data-error-for="' + field + '"]');
  }

  function setError(field, message) {
    var box = container(field);
    if (box) {
      box.textContent = message || '';
      box.classList.toggle('hide', !message);
    }
    var input = document.getElementById(field);
    if (input) {
      input.classList.toggle('is-invalid', !!message);
    }
  }

  function clearError(field) {
    setError(field, '');
  }

  function clearAll() {
    ['oldPassword', 'newPassword', 'confirmPassword', 'global'].forEach(clearError);
  }

  function validateOld() {
    if (!oldInput.value) {
      setError('oldPassword', '请输入原密码');
      return false;
    }
    clearError('oldPassword');
    return true;
  }

  function validateNew() {
    var value = newInput.value;
    if (!value) {
      setError('newPassword', '请输入新密码');
      return false;
    }
    if (!PATTERN.test(value)) {
      setError('newPassword', '新密码需为 6–20 位且同时包含字母和数字');
      return false;
    }
    if (oldInput.value && value === oldInput.value) {
      setError('newPassword', '新密码不能与原密码相同');
      return false;
    }
    clearError('newPassword');
    return true;
  }

  function validateConfirm() {
    var value = confirmInput.value;
    if (!value) {
      setError('confirmPassword', '请再次输入新密码');
      return false;
    }
    if (value !== newInput.value) {
      setError('confirmPassword', '两次输入的新密码不一致');
      return false;
    }
    clearError('confirmPassword');
    return true;
  }

  /** 新密码相关错误：清空「新密码 / 确认新密码」，原密码保留 */
  function clearNewFields() {
    newInput.value = '';
    confirmInput.value = '';
    newInput.focus();
  }

  function focusOld() {
    oldInput.focus();
    oldInput.select();
  }

  function applyError(field, message) {
    setError(field || 'newPassword', message || '修改失败，请稍后再试');
    if (field === 'oldPassword') {
      focusOld();
    } else {
      clearNewFields();
    }
  }

  function setLoading(loading) {
    if (!submitBtn) {
      return;
    }
    submitBtn.disabled = loading;
    submitBtn.classList.toggle('is-disabled', loading);
    submitBtn.textContent = loading ? '保存中…' : '保存并重新登录';
  }

  // 输入时实时校验：格式、与原密码相同、两次输入不一致都能立刻看到提示
  oldInput.addEventListener('input', function () {
    clearError('oldPassword');
    if (newInput.value) {
      validateNew();
    }
  });
  newInput.addEventListener('input', function () {
    validateNew();
    if (confirmInput.value) {
      validateConfirm();
    }
  });
  confirmInput.addEventListener('input', validateConfirm);

  form.addEventListener('submit', function (event) {
    event.preventDefault();
    clearError('global');

    var oldOk = validateOld();
    var newOk = validateNew();
    var confirmOk = validateConfirm();
    if (!oldOk || !newOk || !confirmOk) {
      if (!oldOk) {
        focusOld();
      } else {
        clearNewFields();
      }
      return;
    }

    setLoading(true);
    fetch('/user/password/async', {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
        'Accept': 'application/json'
      },
      body: new FormData(form),
      cache: 'no-store'
    }).then(function (response) {
      if (response.status === 401) {
        window.location.href = '/login?redirect=/user/password';
        return null;
      }
      return response.text().then(function (text) {
        if (!text) {
          return {code: -1, message: '服务返回异常，请稍后再试'};
        }
        try {
          return JSON.parse(text);
        } catch (e) {
          return {code: -1, message: '服务返回异常，请稍后再试'};
        }
      });
    }).then(function (result) {
      if (!result) {
        return;
      }
      if (result.code === 200) {
        var redirect = result.data && result.data.redirect ? result.data.redirect : '/login?changed=1';
        window.location.href = redirect;
        return;
      }
      applyError(result.data && result.data.field, result.message);
      setLoading(false);
    }).catch(function () {
      setError('global', '网络异常，请稍后再试');
      setLoading(false);
    });
  });
})();
