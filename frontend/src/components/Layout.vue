<template>
  <div class="layout">
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="logo">
            <el-icon :size="24" color="#667eea"><Trophy /></el-icon>
          </div>
          <h2 class="site-title">志愿服务平台</h2>
        </div>
        
        <!-- 桌面端菜单 -->
        <el-menu
          v-if="!isMobile"
          :default-active="activeMenu"
          mode="horizontal"
          :ellipsis="false"
          @select="handleMenuSelect"
          class="desktop-menu"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/activities">
            <el-icon><Flag /></el-icon>
            <span>志愿活动</span>
          </el-menu-item>
          <el-menu-item index="/my">
            <el-icon><Stamp /></el-icon>
            <span>我的志愿足迹</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin/activities">
            <el-icon><Setting /></el-icon>
            <span>管理后台</span>
          </el-menu-item>
        </el-menu>
        
        <div class="header-right">
          <el-dropdown @command="handleCommand" v-if="!isMobile">
            <span class="user-info">
              <el-avatar :size="32" :style="{ background: '#667eea' }">
                {{ userStore.userInfo.realName?.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo.realName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item command="myRegistrations" divided>
                  <el-icon><List /></el-icon>
                  我的报名
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          
          <!-- 移动端菜单按钮 -->
          <el-button v-if="isMobile" text @click="drawer = true">
            <el-icon :size="24"><Menu /></el-icon>
          </el-button>
        </div>
      </el-header>
      
      <el-main class="main">
        <slot />
      </el-main>
      
      <!-- 移动端抽屉菜单 -->
      <el-drawer v-model="drawer" direction="rtl" :size="280">
        <template #header>
          <div class="drawer-header">
            <el-avatar :size="60" :style="{ background: '#667eea' }">
              {{ userStore.userInfo.realName?.charAt(0) }}
            </el-avatar>
            <div class="drawer-user-info">
              <div class="drawer-user-name">{{ userStore.userInfo.realName }}</div>
              <div class="drawer-user-role">{{ userStore.isAdmin ? '管理员' : '志愿者' }}</div>
            </div>
          </div>
        </template>
        
        <el-menu :default-active="activeMenu" @select="handleMobileMenuSelect">
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/activities">
            <el-icon><Flag /></el-icon>
            <span>志愿活动</span>
          </el-menu-item>
          <el-menu-item index="/my">
            <el-icon><Stamp /></el-icon>
            <span>我的志愿足迹</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin/activities">
            <el-icon><Setting /></el-icon>
            <span>管理后台</span>
          </el-menu-item>
          <el-divider />
          <el-menu-item @click="handleCommand('myRegistrations')">
            <el-icon><List /></el-icon>
            <span>我的报名</span>
          </el-menu-item>
          <el-menu-item @click="handleCommand('logout')">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </el-menu-item>
        </el-menu>
      </el-drawer>
    </el-container>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const drawer = ref(false)
const isMobile = ref(false)

const activeMenu = computed(() => route.path)

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const handleMenuSelect = (index) => {
  router.push(index)
}

const handleMobileMenuSelect = (index) => {
  drawer.value = false
  router.push(index)
}

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    }).catch(() => {})
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'myRegistrations') {
    router.push('/my')
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.site-title {
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0;
  white-space: nowrap;
}

.desktop-menu {
  flex: 1;
  margin: 0 40px;
  border-bottom: none;
}

.desktop-menu :deep(.el-menu-item) {
  font-weight: 500;
  padding: 0 20px;
}

.desktop-menu :deep(.el-menu-item.is-active) {
  color: #667eea;
  background: linear-gradient(to bottom, transparent 85%, #667eea 85%);
}

.header-right {
  margin-left: auto;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 20px;
  transition: all 0.3s;
  background: #f5f7fa;
}

.user-info:hover {
  background: #e8ecf1;
  transform: translateY(-1px);
}

.user-name {
  font-weight: 500;
  color: #333;
}

.main {
  min-height: calc(100vh - 60px);
  padding: 24px;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.drawer-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: -20px -20px 20px -20px;
  border-radius: 0;
}

.drawer-user-info {
  margin-top: 15px;
  text-align: center;
  color: white;
}

.drawer-user-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 5px;
}

.drawer-user-role {
  font-size: 14px;
  opacity: 0.9;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .desktop-menu {
    margin: 0 20px;
  }
  
  .desktop-menu :deep(.el-menu-item) {
    padding: 0 15px;
  }
}

@media (max-width: 768px) {
  .header {
    padding: 0 16px;
  }
  
  .site-title {
    font-size: 16px;
  }
  
  .logo {
    width: 36px;
    height: 36px;
  }
  
  .main {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 0 12px;
  }
  
  .site-title {
    font-size: 14px;
  }
  
  .main {
    padding: 12px;
  }
}
</style>
