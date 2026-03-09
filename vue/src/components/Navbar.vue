<template>
  <nav class="navbar" :class="{ 'eye-protection-mode': eyeProtection }">
    <div class="navbar-left-group">
      <h1 class="logo">轻小说</h1>
      <div class="search-bar">
        <input type="text" placeholder="搜索书名/作者/ID" class="search-input" v-model="searchKeyword" @keyup.enter="handleSearch">
        <button type="button" class="search-btn" @click="handleSearchClick">
          <img src="../assets/搜索.png" alt="搜索" />
        </button>
      </div>
      <div class="nav-menu">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/library" class="nav-link">书库</router-link>
        <router-link to="/recommend" class="nav-link">推荐</router-link>
      </div>
    </div>

    <div class="navbar-right">
      <span v-if="isLoggedIn" class="user-info">您好, {{ username }}</span>
      <span v-else class="user-info">您好, 游客</span>
      <router-link v-if="!isLoggedIn" to="/login" class="nav-link">登录</router-link>
      <router-link v-if="!isLoggedIn" to="/register" class="nav-link">注册</router-link>
      <span v-if="isLoggedIn" class="divider">|</span>
      <!-- 个人中心按钮，登录后可见 -->
      <router-link v-if="isLoggedIn" to="/user/center" class="nav-link">个人中心</router-link>
      <span v-if="isLoggedIn" class="divider">|</span>
      <!-- 后台管理按钮，仅管理员可见 -->
      <router-link v-if="isLoggedIn && isAdmin" to="/admin" class="nav-link admin-btn">后台管理</router-link>
      <span v-if="isLoggedIn && isAdmin" class="divider">|</span>
      <button v-if="isLoggedIn" @click="handleLogout" class="nav-link logout-btn">退出登录</button>
    </div>
  </nav>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref('')
const searchKeyword = ref('')

// 定义组件属性
const props = defineProps({
  // 护眼模式状态
  eyeProtection: {
    type: Boolean,
    default: false
  }
})

const isLoggedIn = computed(() => {
  return !!localStorage.getItem('token')
})

// 判断是否为管理员
const isAdmin = computed(() => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr && userInfoStr !== 'undefined') {
    try {
      const parsedUserInfo = JSON.parse(userInfoStr)
      return parsedUserInfo.isAdmin === 1
    } catch (e) {
      console.error('解析用户信息失败：', e)
      localStorage.removeItem('userInfo')
      return false
    }
  }
  return false
})

onMounted(() => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr && userInfoStr !== 'undefined') {
    try {
      const parsedUserInfo = JSON.parse(userInfoStr)
      username.value = parsedUserInfo.username
    } catch (e) {
      console.error('解析用户信息失败：', e)
      localStorage.removeItem('userInfo')
      username.value = ''
    }
  }
})

const handleSearch = (e) => {
  if (e.key === 'Enter') doSearch()
}
const handleSearchClick = () => doSearch()
const doSearch = () => {
  const q = searchKeyword.value && searchKeyword.value.trim()
  if (!q) return
  router.push({ path: '/library', query: { keyword: q } })
}

const handleLogout = () => {
  // 清除所有登录相关信息
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  height: 60px;
  background: linear-gradient(135deg, #ffffff 0%, #ffe5f0 50%, #ffd6e8 100%);
  box-shadow: 0px 4px 8px rgba(255, 214, 232, 0.2);
  padding: 0 24px;
  color: #333;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 999;
}

/* 左侧紧凑区域：所有元素紧密排列，无空白 */
.navbar-left-group {
  display: flex;
  align-items: center;
  gap: 16px; /* 元素之间仅留16px间距，紧凑排列 */
}

.logo {
  font-size: 24px;
  font-weight: 900;
  margin: 0;
  color: #ff1493;
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Hiragino Sans GB', sans-serif;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(255, 20, 147, 0.2);
}

.search-bar {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: 24px;
  padding: 4px 12px;
  height: 36px;
  width: 220px;
}

.search-input {
  border: none;
  outline: none;
  background: transparent;
  flex: 1;
  font-size: 14px;
  color: #333;
}

.search-input::placeholder {
  color: #999;
}

.search-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  margin-left: 4px;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-btn img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* 导航菜单：首页/书库/推荐 紧凑排列 */
.nav-menu {
  display: flex;
  gap: 16px; /* 菜单之间仅留16px间距，和原来一致 */
}

/* 右侧用户信息：自动推到最右边，和左侧区域分开 */
.navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto; /* 关键：自动左移，推到最右边 */
}

.nav-link {
  color: #555;
  text-decoration: none;
  font-size: 16px;
  font-weight: 600;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  transition: all 0.3s;
}

.nav-link:hover,
.nav-link.active {
  color: #ff1493;
  border-bottom: 2px solid #ff1493;
  padding-bottom: 2px;
  transform: translateY(-1px);
}

.user-info {
  font-size: 14px;
  color: #555;
  font-weight: 500;
}

.divider {
  color: #ccc;
}

/* 护眼模式样式 */
.navbar.eye-protection-mode {
  background: #121212;
  color: #e0e0e0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
}

/* 退出登录按钮样式 */
.logout-btn {
  background: transparent;
  color: #555;
  border: 1px solid #ffb3d9;
  padding: 5px 14px;
  border-radius: 16px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  transition: all 0.3s;
}

.logout-btn:hover {
  background: #ffb3d9;
  color: #fff;
  border-color: #ffb3d9;
}

/* 后台管理按钮样式 */
.admin-btn {
  background: transparent;
  color: #555;
  border: 1px solid #ffd700;
  padding: 5px 14px;
  border-radius: 16px;
  font-weight: 500;
  transition: all 0.3s;
}

.admin-btn:hover {
  background: #ffd700;
  color: #fff;
  border-color: #ffd700;
  border-bottom: 1px solid #ffd700;
}

.navbar.eye-protection-mode .logo {
  color: #e0e0e0;
}

.navbar.eye-protection-mode .search-bar {
  background-color: #1e1e1e;
  border: 1px solid #333;
}

.navbar.eye-protection-mode .search-input {
  color: #e0e0e0;
}

.navbar.eye-protection-mode .search-input::placeholder {
  color: #888;
}

.navbar.eye-protection-mode .nav-link {
  color: #e0e0e0;
}

.navbar.eye-protection-mode .nav-link:hover,
.navbar.eye-protection-mode .nav-link.active {
  color: #fff;
  border-bottom-color: #fff;
}

.navbar.eye-protection-mode .user-info {
  color: rgba(224, 224, 224, 0.9);
}

.navbar.eye-protection-mode .divider {
  color: rgba(224, 224, 224, 0.7);
}

.navbar.eye-protection-mode .search-btn {
  color: #ff8c00;
}
</style>
