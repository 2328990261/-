import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 书库/首页等网格列数：随窗口宽度变化（不含后台管理）。
 * @param {number} desktopCols 大屏（≥1100px）时的列数，如 4、5
 */
export function useResponsiveBookColumns(desktopCols = 5) {
  const maxC = Math.min(Math.max(Number(desktopCols) || 5, 2), 6)
  const columns = ref(maxC)

  function update() {
    const w = typeof window !== 'undefined' ? window.innerWidth : 1200
    let n
    if (w < 520) n = 2
    else if (w < 720) n = Math.min(2, maxC)
    else if (w < 900) n = Math.min(3, maxC)
    else if (w < 1100) n = Math.min(4, maxC)
    else n = maxC
    columns.value = n
  }

  onMounted(() => {
    update()
    window.addEventListener('resize', update, { passive: true })
  })

  onUnmounted(() => {
    window.removeEventListener('resize', update)
  })

  return columns
}
