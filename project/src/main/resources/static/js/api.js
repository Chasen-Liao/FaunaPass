/* ============================================================
 * api.js
 * fetch 封装 + token 注入 + 统一错误处理
 * ============================================================ */

const API_BASE = ''; // 同源,后端服务静态文件
const TOKEN_KEY = 'animal_token';
const USER_KEY = 'animal_user';

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setToken(t) {
  localStorage.setItem(TOKEN_KEY, t);
}

function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

function getCurrentUser() {
  const s = localStorage.getItem(USER_KEY);
  try {
    return s ? JSON.parse(s) : null;
  } catch (e) {
    return null;
  }
}

function setCurrentUser(u) {
  localStorage.setItem(USER_KEY, JSON.stringify(u));
}

/**
 * 统一请求函数
 * @param {string} method - HTTP 方法 GET/POST/PUT/DELETE
 * @param {string} url    - 接口路径,以 / 开头
 * @param {*}      body   - 请求体: 对象(json) 或 FormData
 * @param {boolean} isFormData - body 是否为 FormData
 * @returns 返回 response.data,失败时 throw Error
 */
async function request(method, url, body, isFormData) {
  const headers = {};
  const t = getToken();
  if (t) headers['Authorization'] = 'Bearer ' + t;

  let payload;
  if (isFormData) {
    payload = body; // FormData 时浏览器自动加 Content-Type + boundary
  } else if (body !== undefined && body !== null) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  let res;
  try {
    res = await fetch(API_BASE + url, { method, headers, body: payload });
  } catch (e) {
    alert('网络异常,请检查后端服务是否启动');
    throw e;
  }

  // 401: 未登录 / token 失效
  if (res.status === 401) {
    clearAuth();
    // 已经在登录/注册页就不弹了
    const p = location.pathname;
    if (!p.endsWith('/login.html') && !p.endsWith('/register.html')) {
      alert('登录已失效,请重新登录');
      location.href = '/login.html';
    }
    throw new Error('UNAUTHORIZED');
  }

  // 403: 无权限
  if (res.status === 403) {
    alert('无权限访问');
    throw new Error('FORBIDDEN');
  }

  let data;
  try {
    data = await res.json();
  } catch (e) {
    throw new Error('响应解析失败 (HTTP ' + res.status + ')');
  }

  if (data.code !== 0) {
    throw new Error(data.msg || ('error ' + data.code));
  }

  return data.data;
}
