<template>
  <div class="user-center">
    <Navbar />

    <div class="user-center-content">
      <h2>个人中心</h2>

      <div class="user-info-section">
        <div class="section-header">
          <h3>基本信息</h3>
          <button @click="openEditInfoDialog()" class="edit-info-btn">更改</button>
        </div>
        <div class="info-item">
          <label>用户名：</label>
          <span>{{ userInfo.username }}</span>
        </div>
        <div class="info-item">
          <label>邮箱：</label>
          <span>{{ userInfo.email || '未设置' }}</span>
        </div>
        <div class="info-item">
          <label>电话：</label>
          <span>{{ userInfo.phone || '未设置' }}</span>
        </div>
        <div class="info-item">
          <label>注册时间：</label>
          <span>{{ formatDate(userInfo.createdAt) || '未知' }}</span>
        </div>
      </div>

      <div class="user-preference-section">
        <div class="section-header">
          <h3>阅读偏好</h3>
          <div class="sort-buttons">
            <button 
              class="sort-btn" 
              :class="{ 'active': sortMode === 'collection' }"
              @click="switchToCollectionSort"
            >
              收藏排序
            </button>
            <button 
              class="sort-btn" 
              :class="{ 'active': sortMode === 'custom' }"
              @click="switchToCustomSort"
            >
              自定义排序
            </button>
          </div>
        </div>
        <div class="preference-tags">
          <span v-for="(tag, index) in displayTags" :key="tag" class="tag">
            {{ tag }}
            <button v-if="sortMode === 'custom'" @click="removePreferenceTag(tag)" class="tag-remove-btn">×</button>
          </span>
          <button 
            v-if="sortMode === 'custom'"
            @click="openAddTagDialog" 
            class="add-tag-btn"
          >
            + 添加标签
          </button>
        </div>
        <div class="preference-actions">
          <p v-if="sortMode === 'collection'" class="preference-hint">收藏排序根据您的收藏与阅读情况实时更新，无需保存</p>
          <button v-else @click="savePreferenceTags" class="save-tags-btn">
            保存偏好设置
          </button>
        </div>
      </div>

      <div class="user-behavior-section">
        <h3>阅读行为</h3>
        <div class="behavior-stats">
          <div class="stat-item stat-item-clickable" @click="switchToReadingTab">
            <div class="stat-value">{{ behaviorStats.collectionCount }}</div>
            <div class="stat-label">收藏小说</div>
          </div>
          <div class="stat-item stat-item-clickable" @click="switchToFinishedTab">
            <div class="stat-value">{{ behaviorStats.finishedCount }}</div>
            <div class="stat-label">完读小说</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ behaviorStats.readingTime }}</div>
            <div class="stat-label">阅读时长(小时)</div>
          </div>
        </div>
      </div>

      <div class="user-books-section">
        <h3>我的书架</h3>
        <div class="bookshelf-tabs">
          <button
            class="tab-btn"
            :class="{ 'active': activeTab === 'reading' }"
            @click="activeTab = 'reading'; pageSize = 10"
          >
            正在读
          </button>
          <button
            class="tab-btn"
            :class="{ 'active': activeTab === 'finished' }"
            @click="activeTab = 'finished'; pageSize = 10"
          >
            已看完
          </button>
        </div>
        <div class="books-grid">
          <div v-for="book in displayBooks" :key="book.id" class="book-item" @click="goToBookDetail(book.id)">
            <img :src="`http://localhost:8081/novel/cover/${encodeURIComponent(book.cover)}`" :alt="book.bookMainName" class="book-cover">
            <div class="book-info">
              <div class="book-name">{{ book.bookMainName }}</div>
              <div class="book-author">作者：{{ book.author }}</div>
            </div>
          </div>
        </div>
        <div v-if="filteredBooks.length === 0" class="empty-bookshelf">
          <p>{{ activeTab === 'reading' ? '书架为空，去收藏喜欢的小说吧！' : '暂无已完成的小说' }}</p>
        </div>
        <button
          v-else-if="displayBooks.length < filteredBooks.length"
          class="load-more-btn"
          @click="loadMore"
        >
          加载更多
        </button>
      </div>
    </div>

    <!-- 添加标签对话框 -->
    <div v-if="showAddTagDialog" class="dialog-overlay">
      <div class="dialog" style="width: 500px;">
        <h4>选择偏好标签</h4>
        <div class="tag-categories">
          <div class="category-section">
            <h5>五大标签（选择1个）</h5>
            <div class="category-tags">
              <span 
                v-for="tag in firstRowTags" 
                :key="tag" 
                class="category-tag"
                :class="{ 'selected': selectedTags.includes(tag) }"
                @click="toggleTagSelection(tag)"
              >
                {{ tag }}
              </span>
            </div>
          </div>
          <div class="category-section">
            <h5>其他标签（最多选择4个）</h5>
            <div class="category-tags">
              <span 
                v-for="tag in otherTags" 
                :key="tag" 
                class="category-tag"
                :class="{ 'selected': selectedTags.includes(tag) }"
                @click="toggleTagSelection(tag)"
              >
                {{ tag }}
              </span>
            </div>
          </div>
        </div>
        <div class="dialog-buttons">
          <button @click="closeAddTagDialog" class="btn cancel-btn">取消</button>
          <button @click="confirmAddTags" class="btn confirm-btn">确认</button>
        </div>
      </div>
    </div>

    <!-- 修改基本信息对话框 -->
    <div v-if="showEditInfoDialog" class="dialog-overlay">
      <div class="dialog">
        <h4>修改基本信息</h4>
        <div class="form-item">
          <label>用户名：</label>
          <input v-model="editForm.username" type="text" placeholder="请输入用户名" class="form-input">
        </div>
        <div class="form-item">
          <label>邮箱：</label>
          <input v-model="editForm.email" type="email" placeholder="请输入邮箱" class="form-input">
        </div>
        <div class="form-item">
          <label>电话：</label>
          <input v-model="editForm.phone" type="tel" placeholder="请输入电话" class="form-input">
        </div>
        <div class="dialog-buttons">
          <button @click="showEditInfoDialog = false" class="btn cancel-btn">取消</button>
          <button @click="saveUserInfo" class="btn confirm-btn">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import Navbar from '@/components/Navbar.vue'
import { getCollectionList } from '@/api/novel'
import { updateUserInfo, getUserInfo } from '@/api/auth'
import { getFinishedNovels, markNovelAsFinished, getTotalReadingDuration } from '@/api/behavior'

const router = useRouter()
const userInfo = ref({})
const userPreferenceTags = ref(['日常', '奇幻', '冒险']) // 收藏排序的标签
const customTags = ref([]) // 自定义排序的标签
const sortMode = ref('collection') // 当前排序模式：collection 或 custom
const behaviorStats = ref({
  collectionCount: 0,
  finishedCount: 0,
  commentCount: 8,
  readingTime: 24
})
const collectedBooks = ref([])
const sortedBooks = ref([])
const finishedNovels = ref([])
const activeTab = ref('reading') // 默认为正在读
const pageSize = ref(10) // 初始显示10个，一行5个，显示2行
const showAddTagDialog = ref(false)
const newTag = ref('')
const selectedTags = ref([])

// 定义标签分类
const firstRowTags = ['日常', '奇幻', '校园', '冒险', '异世界'] // 五大标签
const otherTags = [
  '轻松', '搞笑', '治愈', '致郁', '甜宠', '热血', '恋爱', '成长',
  '智斗', '悬疑', '推理', '心理惊悚', '战斗', '竞技', '基建',
  '宫廷', '虚拟网游', '现实题材', '种田文', '转生', '穿越', '魔法',
  '全年龄', '轻百'
]

// 显示的标签，根据当前排序模式切换
const displayTags = computed(() => {
  return sortMode.value === 'collection' ? userPreferenceTags.value : customTags.value
})
const showEditInfoDialog = ref(false)
const editForm = ref({
  username: '',
  email: '',
  phone: ''
})

// 计算已完成的小说列表（从收藏的小说中过滤）
const finishedBooks = computed(() => {
  return sortedBooks.value.filter(book => finishedNovels.value.includes(String(book.id)))
})

// 计算当前显示的书籍
const displayBooks = computed(() => {
  const books = activeTab.value === 'reading' ? sortedBooks.value : finishedBooks.value
  return books.slice(0, pageSize.value)
})

// 计算当前过滤后的书籍
const filteredBooks = computed(() => {
  return activeTab.value === 'reading' ? sortedBooks.value : finishedBooks.value
})

// 保存书架排序到 localStorage
const saveBookshelfOrder = () => {
  const bookIds = sortedBooks.value.map(book => String(book.id))
  localStorage.setItem('bookshelfOrder', JSON.stringify(bookIds))
  console.log('书架排序已保存:', bookIds)
}

// 从 localStorage 加载书架排序
const loadBookshelfOrder = () => {
  try {
    const savedOrder = JSON.parse(localStorage.getItem('bookshelfOrder') || '[]')
    if (savedOrder.length > 0 && collectedBooks.value.length > 0) {
      // 根据保存的顺序重新排序
      const orderedBooks = []
      const remainingBooks = [...collectedBooks.value]

      // 先按保存的顺序添加
      for (const id of savedOrder) {
        const bookIndex = remainingBooks.findIndex(book => String(book.id) === String(id))
        if (bookIndex > -1) {
          orderedBooks.push(remainingBooks.splice(bookIndex, 1)[0])
        }
      }

      // 再添加剩余的书籍
      orderedBooks.push(...remainingBooks)

      sortedBooks.value = orderedBooks
      console.log('书架排序已加载:', orderedBooks.map(book => book.id))
    } else {
      // 如果没有保存的排序，使用默认顺序
      sortedBooks.value = [...collectedBooks.value]
    }
  } catch (error) {
    console.error('加载书架排序失败:', error)
    sortedBooks.value = [...collectedBooks.value]
  }
}

onMounted(async () => {
  await loadUserInfo()
  await loadFinishedNovels()
  await loadCollectedBooks().then(() => {
    // 加载保存的排序
    loadBookshelfOrder()
    // 检查最近点击的小说
    checkLastClickedNovel()
    loadUserBehaviors()
    // 加载用户偏好标签（从数据库加载）
    loadUserPreferenceTagsFromDB()
  })
})

// 检查最近点击的小说并调整排序
const checkLastClickedNovel = () => {
  const lastClickedNovelId = localStorage.getItem('lastClickedNovelId')
  if (lastClickedNovelId && sortedBooks.value.length > 0) {
    reorderBookshelf(lastClickedNovelId)
    // 清除记录，避免重复调整
    localStorage.removeItem('lastClickedNovelId')
  }
}

const loadUserInfo = async () => {
  // 从本地存储获取用户信息
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    try {
      userInfo.value = JSON.parse(userInfoStr)
      console.log('加载的用户信息:', userInfo.value)
      
      // 如果没有createdAt字段，从后端获取最新的用户信息
      if (!userInfo.value.createdAt) {
        console.log('用户信息中没有createdAt字段，从后端获取')
        try {
          const res = await getUserInfo(userInfo.value.username)
          if (res.code === 200) {
            console.log('从后端获取的用户信息:', res.data)
            // 更新本地存储和状态
            localStorage.setItem('userInfo', JSON.stringify(res.data))
            userInfo.value = res.data
          }
        } catch (error) {
          console.error('从后端获取用户信息失败:', error)
        }
      }
    } catch (e) {
      console.error('解析用户信息失败', e)
    }
  }
}

// 打开编辑对话框
const openEditInfoDialog = () => {
  // 初始化表单数据
  editForm.value = {
    username: userInfo.value.username || '',
    email: userInfo.value.email || '',
    phone: userInfo.value.phone || ''
  }
  showEditInfoDialog.value = true
}

// 保存用户信息
const saveUserInfo = async () => {
  try {
    console.log('保存用户信息:', editForm.value)

    // 调用API更新用户信息
    const userId = userInfo.value.id
    if (!userId) {
      throw new Error('用户ID不存在')
    }

    const res = await updateUserInfo(userId, editForm.value.username, editForm.value.email, editForm.value.phone)

    if (res.code === 200) {
      // 更新本地存储的用户信息
      const updatedUserInfo = {
        ...userInfo.value,
        ...editForm.value
      }
      localStorage.setItem('userInfo', JSON.stringify(updatedUserInfo))

      // 更新本地状态
      userInfo.value = updatedUserInfo
      console.log('更新后的用户信息:', userInfo.value)

      // 关闭对话框
      showEditInfoDialog.value = false

      // 提示用户保存成功
      alert('信息保存成功！')
    } else {
      throw new Error(res.msg || '保存失败')
    }
  } catch (error) {
    console.error('保存用户信息失败:', error)
    alert('保存失败，请重试')
  }
}

// 保存阅读偏好标签到数据库
const savePreferenceTags = async () => {
  try {
    const userId = userInfo.value.id
    const currentMode = sortMode.value  // 当前模式：collection 或 custom
    const tagsToSave = currentMode === 'collection' ? userPreferenceTags.value : customTags.value
    
    if (tagsToSave.length === 0) {
      alert('请至少添加一个标签')
      return
    }
    
    // 构建标签数据，添加tagType字段
    const tagData = tagsToSave.map((tagName, index) => ({
      userId: userId,
      tagName: tagName,
      tagType: currentMode,  // 标记是收藏标签还是自定义标签
      tagOrder: index + 1,
      isFirstRow: currentMode === 'collection' ? (index < 5 ? 1 : 0) : 0
    }))
    
    console.log('=== 保存标签调试信息 ===')
    console.log('当前模式:', currentMode)
    console.log('保存的标签:', tagsToSave)
    console.log('标签数据:', tagData)
    
    // 调用API保存标签，传递tagType参数
    const response = await axios.post(
      `http://localhost:8081/api/user/behavior/preference/tags?userId=${userId}&tagType=${currentMode}`, 
      tagData
    )
    
    if (response.data) {
      alert(`${currentMode === 'collection' ? '收藏排序' : '自定义排序'}标签保存成功！`)
    } else {
      alert('保存失败，请重试')
    }
  } catch (error) {
    console.error('保存标签失败:', error)
    alert('保存失败：' + (error.response?.data?.msg || error.message))
  }
}

// 从数据库加载用户偏好标签
const loadUserPreferenceTagsFromDB = async () => {
  try {
    const userId = userInfo.value.id
    if (!userId) return
    
    // 加载收藏排序标签
    const collectionResponse = await axios.get(
      `http://localhost:8081/api/user/behavior/preference/tags?userId=${userId}`
    )
    
    if (collectionResponse.data && Array.isArray(collectionResponse.data)) {
      // 分离收藏标签和自定义标签
      const collectionTags = collectionResponse.data
        .filter(tag => tag.tagType === 'collection')
        .sort((a, b) => a.tagOrder - b.tagOrder)
        .map(tag => tag.tagName)
      
      const customTagsList = collectionResponse.data
        .filter(tag => tag.tagType === 'custom')
        .sort((a, b) => a.tagOrder - b.tagOrder)
        .map(tag => tag.tagName)
      
      // 如果有收藏标签,使用数据库的;否则统计生成
      if (collectionTags.length > 0) {
        userPreferenceTags.value = collectionTags
      } else {
        // 如果数据库没有收藏标签,从收藏书籍统计生成
        loadUserPreferences()
      }
      
      // 设置自定义标签
      if (customTagsList.length > 0) {
        customTags.value = customTagsList
      }
    } else {
      // 如果数据库没有标签,从收藏书籍统计生成收藏标签
      loadUserPreferences()
    }
  } catch (error) {
    console.error('加载用户偏好标签失败:', error)
    // 如果加载失败,从收藏书籍统计生成
    loadUserPreferences()
  }
}

const loadUserPreferences = () => {
  // 定义标签分类
  const firstRowTags = ['日常', '奇幻', '校园', '冒险', '异世界'] // 第一行标签
  const allTags = [
    '日常', '奇幻', '校园', '冒险', '异世界', // 第一行
    '轻松', '搞笑', '治愈', '致郁', '甜宠', '热血', '恋爱', '成长', // 第二行
    '智斗', '悬疑', '推理', '心理惊悚', '战斗', '竞技', '基建', // 第三行
    '宫廷', '虚拟网游', '现实题材', '种田文', '转生', '穿越', '魔法', // 第四行
    '全年龄', '轻百' // 第五行
  ]
  
  // 从收藏书籍中统计标签
  if (collectedBooks.value.length > 0) {
    const tagCount = {}
    
    // 遍历所有收藏书籍，统计所有标签出现次数
    collectedBooks.value.forEach(book => {
      if (book.label) {
        // 假设标签是用逗号分隔的字符串，统计所有标签
        const tags = book.label.split(',').map(tag => tag.trim()).filter(Boolean)
        tags.forEach(tag => {
          tagCount[tag] = (tagCount[tag] || 0) + 1
        })
      }
    })
    
    console.log('=== 标签统计调试信息 ===')
    console.log('收藏书籍数量:', collectedBooks.value.length)
    console.log('所有标签出现次数:', tagCount)
    
    // 首先处理第一行标签，按出现次数排序
    const firstRowTagCount = firstRowTags
      .filter(tag => tagCount[tag]) // 只保留在收藏中出现过的第一行标签
      .sort((a, b) => (tagCount[b] || 0) - (tagCount[a] || 0))
    
    console.log('第一行标签统计:', firstRowTagCount)
    
    // 然后处理其他标签，按出现次数排序，确保排除第一行标签
    const otherTagCount = allTags
      .filter(tag => !firstRowTags.includes(tag) && tagCount[tag]) // 明确排除第一行标签，只保留在收藏中出现过的其他标签
      .sort((a, b) => (tagCount[b] || 0) - (tagCount[a] || 0))
    
    console.log('其他标签统计:', otherTagCount)
    console.log('其他标签数量:', otherTagCount.length)
    
    // 组合标签：严格按照要求排序
    const combinedTags = []
    
    // 1. 首先添加第一行标签中出现次数最多的标签作为第一个标签
    if (firstRowTagCount.length > 0) {
      combinedTags.push(firstRowTagCount[0])
    }
    
    // 2. 然后添加其他标签中出现次数最多的标签，排除第一行标签，最多4个
    let addedOtherTags = 0
    for (const tag of otherTagCount) {
      if (combinedTags.length < 5) {
        combinedTags.push(tag)
        addedOtherTags++
      } else {
        break
      }
    }
    
    console.log('已添加的其他标签数量:', addedOtherTags)
    
    // 3. 如果其他标签不足4个，从第一行剩余标签中按出现次数排序补充
    if (combinedTags.length < 5) {
      // 遍历第一行标签，按出现次数排序，跳过已添加的标签
      for (const tag of firstRowTagCount) {
        if (combinedTags.length < 5 && !combinedTags.includes(tag)) {
          combinedTags.push(tag)
        }
      }
    }
    
    // 4. 如果仍然不足5个，从默认标签中补充
    if (combinedTags.length < 5) {
      const defaultTags = ['日常', '奇幻', '冒险', '异世界', '轻松']
      for (const tag of defaultTags) {
        if (combinedTags.length < 5 && !combinedTags.includes(tag)) {
          combinedTags.push(tag)
        }
      }
    }
    
    console.log('最终标签组合:', combinedTags)
    
    userPreferenceTags.value = combinedTags
    console.log('统计的偏好标签:', userPreferenceTags.value)
  } else {
    // 如果没有收藏书籍，使用默认标签
    userPreferenceTags.value = ['日常', '奇幻', '冒险', '异世界', '轻松']
  }
}

// 获取当前用户 ID（与个人中心其它接口一致）
const getCurrentUserId = () => {
  if (userInfo.value?.id) return userInfo.value.id
  try {
    const uid = localStorage.getItem('userId')
    return uid ? JSON.parse(uid) : null
  } catch {
    return null
  }
}

// 加载已完成的小说列表（从后端获取，并可选将本地完读同步到后端）
const loadFinishedNovels = async () => {
  const userId = getCurrentUserId()
  if (!userId) {
    try {
      const list = JSON.parse(localStorage.getItem('finishedNovels') || '[]')
      finishedNovels.value = list
    } catch {
      finishedNovels.value = []
    }
    return
  }
  try {
    const res = await getFinishedNovels(userId)
    const list = (res.data || []).map((item) => String(item.novelId))
    finishedNovels.value = list
    // 将本地已完读但尚未同步到后端的也写入后端（仅做一次迁移）
    const localList = JSON.parse(localStorage.getItem('finishedNovels') || '[]')
    for (const novelId of localList) {
      if (!list.includes(String(novelId))) {
        try {
          await markNovelAsFinished(userId, novelId)
          finishedNovels.value = [...finishedNovels.value, String(novelId)]
        } catch (_) {}
      }
    }
  } catch (e) {
    console.error('加载完读列表失败', e)
    try {
      finishedNovels.value = JSON.parse(localStorage.getItem('finishedNovels') || '[]')
    } catch {
      finishedNovels.value = []
    }
  }
}

// 计算已完成且已收藏的小说数量
const finishedCount = computed(() => {
  return sortedBooks.value.filter(book => finishedNovels.value.includes(String(book.id))).length
})

const switchToReadingTab = () => {
  activeTab.value = 'reading'
  pageSize.value = 10
}
const switchToFinishedTab = () => {
  activeTab.value = 'finished'
  pageSize.value = 10
}

const loadUserBehaviors = async () => {
  const userId = getCurrentUserId()
  let totalHours = 0
  if (userId) {
    try {
      const res = await getTotalReadingDuration(userId)
      const totalSeconds = typeof res === 'number' ? res : (res?.data ?? res ?? 0)
      totalHours = Math.floor(Number(totalSeconds) / 3600)
    } catch (_) {}
  }
  behaviorStats.value = {
    collectionCount: sortedBooks.value.length,
    finishedCount: finishedCount.value,
    commentCount: behaviorStats.value?.commentCount ?? 0,
    readingTime: totalHours
  }
}

const loadCollectedBooks = async () => {
  const userId = JSON.parse(localStorage.getItem('userId') || '1')
  try {
    const res = await getCollectionList(userId)
    if (res.code === 200) {
      collectedBooks.value = res.data
    }
  } catch (error) {
    console.error('获取收藏列表失败:', error)
    collectedBooks.value = []
  }
  return collectedBooks.value
}

const removePreferenceTag = (tag) => {
  if (sortMode.value === 'collection') {
    const index = userPreferenceTags.value.indexOf(tag)
    if (index > -1) {
      userPreferenceTags.value.splice(index, 1)
    }
  } else {
    const index = customTags.value.indexOf(tag)
    if (index > -1) {
      customTags.value.splice(index, 1)
    }
  }
}

const addPreferenceTag = () => {
  if (newTag.value.trim()) {
    // 检查标签数量是否已达上限
    if (userPreferenceTags.value.length >= 5) {
      alert('最多只能添加5个标签')
      return
    }
    // 检查标签是否已存在
    if (userPreferenceTags.value.includes(newTag.value.trim())) {
      alert('标签已存在')
      return
    }
    userPreferenceTags.value.push(newTag.value.trim())
    newTag.value = ''
    showAddTagDialog.value = false
  }
}

// 切换标签选择状态
const toggleTagSelection = (tag) => {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    // 取消选择
    selectedTags.value.splice(index, 1)
  } else {
    // 检查选择规则
    const isFirstRowTag = firstRowTags.includes(tag)
    
    if (isFirstRowTag) {
      // 五大标签只能选择一个，先清空其他五大标签的选择
      selectedTags.value = selectedTags.value.filter(t => !firstRowTags.includes(t))
      // 将五大标签添加到第一个位置
      selectedTags.value.unshift(tag)
    } else {
      // 其他标签最多选择4个
      const otherSelectedCount = selectedTags.value.filter(t => !firstRowTags.includes(t)).length
      if (otherSelectedCount < 4) {
        selectedTags.value.push(tag)
      } else {
        alert('其他标签最多只能选择4个')
        return
      }
    }
  }
  
  // 实时更新页面上的标签（保持选择顺序）
  customTags.value = [...selectedTags.value]
  
  console.log('当前选择的标签顺序:', selectedTags.value)
}

// 保存打开弹窗前的标签状态，用于取消时恢复
let beforeOpenTags = []

// 打开添加标签对话框
const openAddTagDialog = () => {
  // 保存当前标签状态
  beforeOpenTags = [...customTags.value]
  // 同步当前自定义标签状态
  selectedTags.value = [...customTags.value]
  showAddTagDialog.value = true
}

// 关闭添加标签对话框
const closeAddTagDialog = () => {
  // 恢复到打开弹窗前的状态
  customTags.value = [...beforeOpenTags]
  selectedTags.value = []
  showAddTagDialog.value = false
}

// 切换到收藏排序
const switchToCollectionSort = () => {
  sortMode.value = 'collection'
}

// 切换到自定义排序
const switchToCustomSort = () => {
  sortMode.value = 'custom'
}

// 确认添加标签
const confirmAddTags = () => {
  if (selectedTags.value.length === 0) {
    alert('请至少选择一个标签')
    return
  }
  
  // 保持当前选择状态
  customTags.value = [...selectedTags.value]
  
  // 关闭对话框
  selectedTags.value = []
  showAddTagDialog.value = false
}

const goToBookDetail = (bookId) => {
  // 记录最近点击的小说 ID 到 localStorage
  localStorage.setItem('lastClickedNovelId', String(bookId))
  // 调整书架排序，将点击的小说移到第一个位置
  reorderBookshelf(bookId)
  router.push(`/book/detail/${bookId}`)
}

// 调整书架排序，将指定小说移到第一个位置
const reorderBookshelf = (bookId) => {
  const bookIndex = sortedBooks.value.findIndex(book => String(book.id) === String(bookId))
  if (bookIndex > 0) {
    // 移除并重新添加到第一个位置
    const book = sortedBooks.value.splice(bookIndex, 1)[0]
    sortedBooks.value.unshift(book)
    console.log('书架排序已调整，小说已移到第一个位置:', bookId)
    // 保存排序状态
    saveBookshelfOrder()
  }
}

// 加载更多方法
const loadMore = () => {
  pageSize.value += 10 // 每次加载10个，即2行
}

// 格式化日期，只显示年月日
const formatDate = (dateString) => {
  if (!dateString) return ''
  // 检查是否是ISO格式的日期字符串
  if (typeof dateString === 'string' && dateString.includes('T')) {
    // 提取年月日部分
    return dateString.split('T')[0]
  }
  return dateString
}

</script>

<style scoped>
.user-center {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-top: 70px;
}

.user-center-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

h2 {
  color: #333;
  margin-bottom: 24px;
  font-size: 24px;
}

h3 {
  color: #666;
  margin: 24px 0 16px;
  font-size: 18px;
  border-bottom: 1px solid #e8e8e8;
  padding-bottom: 8px;
}

.user-info-section,
.user-preference-section,
.user-behavior-section,
.user-books-section {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  border-bottom: none;
  padding-bottom: 0;
}

.edit-info-btn {
  background-color: #f8a555;
  color: #fff;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.edit-info-btn:hover {
  background-color: #e79544;
}

.info-item {
  display: flex;
  margin-bottom: 12px;
}

.info-item label {
  width: 100px;
  font-weight: 500;
  color: #666;
}

.info-item span {
  color: #333;
}

.form-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.form-item label {
  width: 80px;
  font-weight: 500;
  color: #666;
}

.form-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
}

.form-input:focus {
  outline: none;
  border-color: #f8a555;
  box-shadow: 0 0 0 2px rgba(248, 165, 85, 0.1);
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tag {
  display: inline-flex;
  align-items: center;
  background-color: #f0f0f0;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 14px;
  color: #666;
}

.tag-remove-btn {
  margin-left: 8px;
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
  font-size: 16px;
  line-height: 1;
}

.add-tag-btn {
  background-color: #f8a555;
  color: #fff;
  border: none;
  padding: 6px 12px;
  border-radius: 16px;
  cursor: pointer;
  font-size: 14px;
}

/* 排序按钮样式 */
.sort-buttons {
  display: flex;
  gap: 12px;
}

.sort-btn {
  padding: 8px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  background-color: #fff;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.sort-btn:hover {
  border-color: #f8a555;
  color: #f8a555;
}

.sort-btn.active {
  background-color: #f8a555;
  color: #fff;
  border-color: #f8a555;
}

/* 标签分类样式 */
.tag-categories {
  margin-bottom: 20px;
}

.category-section {
  margin-bottom: 16px;
}

.category-section h5 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-tag {
  display: inline-block;
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 16px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-tag:hover {
  border-color: #f8a555;
  color: #f8a555;
}

.category-tag.selected {
  background-color: #f8a555;
  color: #fff;
  border-color: #f8a555;
}

.behavior-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 20px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background-color: #f9f9f9;
  border-radius: 8px;
}
.stat-item-clickable {
  cursor: pointer;
}
.stat-item-clickable:hover {
  background-color: #f0f0f0;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #f8a555;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.bookshelf-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.tab-btn {
  padding: 10px 24px;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  background-color: #fff;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  border-color: #f8a555;
  color: #f8a555;
}

.tab-btn.active {
  background-color: #f8a555;
  color: #fff;
  border-color: #f8a555;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.book-item {
  text-align: center;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  cursor: pointer;
  box-sizing: border-box;
  position: relative;
}

.book-item:hover {
  transform: translateY(-6px) scale(1.03);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
  border-color: #f0b075;
  z-index: 10;
}

.book-cover {
  width: 120px;
  height: 180px;
  object-fit: cover;
  border-radius: 4px;
  margin-bottom: 8px;
  transition: transform 0.3s ease;
}

.book-item:hover .book-cover {
  transform: scale(1.02);
}

.book-info {
  width: 100%;
}

.book-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-author {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.load-more-btn {
  margin-top: 20px;
  padding: 10px 24px;
  background-color: #f8a555;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.load-more-btn:hover {
  background-color: #e79544;
}

.load-more-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  width: 400px;
  max-width: 90%;
}

.dialog h4 {
  margin-top: 0;
  margin-bottom: 16px;
  color: #333;
}

.tag-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  margin-bottom: 16px;
}

.dialog-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn {
  background-color: #f0f0f0;
  color: #333;
}

.confirm-btn {
  background-color: #f8a555;
  color: #fff;
}

.empty-bookshelf {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  background-color: #f9f9f9;
  border-radius: 8px;
  margin-top: 20px;
}

.empty-bookshelf p {
  margin: 0;
  font-size: 16px;
}
</style>



.preference-actions {
  margin-top: 20px;
  text-align: center;
}

.preference-hint {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.save-tags-btn {
  padding: 10px 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.save-tags-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}
