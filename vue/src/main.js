

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus' // 新增
import 'element-plus/dist/index.css' // 新增Element Plus样式

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus) // 新增注册
app.mount('#app')

