<template>
  <div class="recommend-hub">
    <router-link to="/admin/recommend/dashboard" class="dash-entry">
      <div class="dash-entry-glow" aria-hidden="true" />
      <div class="dash-entry-icon">
        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M21 21H4V4"/>
          <path d="M21 9l-5 5-4-4-3 3"/>
          <circle cx="9" cy="9" r="2"/>
        </svg>
      </div>
      <div class="dash-entry-body">
        <h2 class="dash-entry-title">仪表盘</h2>
        <p class="dash-entry-desc">查看全站标签热度、推荐权重分布与 Top 排行</p>
      </div>
      <div class="dash-entry-action">
        <span>进入</span>
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
      </div>
    </router-link>
  </div>
</template>

<script setup>
</script>

<style scoped>
.recommend-hub {
  max-width: 720px;
  padding: 8px 0 24px;
}

.dash-entry {
  position: relative;
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 28px 30px;
  background: #fff;
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 12px 40px rgba(99, 102, 241, 0.08), 0 2px 8px rgba(15, 23, 42, 0.04);
  text-decoration: none;
  color: inherit;
  overflow: hidden;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.dash-entry-glow {
  position: absolute;
  right: -40px;
  top: -40px;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(167, 139, 250, 0.28) 0%, transparent 68%);
  pointer-events: none;
  transition: opacity 0.22s ease;
}

.dash-entry:hover {
  transform: translateY(-3px);
  border-color: rgba(167, 139, 250, 0.55);
  box-shadow: 0 18px 48px rgba(124, 58, 237, 0.14), 0 4px 12px rgba(15, 23, 42, 0.06);
}

.dash-entry:hover .dash-entry-action {
  background: linear-gradient(135deg, #7c3aed 0%, #a855f7 100%);
  color: #fff;
  border-color: transparent;
}

.dash-entry-icon {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #6d28d9 0%, #a855f7 55%, #ec4899 100%);
  box-shadow: 0 8px 22px rgba(109, 40, 217, 0.32);
}

.dash-entry-body {
  flex: 1;
  min-width: 0;
}

.dash-entry-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 800;
  color: #312e81;
  letter-spacing: -0.02em;
}

.dash-entry-desc {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.55;
}

.dash-entry-action {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: #f8fafc;
  color: #6d28d9;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.22s ease, color 0.22s ease, border-color 0.22s ease;
}

@media (max-width: 640px) {
  .dash-entry {
    flex-wrap: wrap;
    padding: 22px 20px;
  }

  .dash-entry-action {
    width: 100%;
    justify-content: center;
  }
}
</style>
