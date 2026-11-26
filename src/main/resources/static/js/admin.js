/*
 * 后台交互：轻提示、fetch 提交、图片上传、动态行、二次确认。
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
  function request(method, url, params, jsonBody) {
    var options = {
      method: method,
      headers: Object.assign({}, JSON_HEADERS),
      credentials: 'same-origin',
      cache: 'no-store'
    };
    if (jsonBody) {
      options.headers['Content-Type'] = 'application/json;charset=UTF-8';
      options.body = JSON.stringify(jsonBody);
    } else if (params) {
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

  // ------------------------------------------------------------------ 通用二次确认

  document.querySelectorAll('[data-confirm]').forEach(function (element) {
    element.addEventListener('click', function (event) {
      var message = element.getAttribute('data-confirm');
      if (message && !window.confirm(message)) {
        event.preventDefault();
      }
    });
  });

  // ------------------------------------------------------------------ 普通表单（form-encoded）

  // 普通表单（form-encoded）：页面上可能有多个（如订单详情的发货 + 改收货信息）
  document.querySelectorAll('[data-admin-form]').forEach(function (simpleForm) {
    simpleForm.addEventListener('submit', function (event) {
      event.preventDefault();
      var submit = simpleForm.querySelector('button[type="submit"]');
      if (submit && submit.disabled) {
        return;
      }
      var payload = {};
      new FormData(simpleForm).forEach(function (value, key) {
        payload[key] = value;
      });
      if (payload.visible === undefined) {
        payload.visible = '0';
      }
      if (submit) {
        submit.disabled = true;
      }
      request('POST', simpleForm.getAttribute('action'), payload).then(function (result) {
        if (result && result.code === 200) {
          toast(result.message || '保存成功');
          setTimeout(function () {
            window.location.href = simpleForm.getAttribute('data-redirect') || '/admin';
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
  });

  // ------------------------------------------------------------------ 图片上传

  function readJson(response) {
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
  }

  function uploadImage(file, target, button) {
    var body = new FormData();
    body.append('file', file);
    if (button) {
      button.disabled = true;
    }
    return fetch('/admin/upload/image', {
      method: 'POST',
      body: body,
      headers: {'X-Requested-With': 'XMLHttpRequest'},
      credentials: 'same-origin'
    }).then(readJson).then(function (result) {
      if (result && result.code === 200) {
        toast('图片已上传');
        applyUploadedUrl(result.data, target);
        return result.data;
      }
      toast((result && result.message) || '上传失败');
      return null;
    }).catch(function () {
      toast('网络异常，请稍后再试');
      return null;
    }).then(function (url) {
      if (button) {
        button.disabled = false;
      }
      return url;
    });
  }

  function applyUploadedUrl(url, target) {
    if (!url) {
      return;
    }
    if (target === 'mainImage') {
      var input = document.querySelector('[data-product-field="mainImage"]');
      if (input) {
        input.value = url;
      }
      var preview = document.querySelector('[data-main-preview]');
      if (preview) {
        preview.innerHTML = '';
        var img = document.createElement('img');
        img.src = url;
        img.alt = '主图';
        img.style.cssText = 'width:100%;height:100%;object-fit:cover;border-radius:8px';
        preview.appendChild(img);
      }
      return;
    }
    if (target === 'gallery') {
      var gallery = document.querySelector('[data-gallery]');
      if (!gallery) {
        return;
      }
      if (gallery.querySelectorAll('[data-url]').length >= 5) {
        toast('相册最多 5 张');
        return;
      }
      var chip = document.createElement('span');
      chip.className = 'chip';
      chip.setAttribute('data-url', url);
      var thumb = document.createElement('img');
      thumb.src = url;
      thumb.alt = '相册';
      thumb.style.cssText = 'height:22px;vertical-align:middle;margin-right:4px;border-radius:4px';
      var remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'chip-remove';
      remove.setAttribute('data-gallery-remove', '');
      remove.textContent = '×';
      chip.appendChild(thumb);
      chip.appendChild(remove);
      gallery.appendChild(chip);
      return;
    }
    var field = document.querySelector(target);
    if (field) {
      field.value = url;
    }
  }

  document.querySelectorAll('[data-upload-target]').forEach(function (button) {
    button.addEventListener('click', function () {
      var chooser = document.createElement('input');
      chooser.type = 'file';
      chooser.accept = 'image/jpeg,image/png,image/webp';
      chooser.addEventListener('change', function () {
        var file = chooser.files && chooser.files[0];
        if (file) {
          uploadImage(file, button.getAttribute('data-upload-target'), button);
        }
      });
      chooser.click();
    });
  });

  // ------------------------------------------------------------------ 商品表单（JSON）

  function textValue(form, name) {
    var el = form.querySelector('[data-product-field="' + name + '"]');
    if (!el) {
      return null;
    }
    if (el.type === 'checkbox') {
      return el.checked ? 1 : 0;
    }
    return el.value;
  }

  function collectProductPayload(form) {
    var images = Array.prototype.slice.call(form.querySelectorAll('[data-gallery] [data-url]'))
      .map(function (chip) {
        return chip.getAttribute('data-url');
      })
      .join(',');

    var specs = [];
    form.querySelectorAll('[data-spec-row]').forEach(function (row) {
      var key = row.querySelector('[data-spec-key]').value.trim();
      var value = row.querySelector('[data-spec-value]').value.trim();
      if (key) {
        specs.push({key: key, value: value});
      }
    });

    var skus = [];
    form.querySelectorAll('[data-sku-row]').forEach(function (row) {
      var id = row.querySelector('[data-sku-id]').value;
      var spec = row.querySelector('[data-sku-spec]').value.trim();
      var price = row.querySelector('[data-sku-price]').value.trim();
      var stock = row.querySelector('[data-sku-stock]').value.trim();
      var image = row.querySelector('[data-sku-image]').value.trim();
      var version = row.querySelector('[data-sku-version]').value;
      if (!spec && !price && !stock) {
        return;
      }
      skus.push({
        id: id ? Number(id) : null,
        spec: spec,
        price: price === '' ? null : Number(price),
        stock: stock === '' ? null : Number(stock),
        image: image,
        version: version === '' ? null : Number(version)
      });
    });

    var categoryId = textValue(form, 'categoryId');
    var productId = form.getAttribute('data-product-id');
    return {
      id: productId ? Number(productId) : null,
      categoryId: categoryId ? Number(categoryId) : null,
      name: textValue(form, 'name'),
      brand: textValue(form, 'brand'),
      descn: textValue(form, 'descn'),
      detail: textValue(form, 'detail'),
      mainImage: textValue(form, 'mainImage'),
      images: images,
      status: textValue(form, 'status'),
      specs: specs,
      skus: skus
    };
  }

  var productForm = document.querySelector('[data-product-form]');
  if (productForm) {
    productForm.addEventListener('submit', function (event) {
      event.preventDefault();
      var submit = productForm.querySelector('button[type="submit"]');
      if (submit && submit.disabled) {
        return;
      }
      var payload = collectProductPayload(productForm);
      if (submit) {
        submit.disabled = true;
      }
      request('POST', '/admin/product/save', null, payload).then(function (result) {
        if (result && result.code === 200) {
          toast(result.message || '保存成功');
          setTimeout(function () {
            window.location.href = productForm.getAttribute('data-redirect') || '/admin/product';
          }, 600);
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

  // ------------------------------------------------------------------ 动态行（参数 / SKU / 相册删除）

  document.addEventListener('click', function (event) {
    var specRemove = event.target.closest('[data-spec-remove]');
    if (specRemove) {
      specRemove.closest('[data-spec-row]').remove();
      return;
    }
    var skuRemove = event.target.closest('[data-sku-remove]');
    if (skuRemove) {
      if (document.querySelectorAll('[data-sku-row]').length <= 1) {
        toast('每个商品至少保留 1 个 SKU');
        return;
      }
      skuRemove.closest('[data-sku-row]').remove();
      return;
    }
    var galleryRemove = event.target.closest('[data-gallery-remove]');
    if (galleryRemove) {
      galleryRemove.closest('[data-url]').remove();
    }
  });

  document.querySelectorAll('[data-spec-add]').forEach(function (button) {
    button.addEventListener('click', function () {
      var tbody = document.querySelector('[data-specs]');
      var template = document.getElementById('specRowTemplate');
      if (!tbody || !template) {
        return;
      }
      if (tbody.querySelectorAll('[data-spec-row]').length >= 8) {
        toast('商品参数最多 8 组');
        return;
      }
      tbody.appendChild(template.content.cloneNode(true));
    });
  });

  document.querySelectorAll('[data-sku-add]').forEach(function (button) {
    button.addEventListener('click', function () {
      var tbody = document.querySelector('[data-skus]');
      var template = document.getElementById('skuRowTemplate');
      if (!tbody || !template) {
        return;
      }
      tbody.appendChild(template.content.cloneNode(true));
    });
  });

  // ------------------------------------------------------------------ 商品列表：上下架 / 删除

  document.querySelectorAll('[data-product-status]').forEach(function (input) {
    input.addEventListener('change', function () {
      var id = input.getAttribute('data-product-status');
      var status = input.checked ? 1 : 0;
      request('POST', '/admin/product/' + id + '/status', {status: status}).then(function (result) {
        if (result && result.code === 200) {
          toast(result.message || '已更新');
          return;
        }
        input.checked = !input.checked;
        toast((result && result.message) || '操作失败');
      }).catch(function () {
        input.checked = !input.checked;
        toast('网络异常，请稍后再试');
      });
    });
  });

  document.querySelectorAll('[data-delete-product]').forEach(function (button) {
    button.addEventListener('click', function () {
      var id = button.getAttribute('data-delete-product');
      var name = button.getAttribute('data-product-name') || '该商品';
      if (!window.confirm('确定删除「' + name + '」吗？存在未完结订单时会被拒绝。')) {
        return;
      }
      request('DELETE', '/admin/product/' + id).then(function (result) {
        if (result && result.code === 200) {
          toast(result.message || '商品已删除');
          setTimeout(function () {
            window.location.href = '/admin/product';
          }, 500);
          return;
        }
        toast((result && result.message) || '删除失败');
      }).catch(function () {
        toast('网络异常，请稍后再试');
      });
    });
  });

  // ------------------------------------------------------------------ 订单详情：删除订单

  document.querySelectorAll('[data-delete-order]').forEach(function (button) {
    button.addEventListener('click', function () {
      var id = button.getAttribute('data-delete-order');
      if (!window.confirm('删除后订单不再显示（逻辑删除，不回补库存），确定删除吗？')) {
        return;
      }
      request('DELETE', '/admin/order/' + id).then(function (result) {
        if (result && result.code === 200) {
          toast(result.message || '订单已删除');
          setTimeout(function () {
            window.location.href = '/admin/order';
          }, 500);
          return;
        }
        toast((result && result.message) || '删除失败');
      }).catch(function () {
        toast('网络异常，请稍后再试');
      });
    });
  });

  // ------------------------------------------------------------------ SKU 列表：删除

  document.querySelectorAll('[data-delete-sku]').forEach(function (button) {
    button.addEventListener('click', function () {
      var id = button.getAttribute('data-delete-sku');
      if (!window.confirm('确定删除这条 SKU 吗？每个商品至少要保留一条。')) {
        return;
      }
      request('DELETE', '/admin/item/' + id).then(function (result) {
        if (result && result.code === 200) {
          toast(result.message || 'SKU 已删除');
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
