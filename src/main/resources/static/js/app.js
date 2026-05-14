// ── 狀態 ──
let currentUser = null;
let currentBookId = null;
let currentPage = 1;
let totalPages = 1;
const pageHistory = [];
let searchTimer = null;
let activeCategory = '';
let bookPage = 0;

// ── Token 工具 ──
function getToken()        { return localStorage.getItem('token'); }
function setToken(t)       { localStorage.setItem('token', t); }
function clearToken()      { localStorage.removeItem('token'); }
function getRefreshToken() { return localStorage.getItem('refreshToken'); }          
function setRefreshToken(t){ localStorage.setItem('refreshToken', t); }             
function clearRefreshToken(){ localStorage.removeItem('refreshToken'); } 

async function authFetch(url, options = {}) {
  const token = getToken();

  const isFormData = options.body instanceof FormData;
  const headers = {
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const res = await fetch(url, { ...options, headers });

  if (res.status === 401) {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      try {
        const refreshRes = await fetch('/api/auth/refresh', {
          method: 'POST',
          headers: { 'X-Refresh-Token': refreshToken }
        });
        const refreshData = await refreshRes.json();
        if (refreshData.success) {
          setToken(refreshData.token);
          const retryHeaders = {
            ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
            'Authorization': `Bearer ${refreshData.token}`,
            ...options.headers,
          };
          return await fetch(url, { ...options, headers: retryHeaders });
        }
      } catch(e) { console.error(e); }
    }
    clearToken();
    clearRefreshToken();
    currentUser = null;
    updateNav();
    openModal('login');
    throw new Error('未登入');
  }

  return res;
}

// ── 工具 ──
function toast(msg, dur = 2800) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), dur);
}

// ── 頁面導航 ──
function showPage(name, addToHistory = true) {
  const currentPageId = document.querySelector('.page.active')?.id?.replace('page-', '');

  if (addToHistory && currentPageId && currentPageId !== name) {
    pageHistory.push(currentPageId);
  }

  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById('page-' + name).classList.add('active');
  window.scrollTo(0, 0);

  if (name === 'home')   loadPopular();
  if (name === 'books')  loadBooks();
  if (name === 'shelf')  loadShelf();
  if (name === 'orders') loadOrders();
  if (name === 'admin')  loadAdminBooks();
}

function goBack() {
  if (pageHistory.length > 0) {
    const prev = pageHistory.pop();
    showPage(prev, false);
  } else {
    showPage('home', false);
  }
}

function requireLogin(fn) {
  if (!currentUser) { openModal('login'); return; }
  fn();
}

// ── 導覽列 ──
function updateNav() {
  const loggedIn = !!currentUser;
  document.getElementById('loginNavBtn').style.display  = loggedIn ? 'none' : '';
  document.getElementById('logoutNavBtn').style.display = loggedIn ? '' : 'none';
  document.getElementById('navUser').style.display      = loggedIn ? 'flex' : 'none';
  const adminBtn = document.getElementById('adminNavBtn');
  if (adminBtn) adminBtn.style.display = (loggedIn && currentUser.role === 'ADMIN') ? '' : 'none';
  if (currentUser) document.getElementById('navUsername').textContent = currentUser.username;
}

// ── Modal ──
function openModal(type) {
  document.getElementById(type + 'Modal').classList.add('open');
  clearMsg(type);
}
function closeModal(type) {
  document.getElementById(type + 'Modal').classList.remove('open');
}
function switchModal(from, to) {
  closeModal(from); openModal(to);
}
function clearMsg(type) {
  const m = document.getElementById(type + 'Msg');
  if (m) { m.className = 'form-msg'; m.textContent = ''; }
}
function showMsg(type, msg, isError = true) {
  const m = document.getElementById(type + 'Msg');
  m.textContent = msg;
  m.className = 'form-msg ' + (isError ? 'error' : 'success');
}

// ── 登入 ──
async function doLogin() {
  const email  = document.getElementById('loginEmail').value.trim();
  const pw     = document.getElementById('loginPassword').value;
  const msgEl  = document.getElementById('loginMsg');
  const btn    = document.querySelector('#loginModal .btn-gold');

  if (!email || !pw) {
    msgEl.textContent = '請填寫所有欄位';
    msgEl.style.color = 'red';
    msgEl.style.display = 'block';
    return;
  }

  btn.disabled = true;
  btn.textContent = '登入中⋯';

  try {
    const res  = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password: pw, deviceType: 'WEB' })
    });
    const data = await res.json();
    if (data.success) {
      setToken(data.token);
	  setRefreshToken(data.refreshToken);
      currentUser = data.user;
      msgEl.textContent = '';
      msgEl.style.display = 'none';
      updateNav();
      closeModal('login');
      toast('歡迎回來，' + currentUser.username + '！');
    } else {
      msgEl.textContent = data.message;
      msgEl.style.color = 'red';
      msgEl.style.display = 'block';
    }
  } catch(e) {
    console.error(e);
    msgEl.textContent = '連線失敗，請確認伺服器是否啟動';
    msgEl.style.color = 'red';
    msgEl.style.display = 'block';
  } finally {
    btn.disabled = false;
    btn.textContent = '登入';
  }
}

// ── 註冊 ──
async function doRegister() {
  const username = document.getElementById('regUsername').value.trim();
  const email    = document.getElementById('regEmail').value.trim();
  const pw       = document.getElementById('regPassword').value;
  if (!username || !email || !pw) { showMsg('register', '請填寫所有欄位'); return; }
  if (pw.length < 6) { showMsg('register', '密碼至少需要 6 個字元'); return; }
  try {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email, password: pw })
    });
    const data = await res.json();
    if (data.success) {
      showMsg('register', '註冊成功！請登入', false);
      setTimeout(() => switchModal('register', 'login'), 1200);
    } else {
      showMsg('register', data.message);
    }
  } catch(e) {
    console.error(e);
    showMsg('register', '連線失敗，請確認伺服器是否啟動');
  }
}

// ── 登出 ──
async function doLogout() {
  try {
    await authFetch('/api/auth/logout', { method: 'POST' });
  } catch(e) { console.error(e); }
  clearToken();
  clearRefreshToken();
  currentUser = null;
  updateNav();
  showPage('home', false);
  toast('已成功登出');
}

// ── 忘記密碼 ──
async function doForgotPassword() {
  const email = document.getElementById('forgotEmail').value.trim();
  const msgEl = document.getElementById('forgotMsg');
  if (!email) {
    msgEl.textContent = '請輸入 Email';
    msgEl.style.color = 'red';
    msgEl.style.display = 'block';
    return;
  }

  const btn = document.querySelector('#forgotModal .btn-gold');
  btn.textContent = '寄送中⋯';
  btn.disabled = true;
  msgEl.textContent = '';

  try {
    const res  = await fetch('/api/auth/forgot-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });
    const data = await res.json();
    if (data.success) {
      msgEl.textContent = '✓ 若此 Email 已註冊，請查收信箱';
      msgEl.style.color = 'green';
      msgEl.style.display = 'block';
      toast('重設密碼信已寄出！');
      document.getElementById('forgotEmail').value = '';
      setTimeout(() => { closeModal('forgot'); msgEl.textContent = ''; }, 3000);
    } else {
      msgEl.textContent = data.message;
      msgEl.style.color = 'red';
      msgEl.style.display = 'block';
    }
  } catch(e) {
    console.error(e);
    msgEl.textContent = '發生錯誤，請稍後再試';
    msgEl.style.color = 'red';
    msgEl.style.display = 'block';
  } finally {
    btn.textContent = '寄送重設連結';
    btn.disabled = false;
  }
}

// ── 重設密碼 ──
async function doResetPassword() {
  const pwd     = document.getElementById('resetPassword').value;
  const confirm = document.getElementById('resetPasswordConfirm').value;
  const msg     = document.getElementById('resetMsg');
  const token   = new URLSearchParams(location.search).get('token');

  msg.style.display = 'block';
  if (pwd.length < 6)  { msg.style.color = 'red'; msg.textContent = '密碼至少 6 字元'; return; }
  if (pwd !== confirm) { msg.style.color = 'red'; msg.textContent = '兩次密碼不一致'; return; }
  if (!token)          { msg.style.color = 'red'; msg.textContent = '無效的重設連結，請重新申請'; return; }

  const btn = document.querySelector('#page-reset .btn-gold');
  btn.textContent = '重設中⋯';
  btn.disabled = true;

  try {
    const res  = await fetch('/api/auth/reset-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, newPassword: pwd })
    });
    const data = await res.json();
    if (data.success) {
      msg.style.color = 'green';
      msg.textContent = '✓ 密碼重設成功！即將跳轉至登入頁⋯';
      toast('密碼已重設，請重新登入');
      document.getElementById('resetPassword').value = '';
      document.getElementById('resetPasswordConfirm').value = '';
      setTimeout(() => {
        history.replaceState({}, '', location.pathname);
        msg.textContent = '';
        showPage('home', false);
        openModal('login');
      }, 2000);
    } else {
      msg.style.color = 'red';
      msg.textContent = data.message;
      btn.textContent = '確認重設';
      btn.disabled = false;
    }
  } catch(e) {
    console.error(e);
    msg.style.color = 'red';
    msg.textContent = '發生錯誤，請稍後再試';
    btn.textContent = '確認重設';
    btn.disabled = false;
  }
}


let editingBookId = null;

async function loadAdminBooks() {
  const el = document.getElementById('adminBookList');
  if (!el) return;
  el.innerHTML = '<div class="loading">載入中⋯</div>';
  try {
    const res = await authFetch('/api/admin/books');
    if (res.status === 403) {
      el.innerHTML = '<p style="color:red">權限不足，請以管理員帳號登入</p>';
      return;
    }
    const list = await res.json();
    el.innerHTML = `
      <div style="margin-bottom:1rem">
        <button class="btn btn-gold" onclick="openBookModal()">＋ 新增書籍</button>
      </div>
      <table style="width:100%;border-collapse:collapse;font-size:.9rem">
        <thead>
          <tr style="border-bottom:1px solid #ccc;text-align:left">
            <th style="padding:.6rem">封面</th>
            <th style="padding:.6rem">書名</th>
            <th style="padding:.6rem">作者</th>
            <th style="padding:.6rem">售價</th>
            <th style="padding:.6rem">狀態</th>
            <th style="padding:.6rem">操作</th>
          </tr>
        </thead>
        <tbody>
          ${list.length ? list.map(b => `
            <tr style="border-bottom:1px solid #eee" id="adminRow-${b.id}">
              <td style="padding:.6rem">
                <img src="${b.coverUrl || 'https://picsum.photos/seed/' + b.id + '/300/400'}"
                  style="width:40px;height:55px;object-fit:cover;border-radius:4px">
              </td>
              <td style="padding:.6rem">${b.title}</td>
              <td style="padding:.6rem">${b.author}</td>
              <td style="padding:.6rem">${b.price == 0 ? '免費' : 'NT$ ' + b.price}</td>
              <td style="padding:.6rem">
                <span style="color:${b.published ? 'green' : 'gray'}">
                  ${b.published ? '● 上架中' : '○ 已下架'}
                </span>
              </td>
              <td style="padding:.6rem;display:flex;gap:.4rem;flex-wrap:wrap">
                <button class="btn btn-outline-dark" style="font-size:.8rem;padding:.3rem .8rem"
                  onclick="openBookModal(${b.id})">編輯</button>
                ${b.published
                  ? `<button class="btn btn-outline-dark" style="font-size:.8rem;padding:.3rem .8rem"
                      onclick="adminUnpublish(${b.id})">下架</button>`
                  : `<button class="btn btn-gold" style="font-size:.8rem;padding:.3rem .8rem"
                      onclick="adminPublish(${b.id})">上架</button>`}
                <button class="btn" style="font-size:.8rem;padding:.3rem .8rem;background:#fee2e2;color:#991b1b;border:1px solid #fca5a5"
                  onclick="adminDeleteBook(${b.id}, '${escHtml(b.title)}')">刪除</button>
              </td>
            </tr>`).join('') :
            '<tr><td colspan="6" style="padding:1rem;text-align:center">尚無書籍</td></tr>'}
        </tbody>
      </table>

      <!-- 新增/編輯 Modal -->
      <div id="bookFormModal" style="display:none;position:fixed;inset:0;background:rgba(0,0,0,.5);z-index:999;overflow-y:auto">
        <div style="background:#fff;max-width:560px;margin:60px auto;border-radius:16px;padding:2rem;position:relative">
          <button onclick="closeBookModal()" style="position:absolute;top:1rem;right:1rem;background:none;border:none;font-size:1.5rem;cursor:pointer">×</button>
          <h3 id="bookFormTitle" style="margin-bottom:1.5rem">新增書籍</h3>
          <div class="form-group">
            <label class="form-label">書名 *</label>
            <input type="text" class="form-input" id="bfTitle" placeholder="書名">
          </div>
          <div class="form-group">
            <label class="form-label">作者 *</label>
            <input type="text" class="form-input" id="bfAuthor" placeholder="作者">
          </div>
          <div class="form-group">
            <label class="form-label">分類</label>
            <input type="text" class="form-input" id="bfCategory" placeholder="例：程式設計">
          </div>
          <div class="form-group">
            <label class="form-label">簡介</label>
            <textarea class="form-input" id="bfDesc" rows="3" placeholder="書籍簡介" style="resize:vertical"></textarea>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:.8rem">
            <div class="form-group">
              <label class="form-label">總頁數</label>
              <input type="number" class="form-input" id="bfPages" placeholder="0" min="0">
            </div>
            <div class="form-group">
              <label class="form-label">售價（NT$，0 = 免費）</label>
              <input type="number" class="form-input" id="bfPrice" placeholder="0" min="0">
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">封面圖片網址</label>
            <input type="text" class="form-input" id="bfCoverUrl" placeholder="https://...">
          </div>
          <div class="form-group" id="bfUploadGroup" style="display:none">
            <label class="form-label">或上傳封面圖片</label>
            <input type="file" id="bfCoverFile" accept="image/*" onchange="previewCover(this)">
            <img id="bfCoverPreview" style="margin-top:.5rem;max-width:100px;border-radius:6px;display:none">
          </div>
          <div class="form-group">
            <label class="form-label">PDF 檔案路徑</label>
            <input type="text" class="form-input" id="bfFileUrl" placeholder="/upload/book.pdf">
          </div>
          <div class="form-group" style="display:flex;align-items:center;gap:.5rem">
            <input type="checkbox" id="bfPublished" checked>
            <label for="bfPublished">上架</label>
          </div>
          <div id="bfMsg" style="min-height:1.2rem;font-size:.9rem;margin:.5rem 0"></div>
          <div style="display:flex;gap:.8rem;margin-top:1rem">
            <button class="btn btn-gold btn-full" onclick="submitBookForm()">儲存</button>
            <button class="btn btn-outline-dark btn-full" onclick="closeBookModal()">取消</button>
          </div>
        </div>
      </div>`;
  } catch(e) {
    console.error(e);
    el.innerHTML = '<p style="color:red">載入失敗</p>';
  }
}

async function openBookModal(bookId = null) {
  editingBookId = bookId;
  document.getElementById('bookFormTitle').textContent = bookId ? '編輯書籍' : '新增書籍';
  document.getElementById('bfMsg').textContent = '';
  document.getElementById('bfCoverPreview').style.display = 'none';

  if (bookId) {
    const res  = await authFetch('/api/admin/books');
    const list = await res.json();
    const book = list.find(b => b.id === bookId);
    if (book) {
      document.getElementById('bfTitle').value      = book.title || '';
      document.getElementById('bfAuthor').value     = book.author || '';
      document.getElementById('bfCategory').value   = book.category || '';
      document.getElementById('bfDesc').value       = book.description || '';
      document.getElementById('bfPages').value      = book.totalPages || 0;
      document.getElementById('bfPrice').value      = book.price || 0;
      document.getElementById('bfCoverUrl').value   = book.coverUrl || '';
      document.getElementById('bfFileUrl').value    = book.fileUrl || '';
      document.getElementById('bfPublished').checked = book.published;
      document.getElementById('bfUploadGroup').style.display = '';
    }
  } else {
    document.getElementById('bfTitle').value      = '';
    document.getElementById('bfAuthor').value     = '';
    document.getElementById('bfCategory').value   = '';
    document.getElementById('bfDesc').value       = '';
    document.getElementById('bfPages').value      = '';
    document.getElementById('bfPrice').value      = '0';
    document.getElementById('bfCoverUrl').value   = '';
    document.getElementById('bfFileUrl').value    = '';
    document.getElementById('bfPublished').checked = true;
    document.getElementById('bfUploadGroup').style.display = 'none';
  }

  document.getElementById('bookFormModal').style.display = '';
}

function closeBookModal() {
  document.getElementById('bookFormModal').style.display = 'none';
  editingBookId = null;
}

function previewCover(input) {
  const preview = document.getElementById('bfCoverPreview');
  if (input.files?.[0]) {
    preview.src = URL.createObjectURL(input.files[0]);
    preview.style.display = 'block';
  }
}

async function submitBookForm() {
  const title  = document.getElementById('bfTitle').value.trim();
  const author = document.getElementById('bfAuthor').value.trim();
  const msgEl  = document.getElementById('bfMsg');

  if (!title)  { msgEl.style.color = 'red'; msgEl.textContent = '請填寫書名'; return; }
  if (!author) { msgEl.style.color = 'red'; msgEl.textContent = '請填寫作者'; return; }

  const body = {
    title,
    author,
    category:    document.getElementById('bfCategory').value.trim(),
    description: document.getElementById('bfDesc').value.trim(),
    totalPages:  Number.parseInt(document.getElementById('bfPages').value) || 0,
    price:       Number.parseFloat(document.getElementById('bfPrice').value) || 0,
    coverUrl:    document.getElementById('bfCoverUrl').value.trim(),
    fileUrl:     document.getElementById('bfFileUrl').value.trim(),
    published:   document.getElementById('bfPublished').checked,
  };

  try {
    const url    = editingBookId ? `/api/admin/books/${editingBookId}` : '/api/admin/books';
    const method = editingBookId ? 'PUT' : 'POST';
    const res    = await authFetch(url, { method, body: JSON.stringify(body) });
    const book   = await res.json();

    // 若有選擇封面檔案，上傳封面
    const fileInput = document.getElementById('bfCoverFile');
    if (fileInput?.files[0] && book.id) {
      const formData = new FormData();
      formData.append('file', fileInput.files[0]);
      await authFetch(`/api/admin/books/${book.id}/cover`, {
        method: 'POST',
        headers: {},   // 讓瀏覽器自動設定 multipart Content-Type
        body: formData,
      });
    }

    msgEl.style.color = 'green';
    msgEl.textContent = editingBookId ? '✓ 更新成功' : '✓ 新增成功';
    setTimeout(() => { closeBookModal(); loadAdminBooks(); }, 800);
  } catch(e) {
    console.error(e);
    msgEl.style.color = 'red';
    msgEl.textContent = '操作失敗，請再試一次';
  }
}

async function adminDeleteBook(id, title) {
  if (!confirm(`確定要刪除《${title}》嗎？此操作無法還原。`)) return;
  try {
    await authFetch(`/api/admin/books/${id}`, { method: 'DELETE' });
    toast('書籍已刪除');
    loadAdminBooks();
  } catch(e) {
    console.error(e);
    toast('刪除失敗');
  }
}

async function adminPublish(id) {
  await authFetch(`/api/admin/books/${id}/publish`, { method: 'PATCH' });
  loadAdminBooks();
}

async function adminUnpublish(id) {
  await authFetch(`/api/admin/books/${id}/unpublish`, { method: 'PATCH' });
  loadAdminBooks();
}

// ── 書籍渲染 ──
function priceTag(price) {
  if (price === 0) return '<span class="price-free">免費</span>';
  return `<span class="price-tag price-paid">NT$ <strong>${price}</strong></span>`;
}

function renderBookCard(book) {
  return `
    <div class="book-card" onclick="showBookDetail(${book.id})">
      <div class="book-cover">
        <img src="${book.coverUrl || 'https://picsum.photos/seed/' + book.id + '/300/400'}" alt="${book.title}" loading="lazy">
      </div>
      <div class="book-info">
        <div class="book-title">${book.title}</div>
        <div class="book-author">${book.author}</div>
        <span class="book-category">${book.category || '其他'}</span>
        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:.5rem">
          ${priceTag(book.price || 0)}
          <span class="book-views">瀏覽 ${book.views || 0}</span>
        </div>
      </div>
    </div>`;
}

// ── 熱門書籍 ──
async function loadPopular() {
  const el = document.getElementById('popularBooks');
  try {
    const res = await fetch('/api/books/popular');
    const books = await res.json();
    el.innerHTML = books.length ? books.map(renderBookCard).join('') : '<div class="loading">尚無書籍</div>';
  } catch(e) {
    console.error(e);
    el.innerHTML = '<div class="loading">無法連線到伺服器</div>';
  }
}

// ── 書庫 ──
async function loadBooks(page = bookPage) {
  bookPage = page;
  const el  = document.getElementById('booksList');
  const kw  = document.getElementById('searchInput').value.trim();
  el.innerHTML = '<div class="loading">載入中⋯</div>';
  try {
    const params = new URLSearchParams();
    if (activeCategory) params.set('category', activeCategory);
    if (kw) params.set('keyword', kw);
    params.set('page', bookPage);
    params.set('size', 10);

    const res  = await fetch('/api/books?' + params);
    const data = await res.json();
    const books = data.content || [];

    el.innerHTML = books.length
      ? books.map(renderBookCard).join('') + renderPagination(data)
      : '<div class="loading">找不到符合的書籍</div>';

	  const pageInfo = data.page ?? data;
	  document.getElementById('bookCount').textContent = '共 ' + (pageInfo.totalElements || 0) + ' 本';
  } catch(e) {
    console.error(e);
    el.innerHTML = '<div class="loading">無法連線到伺服器</div>';
  }
  loadCategories();
}

function renderPagination(data) {
  const pageInfo = data.page ?? data;
  if (pageInfo.totalPages <= 1) return '';
  const prev = pageInfo.number > 0
    ? `<button class="btn btn-outline-dark" onclick="loadBooks(${pageInfo.number - 1})">← 上一頁</button>`
    : `<button class="btn btn-outline-dark" disabled>← 上一頁</button>`;
  const next = pageInfo.number < pageInfo.totalPages - 1
    ? `<button class="btn btn-outline-dark" onclick="loadBooks(${pageInfo.number + 1})">下一頁 →</button>`
    : `<button class="btn btn-outline-dark" disabled>下一頁 →</button>`;
  return `
    <div style="display:flex;justify-content:center;align-items:center;gap:1rem;margin-top:1.5rem;padding:1rem 0">
      ${prev}
      <span style="font-size:.9rem;color:#888">第 ${pageInfo.number + 1} / ${pageInfo.totalPages} 頁</span>
      ${next}
    </div>`;
}

async function loadCategories() {
  try {
    const res  = await fetch('/api/books/categories');
    const cats = await res.json();
    const tabs = document.getElementById('categoryTabs');
	const existing = new Set(Array.from(tabs.querySelectorAll('[data-cat]')).map(e => e.dataset.cat));
	cats.forEach(c => {
	  if (!existing.has(c)) {
	    const d = document.createElement('div');
	    d.className = 'cat-tab'; d.dataset.cat = c; d.textContent = c;
	    d.onclick = () => filterCategory(d, c);
	    tabs.appendChild(d);
	  }
    });
  } catch(e) { console.error(e); }
}

function filterCategory(el, cat) {
  activeCategory = cat;
  bookPage = 0;
  document.querySelectorAll('.cat-tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
  loadBooks();
}

function debounceSearch() {
	bookPage = 0;
	clearTimeout(searchTimer);
  searchTimer = setTimeout(loadBooks, 350);
}

// ── 書籍詳情 ──
async function showBookDetail(id) {
  currentBookId = id;
  showPage('detail');
  const el = document.getElementById('bookDetailContent');
  el.innerHTML = '<div class="loading">載入中⋯</div>';
  try {
    const token = getToken();
    const res = await fetch('/api/books/' + id, {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    const data = await res.json();
    const book = data.book;
    totalPages  = book.totalPages || 1;
    currentPage = data.progress  || 1;
    renderDetail(book, data.inShelf, data.progress, data.hasPurchased);
  } catch(e) {
    console.error(e);
    el.innerHTML = '<div class="loading">載入失敗</div>';
  }
}

function renderDetail(book, inShelf, progress, hasPurchased) {
  const isFree = book.price === 0;
  const canRead = isFree || hasPurchased;

  // ✅ 修正：將 progress 傳入 openReader，讓閱讀器從上次頁數開啟
  const readBtn = canRead
    ? `<button class="btn btn-gold" onclick="openReader(${book.id},'${escHtml(book.title)}','${book.fileUrl||''}',${book.totalPages||1},${progress||1})">開始閱讀</button>`
    : `<button class="btn btn-gold" onclick="startCheckout(${book.id})">購買 NT$ ${book.price}</button>`;

  const shelfBtn = `<button class="btn btn-outline-dark" id="shelfBtn" onclick="toggleShelf(${book.id},this)">
    ${inShelf ? '✓ 已收藏' : '+ 加入書架'}
  </button>`;

  const priceBadge = isFree
    ? '<span class="badge" style="background:#f0fdf4;color:#166534;border:1px solid #bbf7d0">免費</span>'
    : `<span class="badge" style="background:#fefce8;color:#854d0e;border:1px solid #fde68a">NT$ ${book.price}</span>`;

  const purchasedHint = !isFree && hasPurchased
    ? '<span class="badge" style="background:#f0fdf4;color:#166534;border:1px solid #bbf7d0;margin-left:.3rem">✓ 已購買</span>' : '';

  document.getElementById('bookDetailContent').innerHTML = `
    <div class="detail-layout">
      <div>
        <div class="detail-cover">
          <img src="${book.coverUrl || 'https://picsum.photos/seed/' + book.id + '/300/400'}" alt="${book.title}">
        </div>
      </div>
      <div class="detail-meta">
        <h2>${book.title}</h2>
        <div class="detail-author">作者：${book.author}</div>
        <div class="detail-badges">
          <span class="badge badge-cat">${book.category || '其他'}</span>
          <span class="badge badge-pages">${book.totalPages || 0} 頁</span>
          ${priceBadge}${purchasedHint}
        </div>
        <p class="detail-desc">${book.description || '暫無簡介'}</p>
        <div class="detail-actions">
          ${readBtn}
          ${shelfBtn}
        </div>
        ${progress > 1 && canRead ? `<p class="progress-hint">上次閱讀至第 ${progress} 頁</p>` : ''}
        ${!isFree && !hasPurchased ? `<p class="progress-hint">購買後即可無限閱讀此書籍</p>` : ''}
      </div>
    </div>`;
}

function escHtml(s) { return s.replaceAll("'", String.raw`\'`); }

// ── 書架 ──
async function loadShelf() {
  if (!currentUser) return;
  const el = document.getElementById('shelfBooks');
  el.innerHTML = '<div class="loading">載入中⋯</div>';
  try {
    const res = await authFetch('/api/shelf');
    const books = await res.json();
    if (!Array.isArray(books) || books.length === 0) {
      el.innerHTML = `<div class="shelf-empty"><div class="shelf-empty-icon">📚</div>書架空空的<br><small>前往書庫加入你喜歡的書吧</small></div>`;
    } else {
      el.innerHTML = books.map(renderBookCard).join('');
    }
  } catch(e) {
    console.error(e);
    el.innerHTML = '<div class="loading">載入失敗</div>';
  }
}

async function toggleShelf(bookId, btn) {
  if (!currentUser) { openModal('login'); return; }
  const inShelf = btn.textContent.includes('已收藏');
  try {
    const url = inShelf ? '/api/shelf/remove' : '/api/shelf/add';
    const res  = await authFetch(url + '?bookId=' + bookId, { method: 'POST' });
    const data = await res.json();
    if (!data.success) {
      toast(data.message);
      return;
    }
    btn.textContent = inShelf ? '+ 加入書架' : '✓ 已收藏';
    toast(data.message);
  } catch(e) {
    console.error(e);
    toast('操作失敗');
  }
}

// ── 閱讀器 ──
let pdfDoc = null;

// ✅ 修正：新增 startPage 參數，開啟閱讀器時從上次頁數開始
function openReader(bookId, title, fileUrl, pages, startPage) {
  currentBookId = bookId;
  totalPages    = pages || 1;
  currentPage   = startPage || 1;
  showPage('reader');
  document.getElementById('readerTitle').textContent = title;
  document.getElementById('readerShelfBtn').textContent = '加入書架';

  const canvas      = document.getElementById('readerCanvas');
  const placeholder = document.getElementById('readerPlaceholder');

  if (!fileUrl) {
    placeholder.style.display = '';
    canvas.style.display = 'none';
    return;
  }

  placeholder.style.display = 'none';
  canvas.style.display = 'block';

  pdfjsLib.GlobalWorkerOptions.workerSrc =
    'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';

  pdfjsLib.getDocument(fileUrl).promise.then(pdf => {
    pdfDoc = pdf;
    totalPages = pdf.numPages;
    // 確保 startPage 不超過實際總頁數
    currentPage = Math.min(currentPage, totalPages);
    updatePageInfo();
    renderPage(currentPage);
  }).catch(err => {
    placeholder.style.display = '';
    canvas.style.display = 'none';
    placeholder.querySelector('.reader-icon').textContent = '⚠️';
    console.error('PDF 載入失敗', err);
  });
}

function changePage(delta) {
  if (!pdfDoc) return;
  currentPage = Math.max(1, Math.min(totalPages, currentPage + delta));
  updatePageInfo();
  saveProgress();
  renderPage(currentPage);
}

async function renderPage(num) {
  pdfDoc.getPage(num).then(async page => {
    const canvas  = document.getElementById('readerCanvas');
    const ctx     = canvas.getContext('2d');
    const containerWidth = canvas.parentElement.clientWidth || 800;
    const unscaled = page.getViewport({ scale: 1 });
    const scale   = containerWidth / unscaled.width;
    const viewport = page.getViewport({ scale });

    canvas.width  = viewport.width;
    canvas.height = viewport.height;

    await page.render({ canvasContext: ctx, viewport }).promise;
  });
}

function updatePageInfo() {
  document.getElementById('readerPageInfo').textContent = `第 ${currentPage} / ${totalPages} 頁`;
}

async function saveProgress() {
  if (!currentUser || !currentBookId) return;
  try {
    await authFetch(`/api/progress?bookId=${currentBookId}&page=${currentPage}`, { method: 'POST' });
  } catch(e) { console.error(e); }
}

async function toggleShelfFromReader() {
  if (!currentUser) { openModal('login'); return; }
  const btn     = document.getElementById('readerShelfBtn');
  const inShelf = btn.textContent.includes('移除');
  try {
    const url  = inShelf ? '/api/shelf/remove' : '/api/shelf/add';
    const res  = await authFetch(url + '?bookId=' + currentBookId, { method: 'POST' });
    const data = await res.json();
    btn.textContent = inShelf ? '加入書架' : '從書架移除';
    toast(data.message);
  } catch(e) {
    console.error(e);
    toast('操作失敗');
  }
}

// ── 結帳流程 ──
let currentOrderId = null;
let currentPayMethod = 'card';
let checkoutBookData = null;

async function startCheckout(bookId) {
  if (!currentUser) { openModal('login'); return; }
  try {
    const res  = await authFetch('/api/orders/create?bookId=' + bookId, { method: 'POST' });
    const data = await res.json();
    if (!data.success) { toast(data.message); return; }

    currentOrderId   = data.orderId;
    checkoutBookData = data;

    const coverUrl = document.querySelector('.detail-cover img')?.src || '';
    document.getElementById('checkoutBook').innerHTML = `
      <img class="checkout-book-cover" src="${coverUrl}" alt="${data.bookTitle}">
      <div class="checkout-book-info">
        <div class="checkout-book-title">${data.bookTitle}</div>
        <div class="checkout-book-author">購買人：${data.username}</div>
      </div>`;
    document.getElementById('checkoutTotal').innerHTML = `
      <span class="checkout-total-label">應付金額</span>
      <span class="checkout-total-amount">NT$ ${data.amount}</span>`;

    showPage('checkout');
  } catch(e) {
    console.error(e);
    toast('連線失敗，請確認伺服器是否啟動');
  }
}

function selectMethod(el, method) {
  currentPayMethod = method;
  document.querySelectorAll('.method-opt').forEach(o => {
    o.style.border = '2px solid #ddd';
    o.style.background = '#fff';
    o.style.color = '#555';
    o.classList.remove('active');
  });
  el.style.border = '2px solid #c9a84c';
  el.style.background = '#fffbf0';
  el.style.color = '#7a5c1e';
  el.classList.add('active');

  const detail = document.getElementById('methodDetail');
  if (method === 'card') {
    detail.innerHTML = `
      <div class="form-group">
        <label class="form-label">持卡人姓名</label>
        <input type="text" class="form-input" placeholder="持卡人姓名" maxlength="50">
      </div>
      <div class="form-group">
        <label class="form-label">卡號</label>
        <input type="text" class="form-input" placeholder="卡號" maxlength="19">
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:.8rem">
        <div class="form-group">
          <label class="form-label">有效期限</label>
          <input type="text" class="form-input" placeholder="MM/YY" maxlength="5">
        </div>
        <div class="form-group">
          <label class="form-label">CVV</label>
          <input type="text" class="form-input" placeholder="123" maxlength="3">
        </div>
      </div>`;
  } else if (method === 'atm') {
    detail.innerHTML = `
      <div style="background:var(--parchment-warm);border:1px solid var(--parchment-deep);border-radius:var(--radius);padding:1rem;font-size:.85rem;color:var(--text-secondary)">
        <div style="font-weight:500;margin-bottom:.5rem;color:var(--ink)">ATM 虛擬帳號（模擬）</div>
        銀行代碼：<strong>812</strong><br>
        帳號：<strong>9380-1234-5678-9012</strong><br>
        <span style="font-size:.75rem;color:var(--text-muted)">繳費期限：30 分鐘內</span>
      </div>`;
  } else {
    detail.innerHTML = `
      <div style="background:var(--parchment-warm);border:1px solid var(--parchment-deep);border-radius:var(--radius);padding:1rem;font-size:.85rem;color:var(--text-secondary)">
        <div style="font-weight:500;margin-bottom:.5rem;color:var(--ink)">超商繳費代碼（模擬）</div>
        超商：<strong>7-ELEVEN / 全家 / 萊爾富</strong><br>
        繳費代碼：<strong>MOCK-${Math.random().toString(36).slice(2,10).toUpperCase()}</strong><br>
        <span style="font-size:.75rem;color:var(--text-muted)">繳費期限：24 小時內</span>
      </div>`;
  }
}

async function submitPayment() {
  if (!currentOrderId) return;

  if (currentPayMethod === 'card') {
    const inputs = document.querySelectorAll('#methodDetail .form-input');
    const name   = inputs[0]?.value.trim();
    const card   = inputs[1]?.value.trim();
    const expiry = inputs[2]?.value.trim();
    const cvv    = inputs[3]?.value.trim();

    if (!name)                                        { toast('請填寫持卡人姓名'); return; }
    if (!card || card.replaceAll(' ', '').length < 16) { toast('請填寫正確的卡號（16碼）'); return; }
    if (!expiry || !/^\d{2}\/\d{2}$/.test(expiry))   { toast('請填寫正確的有效期限（MM/YY）'); return; }
    if (!cvv || cvv.length < 3)                       { toast('請填寫正確的 CVV（3碼）'); return; }
  }

  const btn = document.getElementById('payBtn');
  btn.textContent = '處理中⋯';
  btn.disabled = true;

  await new Promise(r => setTimeout(r, 1500));

  try {
    const res  = await authFetch('/api/orders/confirm?orderId=' + currentOrderId, { method: 'POST' });
    const data = await res.json();
	if (data.success) {
	  console.log('準備跳付款完成頁，data:', data);
	  showPaidPage(data);
	  console.log('showPaidPage 執行完畢');
	  authFetch('/api/shelf/add?bookId=' + data.bookId, { method: 'POST' }).catch(() => {});
	} else {
	  toast(data.message || '付款失敗');
	  btn.textContent = '確認付款';
	  btn.disabled = false;
	}
  } catch(e) {
    console.error(e);
    toast('連線失敗');
    btn.textContent = '確認付款';
    btn.disabled = false;
  }
}

function showPaidPage(data) {
  const subEl = document.getElementById('paidSub');
  const tradeEl = document.getElementById('paidTrade');
  const readBtn = document.getElementById('paidReadBtn');

  if (subEl)   subEl.textContent   = `《${data.bookTitle}》購買成功！`;
  if (tradeEl) tradeEl.textContent = data.tradeNo ? `交易序號：${data.tradeNo}` : '';
  if (readBtn) {
    readBtn.onclick = () => {
      pageHistory.length = 0;
      pageHistory.push('home');
      showBookDetail(data.bookId);
    };
  }

  showPage('paid', false);
}

function cancelCheckout() {
  currentOrderId = null;
  goBack();
}

// ── 購買紀錄 ──
async function loadOrders() {
  if (!currentUser) return;
  const el = document.getElementById('orderList');
  el.innerHTML = '<div class="loading">載入中⋯</div>';
  try {
    const res = await authFetch('/api/orders/history');
    const orders = await res.json();
    if (!orders.length) {
      el.innerHTML = '<div class="shelf-empty"><div class="shelf-empty-icon">🛒</div>尚無購買紀錄<br><small>前往書庫購買你喜歡的書吧</small></div>';
      return;
    }
    el.innerHTML = '<div class="order-list">' + orders.map(o => `
      <div class="order-item">
        <img class="order-cover" src="${o.coverUrl || 'https://picsum.photos/seed/' + o.bookId + '/300/400'}" alt="${o.bookTitle}">
        <div class="order-info">
          <div class="order-title">${o.bookTitle}</div>
          <div class="order-meta">購買時間：${o.createdAt?.slice(0,16).replace('T',' ')}</div>
          ${o.tradeNo ? `<div class="order-trade">交易序號：${o.tradeNo}</div>` : ''}
        </div>
        <div class="order-right">
          <div class="order-amount">NT$ ${o.amount}</div>
          <span class="order-status status-${o.status}">${statusLabel(o.status)}</span>
          ${o.status === 'PAID' ? `<br><button class="btn btn-outline-dark" style="margin-top:.5rem;padding:.3rem .7rem;font-size:.74rem" onclick="showBookDetail(${o.bookId})">閱讀</button>` : ''}
        </div>
      </div>`).join('') + '</div>';
  } catch(e) {
    console.error(e);
    el.innerHTML = '<div class="loading">載入失敗</div>';
  }
}

function statusLabel(s) {
  return { PAID:'已付款', PENDING:'待付款', FAILED:'已取消', REFUNDED:'已退款' ,COMPLETED:'已完成' }[s] || s;
}

(async function init() {
  const token = new URLSearchParams(location.search).get('token');
  if (token) {
    showPage('reset', false);
    return;
  }
  try {
    const res  = await authFetch('/api/auth/me');
    const data = await res.json();
    if (data.success) { currentUser = data.data; updateNav(); }
  } catch(e) { console.error(e); }
  loadPopular();
  loadCategories();
})();

// ===== 管理後台 Tab =====
function switchAdminTab(tab) {
  document.getElementById('adminPanelBooks').style.display  = tab === 'books'  ? '' : 'none';
  document.getElementById('adminPanelOrders').style.display = tab === 'orders' ? '' : 'none';
  document.getElementById('tabBooks').classList.toggle('active',  tab === 'books');
  document.getElementById('tabOrders').classList.toggle('active', tab === 'orders');
  if (tab === 'orders') loadAdminOrders();
}

// ===== 訂單統計 + 列表 =====
async function loadAdminOrders() {
  try {
    const [statsRes, listRes] = await Promise.all([
      authFetch('/api/admin/orders/stats'),
      authFetch('/api/admin/orders')
    ]);
    const stats = await statsRes.json();
    const list  = await listRes.json();
    renderAdminStats(stats);
    renderAdminOrderList(list);
  } catch (e) {
    document.getElementById('adminOrderStats').innerHTML = '<p style="color:red">載入失敗</p>';
  }
}

function renderAdminStats(s) {
  const statusColor = { paid:'#4caf50', pending:'#ff9800', cancelled:'#f44336', revenue:'#c9a84c' };
  document.getElementById('adminOrderStats').innerHTML = `
    ${statCard('已付款', s.paidCount + ' 筆', statusColor.paid)}
    ${statCard('待付款', s.pendingCount + ' 筆', statusColor.pending)}
    ${statCard('已取消', s.cancelledCount + ' 筆', statusColor.cancelled)}
    ${statCard('總營收', 'NT$ ' + Number(s.totalRevenue).toLocaleString(), statusColor.revenue)}
    ${statCard('本月營收', 'NT$ ' + Number(s.monthRevenue).toLocaleString(), statusColor.revenue)}
  `;
}

function statCard(label, value, color) {
  return `
    <div style="background:#fff;border-radius:12px;padding:1.2rem 1.4rem;
                box-shadow:0 2px 8px rgba(0,0,0,.07);border-left:4px solid ${color}">
      <div style="font-size:.8rem;color:#999;margin-bottom:.4rem">${label}</div>
      <div style="font-size:1.4rem;font-weight:600;color:#333">${value}</div>
    </div>`;
}

const STATUS_LABEL = { PAID:'已付款', PENDING:'待付款', FAILED:'已取消', REFUNDED:'已退款', COMPLETED:'已完成' };
const STATUS_COLOR = { PAID:'#4caf50', PENDING:'#ff9800', FAILED:'#f44336', REFUNDED:'#2196f3', COMPLETED:'#9c27b0' };

function renderAdminOrderList(list) {
  const tbody = document.getElementById('adminOrderTbody');
  const table = document.getElementById('adminOrderTable');
  const empty = document.getElementById('adminOrderEmpty');

  if (!list.length) {
    table.style.display = 'none';
    empty.style.display = '';
    return;
  }

  table.style.display = '';
  empty.style.display = 'none';
  tbody.innerHTML = list.map(o => `
    <tr style="border-bottom:1px solid #f0ebe0">
      <td style="padding:.65rem 1rem">#${o.id}</td>
      <td style="padding:.65rem 1rem">
        <div style="font-weight:500">${o.username}</div>
        <div style="font-size:.78rem;color:#999">${o.userEmail}</div>
      </td>
      <td style="padding:.65rem 1rem">${o.bookTitle}</td>
      <td style="padding:.65rem 1rem">NT$ ${Number(o.amount).toLocaleString()}</td>
      <td style="padding:.65rem 1rem">
        <span style="background:${STATUS_COLOR[o.status] ?? '#aaa'}22;color:${STATUS_COLOR[o.status] ?? '#aaa'};
                     padding:.2rem .7rem;border-radius:20px;font-size:.78rem;font-weight:500">
          ${STATUS_LABEL[o.status] ?? o.status}
        </span>
      </td>
      <td style="padding:.65rem 1rem;font-size:.8rem;color:#888">${o.tradeNo ?? '—'}</td>
      <td style="padding:.65rem 1rem;font-size:.8rem;color:#888">${o.paidAt ? o.paidAt.slice(0,16).replace('T',' ') : '—'}</td>
      <td style="padding:.65rem 1rem;font-size:.8rem;color:#888">${o.createdAt ? o.createdAt.slice(0,16).replace('T',' ') : '—'}</td>
    </tr>
  `).join('');
}