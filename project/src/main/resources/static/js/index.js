/* ============================================================
 * index.js
 * 学生端: 图鉴列表 + 详情 + 时间轴 + 发布打卡
 * 依赖: api.js, auth.js, util.js
 * ============================================================ */

(function () {
  // ---- 守卫 ----
  requireRole('STUDENT');
  renderNavbar('同学');

  // ---- 状态 ----
  const state = {
    page: 1,
    size: 10,
    total: 0,
    name: '',
    type: '',
    selectedId: null,
  };

  // ---- DOM ----
  const elList = $('animal-list');
  const elPagination = $('pagination');
  const elPageInfo = $('page-info');
  const elBtnPrev = $('btn-prev');
  const elBtnNext = $('btn-next');
  const elFilterName = $('filter-name');
  const elFilterType = $('filter-type');
  const elBtnSearch = $('btn-search');
  const elBtnReset = $('btn-reset');

  const elDetailCard = $('detail-card');
  const elCheckinCard = $('checkin-card');
  const elTimelineCard = $('timeline-card');
  const elTimeline = $('timeline');
  const elTimelineCount = $('timeline-count');

  const elCheckinForm = $('checkin-form');
  const elCheckinContent = $('checkin-content');
  const elCharCounter = $('char-counter');
  const elCheckinErr = $('checkin-err');
  const elBtnPublish = $('btn-publish');

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
      const active = a.id === state.selectedId ? ' active' : '';
      const cover = a.coverImage
        ? 'style="background-image:url(' + escapeHtml(imageUrl(a.coverImage)) + ')"'
        : '';
      const innerIcon = a.coverImage ? '' : typeIcon(a.type);
      return (
        '<li class="animal-item' + active + '" data-id="' + a.id + '">' +
        '<div class="animal-thumb" ' + cover + '>' + innerIcon + '</div>' +
        '<div class="animal-info">' +
        '<div class="animal-name">' + escapeHtml(a.name) + '</div>' +
        '<div class="animal-meta">' +
        '<span class="' + typeBadgeClass(a.type) + '">' + typeLabel(a.type) + '</span>' +
        '<span>📍 ' + escapeHtml(a.area || '') + '</span>' +
        '</div>' +
        '</div>' +
        '</li>'
      );
    }).join('');
    elList.innerHTML = html;

    // 绑定点击
    elList.querySelectorAll('.animal-item').forEach(function (li) {
      li.addEventListener('click', function () {
        const id = Number(li.dataset.id);
        selectAnimal(id);
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

  // ---- 详情 + 时间轴 ----
  async function selectAnimal(id) {
    state.selectedId = id;
    // 重新渲染列表高亮
    elList.querySelectorAll('.animal-item').forEach(function (li) {
      if (Number(li.dataset.id) === id) li.classList.add('active');
      else li.classList.remove('active');
    });

    elDetailCard.innerHTML = '<div class="loading">加载详情...</div>';
    elCheckinCard.style.display = 'none';
    elTimelineCard.style.display = 'none';

    try {
      const animal = await request('GET', '/api/animals/' + id);
      renderDetail(animal);
      elCheckinCard.style.display = 'block';
      elTimelineCard.style.display = 'block';
      // 重置打卡表单
      elCheckinContent.value = '';
      updateCharCount();
      clearError(elCheckinErr);
      await loadTimeline(id);
    } catch (e) {
      elDetailCard.innerHTML = '<div class="alert alert-error">加载详情失败: ' + escapeHtml(e.message) + '</div>';
    }
  }

  function renderDetail(a) {
    const cover = a.coverImage
      ? 'style="background-image:url(' + escapeHtml(imageUrl(a.coverImage)) + ')"'
      : '';
    const inner = a.coverImage ? '' : typeIcon(a.type);
    elDetailCard.innerHTML =
      '<div class="detail-header">' +
      '<div class="detail-cover" ' + cover + '>' + inner + '</div>' +
      '<div class="detail-fields">' +
      '<div class="detail-name">' + escapeHtml(a.name) + '</div>' +
      '<div class="detail-row"><span class="label">类型</span>' +
      '<span class="' + typeBadgeClass(a.type) + '">' + typeLabel(a.type) + '</span></div>' +
      '<div class="detail-row"><span class="label">常驻区域</span><span>' + escapeHtml(a.area || '') + '</span></div>' +
      '<div class="detail-row"><span class="label">入档时间</span><span>' + escapeHtml(a.createdAt || '') + '</span></div>' +
      '</div>' +
      '</div>';
  }

  async function loadTimeline(animalId) {
    elTimeline.innerHTML = '<li class="loading">加载中...</li>';
    elTimelineCount.textContent = '';
    try {
      const list = await request('GET', '/api/animals/' + animalId + '/check-ins');
      renderTimeline(list || []);
    } catch (e) {
      elTimeline.innerHTML = '<li class="card-empty">加载失败: ' + escapeHtml(e.message) + '</li>';
    }
  }

  function renderTimeline(list) {
    if (!list.length) {
      elTimeline.innerHTML = '<li class="card-empty">还没有打卡,来抢沙发吧 🐾</li>';
      elTimelineCount.textContent = '';
      return;
    }
    elTimelineCount.textContent = '共 ' + list.length + ' 条';
    const html = list.map(function (c) {
      return (
        '<li class="timeline-item">' +
        '<div class="timeline-header">' +
        '<span class="timeline-user">@' + escapeHtml(c.userNickname || ('用户#' + c.userId)) + '</span>' +
        '<span class="timeline-time">' + escapeHtml(c.createdAt || '') + '</span>' +
        '</div>' +
        '<div class="timeline-content">' + escapeHtml(c.content || '') + '</div>' +
        '</li>'
      );
    }).join('');
    elTimeline.innerHTML = html;
  }

  // ---- 发布打卡 ----
  function updateCharCount() {
    const len = elCheckinContent.value.length;
    elCharCounter.textContent = len + ' / 500';
    elCharCounter.classList.toggle('over', len > 500);
  }

  elCheckinContent.addEventListener('input', updateCharCount);

  elCheckinForm.addEventListener('submit', async function (e) {
    e.preventDefault();
    clearError(elCheckinErr);
    if (!state.selectedId) {
      showError(elCheckinErr, '请先从左侧选择一只动物');
      return;
    }
    const content = elCheckinContent.value.trim();
    if (!content) {
      showError(elCheckinErr, '打卡内容不能为空');
      return;
    }
    if (content.length > 500) {
      showError(elCheckinErr, '打卡内容超过 500 字');
      return;
    }
    elBtnPublish.disabled = true;
    elBtnPublish.textContent = '发布中...';
    try {
      await request('POST', '/api/check-ins', {
        animalId: state.selectedId,
        content: content,
      });
      elCheckinContent.value = '';
      updateCharCount();
      await loadTimeline(state.selectedId);
    } catch (e2) {
      showError(elCheckinErr, e2.message || '发布失败');
    } finally {
      elBtnPublish.disabled = false;
      elBtnPublish.textContent = '发布打卡';
    }
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

  // Enter 触发搜索
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
  loadAnimals();
})();
