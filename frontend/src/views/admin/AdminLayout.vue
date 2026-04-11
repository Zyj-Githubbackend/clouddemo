<template>
  <div class="admin-shell">
    <el-container>
      <el-aside v-if="!isMobile" width="236px" class="aside">
        <div class="logo">
          <div class="logo-main">Admin Console</div>
          <div class="logo-sub">Campus Volunteer</div>
        </div>

        <el-menu :default-active="activeMenu" router class="menu" @select="handleMenuSelect">
          <el-menu-item index="/admin/announcements">公告管理</el-menu-item>
          <el-menu-item index="/admin/activities">活动管理</el-menu-item>
          <el-menu-item index="/admin/create">发布活动</el-menu-item>
          <el-menu-item index="/admin/checkin">活动签到</el-menu-item>
          <el-menu-item index="/admin/confirm">时长核销</el-menu-item>
          <el-menu-item index="/admin/hours">时长查询</el-menu-item>
          <el-menu-item index="/home">返回首页</el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="header">
          <div class="header-left">
            <el-button v-if="isMobile" text class="menu-btn" @click="menuDrawer = true">
              <el-icon :size="22"><Menu /></el-icon>
            </el-button>
            <div class="header-title">管理控制台</div>
          </div>

          <el-dropdown @command="handleCommand">
            <span class="user-pill">
              <el-icon><User /></el-icon>
              {{ userStore.userInfo.realName || '管理员' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-header>

        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <el-drawer v-model="menuDrawer" direction="ltr" :size="'min(84vw, 300px)'" :with-header="false">
      <div class="drawer-logo">
        <div class="logo-main">Admin Console</div>
        <div class="logo-sub">Campus Volunteer</div>
      </div>
      <el-menu :default-active="activeMenu" router class="drawer-menu" @select="handleMenuSelect">
        <el-menu-item index="/admin/announcements">公告管理</el-menu-item>
        <el-menu-item index="/admin/activities">活动管理</el-menu-item>
        <el-menu-item index="/admin/create">发布活动</el-menu-item>
        <el-menu-item index="/admin/checkin">活动签到</el-menu-item>
        <el-menu-item index="/admin/confirm">时长核销</el-menu-item>
        <el-menu-item index="/admin/hours">时长查询</el-menu-item>
        <el-menu-item index="/home">返回首页</el-menu-item>
      </el-menu>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const menuDrawer = ref(false)
const isMobile = ref(false)

const activeMenu = computed(() => route.path)

const checkMobile = () => {
  isMobile.value = window.innerWidth < 992
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const handleMenuSelect = () => {
  if (isMobile.value) {
    menuDrawer.value = false
  }
}

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  background: var(--cv-bg);
}

.aside {
  background: linear-gradient(168deg, #08245f, #0d3b9c 56%, #1e65da);
  color: #fff;
  box-shadow: 8px 0 24px rgba(0, 20, 83, 0.16);
}

.logo,
.drawer-logo {
  padding: 22px 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.16);
}

.drawer-logo {
  background: linear-gradient(168deg, #08245f, #0d3b9c 56%, #1e65da);
  color: #fff;
}

.logo-main {
  font-size: 20px;
  font-family: 'Manrope', 'Noto Sans SC', sans-serif;
  font-weight: 800;
}

.logo-sub {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.84;
  letter-spacing: 0.8px;
}

.menu,
.drawer-menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item),
.drawer-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.84);
  font-weight: 700;
  height: 44px;
  margin: 7px 10px;
  border-radius: 10px;
}

.menu :deep(.el-menu-item.is-active),
.menu :deep(.el-menu-item:hover),
.drawer-menu :deep(.el-menu-item.is-active),
.drawer-menu :deep(.el-menu-item:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.17);
}

.header {
  height: 62px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 clamp(12px, 2.2vw, 18px);
  border-bottom: 1px solid rgba(201, 214, 243, 0.58);
  background: rgba(244, 248, 255, 0.75);
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-btn {
  color: var(--cv-primary);
}

.header-title {
  font-size: clamp(16px, 2.2vw, 19px);
  font-weight: 800;
  color: #243152;
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 999px;
  background: #e9f0ff;
  color: #2c3656;
  font-weight: 700;
}

.main {
  padding: clamp(12px, 2.2vw, 18px);
}
</style>
