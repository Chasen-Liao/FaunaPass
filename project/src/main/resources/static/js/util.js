/* ============================================================
 * util.js
 * 公共工具:DOM 渲染、转义、Navbar 渲染
 * 依赖: api.js, auth.js
 * ============================================================ */

/** HTML 转义,防 XSS */
function escapeHtml(s) {
  if (s === null || s === undefined) return '';
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** 短 id 选择器 */
function $(id) {
  return document.getElementById(id);
}

/** type → 中文 */
function typeLabel(type) {
  if (type === 'CAT') return '猫';
  if (type === 'DOG') return '狗';
  return type || '';
}

/** type → 表情 icon */
function typeIcon(type) {
  if (type === 'CAT') return '🐱';
  if (type === 'DOG') return '🐶';
  return '🐾';
}

/** type → badge CSS class */
function typeBadgeClass(type) {
  if (type === 'CAT') return 'badge badge-cat';
  if (type === 'DOG') return 'badge badge-dog';
  return 'badge';
}

/** 渲染顶部 Navbar (传入角色显示后缀:学生/管理员) */
function renderNavbar(roleSuffix) {
  const u = getCurrentUser();
  const nav = document.querySelector('.navbar');
  if (!nav) return;
  const nick = u ? escapeHtml(u.nickname) : '游客';
  nav.innerHTML =
    '<div class="navbar-brand">校园流浪动物图鉴</div>' +
    '<div class="navbar-user">' +
    '<span>欢迎, <span class="username">' + nick + '</span> ' + escapeHtml(roleSuffix) + '</span>' +
    '<button type="button" class="btn btn-sm" id="btn-logout">登出</button>' +
    '</div>';
  const btn = $('btn-logout');
  if (btn) btn.addEventListener('click', logout);
}

/** 简单的按钮防抖:执行期间禁用按钮 */
async function withDisabled(btn, fn) {
  if (!btn) return await fn();
  btn.disabled = true;
  const oldText = btn.textContent;
  try {
    return await fn();
  } finally {
    btn.disabled = false;
    btn.textContent = oldText;
  }
}

/** 拼接上传图片的可访问 URL (后端 cover_image 已是相对路径) */
function imageUrl(path) {
  if (!path) return '';
  if (path.startsWith('http')) return path;
  if (path.startsWith('/')) return path;
  return '/' + path;
}

/** 显示错误文本到指定 dom */
function showError(el, msg) {
  if (!el) return;
  el.textContent = msg || '';
}

/** 清空表单错误 */
function clearError(el) {
  if (el) el.textContent = '';
}
