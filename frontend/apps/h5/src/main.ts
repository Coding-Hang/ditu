import ElementPlus from 'element-plus'
import { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'
import type { ApiErrorDetail } from '@ditu/api-client'
import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { useAuthStore } from './stores/auth'
import './styles.css'

const pinia = createPinia()

window.addEventListener('ditu:api-error', ((event: Event) => {
  const detail = (event as CustomEvent<ApiErrorDetail>).detail
  if (detail?.message) ElMessage.error(detail.message)
}) as EventListener)

window.addEventListener('ditu:unauthorized', (() => {
  const auth = useAuthStore(pinia)
  auth.logout()
  if (router.currentRoute.value.path !== '/login') {
    void router.push('/login')
  }
}) as EventListener)

createApp(App).use(pinia).use(router).use(ElementPlus).mount('#app')
