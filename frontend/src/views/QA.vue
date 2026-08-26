
<template>
  <div class="qa-container app-scene-root">
    <header class="header">
      <div class="logo">AgentAI 图书助手</div>
      <div class="header-right">
        <FlowerButton variant="nav" @click="goBack">{{ userStore.isStaff ? '管理后台' : '首页' }}</FlowerButton>
        <FlowerButton v-if="!userStore.isStaff" variant="nav" @click="goToProfile">{{ userStore.name }}</FlowerButton>
        <FlowerButton variant="nav" @click="handleLogout">退出登录</FlowerButton>
      </div>
    </header>
    
    <div class="qa-main">
      <div class="history-sidebar" :class="{ show: showHistory }">
        <div class="history-header">
          <h3>对话历史</h3>
          <FlowerButton variant="mini" @click="toggleHistory">关闭</FlowerButton>
        </div>
        <div class="history-list">
          <div 
            v-for="(item, index) in chatHistory" 
            :key="index"
            class="history-item"
            @click="loadHistoryItem(item)"
          >
            <div class="history-question">{{ item.userQuestion }}</div>
            <div class="history-time">{{ formatTime(item.createdAt) }}</div>
            <div class="history-type">{{ getTypeLabel(item.responseType) }}</div>
          </div>
          <div v-if="chatHistory.length === 0" class="no-history">暂无历史记录</div>
        </div>
        <div class="history-actions">
          <FlowerButton variant="action" @click="clearHistory">清空历史</FlowerButton>
        </div>
      </div>
      
      <div class="chat-container">
        <div class="chat-toolbar">
          <FlowerButton variant="wide" @click="toggleHistory" class="glass-btn">
            对话历史 ({{ chatHistory.length }})
          </FlowerButton>
          <FlowerButton variant="action" @click="startNewSession">新对话</FlowerButton>
          <span class="kb-tip">{{ agentStatus || '已连接图书馆 Agent' }}</span>
        </div>
        
        <div ref="chatHistoryRef" class="chat-history">
          <div v-if="messages.length === 0" class="welcome-tip">
            <p>你好！我是图书智能助手，你可以问我：</p>
            <ul>
              <li>图书馆一共有多少本书？</li>
              <li>《三体》放在哪个位置？</li>
              <li>推荐几本推理小说</li>
            </ul>
          </div>
          <div 
            v-for="(msg, index) in messages" 
            :key="index" 
            class="message"
            :class="msg.type"
          >
            <div class="avatar">{{ msg.type === 'user' ? '我' : 'AI' }}</div>
            <div class="content">
              <p v-if="msg.type === 'user'" class="question">{{ msg.question }}</p>
              <div v-if="msg.type === 'bot' && (msg.analysis || msg.content)" class="analysis-content">
                <pre>{{ msg.analysis || msg.content }}<span v-if="msg.streaming" class="cursor-blink">▌</span></pre>
              </div>
              <section v-if="msg.catalogProposal" class="catalog-proposal-section">
                <div class="catalog-proposal-heading">
                  <div>
                    <span class="action-label">馆藏管理方案</span>
                    <strong>请选择书目版本与处理方式</strong>
                  </div>
                  <small>方案有效期至 {{ formatDateTime(msg.catalogProposal.expiresAt) }}</small>
                </div>
                <div v-for="group in msg.catalogProposal.groups" :key="group.query" class="catalog-proposal-group">
                  <div class="catalog-query-row">
                    <h4>《{{ group.query }}》</h4>
                    <FlowerButton
                      v-if="group.canAddNewEdition"
                      variant="mini"
                      @click="openCatalogAction(msg, group, null, 'ADD_BOOK')"
                    >新增版本</FlowerButton>
                  </div>
                  <p v-if="group.candidates.length === 0" class="catalog-empty">馆藏中没有匹配版本，可选择新增书目。</p>
                  <article v-for="candidate in group.candidates" :key="candidate.isbn" class="catalog-candidate">
                    <div class="catalog-book-info">
                      <strong>{{ candidate.title }}</strong>
                      <span>{{ candidate.author }} · ISBN {{ candidate.isbn }}</span>
                      <span>馆藏 {{ candidate.totalCount }} · 已借 {{ candidate.borrowedCount }} · {{ candidate.location || '位置待定' }}</span>
                    </div>
                    <div class="catalog-operation-list">
                      <FlowerButton
                        v-for="operation in candidate.operations"
                        :key="operation"
                        variant="mini"
                        @click="openCatalogAction(msg, group, candidate, operation)"
                      >{{ catalogActionLabel(operation) }}</FlowerButton>
                    </div>
                  </article>
                </div>
              </section>
              <section v-if="msg.navigation?.type === 'OPEN_EBOOK'" class="reading-navigation-card">
                <div>
                  <span class="action-label">已核验电子书</span>
                  <strong>《{{ msg.navigation.bookTitle }}》第 {{ msg.navigation.chapterNumber }} 回</strong>
                  <small>{{ msg.navigation.message }}</small>
                </div>
                <FlowerButton variant="wide" @click="openReader(msg.navigation)">打开阅读</FlowerButton>
              </section>
              <div v-if="msg.pendingAction" class="agent-action-card">
                <div>
                  <span class="action-label">待确认操作</span>
                  <strong>{{ msg.pendingAction.summary }}</strong>
                  <small>确认有效期至 {{ formatDateTime(msg.pendingAction.expiresAt) }}</small>
                </div>
                <div v-if="msg.pendingAction.status === 'PENDING'" class="agent-action-buttons">
                  <FlowerButton variant="mini" @click="confirmAgentAction(msg)">确认执行</FlowerButton>
                  <FlowerButton variant="mini" @click="cancelAgentAction(msg)">取消</FlowerButton>
                </div>
                <el-tag v-else size="small" :type="msg.pendingAction.status === 'CONFIRMED' ? 'success' : 'info'">
                  {{ msg.pendingAction.status === 'CONFIRMED' ? '已执行' : '已取消' }}
                </el-tag>
              </div>
              <p v-if="msg.type === 'bot' && !msg.analysis && !msg.content && !msg.streaming" class="no-result">
                暂无回答，请稍后重试
              </p>
            </div>
          </div>
        </div>
        
        <div class="chat-input">
          <el-input 
            v-model="question" 
            placeholder="输入问题，如：图书馆有多少本书？《活着》在哪里？"
            @keyup.enter="sendQuestion"
            :disabled="isStreaming"
            class="glass-input galaxy-input"
          />
          <FlowerButton variant="action" @click="sendQuestion" class="glass-btn" :disabled="isStreaming">
            {{ isStreaming ? '生成中' : '发送' }}
          </FlowerButton>
        </div>
      </div>
      
      <div class="suggestions">
        <h3>快捷功能</h3>
        <div class="suggestion-list">
          <FlowerButton 
            v-for="suggestion in suggestions" 
            :key="suggestion.title"
            @click="quickAsk(suggestion)"
            variant="tab"
            class="suggestion-btn"
          >
            <span class="btn-icon">{{ suggestion.icon }}</span>
            <span class="btn-text">{{ suggestion.title }}</span>
          </FlowerButton>
        </div>
        
        <h3>热门书籍分析</h3>
        <div class="book-analysis-list">
          <div 
            v-for="book in hotBooks" 
            :key="book.title"
            class="analysis-item"
            @click="analyzeBook(book)"
          >
            <h4>{{ book.title }}</h4>
            <p>{{ book.author }}</p>
            <span class="analyze-btn">分析此书</span>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="catalogDialogVisible"
      :title="catalogDialogTitle"
      width="min(560px, 92vw)"
      class="glass-dialog"
      append-to-body
    >
      <el-form label-position="top" class="catalog-action-form">
        <template v-if="catalogForm.actionType === 'ADD_BOOK'">
          <div class="catalog-form-grid">
            <el-form-item label="ISBN" required><el-input v-model="catalogForm.isbn" maxlength="32" class="galaxy-input" /></el-form-item>
            <el-form-item label="书名" required><el-input v-model="catalogForm.title" maxlength="255" class="galaxy-input" /></el-form-item>
            <el-form-item label="作者" required><el-input v-model="catalogForm.author" maxlength="255" class="galaxy-input" /></el-form-item>
            <el-form-item label="出版社"><el-input v-model="catalogForm.publisher" maxlength="255" class="galaxy-input" /></el-form-item>
            <el-form-item label="分类"><el-input v-model="catalogForm.category" maxlength="100" class="galaxy-input" /></el-form-item>
            <el-form-item label="初始馆藏" required><el-input-number v-model="catalogForm.quantity" :min="1" :max="1000" class="galaxy-input" /></el-form-item>
            <el-form-item label="馆藏位置"><el-input v-model="catalogForm.location" maxlength="100" class="galaxy-input" /></el-form-item>
            <el-form-item label="关键词"><el-input v-model="catalogForm.keywords" maxlength="500" class="galaxy-input" /></el-form-item>
          </div>
          <el-form-item label="简介"><el-input v-model="catalogForm.description" type="textarea" :rows="3" maxlength="2000" show-word-limit class="galaxy-input" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="目标书目">
            <el-input :model-value="`${catalogSelection.candidate?.title || ''} · ${catalogSelection.candidate?.isbn || ''}`" disabled class="galaxy-input" />
          </el-form-item>
          <el-form-item label="调整数量" required>
            <el-input-number v-model="catalogForm.quantity" :min="1" :max="1000" class="galaxy-input" />
          </el-form-item>
          <el-form-item v-if="catalogForm.actionType === 'INCREASE_STOCK'" label="更新馆藏位置（可选）">
            <el-input v-model="catalogForm.location" maxlength="100" class="galaxy-input" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <div class="catalog-dialog-actions">
          <FlowerButton variant="action" @click="catalogDialogVisible = false">取消</FlowerButton>
          <FlowerButton class="catalog-confirm-button" variant="action" :disabled="preparingCatalogAction" @click="submitCatalogAction">
            {{ preparingCatalogAction ? '生成中' : '生成确认草案' }}
          </FlowerButton>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import axios from '../axios'
import { ElMessage } from 'element-plus'
import FlowerButton from '../components/FlowerButton.vue'

const router = useRouter()
const userStore = useUserStore()

const question = ref('')
const messages = ref([])
const showHistory = ref(false)
const chatHistory = ref([])
const isStreaming = ref(false)
const chatHistoryRef = ref(null)
const agentStatus = ref('')
const catalogDialogVisible = ref(false)
const preparingCatalogAction = ref(false)
const catalogSelection = ref({ message: null, group: null, candidate: null })
const catalogForm = ref(defaultCatalogForm())

const sessionStorageKey = `library-agent-session:${userStore.userId || 'anonymous'}`
const sessionId = ref(sessionStorage.getItem(sessionStorageKey) || createSessionId())
sessionStorage.setItem(sessionStorageKey, sessionId.value)

onMounted(() => {
  loadChatHistory()
})

async function loadChatHistory() {
  try {
    const response = await axios.get('/qa/history')
    if (response.data.success) {
      chatHistory.value = response.data.data
    }
  } catch (error) {
    console.error('加载历史记录失败:', error)
  }
}

function toggleHistory() {
  showHistory.value = !showHistory.value
}

function loadHistoryItem(item) {
  if (item.sessionId) {
    sessionId.value = item.sessionId
    sessionStorage.setItem(sessionStorageKey, sessionId.value)
  }
  const sessionItems = item.sessionId
    ? chatHistory.value.filter(history => history.sessionId === item.sessionId).slice().reverse()
    : [item]
  messages.value = sessionItems.flatMap(history => [
    { type: 'user', question: history.userQuestion },
    { type: 'bot', content: history.aiResponse, analysis: history.aiResponse, streaming: false }
  ])
  showHistory.value = false
  scrollToBottom()
}

function startNewSession() {
  sessionId.value = createSessionId()
  sessionStorage.setItem(sessionStorageKey, sessionId.value)
  messages.value = []
  agentStatus.value = ''
}

function createSessionId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  return `session_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

async function clearHistory() {
  if (!confirm('确定要清空所有历史记录吗？')) return
  try {
    await axios.delete('/qa/history')
    chatHistory.value = []
    showHistory.value = false
    startNewSession()
    ElMessage.success('历史记录已清空')
  } catch (error) {
    ElMessage.error('清空失败')
  }
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function formatDateTime(dateStr) {
  return dateStr ? new Date(dateStr).toLocaleString('zh-CN', { hour12: false }) : ''
}

function getTypeLabel(type) {
  const typeMap = {
    agent: 'Agent 对话',
    chat: '智能问答',
    book_analyze: '书籍分析',
    book_background: '背景分析',
    book_content: '内容解析',
    book_recommend: '书籍推荐',
    recommend: '偏好推荐'
  }
  return typeMap[type] || '其他'
}

function scrollToBottom() {
  nextTick(() => {
    if (chatHistoryRef.value) {
      chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight
    }
  })
}

async function sendQuestion() {
  if (!question.value.trim() || isStreaming.value) return

  const userQuestion = question.value.trim()
  question.value = ''

  messages.value.push({ type: 'user', question: userQuestion })
  const botIndex = messages.value.length
  messages.value.push({ type: 'bot', content: '', analysis: '', streaming: true })
  scrollToBottom()

  isStreaming.value = true
  agentStatus.value = '正在理解你的目标'
  try {
    const response = await axios.post('/qa/agent/run', {
      question: userQuestion,
      sessionId: sessionId.value
    }, {
      timeout: 120000
    })
    if (response.data.success && response.data.data?.answer) {
      sessionId.value = response.data.data.sessionId || sessionId.value
      sessionStorage.setItem(sessionStorageKey, sessionId.value)
      agentStatus.value = '正在生成回答'
      await typewriterOneByOne(botIndex, response.data.data.answer)
      messages.value[botIndex].pendingAction = response.data.data.pendingAction || null
      messages.value[botIndex].catalogProposal = response.data.data.catalogProposal || null
      messages.value[botIndex].navigation = response.data.data.navigation || null
      if (messages.value[botIndex].navigation?.type === 'OPEN_EBOOK') {
        ElMessage.success(`正在打开《${messages.value[botIndex].navigation.bookTitle}》`)
        await new Promise(resolve => setTimeout(resolve, 350))
        openReader(messages.value[botIndex].navigation)
      }
    } else {
      throw new Error(response.data.message || 'AI 回答失败')
    }
    loadChatHistory()
  } catch (error) {
    messages.value[botIndex].content = `抱歉，${error.response?.data?.message || error.message || 'AI 服务暂时不可用'}`
    messages.value[botIndex].analysis = messages.value[botIndex].content
    ElMessage.error('AI 回答失败')
  } finally {
    isStreaming.value = false
    agentStatus.value = ''
    messages.value[botIndex].streaming = false
    scrollToBottom()
  }
}

async function confirmAgentAction(message) {
  try {
    const response = await axios.post(`/qa/actions/${message.pendingAction.token}/confirm`)
    message.pendingAction = response.data.data
    applyConfirmedCatalogAction(message)
    ElMessage.success('操作已确认并执行')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作执行失败')
  }
}

function defaultCatalogForm() {
  return {
    query: '', actionType: '', isbn: '', title: '', author: '', publisher: '',
    category: '', quantity: 1, location: '', keywords: '', description: ''
  }
}

function catalogActionLabel(action) {
  return {
    ADD_BOOK: '新增版本',
    INCREASE_STOCK: '增加馆藏',
    REDUCE_STOCK: '减少馆藏',
    DISABLE_BOOK: '停止借阅',
    ENABLE_BOOK: '恢复借阅',
    DELETE_BOOK: '彻底删除'
  }[action] || action
}

const catalogDialogTitle = ref('馆藏操作')

function openCatalogAction(message, group, candidate, actionType) {
  catalogSelection.value = { message, group, candidate }
  catalogForm.value = {
    ...defaultCatalogForm(),
    query: group.query,
    actionType,
    isbn: candidate?.isbn || '',
    title: candidate?.title || group.query,
    author: candidate?.author || '',
    publisher: candidate?.publisher || '',
    category: candidate?.category || '',
    location: candidate?.location || ''
  }
  catalogDialogTitle.value = `${catalogActionLabel(actionType)} · 《${candidate?.title || group.query}》`
  if (['ADD_BOOK', 'INCREASE_STOCK', 'REDUCE_STOCK'].includes(actionType)) {
    catalogDialogVisible.value = true
  } else {
    prepareCatalogAction()
  }
}

async function submitCatalogAction() {
  if (catalogForm.value.actionType === 'ADD_BOOK'
      && (!catalogForm.value.isbn.trim() || !catalogForm.value.title.trim() || !catalogForm.value.author.trim())) {
    ElMessage.warning('请填写 ISBN、书名和作者')
    return
  }
  await prepareCatalogAction()
}

async function prepareCatalogAction() {
  const message = catalogSelection.value.message
  const proposal = message?.catalogProposal
  if (!proposal) return
  preparingCatalogAction.value = true
  try {
    const response = await axios.post(`/qa/catalog/proposals/${proposal.token}/actions`, catalogForm.value)
    message.pendingAction = response.data.data
    catalogDialogVisible.value = false
    ElMessage.success('确认草案已生成，请核对后执行')
    scrollToBottom()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '方案生成失败')
  } finally {
    preparingCatalogAction.value = false
  }
}

function applyConfirmedCatalogAction(message) {
  const action = message.pendingAction
  const proposal = message.catalogProposal
  if (!proposal || !action) return
  for (const group of proposal.groups) {
    const index = group.candidates.findIndex(item => item.isbn === action.isbn)
    if (action.actionType === 'ADD_BOOK') {
      if (group.query === action.details?.query && index < 0) {
        group.candidates.unshift({
          isbn: action.isbn,
          title: action.bookTitle,
          author: action.details?.author || '',
          publisher: action.details?.publisher || '',
          category: action.details?.category || '',
          totalCount: action.details?.quantity || 0,
          borrowedCount: 0,
          location: action.details?.location || '',
          borrowable: true,
          operations: ['INCREASE_STOCK', 'REDUCE_STOCK', 'DISABLE_BOOK'].concat(userStore.isAdmin ? ['DELETE_BOOK'] : [])
        })
      }
      continue
    }
    if (index < 0) continue
    const candidate = group.candidates[index]
    const quantity = Number(action.details?.quantity || 0)
    if (action.actionType === 'INCREASE_STOCK') candidate.totalCount += quantity
    if (action.actionType === 'REDUCE_STOCK') candidate.totalCount -= quantity
    if (action.actionType === 'DISABLE_BOOK') {
      candidate.borrowable = false
      candidate.operations = candidate.operations.map(item => item === 'DISABLE_BOOK' ? 'ENABLE_BOOK' : item)
    }
    if (action.actionType === 'ENABLE_BOOK') {
      candidate.borrowable = true
      candidate.operations = candidate.operations.map(item => item === 'ENABLE_BOOK' ? 'DISABLE_BOOK' : item)
    }
    if (action.actionType === 'DELETE_BOOK') group.candidates.splice(index, 1)
  }
}

function openReader(navigation) {
  if (!navigation || navigation.type !== 'OPEN_EBOOK') return
  const chapter = Number.parseInt(navigation.chapterNumber, 10)
  if (!navigation.isbn || !Number.isInteger(chapter) || chapter < 1 || chapter > navigation.chapterCount) {
    ElMessage.error('Agent 返回的阅读位置无效')
    return
  }
  router.push({
    path: `/reader/${encodeURIComponent(navigation.isbn)}`,
    query: { chapter: String(chapter) }
  })
}

async function cancelAgentAction(message) {
  try {
    await axios.delete(`/qa/actions/${message.pendingAction.token}`)
    message.pendingAction.status = 'CANCELLED'
    ElMessage.success('操作草案已取消')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '取消失败')
  }
}

/** 逐字输出，append=true 时在已有内容后追加 */
function typewriterOneByOne(botIndex, text, append = false) {
  return new Promise(resolve => {
    if (!append) {
      messages.value[botIndex].content = ''
      messages.value[botIndex].analysis = ''
    }
    let i = 0
    const next = () => {
      if (i >= text.length) {
        resolve()
        return
      }
      messages.value[botIndex].content += text.charAt(i)
      messages.value[botIndex].analysis = messages.value[botIndex].content
      i++
      scrollToBottom()
      setTimeout(next, 36)
    }
    next()
  })
}

async function quickAsk(suggestion) {
  const presets = {
    chat: '图书馆一共有多少本书？',
    background: '请介绍《三体》的创作背景',
    content: '请解析《白夜行》的主要内容',
    recommend: '请推荐几本推理小说',
    location: '科幻小说都放在哪个区域？',
    reading: '继续阅读《红楼梦》',
    catalog: '请为《Java核心技术》和《深入理解Java虚拟机》生成馆藏添加或调整方案'
  }
  question.value = presets[suggestion.action] || suggestion.title
  await sendQuestion()
}

async function analyzeBook(book) {
  if (isStreaming.value) return
  question.value = `请分析馆藏图书《${book.title}》（作者：${book.author}），并推荐几本馆内同类图书。`
  await sendQuestion()
}

function goBack() {
  router.push(userStore.isStaff ? '/admin' : '/')
}

function goToProfile() {
  router.push('/profile')
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

const suggestions = [
  { title: '馆藏统计', icon: '📊', action: 'chat' },
  { title: '区域查询', icon: '📍', action: 'location' },
  { title: '背景分析', icon: '📚', action: 'background' },
  { title: '内容解析', icon: '📖', action: 'content' },
  { title: '书籍推荐', icon: '🔍', action: 'recommend' },
  ...(!userStore.isStaff ? [{ title: '继续阅读', icon: '阅', action: 'reading' }] : []),
  ...(userStore.isStaff ? [{ title: '馆藏维护', icon: '⚙', action: 'catalog' }] : [])
]

const hotBooks = [
  { title: '三体', author: '刘慈欣' },
  { title: '白夜行', author: '东野圭吾' },
  { title: '活着', author: '余华' },
  { title: '百年孤独', author: '加西亚·马尔克斯' }
]
</script>

<style scoped>
.qa-container {
  height: 100vh;
  height: 100dvh;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  isolation: isolate;
  background:
    linear-gradient(90deg, rgba(3, 18, 24, 0.74), rgba(3, 18, 24, 0.34) 48%, rgba(3, 18, 24, 0.62)),
    url('../assets/app-background.png') center / cover fixed no-repeat;
}

.qa-container::before {
  background: linear-gradient(180deg, rgba(2, 14, 19, 0.16), rgba(2, 14, 19, 0.54));
}

.header {
  color: white;
  padding: 15px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
  min-height: 76px;
  padding: 0 clamp(20px, 3vw, 48px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
  background: transparent;
}

.logo {
  font-size: 22px;
  font-weight: bold;
  font-family: var(--font-display);
  font-weight: 500;
  text-shadow: 0 2px 18px rgba(0, 0, 0, 0.56);
}

.header-right {
  display: flex;
  gap: 20px;
  align-items: center;
}

.qa-main {
  flex: 1;
  min-height: 0;
  padding: clamp(12px, 2.5vh, 24px) 30px;
  display: flex;
  gap: 24px;
  overflow: hidden;
  position: relative;
  z-index: 1;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.history-sidebar {
  width: 0;
  overflow: hidden;
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  min-height: 0;
  height: 100%;
  border-right: 1px solid rgba(255, 255, 255, 0.16);
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.history-sidebar.show {
  width: 300px;
}

.history-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-header h3 {
  margin: 0;
  color: white;
  font-size: 16px;
}

.history-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 10px;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.32) transparent;
}

.history-list::-webkit-scrollbar,
.chat-history::-webkit-scrollbar {
  width: 5px;
}

.history-list::-webkit-scrollbar-track,
.chat-history::-webkit-scrollbar-track {
  background: transparent;
}

.history-list::-webkit-scrollbar-thumb,
.chat-history::-webkit-scrollbar-thumb {
  border-radius: 0;
  background: rgba(255, 255, 255, 0.3);
}

.history-item {
  padding: 14px 8px;
  margin: 0 12px;
  cursor: pointer;
  background: transparent !important;
  border: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 0 !important;
  transition: padding-left 0.2s ease, background 0.2s ease;
}

.history-item:hover {
  padding-left: 14px;
  background: rgba(0, 0, 0, 0.12) !important;
}

.history-question {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  margin-bottom: 6px;
}

.history-time {
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
}

.history-type {
  display: inline-block;
  margin-top: 6px;
  padding: 2px 0;
  background: transparent;
  border-radius: 0;
  font-size: 11px;
  color: white;
}

.no-history {
  text-align: center;
  padding: 40px 20px;
  color: rgba(255, 255, 255, 0.4);
}

.history-actions {
  padding: 12px;
  text-align: center;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0 !important;
  height: 100%;
  overflow: hidden;
  border: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.chat-toolbar {
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
  flex: 0 0 auto;
}

.kb-tip {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

.chat-history {
  flex: 1;
  min-height: 0;
  padding: 20px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.32) transparent;
}

.welcome-tip {
  color: rgba(255, 255, 255, 0.6);
  padding: 20px;
  line-height: 1.8;
}

.welcome-tip ul {
  margin: 10px 0 0 20px;
}

.message {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;
}

.message.user {
  flex-direction: row-reverse;
}

.message.user .content {
  padding-right: 16px;
  border-right: 2px solid rgba(255, 255, 255, 0.58) !important;
  background: transparent !important;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 0;
  background: transparent;
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
  font-weight: bold;
  font-size: 12px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.65);
}

.content {
  max-width: 75%;
  padding: 4px 0 4px 16px;
  background: transparent !important;
  border: 0 !important;
  border-left: 2px solid rgba(118, 209, 218, 0.68) !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.question {
  margin: 0;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.9);
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.7);
}

.analysis-content pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: 'Microsoft YaHei', sans-serif;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.88);
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.7);
}

.agent-action-card {
  margin-top: 14px;
  padding-top: 13px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.reading-navigation-card {
  margin-top: 14px;
  padding-top: 13px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.reading-navigation-card > div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reading-navigation-card strong {
  color: white;
  font-size: 15px;
}

.reading-navigation-card small {
  color: rgba(255, 255, 255, 0.55);
  font-size: 12px;
  line-height: 1.5;
}

.agent-action-card > div:first-child {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.action-label,
.agent-action-card small {
  color: rgba(255, 255, 255, 0.48);
  font-size: 11px;
}

.agent-action-card strong {
  color: white;
  font-size: 14px;
}

.agent-action-buttons {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.catalog-proposal-section {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.catalog-proposal-heading,
.catalog-query-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.catalog-proposal-heading > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.catalog-proposal-heading strong,
.catalog-query-row h4,
.catalog-candidate strong {
  color: white;
}

.catalog-proposal-heading small,
.catalog-book-info span,
.catalog-empty {
  color: rgba(255, 255, 255, 0.55);
  font-size: 12px;
}

.catalog-proposal-group {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}

.catalog-query-row h4 {
  margin: 0;
  font-size: 15px;
}

.catalog-empty {
  margin: 10px 0 0;
}

.catalog-candidate {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto;
  align-items: center;
  gap: 14px;
  margin-top: 10px;
  padding: 10px 12px;
  border: 0;
  border-left: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 0;
  background: transparent;
}

.catalog-book-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.catalog-book-info strong,
.catalog-book-info span {
  overflow-wrap: anywhere;
}

.catalog-operation-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 4px;
  max-width: 260px;
}

.catalog-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.catalog-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.catalog-confirm-button {
  --flower-width: 10em;
  --flower-wrapper-width: 8.2em;
}

:global(.el-dialog.glass-dialog) {
  color: #10272a;
  background: rgba(253, 241, 225, 0.98);
  border: 1px solid rgba(16, 39, 42, 0.18);
  box-shadow: 0 24px 72px rgba(0, 14, 18, 0.38);
}

:global(.el-dialog.glass-dialog .el-dialog__title),
:global(.el-dialog.glass-dialog .el-form-item__label),
:global(.el-dialog.glass-dialog .el-dialog__close) {
  color: #10272a;
}

.cursor-blink {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.no-result {
  color: rgba(255, 255, 255, 0.4);
  margin: 0;
}

.chat-input {
  padding: 16px 20px;
  display: flex;
  gap: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
  flex: 0 0 auto;
  background: transparent;
}

.suggestions {
  width: 280px;
  height: 100%;
  min-height: 0;
  padding: 16px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
  border-left: 1px solid rgba(255, 255, 255, 0.16);
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.suggestions h3 {
  margin: 0;
  color: white;
  font-size: 15px;
}

.suggestion-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.suggestion-btn {
  padding: 0;
  border: 0;
  background: transparent;
}

.btn-icon { font-size: 22px; }
.btn-text { font-size: 12px; }

.book-analysis-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.analysis-item {
  padding: 12px 4px;
  cursor: pointer;
  background: transparent !important;
  border: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 0 !important;
  transition: padding-left 0.2s ease;
}

.analysis-item:hover {
  padding-left: 10px;
}

.analysis-item h4 {
  margin: 0 0 4px;
  color: white;
  font-size: 14px;
}

.analysis-item p {
  margin: 0 0 8px;
  color: rgba(255, 255, 255, 0.55);
  font-size: 12px;
}

.analyze-btn {
  font-size: 12px;
  color: rgba(135, 224, 231, 0.92);
}

@media (max-height: 800px) {
  .suggestion-btn {
    --flower-height: 2.8em;
  }

  .book-analysis-list {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .analysis-item {
    min-width: 0;
    padding: 10px;
  }

  .analysis-item p {
    margin-bottom: 4px;
  }
}

@media (max-width: 1100px) {
  .qa-main {
    gap: 16px;
    padding-inline: 16px;
  }

  .suggestions {
    display: none;
  }
}

@media (max-width: 700px) {
  .qa-main {
    padding: 10px;
  }

  .history-sidebar {
    position: absolute;
    inset: 10px;
    width: auto;
    z-index: 5;
    pointer-events: none;
    opacity: 0;
    transform: translateX(-16px);
    transition: opacity 0.2s ease, transform 0.2s ease;
    border-right: 0;
    background:
      linear-gradient(rgba(3, 18, 24, 0.78), rgba(3, 18, 24, 0.78)),
      url('../assets/app-background.png') center / cover no-repeat;
  }

  .history-sidebar.show {
    width: auto;
    pointer-events: auto;
    opacity: 1;
    transform: translateX(0);
  }

  .chat-toolbar {
    gap: 8px;
    padding: 8px 12px;
  }

  .kb-tip {
    display: none;
  }

  .chat-history {
    padding: 14px 12px;
  }

  .content {
    max-width: 86%;
  }

  .catalog-candidate {
    grid-template-columns: 1fr;
  }

  .catalog-operation-list {
    justify-content: flex-start;
    max-width: none;
  }

  .catalog-form-grid {
    grid-template-columns: 1fr;
  }

  .reading-navigation-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .chat-input {
    padding: 10px 12px;
  }
}
</style>
