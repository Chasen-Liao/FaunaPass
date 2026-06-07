/* ============================================================
 * auth.js
 * 登录/注册/登出/角色守卫
 * 依赖: api.js
 * ============================================================ */

async function login(username, password) {
  const data = await request('POST', '/api/auth/login', { username, password });
  setToken(data.token);
  setCurrentUser({
    id: data.userId,
    username: data.username,
    nickname: data.nickname,
    role: data.role,
  });
  return data;
}

async function register(username, password, nickname) {
  const data = await request('POST', '/api/auth/register', {
    username,
    password,
    nickname,
  });
  setToken(data.token);
  setCurrentUser({
    id: data.userId,
    username: data.username,
    nickname: data.nickname,
    role: data.role,
  });
  return data;
}

function logout() {
  clearAuth();
  location.href = '/login.html';
}

/** 已登录守卫:未登录直接跳登录页 */
function requireAuth() {
  if (!getToken()) {
    location.href = '/login.html';
  }
}

/** 角色守卫:角色不符跳登录页 */
function requireRole(role) {
  const u = getCurrentUser();
  if (!getToken() || !u) {
    location.href = '/login.html';
    return;
  }
  if (u.role !== role) {
    alert('请用 ' + role + ' 账号登录');
    location.href = '/login.html';
  }
}

/** 按当前用户角色跳转到主页 */
function redirectByRole(user) {
  if (!user) {
    location.href = '/login.html';
    return;
  }
  if (user.role === 'ADMIN') {
    location.href = '/admin.html';
  } else {
    location.href = '/index.html';
  }
}
