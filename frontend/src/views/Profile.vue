<template>
  <div class="profile-container app-scene-root">
    <header class="header glass-header">
      <div>
        <div class="logo">个人中心</div>
        <div class="subtitle">{{ userStore.name }} · {{ roleLabel }}</div>
      </div>
      <div class="header-right">
        <FlowerButton variant="nav" @click="goToHome">首页</FlowerButton>
        <FlowerButton variant="nav" @click="handleLogout">退出登录</FlowerButton>
      </div>
    </header>

    <main class="content">
      <section class="profile-summary glass-card">
        <div><span>用户ID</span><strong>{{ userStore.userId }}</strong></div>
        <div><span>当前借阅</span><strong>{{ activeBorrowCount }}</strong></div>
        <div><span>有效预约</span><strong>{{ activeReservationCount }}</strong></div>
        <div><span>未读通知</span><strong>{{ unreadCount }}</strong></div>
      </section>

      <section class="workspace glass-card">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="我的借阅" name="borrows">
            <el-table :data="borrowRecords" class="profile-table" height="470">
              <el-table-column prop="bookTitle" label="书名" min-width="170" show-overflow-tooltip />
              <el-table-column prop="borrowDate" label="借阅日期" width="115" />
              <el-table-column prop="dueDate" label="到期日期" width="115" />
              <el-table-column prop="renewCount" label="续借" width="70" align="center" />
              <el-table-column label="状态" width="105">
                <template #default="scope">
                  <el-tag :type="borrowStatusType(scope.row)" size="small">
                    {{ borrowStatusLabel(scope.row) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="175" fixed="right">
                <template #default="scope">
                  <div v-if="scope.row.status === 'BORROWED'" class="row-actions">
                    <FlowerButton
                      variant="mini"
                      :disabled="!scope.row.renewable"
                      @click="handleRenew(scope.row)"
                    >续借</FlowerButton>
                    <FlowerButton variant="mini" @click="handleReturn(scope.row)">归还</FlowerButton>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`预约队列 (${activeReservationCount})`" name="reservations">
            <el-table :data="reservations" class="profile-table" height="470">
              <el-table-column prop="bookTitle" label="书名" min-width="200" />
              <el-table-column label="状态" width="110">
                <template #default="scope">
                  <el-tag :type="reservationType(scope.row.status)" size="small">
                    {{ reservationLabel(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="排队位置" width="100" align="center">
                <template #default="scope">{{ scope.row.queuePosition || '-' }}</template>
              </el-table-column>
              <el-table-column prop="reservedAt" label="预约时间" min-width="170" :formatter="dateTimeFormatter" />
              <el-table-column label="保留截止" min-width="170">
                <template #default="scope">{{ formatDateTime(scope.row.expiresAt) || '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="scope">
                  <FlowerButton
                    v-if="['WAITING', 'NOTIFIED'].includes(scope.row.status)"
                    variant="mini"
                    @click="cancelReservation(scope.row)"
                  >取消</FlowerButton>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`愿望单 (${wishList.length})`" name="wishlist">
            <div v-if="wishList.length" class="wish-list">
              <article v-for="book in wishList" :key="book.isbn" class="wish-item">
                <div class="wish-info">
                  <h3>{{ book.title }}</h3>
                  <p>{{ book.author }} · {{ book.location || '位置待确认' }}</p>
                  <span>可借 {{ book.availableCount }}/{{ book.totalCount }}</span>
                </div>
                <div class="row-actions">
                  <FlowerButton
                    variant="action"
                    @click="book.availableCount > 0 ? handleBorrow(book) : reserveBook(book)"
                  >{{ book.availableCount > 0 ? '立即借阅' : '排队预约' }}</FlowerButton>
                  <FlowerButton variant="action" @click="removeFromWishList(book)">移除</FlowerButton>
                </div>
              </article>
            </div>
            <el-empty v-else description="愿望单为空" />
          </el-tab-pane>

          <el-tab-pane :label="`站内通知 (${unreadCount})`" name="notifications">
            <div class="notification-toolbar">
              <span>到期、逾期和预约到书通知都会保存在这里</span>
              <FlowerButton variant="mini" :disabled="unreadCount === 0" @click="markAllRead">全部已读</FlowerButton>
            </div>
            <div v-if="notifications.length" class="notification-list">
              <button
                v-for="item in notifications"
                :key="item.id"
                class="notification-item"
                :class="{ unread: !item.read }"
                type="button"
                @click="markRead(item)"
              >
                <span class="notice-dot" aria-hidden="true"></span>
                <span class="notice-content">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.content }}</span>
                </span>
                <time>{{ formatDateTime(item.createdAt) }}</time>
              </button>
            </div>
            <el-empty v-else description="暂无通知" />
          </el-tab-pane>
        </el-tabs>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import axios from '../axios'
import FlowerButton from '../components/FlowerButton.vue'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('borrows')
const borrowRecords = ref([])
const wishList = ref([])
const reservations = ref([])
const notifications = ref([])
const unreadCount = ref(0)

const roleLabel = computed(() => ({ ADMIN: '系统管理员', LIBRARIAN: '馆员', USER: '读者' }[userStore.role] || '读者'))
const activeBorrowCount = computed(() => borrowRecords.value.filter(item => item.status === 'BORROWED').length)
const activeReservationCount = computed(() => reservations.value.filter(item => ['WAITING', 'NOTIFIED'].includes(item.status)).length)

onMounted(loadAll)

async function loadAll() {
  await Promise.all([loadBorrowRecords(), loadWishList(), loadReservations(), loadNotifications()])
}

async function loadBorrowRecords() {
  const response = await axios.get('/user/borrow-records')
  borrowRecords.value = response.data.data || []
}

async function loadWishList() {
  const response = await axios.get('/user/wish-list')
  wishList.value = response.data.data || []
}

async function loadReservations() {
  const response = await axios.get('/user/reservations')
  reservations.value = response.data.data || []
}

async function loadNotifications() {
  const [listResponse, countResponse] = await Promise.all([
    axios.get('/user/notifications'),
    axios.get('/user/notifications/unread-count')
  ])
  notifications.value = listResponse.data.data || []
  unreadCount.value = countResponse.data.data || 0
}

async function handleReturn(record) {
  try {
    await ElMessageBox.confirm(`确认归还《${record.bookTitle}》吗？`, '归还图书')
    await axios.post(`/user/return/${record.isbn}`)
    ElMessage.success('归还成功，如有预约队列系统会自动通知下一位读者')
    await Promise.all([loadBorrowRecords(), loadReservations(), loadNotifications()])
  } catch (error) {
    if (error !== 'cancel') showError(error, '归还失败')
  }
}

async function handleRenew(record) {
  try {
    const response = await axios.post(`/user/renew/${record.isbn}`)
    ElMessage.success(`续借成功，新到期日为 ${response.data.data.dueDate}`)
    loadBorrowRecords()
  } catch (error) {
    showError(error, '续借失败')
  }
}

async function handleBorrow(book) {
  try {
    await axios.post(`/user/borrow/${book.isbn}`)
    ElMessage.success('借阅成功')
    await Promise.all([loadBorrowRecords(), loadWishList(), loadReservations()])
  } catch (error) {
    showError(error, '借阅失败')
  }
}

async function reserveBook(book) {
  try {
    const response = await axios.post(`/user/reservations/${book.isbn}`)
    ElMessage.success(`预约成功，当前排队第 ${response.data.data.queuePosition} 位`)
    loadReservations()
    activeTab.value = 'reservations'
  } catch (error) {
    showError(error, '预约失败')
  }
}

async function cancelReservation(reservation) {
  try {
    await ElMessageBox.confirm(`确认取消《${reservation.bookTitle}》的预约吗？`, '取消预约')
    await axios.delete(`/user/reservations/${reservation.id}`)
    ElMessage.success('预约已取消')
    loadReservations()
  } catch (error) {
    if (error !== 'cancel') showError(error, '取消预约失败')
  }
}

async function removeFromWishList(book) {
  try {
    await axios.delete(`/user/wish-list/${book.isbn}`)
    wishList.value = wishList.value.filter(item => item.isbn !== book.isbn)
    ElMessage.success('已移出愿望单')
  } catch (error) {
    showError(error, '移除失败')
  }
}

async function markRead(item) {
  if (item.read) return
  await axios.put(`/user/notifications/${item.id}/read`)
  item.read = true
  unreadCount.value = Math.max(0, unreadCount.value - 1)
}

async function markAllRead() {
  await axios.put('/user/notifications/read-all')
  notifications.value.forEach(item => { item.read = true })
  unreadCount.value = 0
}

function borrowStatusLabel(record) {
  if (record.status === 'RETURNED') return '已归还'
  if (record.daysOverdue > 0) return `逾期 ${record.daysOverdue} 天`
  return '借阅中'
}

function borrowStatusType(record) {
  if (record.status === 'RETURNED') return 'success'
  return record.daysOverdue > 0 ? 'danger' : 'warning'
}

function reservationLabel(status) {
  return { WAITING: '排队中', NOTIFIED: '待取书', COMPLETED: '已完成', CANCELLED: '已取消', EXPIRED: '已过期' }[status] || status
}

function reservationType(status) {
  return { WAITING: 'warning', NOTIFIED: 'success', COMPLETED: 'info', CANCELLED: 'info', EXPIRED: 'danger' }[status] || 'info'
}

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : ''
}

function dateTimeFormatter(row, column, value) {
  return formatDateTime(value)
}

function showError(error, fallback) {
  ElMessage.error(error.response?.data?.message || fallback)
}

function goToHome() {
  router.push('/')
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.profile-container { min-height: 100vh; display: flex; flex-direction: column; }
.profile-container > * { position: relative; z-index: 1; }
.header { color: white; padding: 14px 30px; display: flex; justify-content: space-between; align-items: center; }
.logo { font-size: 20px; font-weight: 700; }
.subtitle { margin-top: 3px; color: rgba(255,255,255,.55); font-size: 12px; }
.header-right, .row-actions { display: flex; gap: 10px; align-items: center; }
.content { width: min(1380px, calc(100% - 40px)); margin: 0 auto; padding: 24px 0 36px; display: grid; gap: 18px; }
.profile-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); padding: 18px 22px; }
.profile-summary > div { display: flex; flex-direction: column; gap: 5px; padding: 0 20px; border-right: 1px solid rgba(255,255,255,.08); }
.profile-summary > div:last-child { border-right: 0; }
.profile-summary span { color: rgba(255,255,255,.55); font-size: 12px; }
.profile-summary strong { color: white; font-size: 22px; }
.workspace { min-height: 560px; padding: 14px 20px 20px; overflow: hidden; }
.profile-tabs :deep(.el-tabs__item) { color: rgba(255,255,255,.62); }
.profile-tabs :deep(.el-tabs__item.is-active) { color: white; }
.profile-tabs :deep(.el-tabs__nav-wrap::after) { background: rgba(255,255,255,.08); }
.profile-table { width: 100%; --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: rgba(255,255,255,.05); --el-table-row-hover-bg-color: rgba(255,255,255,.06); --el-table-border-color: rgba(255,255,255,.08); color: white; }
.profile-table :deep(th), .profile-table :deep(td) { background: transparent !important; color: rgba(255,255,255,.82); }
.wish-list { max-height: 470px; overflow: auto; }
.wish-item { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 18px 10px; border-bottom: 1px solid rgba(255,255,255,.08); }
.wish-info h3 { margin: 0 0 6px; color: white; font-size: 16px; }
.wish-info p, .wish-info span { margin: 0 0 4px; color: rgba(255,255,255,.56); font-size: 13px; }
.notification-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 6px 4px 14px; color: rgba(255,255,255,.52); font-size: 13px; }
.notification-list { max-height: 430px; overflow: auto; }
.notification-item { width: 100%; min-height: 74px; display: grid; grid-template-columns: 10px minmax(0,1fr) auto; gap: 12px; align-items: start; padding: 14px 10px; color: rgba(255,255,255,.68); background: transparent; border: 0; border-bottom: 1px solid rgba(255,255,255,.08); text-align: left; cursor: pointer; }
.notification-item.unread { color: white; background: rgba(255,255,255,.025); }
.notice-dot { width: 7px; height: 7px; margin-top: 7px; border-radius: 50%; background: transparent; }
.unread .notice-dot { background: #70d4c5; }
.notice-content { display: flex; flex-direction: column; gap: 5px; line-height: 1.5; }
.notice-content strong { font-size: 14px; }
.notice-content span, .notification-item time { font-size: 12px; color: rgba(255,255,255,.48); }
@media (max-width: 760px) {
  .content { width: calc(100% - 20px); }
  .profile-summary { grid-template-columns: repeat(2, minmax(0,1fr)); gap: 18px 0; }
  .profile-summary > div:nth-child(2) { border-right: 0; }
  .wish-item { align-items: flex-start; flex-direction: column; }
  .notification-item { grid-template-columns: 10px minmax(0,1fr); }
  .notification-item time { grid-column: 2; }
}
</style>
