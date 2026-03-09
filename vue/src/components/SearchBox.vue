<template>
  <div class="search-box">
    <input type="text" placeholder="输入作品名/作者名" class="search-input" />
    <button class="search-btn">🔍</button>
  </div>
</template>

<style scoped>
.search-box {
  display: flex;
  max-width: 600px;
  margin: 0 auto 24px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  background-color: #ffffff;
}
.search-input {
  flex: 1;
  height: 40px;
  padding: 0 16px;
  border: none;
  outline: none;
  font-size: 14px;
  background-color: #ffffff;
}
.search-btn {
  width: 40px;
  height: 40px;
  border: none;
  background-color: #f8a555;
  color: #fff;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
}
.search-btn:hover {
  background-color: #e89030;
}
</style>
