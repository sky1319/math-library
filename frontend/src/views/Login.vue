
<template>
  <div class="login-container app-scene-root">
    <div class="login-box">
      <h2>智能图书管理系统</h2>
      <el-form ref="form" :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.userId" placeholder="请输入用户名" class="galaxy-input" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input 
            v-model="form.password" 
            :type="showPassword ? 'text' : 'password'" 
            placeholder="请输入密码"
            suffix-icon="eye"
            @click-suffix="showPassword = !showPassword"
            class="galaxy-input"
          />
        </el-form-item>
        <el-form-item>
          <FlowerButton variant="wide" @click="handleLogin" class="login-btn">登录</FlowerButton>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import FlowerButton from '../components/FlowerButton.vue'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  userId: '',
  password: ''
})

const showPassword = ref(false)

async function handleLogin() {
  if (!form.value.userId || !form.value.password) {
    return
  }
  
  const success = await userStore.login(form.value.userId, form.value.password)
  
  if (success) {
    if (userStore.isStaff) {
      router.push('/admin')
    } else {
      router.push('/')
    }
  } else {
    alert('用户名或密码错误')
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  position: relative;
}

.login-box {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(18px) saturate(1.15);
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.35);
  width: 400px;
  max-width: 100%;
  position: relative;
  z-index: 1;
  border: 1px solid rgba(255, 255, 255, 0.55);
}

.login-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.login-btn {
  width: 100%;
}

</style>
