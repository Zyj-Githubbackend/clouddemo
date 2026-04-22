<template>
  <router-view />
</template>

<script setup>
</script>

<style>
body {
  margin: 0;
  color: var(--cv-text);
  background: var(--cv-bg);
  font-family: 'Manrope', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  line-height: 1.5;
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

html {
  -webkit-text-size-adjust: 100%;
  text-size-adjust: 100%;
  overflow-x: hidden;
}

#app {
  min-height: 100vh;
}

.page-container {
  width: min(1240px, 100%);
  margin: 0 auto;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.table-scroll > .el-table {
  min-width: 760px;
}
.text-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.text-ellipsis-2 {
  overflow: hidden;
  display: -webkit-box;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.text-ellipsis-3 {
  overflow: hidden;
  display: -webkit-box;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}
</style>


