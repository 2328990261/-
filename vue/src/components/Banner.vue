<template>
  <div class="banner-wrapper">
    <el-carousel
      v-if="localBannerList.length > 0"
      height="100%"
      indicator-position="outside"
      class="carousel-container"
    >
      <el-carousel-item v-for="item in localBannerList" :key="item.id" class="carousel-item">
        <div class="banner-img-wrapper">
          <img
            v-show="true"
            :src="item.image"
            :alt="item.title"
            class="banner-img"
          >
        </div>
        <!-- ←【留白处】在这里填写轮播图标题文字 -->
        <div class="banner-title">
          {{ item.title || '' }}
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

const localBannerList = ref([...props.bannerList])

watch(
  () => props.bannerList,
  (newVal) => {
    localBannerList.value = [...newVal]
  },
  { deep: true, immediate: true }
)
</script>

<style scoped>
/* 外层容器：固定轮播图区域尺寸 */
.banner-wrapper {
  width: 100%;
  height: 100%; /* 继承父容器高度 */
  border-radius: 12px;
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
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 核心修改：图片铺满容器，裁剪多余部分 */
.banner-img {
  width: 100%; /* 图片宽度铺满容器 */
  height: 100%; /* 图片高度铺满容器 */
  object-fit: cover; /* 铺满容器，裁剪多余部分，不变形 */
  object-position: center; /* 图片居中 */
}

/* 标题样式 */
.banner-title {
  position: absolute;
  bottom: 20px;
  left: 20px;
  color: white;
  font-size: 18px;
  text-shadow: 0 0 8px rgba(0, 0, 0, 0.8);
  z-index: 10;
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
</style>
