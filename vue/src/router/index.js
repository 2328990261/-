// 只需要一次导入核心方法
import { createRouter, createWebHistory } from 'vue-router'
// 导入页面组件
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import LibraryView from '@/views/LibraryView.vue'
import BookDetailView from '@/views/BookDetailView.vue'
import BookReadView from '@/views/BookReadView.vue'
import RecommendView from '@/views/RecommendView.vue'
import UserCenterView from '@/views/UserCenterView.vue'

// 保留原有AboutView的懒加载写法（更优，按需加载）
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 首页路由（原有）
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    // 新增登录页路由（核心）
    {
      path: '/login',
      name: 'login',
      component: LoginView // 也可以用懒加载：() => import('@/views/LoginView.vue')
    },
    // 新增注册路由
    {
      path: '/register',
      name: 'register',
      component: RegisterView
    },
    {
      path: '/library',
      name: 'library',
      component: LibraryView
    },
    // ========== 修复：启用/recommend路由（暂时重定向到首页，避免警告） ==========
    {
      path: '/recommend',
      name: 'recommend',
      component: RecommendView // 暂时重定向到首页，后续有组件再替换
    },
    // 小说详情页：动态参数id（匹配 /book/detail/1、/book/detail/2 等）
    {
      path: '/book/detail/:id',
      name: 'BookDetail',
      component: BookDetailView,
      props: true // 开启props传参，方便页面接收id
    },
    {
      path: '/book/read/:id',
      name: 'BookRead',
      component: BookReadView,
      props: true
    },
    // 后台管理（布局 + 子路由）
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/views/admin/AdminView.vue'),
      redirect: '/admin/dashboard',
      children: [
        { path: 'dashboard', name: 'adminDashboard', component: () => import('@/views/admin/AdminDashboard.vue') },
        { path: 'publish', name: 'adminPublish', component: () => import('@/views/admin/PublishView.vue') },
        { path: 'books', name: 'adminBooks', component: () => import('@/views/admin/BookManageView.vue') },
        { path: 'users', name: 'adminUsers', component: () => import('@/views/admin/UserManageView.vue') },
        { path: 'admins', name: 'adminManage', component: () => import('@/views/admin/AdminManageView.vue') },
        { path: 'recommend', name: 'adminRecommend', component: () => import('@/views/admin/RecommendConfigView.vue') },
        { path: 'config', name: 'adminConfig', component: () => import('@/views/admin/ConfigCenterView.vue') },
        { path: 'config/banner', name: 'AdminBannerManage', component: () => import('@/views/admin/BannerManageView.vue') },
        { path: 'settings', name: 'adminSettings', component: () => import('@/views/admin/TagManageView.vue') }
      ]
    },
    // 个人中心路由
    {
      path: '/user/center',
      name: 'userCenter',
      component: UserCenterView
    }

  ]
})

router.beforeEach((to, from, next) => {
  // 获取token和用户信息
  const token = localStorage.getItem('token')
  const userInfoStr = localStorage.getItem('userInfo')
  let userInfo = null
  
  try {
    if (userInfoStr && userInfoStr !== 'undefined') {
      userInfo = JSON.parse(userInfoStr)
    }
  } catch (e) {
    console.error('解析用户信息失败：', e)
    // 清除无效的用户信息
    localStorage.removeItem('userInfo')
  }

  // 公开路由，无需登录即可访问
  const publicRoutes = ['login', 'register', 'home', 'library', 'recommend']
  
  // 如果是公开路由，直接放行
  if (publicRoutes.includes(to.name)) {
    // 已登录用户访问登录/注册页时重定向到首页
    if (token && (to.name === 'login' || to.name === 'register')) {
      next('/')
    } else {
      next()
    }
    return
  }
  
  // 非公开路由，需要token验证
  if (!token) {
    next('/login')
    return
  }
  
  const adminRouteNames = [
    'admin',
    'adminDashboard',
    'adminPublish',
    'adminBooks',
    'adminUsers',
    'adminManage',
    'adminRecommend',
    'adminConfig',
    'AdminBannerManage',
    'adminSettings'
  ]
  if (adminRouteNames.includes(to.name)) {
    if (userInfo && userInfo.isAdmin === 1) {
      next()
    } else {
      next('/') // 非管理员重定向到首页
    }
    return
  }
  
  // 其他需要登录的路由，直接放行
  next()
})

export default router
