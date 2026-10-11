<template>
  <div class="banner-wrapper">
    <el-carousel
      v-if="localBannerList.length > 0"
      height="100%"
      indicator-position="inside"
      :interval="5000"
      class="carousel-container"
    >
      <el-carousel-item v-for="item in localBannerList" :key="item.id" class="carousel-item">
        <div class="banner-img-wrapper">
          <img :src="item.image" :alt="item.title" class="banner-img">
        </div>
        <!-- 轮播图文字：白色 + 靠左显示 -->
        <div class="banner-content">
          <h2 class="banner-title">{{ item.title || '' }}</h2>
          <button
            v-if="canRead(item)"
            type="button"
            class="banner-read-btn"
            @click="$emit('select', item)"
          >立即阅读</button>
        </div>
      </el-carousel-item>
    </el-carousel>
    <div v-else class="no-data-tip">
      暂无轮播数据 | 错误信息：{{ errorMsg }}
    </div>
  </div>
</template>

<script setup>
import { ref, watch, defineProps } from 'vue'

const props = defineProps({
  bannerList: {
    type: Array,
    default: () => []
  },
  errorMsg: {
    type: String,
    default: ''
  }
})

defineEmits(['select'])

const localBannerList = ref([...props.bannerList])

/** 轮播项可跳转的条件：绑定了小说，或配置了自定义链接 */
function canRead(item) {
  return item && (item.novelId != null || !!item.linkUrl)
}

watch(
  () => props.bannerList,
  (newVal) => {
    localBannerList.value = [...newVal]
  },
  { deep: true, immediate: true }
)
</script>

<style scoped>
/* 外层容器：全屏轮播区域（继承父容器尺寸） */
.banner-wrapper {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* 轮播容器：继承外层高度 */
.carousel-container {
  height: 100% !important;
}

/* 轮播项：占满容器 */
.carousel-item {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center; /* 图片垂直居中 */
  justify-content: center; /* 图片水平居中 */
}

/* 图片容器：仅作为承载，居中显示图片 */
.banner-img-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 左侧暗色渐变遮罩：保证白色文字可读 */
.banner-img-wrapper::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 2;
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.5) 0%, rgba(0, 0, 0, 0.22) 45%, rgba(0, 0, 0, 0) 75%);
  pointer-events: none;
}

/* 图片铺满容器，裁剪多余部分 */
.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

/* 文字 + 阅读按钮：整体靠左、垂直居中 */
.banner-content {
  position: absolute;
  left: 6%;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 24px;
  max-width: min(560px, 72%);
  pointer-events: none; /* 不拦截轮播拖拽，仅按钮可点击 */
}

/* 标题：白色 + 靠左显示 */
.banner-title {
  margin: 0;
  color: white;
  font-size: 36px;
  font-weight: 700;
  line-height: 1.3;
  text-align: left;
  text-shadow: 0 2px 18px rgba(0, 0, 0, 0.5);
}

/* 阅读按钮：位于标题下方，正常文档流 */
.banner-read-btn {
  pointer-events: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 13px 40px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff1493 0%, #ff6fb5 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  font-family: inherit;
  letter-spacing: 1px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(255, 20, 147, 0.4);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.banner-read-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(255, 20, 147, 0.5);
}

.banner-read-btn:active {
  transform: translateY(0);
}

.no-data-tip {
  text-align: center;
  padding: 50px;
  color: #999;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ========== 移动端响应式 ========== */
@media (max-width: 768px) {
  .banner-content {
    left: 7%;
    gap: 16px;
  }

  .banner-title {
    font-size: 22px;
  }

  .banner-read-btn {
    padding: 10px 26px;
    font-size: 14px;
  }

  .no-data-tip {
    padding: 24px 16px;
    font-size: 13px;
  }
}
</style>
