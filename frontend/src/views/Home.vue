<template>
  <main ref="homeRoot" class="library-home">
    <section id="cinema" ref="cinema" class="cinema-scroll" aria-label="馆藏探索滚动故事">
      <div class="stage">
        <div class="world">
          <img class="scene-img sky-img" :src="scene.sky" alt="" />

          <header class="site-header" aria-label="主导航">
            <a class="site-logo" href="#cinema">The Living Library</a>
            <nav class="site-nav" aria-label="主菜单">
              <a href="#cinema">序章</a>
              <a href="#story">馆藏</a>
              <a href="#catalog">检索</a>
              <FlowerButton variant="nav" @click="goToQA">AI 助手</FlowerButton>
            </nav>
            <div class="account-nav">
              <FlowerButton variant="nav" @click="goToProfile">{{ userStore.name }}</FlowerButton>
              <FlowerButton class="round-action" variant="icon" aria-label="退出登录" title="退出登录" @click="handleLogout">↗</FlowerButton>
            </div>
          </header>

          <div class="back-stack">
            <img class="scene-img back-img back-four" :src="scene.glow" alt="" />
            <div class="featured-slider" :class="{ visible: featuredVisible }">
              <div class="featured-track" :style="featuredTrackStyle">
                <article
                  v-for="(book, index) in featuredBooks"
                  :key="`${book.isbn}-${index}`"
                  class="featured-card"
                  tabindex="0"
                  role="button"
                  :aria-label="`打开《${book.title}》`"
                  @click="goToBookDetail(book.isbn)"
                  @keydown.enter="goToBookDetail(book.isbn)"
                >
                  <span class="card-kicker">{{ book.category || '馆藏精选' }}</span>
                  <span class="card-index">{{ String((index % Math.max(books.length, 1)) + 1).padStart(2, '0') }}</span>
                  <h3>{{ book.title }}</h3>
                  <p>{{ book.author }} · {{ book.location || '馆藏区' }}</p>
                </article>
              </div>
            </div>
            <img class="scene-img back-img back-bazaar" :src="scene.bazaar" alt="" />
          </div>

          <div class="slider-controls" :class="{ ready: controlsVisible }" aria-label="馆藏滑轨控制">
            <FlowerButton variant="icon" aria-label="上一组" @click="moveFeatured(-1)">←</FlowerButton>
            <FlowerButton variant="icon" aria-label="下一组" @click="moveFeatured(1)">→</FlowerButton>
          </div>

          <ParticleText
            class="hero-title"
            text="LIBRARY"
            :particle-size="2"
            :density="4"
            color="#fdf1e1"
            highlight-color="#87e1d5"
            :scatter="150"
            :gather-duration="1300"
            :stagger="260"
            :pointer-repel="42"
            :repel-radius="140"
            :idle-drift="0.55"
            trigger="hover"
            font-size="clamp(4.4rem, 15vw, 14rem)"
            :font-weight="500"
            font-family="'Ogg Medium', Georgia, serif"
            glow
            aria-label="LIBRARY"
          />
          <img class="scene-img splitframe-img splitframe-left" :src="scene.left" alt="" />
          <img class="scene-img splitframe-img splitframe-right" :src="scene.right" alt="" />
          <img class="scene-img bridge-img" :src="scene.bridge" alt="" />
          <img class="scene-img frame-two-img" :src="scene.river" alt="" />
          <div class="shade"></div>
        </div>

        <section class="intro-copy" aria-label="图书馆概览">
          <p>让一页文字成为入口，在缓慢阅读、安静探索与每一次不期而遇之间，找到你的下一本书。</p>
          <div class="hero-tags" aria-label="图书馆特色">
            <span>100 本馆藏</span><span>智能推荐</span><span>随时借阅</span>
          </div>
          <span class="scroll-cue">向下探索 <i></i></span>
        </section>

        <section id="story" class="story-panel story-panel-bridge" aria-label="馆藏故事">
          <span class="panel-kicker">A CURATED COLLECTION</span>
          <h2>每一本书，都是通往另一种生活的桥。</h2>
          <p>从文学经典到科幻想象，从推理迷局到编程实践，我们把知识整理成一条可以自在漫游的路径。</p>
          <dl class="facts">
            <div><dt>{{ books.length || 100 }}</dt><dd>本精选馆藏持续更新</dd></div>
            <div><dt>05</dt><dd>个主题分类等你探索</dd></div>
          </dl>
        </section>

        <section class="story-panel story-panel-bazaar" aria-label="智能阅读体验">
          <span class="panel-kicker">READ WITH INTELLIGENCE</span>
          <h2>知识库，让寻找一本书变得更近。</h2>
          <p>询问馆藏位置、内容背景或阅读建议，AI 图书助手会从真实馆藏出发，为你整理答案。</p>
          <FlowerButton class="note-button" variant="wide" @click="goToQA"><span aria-hidden="true">↗</span> 打开 AI 图书助手</FlowerButton>
        </section>
      </div>
    </section>

    <section id="catalog" class="catalog-section">
      <div class="catalog-heading">
        <span class="eyebrow">EXPLORE THE SHELVES</span>
        <h2>找到你的<br />下一本书。</h2>
        <p>按书名、作者或主题检索馆藏，也可以从分类开始浏览。</p>
      </div>

      <div class="catalog-tools">
        <label class="search-field galaxy-input-shell">
          <span>检索馆藏</span>
          <span class="galaxy-input-glow" aria-hidden="true"></span>
          <input class="galaxy-input" v-model="keyword" type="search" placeholder="书名、作者或关键词" @keyup.enter="handleSearch" />
          <FlowerButton variant="icon" aria-label="搜索" title="搜索" @click="handleSearch">→</FlowerButton>
        </label>
        <div class="category-tabs" aria-label="图书分类">
          <FlowerButton v-for="item in categories" :key="item.value" variant="tab" :class="{ active: category === item.value }" @click="selectCategory(item.value)">{{ item.label }}</FlowerButton>
        </div>
        <span class="result-count">{{ books.length }} titles</span>
      </div>

      <div v-if="loading" class="state-copy">正在整理书架...</div>
      <div v-else-if="books.length === 0" class="state-copy">没有找到匹配的馆藏。</div>
      <div v-else class="book-grid">
        <article v-for="(book, index) in books" :key="book.isbn" class="book-card" @click="goToBookDetail(book.isbn)">
          <div class="book-number">{{ String(index + 1).padStart(2, '0') }}</div>
          <div class="book-status" :class="{ closed: !canBorrow(book) }">{{ borrowButtonText(book) }}</div>
          <div class="book-copy">
            <span>{{ book.category }}</span>
            <h3>{{ book.title }}</h3>
            <p>{{ book.author }}</p>
          </div>
          <dl>
            <div><dt>位置</dt><dd>{{ book.location || '未知' }}</dd></div>
            <div><dt>可借</dt><dd>{{ book.availableCount }}/{{ book.totalCount }}</dd></div>
          </dl>
          <div class="book-actions">
            <FlowerButton variant="action" :disabled="!canBorrow(book)" @click.stop="handleBorrow(book)">借阅</FlowerButton>
            <FlowerButton variant="icon" aria-label="加入心愿单" title="加入心愿单" @click.stop="addToWishList(book)">＋</FlowerButton>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import axios from '../axios'
import { ElMessage } from 'element-plus'
import ParticleText from '../components/ParticleText.vue'
import FlowerButton from '../components/FlowerButton.vue'

const router = useRouter()
const userStore = useUserStore()
const cinema = ref(null)
const homeRoot = ref(null)
const keyword = ref('')
const category = ref('')
const books = ref([])
const loading = ref(false)
const featuredIndex = ref(0)
const featuredVisible = ref(false)
const controlsVisible = ref(false)

const scene = {
  sky: 'https://raft-blast-61784561.figma.site/_assets/v11/16b5007d9c93971e26ffe4e0e3e37946f6bd538c.png',
  glow: 'https://raft-blast-61784561.figma.site/_assets/v11/8a7f8af50e0ce92ec2e228e7b0b4112178c51cf1.png',
  bazaar: 'https://raft-blast-61784561.figma.site/_assets/v11/864afe00e41e2fa20a5aa546e15cb807e0f81384.png',
  left: 'https://raft-blast-61784561.figma.site/_assets/v11/7536d7b60a1fce482cf6edf3f0bffd3bad5d0f8a.png',
  right: 'https://raft-blast-61784561.figma.site/_assets/v11/392db6a6a6b98e868bd7f8d3f55bb719d51e5028.png',
  bridge: 'https://raft-blast-61784561.figma.site/_assets/v11/c6a6d8ef49bca43f708aa852692942c45ec950d4.png',
  river: 'https://raft-blast-61784561.figma.site/_assets/v11/ba75252bab2b1c510987b74837770f7bc8a6b2d4.png'
}

const categories = [
  { label: '全部', value: '' }, { label: '文学经典', value: '文学经典' },
  { label: '科幻小说', value: '科幻小说' }, { label: '推理小说', value: '推理小说' },
  { label: '历史', value: '历史' }, { label: '编程', value: '编程' }
]

const featuredBooks = computed(() => {
  const source = books.value.slice(0, 8)
  return source.length ? [...source, ...source] : []
})
const featuredTrackStyle = computed(() => ({ transform: `translate3d(${-featuredIndex.value * 380}px, 0, 0)` }))

let raf = 0
let targetX = 0
let targetY = 0
let mouseX = 0
let mouseY = 0

onMounted(() => {
  loadBooks()
  window.addEventListener('scroll', requestFrame, { passive: true })
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('resize', requestFrame, { passive: true })
  requestFrame()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', requestFrame)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('resize', requestFrame)
  cancelAnimationFrame(raf)
})

function clamp(value, min = 0, max = 1) { return Math.min(max, Math.max(min, value)) }
function smoothstep(a, b, value) { const x = clamp((value - a) / (b - a)); return x * x * (3 - 2 * x) }
function onPointerMove(event) {
  targetX = event.clientX / window.innerWidth - 0.5
  targetY = event.clientY / window.innerHeight - 0.5
  requestFrame()
}
function requestFrame() {
  if (!raf) raf = requestAnimationFrame(updateScene)
}
function updateScene() {
  raf = 0
  if (!cinema.value || !homeRoot.value) return
  const distance = clamp(-cinema.value.getBoundingClientRect().top, 0, cinema.value.offsetHeight - window.innerHeight)
  mouseX += (targetX - mouseX) * 0.12
  mouseY += (targetY - mouseY) * 0.12
  const intro = smoothstep(90, 650, distance)
  const bridge = smoothstep(560, 900, distance) * (1 - smoothstep(1300, 1620, distance))
  const bridgeEnter = smoothstep(560, 900, distance)
  const bridgeExit = smoothstep(1300, 1620, distance)
  const story = smoothstep(1760, 2140, distance) * (1 - smoothstep(2540, 2700, distance))
  const storyEnter = smoothstep(1760, 2140, distance)
  const storyExit = smoothstep(2540, 2700, distance)
  const slider = Math.pow(smoothstep(2760, 3500, distance), 1.55)
  const controls = smoothstep(3300, 3600, distance)
  const progress = clamp(distance / 2700)
  const root = homeRoot.value.style
  root.setProperty('--title-y', `${intro * -210}px`)
  root.setProperty('--title-scale', 1 - intro * 0.08)
  root.setProperty('--title-opacity', 1 - intro)
  root.setProperty('--intro-y', `${intro * 90}px`)
  root.setProperty('--intro-opacity', 1 - intro)
  root.setProperty('--scene-x', `${mouseX * -12}px`)
  root.setProperty('--scene-y', `${mouseY * -6}px`)
  root.setProperty('--back-scale', 0.78 + progress * 0.2 + bridgeEnter * 0.16 + storyEnter * 0.12)
  root.setProperty('--bridge-y', `${mouseY * 8 + progress * -74 - bridgeExit * 760}px`)
  root.setProperty('--bridge-width', `${68 + bridgeEnter * 37}vw`)
  root.setProperty('--bridge-bottom', `${5 - bridgeEnter * 13}vh`)
  root.setProperty('--split-left', `${-Math.pow(bridgeEnter, 1.5) * 46}vw`)
  root.setProperty('--split-right', `${Math.pow(bridgeEnter, 1.5) * 46}vw`)
  root.setProperty('--split-y', `${mouseY * 10 + progress * -74 - Math.pow(bridgeEnter, 1.5) * 180}px`)
  root.setProperty('--river-opacity', bridge * (1 - storyEnter))
  root.setProperty('--panel2-opacity', bridge * (1 - bridgeExit))
  root.setProperty('--panel2-y', `${-bridgeExit * 86 + (1 - bridgeEnter) * 58}px`)
  root.setProperty('--panel3-opacity', story * (1 - storyExit))
  root.setProperty('--panel3-y', `${-storyExit * 86 + (1 - storyEnter) * 58}px`)
  root.setProperty('--shade-alpha', clamp(bridge + story) * 0.48)
  root.setProperty('--slider-x', `${(1 - slider) * 420}vw`)
  root.setProperty('--slider-opacity', slider)
  const sliderScreenTop = Math.min(220, Math.max(112, window.innerHeight * 0.19)) - 34
  const sliderParentTop = window.innerHeight - (window.innerHeight - sliderScreenTop) / (0.78 + progress * 0.2 + bridgeEnter * 0.16 + storyEnter * 0.12)
  root.setProperty('--slider-top', `${sliderParentTop}px`)
  root.setProperty('--slider-screen-top', `${sliderScreenTop}px`)
  featuredVisible.value = slider > 0.01
  controlsVisible.value = controls > 0.98
  if (Math.abs(mouseX - targetX) > 0.001 || Math.abs(mouseY - targetY) > 0.001) requestFrame()
}

function canBorrow(book) { return book.borrowable !== false && book.availableCount > 0 }
function borrowButtonText(book) {
  if (book.borrowable === false) return '暂不可借'
  if (book.availableCount <= 0) return '已借完'
  return '可借阅'
}
async function loadBooks() {
  loading.value = true
  try {
    const response = await axios.get('/books')
    if (response.data.success) books.value = response.data.data
  } catch { ElMessage.error('加载图书失败') }
  finally { loading.value = false }
}
async function handleSearch() {
  loading.value = true
  try {
    const response = await axios.get('/books/search', { params: { keyword: keyword.value || '' } })
    if (response.data.success) books.value = category.value ? response.data.data.filter(book => book.category === category.value) : response.data.data
  } catch { ElMessage.error('搜索失败') }
  finally { loading.value = false }
}
function selectCategory(value) { category.value = value; handleSearch() }
function moveFeatured(direction) {
  const count = Math.max(books.value.slice(0, 8).length, 1)
  featuredIndex.value = (featuredIndex.value + direction + count) % count
}
function goToBookDetail(isbn) { router.push(`/book/${isbn}`) }
function goToQA() { router.push('/qa') }
function goToProfile() { router.push('/profile') }
async function handleBorrow(book) {
  try {
    const response = await axios.post(`/user/borrow/${book.isbn}`)
    if (response.data.success) { ElMessage.success('借阅成功'); book.availableCount--; book.borrowedCount++ }
    else ElMessage.warning(response.data.message)
  } catch { ElMessage.error('借阅失败') }
}
async function addToWishList(book) {
  try {
    const response = await axios.post(`/user/wish-list/${book.isbn}`)
    response.data.success ? ElMessage.success('已添加到心愿单') : ElMessage.warning(response.data.message)
  } catch { ElMessage.error('添加失败') }
}
function handleLogout() { userStore.logout(); router.push('/login') }
</script>

<style scoped>
.library-home {
  --title-y: 0px; --title-scale: 1; --title-opacity: 1; --intro-y: 0px; --intro-opacity: 1;
  --scene-x: 0px; --scene-y: 0px; --back-scale: .78; --bridge-y: 0px; --bridge-width: 68vw;
  --bridge-bottom: 5vh; --split-left: 0vw; --split-right: 0vw; --split-y: 0px;
  --river-opacity: 0; --panel2-opacity: 0; --panel2-y: 58px; --panel3-opacity: 0;
  --panel3-y: 58px; --shade-alpha: 0; --slider-x: 420vw; --slider-opacity: 0;
  --slider-top: 14vh; --slider-screen-top: 110px;
  color: #fdf1e1; background: #0b1110; overflow: clip;
}
.cinema-scroll { position: relative; height: calc(100vh + 3700px); }
.stage { position: sticky; top: 0; height: 100vh; min-height: 620px; overflow: hidden; isolation: isolate; background: #78b6da; }
.world, .sky-img, .site-header, .back-stack, .shade, .hero-title, .intro-copy, .story-panel, .slider-controls { position: absolute; }
.world { inset: 0; overflow: hidden; background: #78b6da; }
.scene-img { display: block; user-select: none; pointer-events: none; will-change: transform, opacity, filter; }
.sky-img { inset: 0; width: 100%; height: 100%; object-fit: cover; }
.site-header { z-index: 12; top: 0; left: 0; right: 0; display: grid; grid-template-columns: minmax(260px, 1fr) auto minmax(260px, 1fr); align-items: center; gap: 32px; padding: 32px; }
.site-logo { justify-self: start; color: #fdf1e1; font-family: var(--font-display); font-size: 24px; text-decoration: none; white-space: nowrap; }
.site-nav { display: flex; align-items: center; gap: clamp(24px, 2.2vw, 44px); }
.site-nav a, .site-nav button, .account-nav button { border: 0; padding: 0; color: rgba(253,241,225,.9); background: transparent; font-size: 16px; text-decoration: none; cursor: pointer; text-shadow: 0 2px 16px rgba(0,0,0,.2); }
.account-nav { justify-self: end; display: flex; align-items: center; gap: 18px; }
.account-nav .round-action { font-size: 16px; }
.back-stack { z-index: 1; inset: 0 -3vw; transform: translate3d(var(--scene-x), var(--scene-y), 0) scale(var(--back-scale)); transform-origin: 50% 100%; will-change: transform; }
.back-img { position: absolute; inset: auto auto 0 48%; width: 112%; height: auto; object-fit: contain; }
.back-bazaar { z-index: 3; transform: translate3d(-50%, 14vh, 0) scale(.86); }
.back-four { z-index: 1; opacity: .72; mix-blend-mode: screen; transform: translate3d(-50%, calc(10vh - 110px), 0) scale(.8); }
.hero-title { z-index: 6; left: 50%; top: clamp(132px, 20vh, 212px); width: min(94vw, 1780px); height: clamp(190px, 22vw, 310px); min-height: 0; margin: 0; color: #fdf1e1; font-family: var(--font-display); font-size: clamp(8rem, 15vw, 14rem); font-weight: 500; line-height: .78; text-align: center; transform: translate3d(-50%, var(--title-y), 0) scale(var(--title-scale)); opacity: var(--title-opacity); }
.bridge-img { position: absolute; z-index: 5; left: 50%; bottom: var(--bridge-bottom); width: min(var(--bridge-width), 2140px); height: auto; transform: translate3d(-50%, var(--bridge-y), 0) scale(1.02); transform-origin: 50% 48%; }
.splitframe-img { position: absolute; z-index: 7; left: 50%; bottom: -2vh; width: min(118vw, 2240px); height: auto; }
.splitframe-left { transform: translate3d(calc(-50% + var(--split-left)), var(--split-y), 0); transform-origin: 21% 52%; }
.splitframe-right { transform: translate3d(calc(-50% + var(--split-right)), var(--split-y), 0); transform-origin: 79% 52%; }
.frame-two-img { position: absolute; z-index: 6; left: 50%; top: 50%; width: min(122vw, 2160px); height: auto; opacity: var(--river-opacity); transform: translate3d(-50%, -50%, 0) scale(1.08); }
.shade { inset: 0; z-index: 8; pointer-events: none; background: linear-gradient(180deg, rgba(44,139,170,var(--shade-alpha)), rgba(16,91,114,var(--shade-alpha))); }
.intro-copy { z-index: 10; left: 50%; bottom: clamp(56px, 22vh, 300px); width: min(600px, calc(100vw - 40px)); text-align: center; transform: translate3d(-50%, var(--intro-y), 0); opacity: var(--intro-opacity); }
.intro-copy p { margin: 0 auto; font-size: 1.18rem; font-weight: 500; line-height: 1.25; text-shadow: 0 2px 18px rgba(0,0,0,.42); }
.hero-tags { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-top: 26px; }
.hero-tags span { min-height: 42px; display: inline-flex; align-items: center; padding: 0 25px; color: #111411; border-radius: 999px; background: #fdf1e1; font-size: .92rem; }
.scroll-cue { display: flex; align-items: center; justify-content: center; gap: 12px; margin-top: 28px; font-size: 12px; text-transform: uppercase; }
.scroll-cue i { width: 42px; height: 1px; background: currentColor; }
.story-panel { z-index: 11; left: 50%; width: min(760px, calc(100vw - 42px)); text-align: center; pointer-events: none; }
.story-panel-bridge { top: 60%; opacity: var(--panel2-opacity); transform: translate3d(-50%, calc(-50% + var(--panel2-y)), 0); }
.story-panel-bazaar { top: 29%; opacity: var(--panel3-opacity); transform: translate3d(-50%, calc(-50% + var(--panel3-y)), 0); }
.panel-kicker, .eyebrow { display: block; margin-bottom: 20px; font-size: 11px; font-weight: 700; letter-spacing: .14em; }
.story-panel h2 { margin: 0; font-family: var(--font-display); font-size: clamp(3rem, 5vw, 4.75rem); font-weight: 500; line-height: .95; }
.story-panel p { width: min(540px, 100%); margin: 26px auto 0; font-size: 1.08rem; line-height: 1.35; }
.facts { display: grid; grid-template-columns: repeat(2, 1fr); gap: 70px; width: min(470px, 100%); margin: 58px auto 0; }
.facts dt { font-family: var(--font-display); font-size: 4.2rem; line-height: .9; }
.facts dd { margin: 18px 0 0; font-size: .9rem; }
.note-button { margin-top: 28px; pointer-events: auto; }
.featured-slider { position: absolute; z-index: 2; left: 0; right: 0; top: var(--slider-top); visibility: hidden; transform: translate3d(var(--slider-x), 0, 0) scale(calc(1 / var(--back-scale))); transform-origin: 0 0; }
.featured-slider.visible { visibility: visible; }
.featured-track { display: flex; gap: 20px; margin-left: -16vw; transition: transform 640ms cubic-bezier(.22,1,.36,1); }
.featured-card { position: relative; flex: 0 0 360px; height: 220px; padding: 24px; overflow: hidden; border: 1px solid rgba(253,241,225,.42); border-radius: 24px; color: #111411; background: #fdf1e1; box-shadow: 0 18px 52px rgba(2,47,64,.12); cursor: pointer; }
.card-kicker { font-size: 11px; font-weight: 700; text-transform: uppercase; }
.card-index { position: absolute; top: 20px; right: 24px; font-family: var(--font-display); font-size: 3rem; color: #78a9a7; }
.featured-card h3 { position: absolute; left: 24px; right: 24px; bottom: 62px; margin: 0; overflow: hidden; font-family: var(--font-display); font-size: 2rem; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.featured-card p { position: absolute; left: 24px; right: 24px; bottom: 24px; margin: 0; font-size: 14px; }
.slider-controls { z-index: 12; left: 48px; top: calc(var(--slider-screen-top) + 238px); display: flex; gap: 14px; opacity: var(--slider-opacity); pointer-events: none; }
.slider-controls.ready { pointer-events: auto; }
.slider-controls button { --flower-width: 54px; --flower-height: 54px; font-size: 16px; }
.catalog-section { position: relative; z-index: 20; min-height: 100vh; padding: 120px clamp(24px, 5vw, 80px); color: #111411; background: #fdf1e1; }
.catalog-heading { display: grid; grid-template-columns: 1fr 1.3fr .8fr; align-items: end; gap: 40px; padding-bottom: 72px; border-bottom: 1px solid rgba(17,20,17,.25); }
.catalog-heading .eyebrow { align-self: start; }
.catalog-heading h2 { margin: 0; font-family: var(--font-display); font-size: clamp(4rem, 8vw, 8rem); font-weight: 500; line-height: .82; }
.catalog-heading p { max-width: 310px; margin: 0; line-height: 1.5; }
.catalog-tools { display: grid; grid-template-columns: minmax(320px, 1fr) 2fr auto; align-items: end; gap: 36px; padding: 42px 0; }
.search-field { display: grid; grid-template-columns: 1fr auto; border-bottom: 1px solid #111411; }
.search-field.galaxy-input-shell { display: grid; grid-template-columns: 1fr auto; border-bottom: 0; padding: 0 8px; }
.search-field.galaxy-input-shell::before,
.search-field.galaxy-input-shell::after { border-radius: 12px; }
.search-field.galaxy-input-shell .galaxy-input-glow { grid-column: auto; margin: 0; font-size: 0; text-transform: none; }
.search-field span { grid-column: 1 / -1; margin-bottom: 12px; font-size: 11px; font-weight: 700; text-transform: uppercase; }
.search-field input { min-width: 0; padding: 12px 0; border: 0; outline: 0; color: #111411; background: transparent; font: inherit; }
.search-field.galaxy-input-shell > input.galaxy-input { color: #a9c7ff; background: transparent; }
.search-field button { --flower-width: 48px; --flower-height: 48px; width: 48px; height: 48px; font-size: 16px; }
.category-tabs { display: flex; flex-wrap: wrap; gap: 8px; }
.category-tabs button { font-size: 16px; }
.result-count { font-family: var(--font-display); font-size: 1.4rem; white-space: nowrap; }
.book-grid { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); gap: 18px; }
.book-card { position: relative; min-height: 340px; padding: 24px; border: 1px solid rgba(17,20,17,.3); border-radius: 6px; display: flex; flex-direction: column; cursor: pointer; transition: transform .25s ease, background .25s ease; }
.book-card:hover { transform: translateY(-5px); background: #fff8ee; }
.book-number { font-family: var(--font-display); font-size: 3.4rem; color: #73a9a7; }
.book-status { position: absolute; top: 24px; right: 24px; padding: 7px 10px; border-radius: 999px; color: #165b48; background: #cde7d7; font-size: 11px; }
.book-status.closed { color: #8f362e; background: #f1d3cb; }
.book-copy { margin-top: auto; }
.book-copy span { font-size: 11px; font-weight: 700; text-transform: uppercase; }
.book-copy h3 { margin: 10px 0 6px; font-family: var(--font-display); font-size: 2.15rem; font-weight: 500; line-height: 1; }
.book-copy p { margin: 0; color: rgba(17,20,17,.68); }
.book-card dl { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin: 24px 0 0; padding-top: 18px; border-top: 1px solid rgba(17,20,17,.18); }
.book-card dt { font-size: 10px; text-transform: uppercase; }
.book-card dd { margin: 5px 0 0; font-weight: 600; }
.book-actions { display: flex; gap: 8px; margin-top: 18px; }
.book-actions button { font-size: 16px; }
.book-actions button:last-child { --flower-width: 48px; --flower-height: 48px; font-size: 16px; }
.book-actions button:disabled { opacity: .35; cursor: not-allowed; }
.state-copy { padding: 100px 0; text-align: center; font-family: var(--font-display); font-size: 2rem; }
@media (max-width: 1100px) {
  .site-header { grid-template-columns: 1fr auto; }
  .site-nav { display: none; }
  .hero-title { height: 190px; font-size: 7.5rem; }
  .bridge-img { width: 138vw; }
  .catalog-heading { grid-template-columns: 1fr 1fr; }
  .catalog-heading p { display: none; }
  .catalog-tools { grid-template-columns: 1fr; }
  .book-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .stage { min-height: 640px; }
  .site-header { padding: 22px; }
  .site-logo { font-size: 19px; }
  .account-nav > button:first-child { display: none; }
  .hero-title { top: 17vh; height: 132px; font-size: 4.4rem; }
  .bridge-img { width: 190vw; }
  .splitframe-img { width: 190vw; }
  .frame-two-img { width: 176vw; }
  .intro-copy { bottom: 42px; }
  .story-panel h2 { font-size: 2.45rem; }
  .story-panel p { font-size: .95rem; }
  .facts { gap: 18px; margin-top: 34px; }
  .facts dt { font-size: 2.5rem; }
  .featured-card { flex-basis: min(82vw, 330px); }
  .slider-controls { left: 22px; }
  .catalog-section { padding: 80px 20px; }
  .catalog-heading { grid-template-columns: 1fr; gap: 20px; padding-bottom: 48px; }
  .catalog-heading h2 { font-size: 4rem; }
  .book-grid { grid-template-columns: 1fr; }
  .book-card { min-height: 320px; }
}
@media (prefers-reduced-motion: reduce) { .scene-img, .back-stack, .hero-title, .intro-copy, .story-panel, .featured-track { transition: none; } }
</style>
