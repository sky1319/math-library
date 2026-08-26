
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/SimpleLogin.vue')
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true, requiresReader: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true, requiresStaff: true }
  },
  {
    path: '/book/:isbn',
    name: 'BookDetail',
    component: () => import('../views/BookDetail.vue'),
    meta: { requiresAuth: true, requiresReader: true }
  },
  {
    path: '/reader/:isbn',
    name: 'Reader',
    component: () => import('../views/Reader.vue'),
    meta: { requiresAuth: true, requiresReader: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true, requiresReader: true }
  },
  {
    path: '/qa',
    name: 'QA',
    component: () => import('../views/QA.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to) {
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
    return
  }
  
  if (to.meta.requiresStaff && !userStore.isStaff) {
    next('/')
    return
  }

  if (to.meta.requiresReader && userStore.isStaff) {
    next('/admin')
    return
  }
  
  next()
})

export default router
