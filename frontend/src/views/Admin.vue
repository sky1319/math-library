
<template>
  <div class="admin-container app-scene-root">
    <header class="header glass-header">
      <div class="logo">管理员后台</div>
      <div class="header-right">
        <FlowerButton variant="wide" @click="goToQA">AgentAI 图书助手</FlowerButton>
        <span class="admin-name">{{ userStore.name }}</span>
        <FlowerButton variant="nav" @click="handleLogout">退出登录</FlowerButton>
      </div>
    </header>
    
    <div class="main-content">
      <aside class="sidebar">
        <el-menu :default-active="activeMenu" class="sidebar-menu">
          <el-menu-item index="books" @click="activeMenu = 'books'">图书管理</el-menu-item>
          <el-menu-item index="records" @click="activeMenu = 'records'">借阅记录</el-menu-item>
          <el-menu-item index="overdue" @click="activeMenu = 'overdue'">逾期预警</el-menu-item>
          <el-menu-item index="statistics" @click="activeMenu = 'statistics'">统计报表</el-menu-item>
          <el-menu-item index="logs" @click="activeMenu = 'logs'">操作日志</el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="users" @click="activeMenu = 'users'">用户与权限</el-menu-item>
        </el-menu>
      </aside>
      
      <main class="content">
        <div v-if="activeMenu === 'books'" class="content-section">
          <div class="section-header">
            <h2>图书管理</h2>
            <FlowerButton variant="action" @click="openAddModal" class="glass-btn primary">添加图书</FlowerButton>
          </div>
          <el-table :data="books" class="admin-table" stripe height="calc(100vh - 180px)">
            <el-table-column prop="isbn" label="ISBN" min-width="130" />
            <el-table-column prop="title" label="书名" min-width="140" show-overflow-tooltip />
            <el-table-column prop="author" label="作者" min-width="100" />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column prop="totalCount" label="馆藏" width="70" align="center" />
            <el-table-column prop="borrowedCount" label="已借" width="70" align="center" />
            <el-table-column prop="location" label="位置" min-width="120" />
            <el-table-column label="可借阅" width="90" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.borrowable !== false ? 'success' : 'danger'" size="small">
                  {{ scope.row.borrowable !== false ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="scope">
                <FlowerButton variant="mini" @click="editBook(scope.row)" class="table-btn">编辑</FlowerButton>
                <FlowerButton variant="mini" @click="deleteBook(scope.row)" class="table-btn">删除</FlowerButton>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div v-if="activeMenu === 'records'" class="content-section">
          <div class="section-header">
            <h2>读者借阅情况</h2>
            <el-input v-model="readerFilter" placeholder="按读者姓名筛选" clearable class="filter-input glass-input galaxy-input" />
          </div>
          <el-table :data="filteredBorrowRecords" class="admin-table" stripe height="calc(100vh - 180px)">
            <el-table-column prop="userName" label="读者" width="100" />
            <el-table-column prop="userId" label="用户ID" width="110" />
            <el-table-column prop="bookTitle" label="书名" min-width="140" show-overflow-tooltip />
            <el-table-column prop="borrowDate" label="借阅日期" width="120" />
            <el-table-column prop="dueDate" label="到期日期" width="120" />
            <el-table-column prop="returnDate" label="归还日期" width="120" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'BORROWED' ? 'warning' : 'success'" size="small">
                  {{ scope.row.status === 'BORROWED' ? '借阅中' : '已归还' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div v-if="activeMenu === 'overdue'" class="content-section">
          <h2>逾期预警</h2>
          <el-table :data="overdueRecords" class="admin-table" stripe height="calc(100vh - 180px)">
            <el-table-column prop="userName" label="读者" width="100" />
            <el-table-column prop="bookTitle" label="书名" min-width="140" />
            <el-table-column prop="borrowDate" label="借阅日期" width="120" />
            <el-table-column prop="dueDate" label="到期日期" width="120" />
            <el-table-column prop="daysOverdue" label="逾期天数" width="100" align="center">
              <template #default="scope">
                <el-tag type="danger" size="small">{{ scope.row.daysOverdue }} 天</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div v-if="activeMenu === 'statistics'" class="content-section">
          <h2>分类借阅统计</h2>
          <el-table :data="categoryStats" class="admin-table" stripe height="calc(100vh - 180px)">
            <el-table-column prop="category" label="分类" />
            <el-table-column prop="borrowCount" label="借阅数量" align="center" />
          </el-table>
        </div>
        
        <div v-if="activeMenu === 'logs'" class="content-section">
          <h2>操作日志</h2>
          <el-table :data="logs" class="admin-table" stripe height="calc(100vh - 180px)">
            <el-table-column prop="timestamp" label="时间" min-width="160" />
            <el-table-column prop="userRole" label="角色" width="80" />
            <el-table-column prop="userId" label="用户ID" width="110" />
            <el-table-column prop="operation" label="操作" width="120" />
            <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
          </el-table>
        </div>

        <div v-if="activeMenu === 'users' && userStore.isAdmin" class="content-section">
          <div class="section-header">
            <h2>用户与权限</h2>
            <FlowerButton variant="action" @click="openUserModal">新增用户</FlowerButton>
          </div>
          <el-table :data="users" class="admin-table" stripe height="calc(100vh - 180px)">
            <el-table-column prop="userId" label="用户ID" width="140" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column label="角色" width="130">
              <template #default="scope">
                <el-tag size="small">{{ roleLabel(scope.row.role) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.enabled ? 'success' : 'danger'" size="small">
                  {{ scope.row.enabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="245" fixed="right">
              <template #default="scope">
                <FlowerButton variant="mini" @click="editUser(scope.row)">编辑</FlowerButton>
                <FlowerButton variant="mini" @click="toggleUser(scope.row)">{{ scope.row.enabled ? '停用' : '启用' }}</FlowerButton>
                <FlowerButton variant="mini" @click="resetUserPassword(scope.row)">重置密码</FlowerButton>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </main>
    </div>
    
    <el-dialog v-model="showAddModal" :title="editingBook ? '编辑图书' : '添加图书'" width="560px" class="glass-dialog">
      <el-form :model="form" label-width="100px" class="glass-form">
        <el-form-item label="ISBN">
          <el-input v-model="form.isbn" :disabled="!!editingBook" class="glass-input galaxy-input" />
        </el-form-item>
        <el-form-item label="书名">
          <el-input v-model="form.title" class="glass-input galaxy-input" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="form.author" class="glass-input galaxy-input" />
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="form.publisher" class="glass-input galaxy-input" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" class="glass-input galaxy-input" style="width: 100%">
            <el-option label="科幻小说" value="科幻小说" />
            <el-option label="推理小说" value="推理小说" />
            <el-option label="文学经典" value="文学经典" />
            <el-option label="历史" value="历史" />
            <el-option label="编程" value="编程" />
          </el-select>
        </el-form-item>
        <el-form-item label="馆藏数量">
          <el-input-number v-model="form.totalCount" :min="1" class="glass-input galaxy-input" />
        </el-form-item>
        <el-form-item label="馆藏位置">
          <el-input v-model="form.location" placeholder="如 A区-01-001" class="glass-input galaxy-input" />
        </el-form-item>
        <el-form-item label="允许借阅">
          <el-switch v-model="form.borrowable" active-text="可借" inactive-text="不可借" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" class="glass-input galaxy-input" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" class="glass-input galaxy-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <FlowerButton variant="action" @click="showAddModal = false" class="glass-btn">取消</FlowerButton>
        <FlowerButton variant="action" @click="saveBook" class="glass-btn primary">保存</FlowerButton>
      </template>
    </el-dialog>

    <el-dialog v-model="showUserModal" :title="editingUser ? '编辑用户' : '新增用户'" width="480px" class="glass-dialog">
      <el-form :model="userForm" label-width="90px" class="glass-form">
        <el-form-item label="用户ID">
          <el-input v-model="userForm.userId" :disabled="!!editingUser" class="glass-input galaxy-input" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="userForm.name" class="glass-input galaxy-input" /></el-form-item>
        <el-form-item v-if="!editingUser" label="初始密码"><el-input v-model="userForm.password" type="password" show-password class="glass-input galaxy-input" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role" class="glass-input galaxy-input" style="width: 100%">
            <el-option label="读者" value="USER" />
            <el-option label="馆员" value="LIBRARIAN" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱"><el-input v-model="userForm.email" class="glass-input galaxy-input" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="userForm.phone" class="glass-input galaxy-input" /></el-form-item>
        <el-form-item label="账号状态"><el-switch v-model="userForm.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
      </el-form>
      <template #footer>
        <FlowerButton variant="action" @click="showUserModal = false">取消</FlowerButton>
        <FlowerButton variant="action" class="primary" @click="saveUser">保存</FlowerButton>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import axios from '../axios'
import { ElMessage } from 'element-plus'
import FlowerButton from '../components/FlowerButton.vue'

const router = useRouter()
const userStore = useUserStore()

const activeMenu = ref('books')
const books = ref([])
const borrowRecords = ref([])
const overdueRecords = ref([])
const categoryStats = ref([])
const logs = ref([])
const users = ref([])
const readerFilter = ref('')

const showAddModal = ref(false)
const editingBook = ref(null)
const defaultForm = () => ({
  isbn: '',
  title: '',
  author: '',
  publisher: '',
  category: '',
  totalCount: 1,
  location: '',
  keywords: '',
  description: '',
  borrowable: true
})
const form = ref(defaultForm())
const showUserModal = ref(false)
const editingUser = ref(null)
const defaultUserForm = () => ({ userId: '', name: '', password: '', role: 'USER', email: '', phone: '', enabled: true })
const userForm = ref(defaultUserForm())

const filteredBorrowRecords = computed(() => {
  if (!readerFilter.value) return borrowRecords.value
  const key = readerFilter.value.toLowerCase()
  return borrowRecords.value.filter(r =>
    (r.userName && r.userName.toLowerCase().includes(key)) ||
    (r.userId && r.userId.toLowerCase().includes(key))
  )
})

onMounted(() => {
  loadBooks()
})

function goToQA() {
  router.push('/qa')
}

function openAddModal() {
  editingBook.value = null
  form.value = defaultForm()
  showAddModal.value = true
}

async function loadBooks() {
  const response = await axios.get('/admin/books')
  if (response.data.success) {
    books.value = response.data.data
  }
}

async function loadBorrowRecords() {
  const response = await axios.get('/admin/borrow-records')
  if (response.data.success) {
    borrowRecords.value = response.data.data
  }
}

async function loadOverdueRecords() {
  const response = await axios.get('/admin/overdue-warnings')
  if (response.data.success) {
    overdueRecords.value = response.data.data
  }
}

async function loadStatistics() {
  const response = await axios.get('/admin/statistics/categories')
  if (response.data.success) {
    categoryStats.value = response.data.data
  }
}

async function loadLogs() {
  const response = await axios.get('/admin/logs')
  if (response.data.success) {
    logs.value = response.data.data
  }
}

async function loadUsers() {
  const response = await axios.get('/admin/users')
  if (response.data.success) users.value = response.data.data
}

function roleLabel(role) {
  return { USER: '读者', LIBRARIAN: '馆员', ADMIN: '管理员' }[role] || role
}

function openUserModal() {
  editingUser.value = null
  userForm.value = defaultUserForm()
  showUserModal.value = true
}

function editUser(user) {
  editingUser.value = user
  userForm.value = { ...user, password: '' }
  showUserModal.value = true
}

async function saveUser() {
  try {
    if (editingUser.value) {
      await axios.put(`/admin/users/${userForm.value.userId}`, userForm.value)
    } else {
      await axios.post('/admin/users', userForm.value)
    }
    showUserModal.value = false
    await loadUsers()
    ElMessage.success('用户保存成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '用户保存失败')
  }
}

async function toggleUser(user) {
  try {
    await axios.put(`/admin/users/${user.userId}`, { enabled: !user.enabled })
    await loadUsers()
    ElMessage.success(user.enabled ? '账号已停用' : '账号已启用')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '状态更新失败')
  }
}

async function resetUserPassword(user) {
  const newPassword = window.prompt(`请输入 ${user.userId} 的新密码（至少6位）`)
  if (!newPassword) return
  try {
    await axios.post(`/admin/users/${user.userId}/reset-password`, { newPassword })
    ElMessage.success('密码已重置')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '密码重置失败')
  }
}

function editBook(book) {
  editingBook.value = book
  form.value = { ...book, borrowable: book.borrowable !== false }
  showAddModal.value = true
}

async function saveBook() {
  try {
    if (editingBook.value) {
      await axios.put(`/admin/books/${form.value.isbn}`, form.value)
    } else {
      await axios.post('/admin/books', form.value)
    }
    showAddModal.value = false
    editingBook.value = null
    form.value = defaultForm()
    loadBooks()
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

async function deleteBook(book) {
  if (!confirm(`确定删除图书《${book.title}》吗？`)) return
  const response = await axios.delete(`/admin/books/${book.isbn}`)
  if (response.data.success) {
    loadBooks()
    ElMessage.success('删除成功')
  } else {
    ElMessage.warning(response.data.message)
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

watch(activeMenu, (newVal) => {
  if (newVal === 'books') loadBooks()
  else if (newVal === 'records') loadBorrowRecords()
  else if (newVal === 'overdue') loadOverdueRecords()
  else if (newVal === 'statistics') loadStatistics()
  else if (newVal === 'logs') loadLogs()
  else if (newVal === 'users') loadUsers()
})
</script>

<style scoped>
.admin-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.admin-container > .header,
.admin-container > .main-content {
  position: relative;
  z-index: 1;
}

.header {
  color: white;
  padding: 12px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}

.header-right {
  display: flex;
  gap: 16px;
  align-items: center;
}

.admin-name {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
}

.main-content {
  flex: 1;
  display: flex;
  gap: 12px;
  padding: 12px 16px 16px;
  min-height: calc(100vh - 56px);
  box-sizing: border-box;
}

.sidebar {
  width: 180px;
  flex-shrink: 0;
  background: rgba(15, 23, 42, 0.28);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  padding: 10px 8px;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

.sidebar-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 4px;
  border-radius: 10px;
  height: 44px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.12);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: rgba(102, 126, 234, 0.35);
  color: white;
}

.content {
  flex: 1;
  min-width: 0;
  background: rgba(15, 23, 42, 0.22);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  padding: 16px 18px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 16px;
}

.section-header h2,
.content-section > h2 {
  color: white;
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 12px 0;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.35);
}

.section-header h2 {
  margin-bottom: 0;
}

.filter-input {
  width: 220px;
}

.admin-table {
  width: 100%;
  background: rgba(15, 23, 42, 0.18) !important;
  border-radius: 10px;
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(255, 255, 255, 0.06);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.08);
  --el-table-border-color: rgba(255, 255, 255, 0.1);
}

.admin-table :deep(th.el-table__cell),
.admin-table :deep(td.el-table__cell) {
  color: rgba(255, 255, 255, 0.92);
  background: transparent !important;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
}

.admin-table :deep(.el-table__row--striped td.el-table__cell) {
  background: rgba(255, 255, 255, 0.045) !important;
}

.admin-table :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: rgba(255, 255, 255, 0.09) !important;
}

.admin-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.table-btn {
  padding: 0;
  border: 0;
  background: transparent;
  font-size: 12px;
}

.glass-dialog :deep(.el-dialog) {
  background: rgba(15, 23, 42, 0.88);
  backdrop-filter: blur(16px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.glass-dialog :deep(.el-dialog__title) {
  color: white;
}

.glass-form :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.75);
}

.glass-btn.primary {
  background: transparent;
  border: none;
}
</style>
