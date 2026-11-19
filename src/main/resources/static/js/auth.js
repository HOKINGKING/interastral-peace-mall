/* 认证页公共脚本：验证码刷新 + 防重复提交 */
(function () {
  'use strict';

  function refreshCaptcha(img) {
    if (!img) {
      return;
    }
    var src = img.getAttribute('src') || '';
    var base = src.split('?')[0];
    img.src = base + '?t=' + Date.now();
  }

  document.addEventListener('click', function (event) {
    var img = event.target.closest('img.captcha-img');
    if (!img) {
      return;
    }
    event.preventDefault();
    refreshCaptcha(img);
  });

  var forms = document.querySelectorAll('form[data-auth-form]');
  forms.forEach(function (form) {
    form.addEventListener('submit', function (event) {
      if (form.dataset.submitting === '1') {
        event.preventDefault();
        return;
      }
      form.dataset.submitting = '1';
      var button = form.querySelector('button[type="submit"]');
      if (button) {
        // 让 submit 先完成，再禁用按钮，避免阻止表单提交
        window.setTimeout(function () {
          button.disabled = true;
          button.classList.add('is-disabled');
        }, 0);
      }
    });
  });
})();
