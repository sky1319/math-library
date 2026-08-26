
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from '../axios'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || null)
  const userId = ref(localStorage.getItem('userId') || null)
  const name = ref(localStorage.getItem('name') || null)
  const role = ref(localStorage.getItem('role') || null)
  
  const isLoggedIn = computed(() => token.value !== null)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isStaff = computed(() => ['ADMIN', 'LIBRARIAN'].includes(role.value))
  
  async function login(userIdInput, password) {
    try {
      const response = await axios.post('/auth/login', { userId: userIdInput, password })
      const data = response.data
      
      if (data.success) {
        token.value = data.data.token
        userId.value = data.data.userId
        name.value = data.data.name
        role.value = data.data.role
        
        localStorage.setItem('token', token.value)
        localStorage.setItem('userId', userId.value)
        localStorage.setItem('name', name.value)
        localStorage.setItem('role', role.value)
        
        return true
      } else {
        return false
      }
    } catch (error) {
      return false
    }
  }
  
  function logout() {
    token.value = null
    userId.value = null
    name.value = null
    role.value = null
    
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('name')
    localStorage.removeItem('role')
  }
  
  return {
    token,
    userId,
    name,
    role,
    isLoggedIn,
    isAdmin,
    isStaff,
    login,
    logout
  }
})
