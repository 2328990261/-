

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
const app = createApp(App)

// 依次注册状态管理、路由与组件库
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
// 挂载到页面根节点
app.mount('#app')
