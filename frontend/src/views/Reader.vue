<template>
  <div class="reader-shell" :data-theme="theme">
    <header class="reader-toolbar">
      <div class="reader-identity">
        <FlowerButton variant="icon" title="返回图书详情" @click="router.push(`/book/${isbn}`)">
          <el-icon><ArrowLeft /></el-icon>
        </FlowerButton>
        <div>
          <p>{{ resource?.author || '古典原文' }}</p>
          <h1>{{ resource?.title || '正文加载中' }}</h1>
        </div>
      </div>

      <div class="reader-tools" aria-label="阅读设置">
        <el-tooltip content="减小字号" placement="bottom">
          <FlowerButton variant="icon" :disabled="fontSize <= 16" @click="fontSize--">
            <el-icon><Minus /></el-icon>
          </FlowerButton>
        </el-tooltip>
        <span class="font-value">{{ fontSize }}</span>
        <el-tooltip content="增大字号" placement="bottom">
          <FlowerButton variant="icon" :disabled="fontSize >= 28" @click="fontSize++">
            <el-icon><Plus /></el-icon>
          </FlowerButton>
        </el-tooltip>
        <div class="theme-switch" role="group" aria-label="阅读主题">
          <button type="button" :class="{ active: theme === 'paper' }" title="纸张主题" @click="theme = 'paper'"><Sunny /></button>
          <button type="button" :class="{ active: theme === 'night' }" title="夜间主题" @click="theme = 'night'"><Moon /></button>
        </div>
      </div>
    </header>

    <div v-if="resource" class="mobile-chapter-picker">
      <label for="mobile-chapter">章节</label>
      <select id="mobile-chapter" v-model.number="currentChapter" @change="selectChapter(currentChapter)">
        <option v-for="number in resource.chapterCount" :key="number" :value="number">第 {{ number }} 回</option>
      </select>
    </div>

    <main class="reader-layout">
      <aside v-if="resource" class="chapter-sidebar" aria-label="章节目录">
        <div class="sidebar-heading">
          <span>目录</span>
          <small>{{ resource.chapterCount }} 回</small>
        </div>
        <nav>
          <button
            v-for="number in resource.chapterCount"
            :key="number"
            type="button"
            :class="{ active: number === currentChapter }"
            @click="selectChapter(number)"
          >
            <span>第 {{ number }} 回</span>
            <small v-if="number === resource.progress.chapterNumber && resource.progress.updatedAt">上次</small>
          </button>
        </nav>
      </aside>

      <section ref="readingPane" class="reading-pane" @scroll.passive="handleScroll">
        <div v-if="loading" class="reader-state">
          <span class="loading-line"></span>
          <p>正在从已核验来源载入古典原文</p>
        </div>
        <div v-else-if="errorMessage" class="reader-state error-state">
          <h2>本章暂时无法显示</h2>
          <p>{{ errorMessage }}</p>
          <FlowerButton variant="wide" @click="loadChapter(currentChapter, false)">重新加载</FlowerButton>
        </div>
        <article v-else-if="chapter" class="reading-paper" :style="{ '--reader-font-size': `${fontSize}px` }">
          <div class="chapter-kicker">{{ resource.title }} · 第 {{ chapter.chapterNumber }} 回</div>
          <h2>{{ chapter.chapterTitle }}</h2>
          <div class="chapter-content" v-html="chapter.contentHtml"></div>

          <footer class="source-footer">
            <strong>来源与许可</strong>
            <p>{{ chapter.attribution }}。{{ chapter.modificationNotice }}</p>
            <div>
              <a :href="chapter.sourcePageUrl" target="_blank" rel="noopener noreferrer">查看中文维基文库原始章节</a>
              <a :href="chapter.licenseUrl" target="_blank" rel="noopener noreferrer">{{ chapter.licenseName }}</a>
            </div>
          </footer>

          <div class="chapter-navigation">
            <FlowerButton variant="wide" :disabled="currentChapter <= 1" @click="selectChapter(currentChapter - 1)">上一回</FlowerButton>
            <span>{{ currentChapter }} / {{ resource.chapterCount }}</span>
            <FlowerButton variant="wide" :disabled="currentChapter >= resource.chapterCount" @click="selectChapter(currentChapter + 1)">下一回</FlowerButton>
          </div>
        </article>
      </section>
    </main>

    <footer v-if="resource" class="reader-statusbar">
      <div><span class="verified-mark"></span> 权属已核验 · 仅古典原文</div>
      <div>{{ resource.sourceName }} · {{ resource.licenseName }}</div>
      <div>第 {{ currentChapter }} 回 · {{ scrollPercent }}%</div>
    </footer>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Minus, Moon, Plus, Sunny } from '@element-plus/icons-vue'
import axios from '../axios'
import FlowerButton from '../components/FlowerButton.vue'

const route = useRoute()
const router = useRouter()
const isbn = route.params.isbn
const resource = ref(null)
const chapter = ref(null)
const currentChapter = ref(1)
const scrollPercent = ref(0)
const fontSize = ref(20)
const theme = ref('paper')
const loading = ref(true)
const errorMessage = ref('')
const readingPane = ref(null)
let saveTimer = null

onMounted(loadReader)
onBeforeUnmount(() => {
  clearTimeout(saveTimer)
  if (resource.value) saveProgress()
})

async function loadReader() {
  try {
    const response = await axios.get(`/books/${isbn}/ebook`)
    resource.value = response.data.data
    const requestedChapter = Number.parseInt(Array.isArray(route.query.chapter)
      ? route.query.chapter[0]
      : route.query.chapter, 10)
    const hasValidRequestedChapter = Number.isInteger(requestedChapter)
      && requestedChapter >= 1
      && requestedChapter <= resource.value.chapterCount
    currentChapter.value = hasValidRequestedChapter
      ? requestedChapter
      : (resource.value.progress?.chapterNumber || 1)
    scrollPercent.value = !hasValidRequestedChapter
      || currentChapter.value === resource.value.progress?.chapterNumber
      ? (resource.value.progress?.scrollPercent || 0)
      : 0
    await loadChapter(currentChapter.value, true)
  } catch (error) {
    loading.value = false
    errorMessage.value = error.response?.data?.message || '电子资源信息加载失败'
  }
}

async function loadChapter(number, restoreProgress) {
  loading.value = true
  errorMessage.value = ''
  chapter.value = null
  try {
    const response = await axios.get(`/books/${isbn}/ebook/chapters/${number}`)
    chapter.value = response.data.data
    await nextTick()
    const pane = readingPane.value
    if (pane) {
      const percent = restoreProgress ? scrollPercent.value : 0
      pane.scrollTop = Math.round((pane.scrollHeight - pane.clientHeight) * percent / 100)
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '正版文本来源暂时不可用，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function selectChapter(number) {
  if (!resource.value || number < 1 || number > resource.value.chapterCount) return
  currentChapter.value = number
  scrollPercent.value = 0
  await loadChapter(number, false)
  scheduleSave()
}

function handleScroll() {
  const pane = readingPane.value
  if (!pane || !chapter.value) return
  const maximum = pane.scrollHeight - pane.clientHeight
  scrollPercent.value = maximum > 0 ? Math.min(100, Math.round(pane.scrollTop / maximum * 100)) : 100
  scheduleSave()
}

function scheduleSave() {
  clearTimeout(saveTimer)
  saveTimer = setTimeout(saveProgress, 700)
}

async function saveProgress() {
  if (!resource.value) return
  try {
    await axios.put(`/books/${isbn}/ebook/progress`, {
      chapterNumber: currentChapter.value,
      scrollPercent: scrollPercent.value
    })
  } catch {
    // Progress is helpful but must never interrupt reading.
  }
}
</script>

<style scoped>
.reader-shell {
  --reader-bg: #e8e3d7;
  --reader-paper: #f7f2e7;
  --reader-ink: #292720;
  --reader-muted: #716d61;
  --reader-line: rgba(58,54,43,.15);
  height: 100dvh;
  min-height: 600px;
  display: grid;
  grid-template-rows: 72px minmax(0, 1fr) 34px;
  overflow: hidden;
  color: var(--reader-ink);
  background: var(--reader-bg);
}
.reader-shell[data-theme='night'] { --reader-bg: #151918; --reader-paper: #202523; --reader-ink: #e8e5dc; --reader-muted: #aaa99f; --reader-line: rgba(255,255,255,.12); }
.reader-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 8px 22px; border-bottom: 1px solid var(--reader-line); background: var(--reader-paper); z-index: 3; }
.reader-identity, .reader-tools { display: flex; align-items: center; }
.reader-identity { min-width: 0; gap: 10px; }
.reader-identity > div:last-child { min-width: 0; }
.reader-identity p { color: var(--reader-muted); font-size: 11px; }
.reader-identity h1 { max-width: 360px; overflow: hidden; font: 500 19px/1.35 var(--font-display); text-overflow: ellipsis; white-space: nowrap; }
.reader-tools { gap: 2px; }
.font-value { width: 28px; color: var(--reader-muted); font-size: 12px; text-align: center; }
.theme-switch { height: 34px; display: flex; margin-left: 10px; padding: 3px; border: 1px solid var(--reader-line); border-radius: 5px; }
.theme-switch button { width: 32px; border: 0; border-radius: 3px; color: var(--reader-muted); background: transparent; cursor: pointer; }
.theme-switch svg { width: 16px; height: 16px; fill: currentColor; }
.theme-switch button.active { color: var(--reader-ink); background: rgba(94,169,177,.23); }
.reader-layout { min-height: 0; display: grid; grid-template-columns: 220px minmax(0, 1fr); }
.chapter-sidebar { min-height: 0; display: grid; grid-template-rows: 52px minmax(0,1fr); border-right: 1px solid var(--reader-line); background: var(--reader-paper); }
.sidebar-heading { display: flex; align-items: center; justify-content: space-between; padding: 0 18px; border-bottom: 1px solid var(--reader-line); font-size: 14px; font-weight: 700; }
.sidebar-heading small { color: var(--reader-muted); font-size: 11px; font-weight: 400; }
.chapter-sidebar nav { min-height: 0; overflow-y: auto; padding: 8px; scrollbar-width: thin; }
.chapter-sidebar button { width: 100%; min-height: 38px; display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; border: 0; border-left: 2px solid transparent; color: var(--reader-muted); background: transparent; cursor: pointer; text-align: left; }
.chapter-sidebar button:hover { color: var(--reader-ink); background: rgba(94,169,177,.09); }
.chapter-sidebar button.active { border-left-color: #3b8c94; color: var(--reader-ink); background: rgba(94,169,177,.17); font-weight: 700; }
.chapter-sidebar button small { color: #3b8c94; font-size: 10px; }
.reading-pane { min-height: 0; overflow-y: auto; overscroll-behavior: contain; scrollbar-gutter: stable; background: var(--reader-bg); }
.reading-paper { width: min(780px, calc(100% - 48px)); min-height: 100%; margin: 0 auto; padding: 72px 68px 54px; color: var(--reader-ink); background: var(--reader-paper); box-shadow: 0 0 34px rgba(34,32,25,.08); }
.chapter-kicker { margin-bottom: 12px; color: #3b8c94; font-size: 12px; font-weight: 700; text-align: center; }
.reading-paper h2 { margin-bottom: 46px; font: 500 30px/1.45 var(--font-display); text-align: center; }
.chapter-content { font-family: 'Noto Serif SC', 'Songti SC', SimSun, serif; font-size: var(--reader-font-size); line-height: 2.05; }
.chapter-content :deep(p) { margin: 0 0 1.1em; text-align: justify; }
.chapter-content :deep(h2), .chapter-content :deep(h3), .chapter-content :deep(h4) { margin: 2em 0 1em; font-size: 1.15em; text-align: center; }
.chapter-content :deep(a) { color: inherit; text-decoration: none; }
.chapter-content :deep(table) { width: 100%; border-collapse: collapse; }
.chapter-content :deep(td), .chapter-content :deep(th) { padding: 4px 8px; }
.source-footer { margin-top: 70px; padding-top: 22px; border-top: 1px solid var(--reader-line); color: var(--reader-muted); font: 12px/1.7 system-ui, sans-serif; }
.source-footer strong { color: var(--reader-ink); }
.source-footer p { margin: 7px 0 10px; }
.source-footer div { display: flex; flex-wrap: wrap; gap: 10px 22px; }
.source-footer a { color: #277b84; text-underline-offset: 3px; }
.chapter-navigation { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-top: 34px; }
.chapter-navigation > span { flex: 0 0 auto; color: var(--reader-muted); font-size: 12px; }
.reader-state { height: 100%; min-height: 360px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; color: var(--reader-muted); }
.reader-state p { max-width: 430px; padding: 0 20px; line-height: 1.7; text-align: center; }
.reader-state h2 { color: var(--reader-ink); font-size: 22px; }
.loading-line { width: 100px; height: 2px; overflow: hidden; background: var(--reader-line); }
.loading-line::after { content: ''; width: 44px; height: 100%; display: block; background: #3b8c94; animation: load 1.2s ease-in-out infinite alternate; }
@keyframes load { from { transform: translateX(-20px); } to { transform: translateX(76px); } }
.reader-statusbar { display: grid; grid-template-columns: 1fr 1fr 1fr; align-items: center; padding: 0 18px; border-top: 1px solid var(--reader-line); color: var(--reader-muted); background: var(--reader-paper); font-size: 11px; }
.reader-statusbar div:nth-child(2) { text-align: center; }
.reader-statusbar div:last-child { text-align: right; }
.verified-mark { width: 7px; height: 7px; display: inline-block; margin-right: 5px; border-radius: 50%; background: #3f9b7c; }
.mobile-chapter-picker { display: none; }
@media (max-width: 760px) {
  .reader-shell { min-height: 520px; grid-template-rows: 64px 48px minmax(0,1fr) 32px; }
  .reader-toolbar { padding: 5px 8px; }
  .reader-identity h1 { max-width: 135px; font-size: 15px; }
  .reader-identity p { display: none; }
  .reader-tools { gap: 0; }
  .font-value { display: none; }
  .theme-switch { margin-left: 2px; }
  .reader-layout { grid-template-columns: 1fr; }
  .chapter-sidebar { display: none; }
  .mobile-chapter-picker { display: flex; align-items: center; gap: 10px; padding: 7px 14px; border-bottom: 1px solid var(--reader-line); color: var(--reader-muted); background: var(--reader-paper); font-size: 12px; }
  .mobile-chapter-picker select { min-width: 0; flex: 1; height: 32px; padding: 0 8px; border: 1px solid var(--reader-line); border-radius: 4px; color: var(--reader-ink); background: var(--reader-paper); }
  .reading-paper { width: 100%; padding: 44px 24px 38px; box-shadow: none; }
  .reading-paper h2 { margin-bottom: 34px; font-size: 25px; }
  .chapter-navigation { gap: 2px; }
  .reader-statusbar { grid-template-columns: 1fr auto; padding: 0 10px; }
  .reader-statusbar div:nth-child(2) { display: none; }
}
</style>
