/* ============================================================
 * admin.js
 * 管理员端: 图鉴 CRUD + 文件上传
 * 依赖: api.js, auth.js, util.js
 * ============================================================ */

(function () {
  // ---- 守卫 ----
  requireRole('ADMIN');
  renderNavbar('管理员');

  // ---- 状态 ----
  const state = {
    page: 1,
    size: 10,
    total: 0,
    name: '',
    type: '',
    editingId: null,      // null = 新增模式, 数字 = 编辑模式
    originalCover: '',    // 编辑时原图,用于"不选则保留"
  };

  // ---- DOM: 左侧 ----
  const elList = $('animal-list');
  const elPagination = $('pagination');
  const elPageInfo = $('page-info');
  const elBtnPrev = $('btn-prev');
  const elBtnNext = $('btn-next');
  const elFilterName = $('filter-name');
  const elFilterType = $('filter-type');
  const elBtnSearch = $('btn-search');
  const elBtnReset = $('btn-reset');
  const elBtnNew = $('btn-new');

  // ---- DOM: 右侧表单 ----
  const elFormTitle = $('form-title');
  const elFormModeTip = $('form-mode-tip');
  const elAnimalId = $('animal-id');
  const elFName = $('f-name');
  const elFType = $('f-type');
  const elFArea = $('f-area');
  const elFCover = $('f-cover');
  const elCoverPreview = $('cover-preview');
  const elFormErr = $('form-err');
  const elForm = $('animal-form');
  const elBtnSubmit = $('btn-submit');
  const elBtnCancel = $('btn-cancel');

  // ---- 列表 ----
  async function loadAnimals() {
    elList.innerHTML = '<li class="loading">加载中...</li>';
    elPagination.style.display = 'none';
    const params = new URLSearchParams();
    if (state.name) params.append('name', state.name);
    if (state.type) params.append('type', state.type);
    params.append('page', state.page);
    params.append('size', state.size);
    try {
      const data = await request('GET', '/api/animals?' + params.toString());
      state.total = data.total || 0;
      renderList(data.records || []);
      renderPagination();
    } catch (e) {
      elList.innerHTML = '<li class="card-empty">加载失败: ' + escapeHtml(e.message) + '</li>';
    }
  }

  function renderList(records) {
    if (!records.length) {
      elList.innerHTML = '<li class="card-empty">暂无动物档案</li>';
      return;
    }
    const html = records.map(function (a) {
      const active = a.id === state.editingId ? ' active' : '';
      const cover = a.coverImage
        ? 'style="background-image:url(' + escapeHtml(imageUrl(a.coverImage)) + ')"'
        : '';
      const inner = a.coverImage ? '' : typeIcon(a.type);
      return (
        '<li class="animal-item' + active + '" data-id="' + a.id + '">' +
        '<div class="animal-thumb" ' + cover + '>' + inner + '</div>' +
        '<div class="animal-info">' +
        '<div class="animal-name">' + escapeHtml(a.name) + '</div>' +
        '<div class="animal-meta">' +
        '<span class="' + typeBadgeClass(a.type) + '">' + typeLabel(a.type) + '</span>' +
        '<span>📍 ' + escapeHtml(a.area || '') + '</span>' +
        '</div>' +
        '</div>' +
        '<div class="animal-actions">' +
        '<button type="button" class="btn btn-sm btn-edit" data-id="' + a.id + '">编辑</button>' +
        '<button type="button" class="btn btn-sm btn-danger btn-del" data-id="' + a.id + '">删除</button>' +
        '</div>' +
        '</li>'
      );
    }).join('');
    elList.innerHTML = html;

    elList.querySelectorAll('.btn-edit').forEach(function (b) {
      b.addEventListener('click', function (e) {
        e.stopPropagation();
        beginEdit(Number(b.dataset.id));
      });
    });

    elList.querySelectorAll('.btn-del').forEach(function (b) {
      b.addEventListener('click', function (e) {
        e.stopPropagation();
        confirmDelete(Number(b.dataset.id));
      });
    });
  }

  function renderPagination() {
    const totalPages = Math.max(1, Math.ceil(state.total / state.size));
    elPagination.style.display = state.total > 0 ? 'flex' : 'none';
    elPageInfo.textContent = '第 ' + state.page + ' / ' + totalPages + ' 页 (共 ' + state.total + ' 条)';
    elBtnPrev.disabled = state.page <= 1;
    elBtnNext.disabled = state.page >= totalPages;
  }

  // ---- 表单 ----
  function resetForm() {
    state.editingId = null;
    state.originalCover = '';
    elAnimalId.value = '';
    elFName.value = '';
    elFType.value = '';
    elFArea.value = '';
    elFCover.value = '';
    elCoverPreview.style.backgroundImage = '';
    elCoverPreview.textContent = '未选择图片';
    elFormTitle.textContent = '新增动物档案';
    elFormModeTip.textContent = '';
    clearError(elFormErr);
    // 重新渲染列表去掉高亮
    elList.querySelectorAll('.animal-item').forEach(function (li) {
      li.classList.remove('active');
    });
  }

  async function beginEdit(id) {
    clearError(elFormErr);
    try {
      const a = await request('GET', '/api/animals/' + id);
      state.editingId = a.id;
      state.originalCover = a.coverImage || '';
      elAnimalId.value = a.id;
      elFName.value = a.name || '';
      elFType.value = a.type || '';
      elFArea.value = a.area || '';
      elFCover.value = '';
      if (a.coverImage) {
        elCoverPreview.style.backgroundImage = 'url(' + imageUrl(a.coverImage) + ')';
        elCoverPreview.textContent = '';
      } else {
        elCoverPreview.style.backgroundImage = '';
        elCoverPreview.textContent = '未选择图片';
      }
      elFormTitle.textContent = '编辑动物档案 #' + a.id;
      elFormModeTip.textContent = '不选新图则保留原图';
      // 列表高亮
      elList.querySelectorAll('.animal-item').forEach(function (li) {
        if (Number(li.dataset.id) === id) li.classList.add('active');
        else li.classList.remove('active');
      });
      // 滚动到表单(移动端有用)
      const formCard = $('form-card');
      if (formCard) formCard.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (e) {
      showError(elFormErr, '加载详情失败: ' + e.message);
    }
  }

  async function confirmDelete(id) {
    if (!confirm('确定要删除该动物档案吗？(打卡记录会保留)')) return;
    try {
      await request('DELETE', '/api/animals/' + id);
      // 如果删的是当前编辑的,重置表单
      if (state.editingId === id) resetForm();
      await loadAnimals();
    } catch (e) {
      alert('删除失败: ' + e.message);
    }
  }

  // 文件选择 → 本地预览
  elFCover.addEventListener('change', function () {
    const f = elFCover.files && elFCover.files[0];
    if (!f) {
      // 恢复原图(编辑模式)或清空(新增模式)
      if (state.editingId && state.originalCover) {
        elCoverPreview.style.backgroundImage = 'url(' + imageUrl(state.originalCover) + ')';
        elCoverPreview.textContent = '';
      } else {
        elCoverPreview.style.backgroundImage = '';
        elCoverPreview.textContent = '未选择图片';
      }
      return;
    }
    if (f.size > 5 * 1024 * 1024) {
      showError(elFormErr, '图片大小超过 5MB');
      elFCover.value = '';
      return;
    }
    clearError(elFormErr);
    const reader = new FileReader();
    reader.onload = function (e) {
      elCoverPreview.style.backgroundImage = 'url(' + e.target.result + ')';
      elCoverPreview.textContent = '';
    };
    reader.readAsDataURL(f);
  });

  elForm.addEventListener('submit', async function (e) {
    e.preventDefault();
    clearError(elFormErr);
    const name = elFName.value.trim();
    const type = elFType.value;
    const area = elFArea.value.trim();
    if (!name || !type || !area) {
      showError(elFormErr, '名字、类型、常驻区域为必填');
      return;
    }
    const file = elFCover.files && elFCover.files[0];

    // 新增时图片必填(后端也会校验,这里提前提示)
    if (!state.editingId && !file) {
      showError(elFormErr, '请上传封面图');
      return;
    }

    const fd = new FormData();
    fd.append('name', name);
    fd.append('type', type);
    fd.append('area', area);
    if (file) fd.append('cover', file);

    elBtnSubmit.disabled = true;
    const oldText = elBtnSubmit.textContent;
    elBtnSubmit.textContent = '提交中...';

    try {
      if (state.editingId) {
        await request('PUT', '/api/animals/' + state.editingId, fd, true);
      } else {
        await request('POST', '/api/animals', fd, true);
      }
      resetForm();
      await loadAnimals();
    } catch (e2) {
      showError(elFormErr, e2.message || '提交失败');
    } finally {
      elBtnSubmit.disabled = false;
      elBtnSubmit.textContent = oldText;
    }
  });

  elBtnCancel.addEventListener('click', function () {
    resetForm();
  });

  elBtnNew.addEventListener('click', function () {
    resetForm();
    elFName.focus();
  });

  // ---- 工具栏 ----
  elBtnSearch.addEventListener('click', function () {
    state.name = elFilterName.value.trim();
    state.type = elFilterType.value;
    state.page = 1;
    loadAnimals();
  });

  elBtnReset.addEventListener('click', function () {
    elFilterName.value = '';
    elFilterType.value = '';
    state.name = '';
    state.type = '';
    state.page = 1;
    loadAnimals();
  });

  elFilterName.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') {
      e.preventDefault();
      elBtnSearch.click();
    }
  });

  elBtnPrev.addEventListener('click', function () {
    if (state.page > 1) {
      state.page--;
      loadAnimals();
    }
  });

  elBtnNext.addEventListener('click', function () {
    const totalPages = Math.max(1, Math.ceil(state.total / state.size));
    if (state.page < totalPages) {
      state.page++;
      loadAnimals();
    }
  });

  // ---- 启动 ----
  resetForm();
  loadAnimals();
})();
