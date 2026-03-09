<!-- src/App.vue -->
<template>
  <!-- 路由视图容器：所有页面组件都会渲染到这里 -->
  <router-view />
</template>

<style>
/* 全局基础样式，可替代你之前的 base.css */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html, body, #app {
  width: 100%;
  min-height: 100vh;
  font-family: "Microsoft Yahei", sans-serif;
  background-color: #ffffff;
}
</style>
