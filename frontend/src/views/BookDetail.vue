<template>
  <div class="book-detail-container app-scene-root">
    <header class="header">
      <div class="logo">智能图书管理系统</div>
      <div class="header-right">
        <FlowerButton variant="nav" @click="router.push('/')">返回藏馆</FlowerButton>
        <FlowerButton variant="nav" @click="router.push('/qa')">Agent 助手</FlowerButton>
        <FlowerButton variant="nav" @click="router.push('/profile')">{{ userStore.name }}</FlowerButton>
        <FlowerButton variant="nav" @click="handleLogout">退出登录</FlowerButton>
      </div>
    </header>

    <main class="content" v-if="book">
      <section class="book-detail">
        <div class="title-row">
          <div>
            <p class="eyebrow">馆藏详情</p>
            <h1>{{ book.title }}</h1>
          </div>
          <span v-if="ebook" class="rights-badge">权属已核验</span>
        </div>

        <div class="book-meta">
          <span>作者：{{ book.author }}</span>
          <span>出版社：{{ book.publisher }}</span>
          <span>分类：{{ book.category }}</span>
        </div>
        <p class="description">{{ book.description }}</p>
        <div class="book-stats">
          <span>ISBN：{{ book.isbn }}</span>
          <span>馆藏位置：{{ book.location }}</span>
          <span>可借数量：{{ book.availableCount }}/{{ book.totalCount }}</span>
          <span>借阅状态：{{ book.borrowable === false ? '暂停借阅' : '开放借阅' }}</span>
        </div>

        <div class="actions">
          <FlowerButton @click="handleBorrow" :disabled="!canBorrow" variant="wide">
            {{ borrowText }}
          </FlowerButton>
          <FlowerButton variant="wide" @click="addToWishList">加入愿望单</FlowerButton>
          <FlowerButton
            v-if="book.borrowable !== false && book.availableCount === 0"
            variant="wide"
            @click="reserveBook"
          >排队预约</FlowerButton>
          <FlowerButton v-if="ebook" variant="wide" @click="startReading">
            {{ ebook.progress?.updatedAt ? `继续阅读 · 第${ebook.progress.chapterNumber}回` : '开始阅读' }}
          </FlowerButton>
        </div>

        <section v-if="ebook" class="digital-rights" aria-label="电子资源版权信息">
          <div class="rights-heading">
            <span class="rights-dot" aria-hidden="true"></span>
            <strong>可阅读古典原文</strong>
            <span>{{ ebook.chapterCount }} 回</span>
          </div>
          <p>{{ ebook.contentNotice }}</p>
          <dl>
            <div><dt>文本来源</dt><dd><a :href="ebook.sourceUrl" target="_blank" rel="noopener noreferrer">{{ ebook.sourceName }}</a></dd></div>
            <div><dt>许可协议</dt><dd><a :href="ebook.licenseUrl" target="_blank" rel="noopener noreferrer">{{ ebook.licenseName }}</a></dd></div>
            <div><dt>适用说明</dt><dd>{{ ebook.jurisdiction }}</dd></div>
            <div><dt>核验时间</dt><dd>{{ formatDate(ebook.verifiedAt) }}</dd></div>
          </dl>
        </section>
        <p v-else-if="ebookChecked" class="no-digital-copy">
          此书仅提供馆藏信息。未取得可证明的全文授权，因此不提供在线正文。
        </p>
      </section>

      <section class="recommendations" v-if="similarBooks.length > 0">
        <h2>你可能也喜欢</h2>
        <div class="recommend-list">
          <button
            v-for="similar in similarBooks"
            :key="similar.isbn"
            class="recommend-card"
            type="button"
            @click="goToBookDetail(similar.isbn)"
          >
            <strong>{{ similar.title }}</strong>
            <span>作者：{{ similar.author }}</span>
            <span>分类：{{ similar.category }}</span>
          </button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../axios'
import FlowerButton from '../components/FlowerButton.vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const book = ref(null)
const ebook = ref(null)
const ebookChecked = ref(false)
const similarBooks = ref([])

const canBorrow = computed(() => book.value?.borrowable !== false && book.value?.availableCount > 0)
const borrowText = computed(() => {
  if (!book.value) return '借阅此书'
  if (book.value.borrowable === false) return '暂停借阅'
  if (book.value.availableCount === 0) return '已借完'
  return '借阅此书'
})

watch(() => route.params.isbn, loadPage, { immediate: true })

async function loadPage() {
  book.value = null
  ebook.value = null
  ebookChecked.value = false
  await Promise.all([loadBook(), loadRecommendations(), loadEbook()])
}

async function loadBook() {
  try {
    const response = await axios.get(`/books/${route.params.isbn}`)
    book.value = response.data.data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '图书信息加载失败')
  }
}

async function loadEbook() {
  try {
    const response = await axios.get(`/books/${route.params.isbn}/ebook`)
    ebook.value = response.data.data
  } catch (error) {
    if (error.response?.status !== 404) {
      ElMessage.error(error.response?.data?.message || '电子资源信息加载失败')
    }
  } finally {
    ebookChecked.value = true
  }
}

async function loadRecommendations() {
  try {
    const response = await axios.get(`/user/books/${route.params.isbn}/similar`)
    similarBooks.value = response.data.data || []
  } catch {
    similarBooks.value = []
  }
}

async function handleBorrow() {
  try {
    const response = await axios.post(`/user/borrow/${route.params.isbn}`)
    ElMessage.success(response.data.message || '借阅成功')
    book.value.availableCount--
    book.value.borrowedCount++
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '借阅失败')
  }
}

async function addToWishList() {
  try {
    await axios.post(`/user/wish-list/${route.params.isbn}`)
    ElMessage.success('已添加到愿望单')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '添加失败')
  }
}

async function reserveBook() {
  try {
    const response = await axios.post(`/user/reservations/${route.params.isbn}`)
    ElMessage.success(`预约成功，当前排队第 ${response.data.data.queuePosition} 位`)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '预约失败')
  }
}

function startReading() {
  router.push(`/reader/${route.params.isbn}`)
}

function goToBookDetail(isbn) {
  router.push(`/book/${isbn}`)
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString('zh-CN') : '未记录'
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.book-detail-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  isolation: isolate;
  background:
    linear-gradient(90deg, rgba(3,18,24,.72), rgba(3,18,24,.3) 52%, rgba(3,18,24,.62)),
    url('../assets/app-background.png') center / cover fixed no-repeat;
}
.book-detail-container::before { background: linear-gradient(180deg, rgba(2,14,19,.15), rgba(2,14,19,.58)); }
.header, .content { position: relative; z-index: 1; }
.header {
  min-height: 76px;
  color: white;
  padding: 0 clamp(20px, 3vw, 48px);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255,255,255,.14);
  background: transparent;
}
.logo { font-family: var(--font-display); font-size: 22px; font-weight: 500; text-shadow: 0 2px 18px rgba(0,0,0,.56); }
.header-right { display: flex; gap: 10px; align-items: center; }
.content { width: min(1180px, 100%); flex: 1; margin: 0 auto; padding: 30px; }
.book-detail {
  padding: 40px;
  margin-bottom: 30px;
  border: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}
.title-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.eyebrow { margin-bottom: 8px; color: #93e0ee; font-size: 12px; font-weight: 700; text-transform: uppercase; text-shadow: 0 2px 12px rgba(0,0,0,.65); }
.book-detail h1 { margin-bottom: 20px; color: white; font-size: 32px; text-shadow: 0 3px 24px rgba(0,0,0,.68); }
.rights-badge { flex: 0 0 auto; padding: 7px 0; border: 0; border-bottom: 1px solid rgba(147,224,238,.62); border-radius: 0; color: #c7f4f6; background: transparent; font-size: 13px; }
.book-meta, .book-stats { display: flex; flex-wrap: wrap; gap: 12px 30px; margin-bottom: 20px; color: rgba(255,255,255,.7); font-size: 14px; }
.description { max-width: 850px; margin-bottom: 20px; color: rgba(255,255,255,.86); font-size: 15px; line-height: 1.8; text-shadow: 0 2px 12px rgba(0,0,0,.68); }
.actions { display: flex; flex-wrap: wrap; align-items: center; gap: 4px; }
.digital-rights { margin-top: 26px; padding-top: 22px; border-top: 1px solid rgba(255,255,255,.13); color: rgba(255,255,255,.76); }
.rights-heading { display: flex; align-items: center; gap: 9px; margin-bottom: 10px; color: white; }
.rights-heading span:last-child { margin-left: auto; color: rgba(255,255,255,.55); font-size: 13px; }
.rights-dot { width: 8px; height: 8px; border-radius: 50%; background: #76d7c4; box-shadow: 0 0 0 4px rgba(118,215,196,.12); }
.digital-rights > p { margin-bottom: 16px; font-size: 13px; line-height: 1.7; }
.digital-rights dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 28px; }
.digital-rights dl div { display: grid; grid-template-columns: 72px 1fr; gap: 8px; font-size: 13px; }
.digital-rights dt { color: rgba(255,255,255,.46); }
.digital-rights a { color: #a9e4ee; text-underline-offset: 3px; }
.no-digital-copy { margin-top: 24px; padding: 14px 16px; border-left: 2px solid rgba(255,255,255,.32); color: rgba(255,255,255,.62); background: transparent; font-size: 13px; line-height: 1.7; }
.recommendations {
  padding: 30px;
  border: 0;
  border-top: 1px solid rgba(255,255,255,.18);
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}
.recommendations h2 { margin-bottom: 20px; color: white; font-size: 20px; }
.recommend-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 14px; }
.book-detail-container .recommend-card { min-height: 106px; padding: 18px; border: 0; border-left: 1px solid rgba(147,224,238,.42); border-radius: 0 !important; color: white; background: transparent !important; cursor: pointer; text-align: left; transition: transform .2s ease, border-color .2s ease; }
.book-detail-container .recommend-card:hover { transform: translateX(5px); border-left-color: #93e0ee; background: transparent !important; }
.recommend-card strong, .recommend-card span { display: block; }
.recommend-card strong { margin-bottom: 10px; font-size: 16px; }
.recommend-card span { margin-top: 5px; color: rgba(255,255,255,.6); font-size: 13px; }
@media (max-width: 760px) {
  .header { min-height: 72px; padding: 10px 14px; align-items: flex-start; }
  .logo { padding-top: 12px; font-size: 16px; }
  .header-right { max-width: 230px; flex-wrap: wrap; justify-content: flex-end; gap: 0; }
  .content { padding: 16px; }
  .book-detail { padding: 24px 20px; }
  .book-detail h1 { font-size: 27px; }
  .title-row { display: block; }
  .rights-badge { display: inline-block; margin-bottom: 18px; }
  .digital-rights dl { grid-template-columns: 1fr; }
  .recommend-list { grid-template-columns: 1fr; }
}
</style>
