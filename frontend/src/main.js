
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/app-background.css'
import App from './App.vue'
import router from './router'

const resizeObserverErrHandler = (e) => {
  if (e.message && e.message.includes('ResizeObserver loop completed with undelivered notifications')) {
    e.preventDefault?.()
    return
  }
  console.error(e)
}
window.addEventListener('error', resizeObserverErrHandler, { capture: true })

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
