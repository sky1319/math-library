<template>
  <main class="login-page">
    <img class="login-sky" :src="scene.sky" alt="" />
    <img class="login-city" :src="scene.city" alt="" />
    <img class="login-bridge" :src="scene.bridge" alt="" />
    <div class="login-shade"></div>

    <header class="login-header">
      <span>The Living Library</span>
      <span>Smart reading · 2026</span>
    </header>

    <section class="login-intro">
      <span class="eyebrow">WELCOME TO THE COLLECTION</span>
      <h1>READ<br />BEYOND.</h1>
      <p>从一座安静的知识之城开始，找到、借阅并理解你的下一本书。</p>
    </section>

    <form class="login-form" @submit.prevent="handleSubmit">
      <div class="form-heading">
        <span>MEMBER ACCESS</span>
        <h2>进入馆藏</h2>
      </div>
      <label>
        <span>用户账号</span>
        <div class="galaxy-input-shell">
          <span class="galaxy-input-glow" aria-hidden="true"></span>
          <input class="galaxy-input" v-model="username" type="text" autocomplete="username" placeholder="请输入用户账号" />
        </div>
      </label>
      <label>
        <span>登录密码</span>
        <div class="galaxy-input-shell">
          <span class="galaxy-input-glow" aria-hidden="true"></span>
          <input class="galaxy-input" v-model="password" type="password" autocomplete="current-password" placeholder="••••••••" />
        </div>
      </label>
      <FlowerButton class="submit-button" variant="wide" type="submit" :disabled="submitting">
        <span>{{ submitting ? '正在进入' : '登录系统' }}</span><span aria-hidden="true">→</span>
      </FlowerButton>
    </form>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import FlowerButton from '../components/FlowerButton.vue'

const router = useRouter()
const userStore = useUserStore()
const username = ref('')
const password = ref('')
const submitting = ref(false)
const scene = {
  sky: 'https://raft-blast-61784561.figma.site/_assets/v11/16b5007d9c93971e26ffe4e0e3e37946f6bd538c.png',
  city: 'https://raft-blast-61784561.figma.site/_assets/v11/864afe00e41e2fa20a5aa546e15cb807e0f81384.png',
  bridge: 'https://raft-blast-61784561.figma.site/_assets/v11/c6a6d8ef49bca43f708aa852692942c45ec950d4.png'
}

async function handleSubmit() {
  if (!username.value || !password.value) { ElMessage.warning('请输入用户名和密码'); return }
  submitting.value = true
  try {
    const success = await userStore.login(username.value, password.value)
    if (success) router.push(userStore.isStaff ? '/admin' : '/')
    else ElMessage.error('用户名或密码错误')
  } catch (error) { ElMessage.error(`登录异常：${error.message}`) }
  finally { submitting.value = false }
}
</script>

<style scoped>
.login-page { position: relative; min-height: 100vh; overflow: hidden; color: #fdf1e1; background: #77b6dc; }
.login-sky, .login-city, .login-bridge { position: absolute; display: block; pointer-events: none; user-select: none; }
.login-sky { inset: 0; width: 100%; height: 100%; object-fit: cover; }
.login-city { z-index: 1; left: 48%; bottom: -11vh; width: 116%; transform: translateX(-50%) scale(.9); }
.login-bridge { z-index: 2; left: 48%; bottom: -13vh; width: min(72vw, 1400px); transform: translateX(-50%) scale(1.08); }
.login-shade { position: absolute; inset: 0; z-index: 3; background: linear-gradient(90deg, rgba(3,31,35,.22), rgba(4,30,33,.02) 48%, rgba(3,27,29,.78)), linear-gradient(180deg, rgba(13,61,73,.05), rgba(5,25,27,.42)); }
.login-header { position: absolute; z-index: 5; top: 0; left: 0; right: 0; display: flex; justify-content: space-between; padding: 32px; font-size: 13px; }
.login-header span:first-child { font-family: var(--font-display); font-size: 24px; }
.login-intro { position: absolute; z-index: 5; left: clamp(24px, 5vw, 76px); top: 50%; width: min(55vw, 850px); transform: translateY(-50%); }
.eyebrow { display: block; margin-bottom: 24px; font-size: 11px; font-weight: 700; letter-spacing: .16em; }
.login-intro h1 { margin: 0; font-family: var(--font-display); font-size: clamp(6rem, 12vw, 12rem); font-weight: 500; line-height: .72; }
.login-intro p { width: min(440px, 100%); margin: 34px 0 0; font-size: 1.1rem; line-height: 1.45; text-shadow: 0 2px 18px rgba(0,0,0,.35); }
.login-form { position: absolute; z-index: 6; top: 50%; right: clamp(24px, 5vw, 76px); width: min(390px, calc(100vw - 48px)); padding: 32px; color: #111411; border-radius: 6px; background: #fdf1e1; box-shadow: 0 28px 80px rgba(0,0,0,.22); transform: translateY(-50%); }
.form-heading span, label span { display: block; font-size: 10px; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
.form-heading h2 { margin: 8px 0 38px; font-family: var(--font-display); font-size: 3.2rem; font-weight: 500; }
label { display: block; margin-bottom: 24px; }
label input { width: 100%; padding: 13px 0; border: 0; border-bottom: 1px solid rgba(17,20,17,.42); outline: 0; color: #111411; background: transparent; font-size: 16px; }
label input:focus { border-color: #111411; }
label .galaxy-input-shell { margin-top: 8px; }
label .galaxy-input-shell > input.galaxy-input { width: 100%; padding: 0 18px 0 58px; border: 0; border-bottom: 0; color: #a9c7ff; background: transparent; }
label .galaxy-input-glow { display: block; margin: 0; font-size: 0; letter-spacing: 0; }
.submit-button { --flower-width: 100%; --flower-wrapper-width: 82%; width: 100%; margin-top: 8px; padding: 0; border: 0; background: transparent; }
.submit-button:disabled { opacity: .55; }
@media (max-width: 900px) {
  .login-intro { top: 25%; width: calc(100vw - 48px); transform: none; }
  .login-intro h1 { font-size: 6rem; }
  .login-intro p { display: none; }
  .login-form { top: auto; bottom: 24px; left: 50%; right: auto; transform: translateX(-50%); }
  .login-city { width: 190%; }
  .login-bridge { width: 160vw; }
}
@media (max-width: 520px) {
  .login-header { padding: 22px; }
  .login-header span:last-child { display: none; }
  .login-intro { top: 16%; left: 22px; }
  .login-intro h1 { font-size: 4.2rem; }
  .login-form { bottom: 16px; width: calc(100vw - 32px); padding: 24px; }
  .form-heading h2 { margin-bottom: 26px; font-size: 2.5rem; }
}
</style>
