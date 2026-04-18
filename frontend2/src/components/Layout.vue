<template>
  <div class="user-shell">
    <header class="shell-header">
      <div class="page-container shell-header-inner">
        <div class="shell-brand-group">
          <button type="button" class="shell-brand" @click="handleNavigate('/home')">
            <span class="brand-mark">
              <el-icon><Trophy /></el-icon>
            </span>
            <span class="brand-copy">
              <strong>校园志愿</strong>
              <small>服务平台</small>
            </span>
          </button>

          <nav v-if="!isMobile" class="shell-nav" aria-label="主导航">
            <button
              v-for="item in menuItems"
              :key="item.path"
              type="button"
              :class="['shell-nav-item', isMenuActive(item.path) && 'is-active']"
              @click="handleNavigate(item.path)"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>

        <div class="shell-actions">
          <el-dropdown v-if="!isMobile" @command="handleCommand">
            <button type="button" class="user-trigger">
              <el-avatar :size="38" class="user-avatar">{{ displayName.charAt(0) || 'U' }}</el-avatar>
              <span class="user-copy">
                <strong>{{ displayName }}</strong>
                <small>{{ userStore.isAdmin ? '管理员' : '志愿者' }}</small>
              </span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="myRegistrations">我的报名</el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" command="admin">管理后台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <button v-else type="button" class="mobile-menu-btn" @click="drawer = true">
            <el-icon :size="20"><Menu /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <main class="shell-main">
      <slot />
    </main>

    <el-drawer
      v-model="drawer"
      direction="rtl"
      :size="'min(86vw, 320px)'"
      class="mobile-drawer"
      :with-header="false"
    >
      <div class="drawer-panel">
        <div class="drawer-profile">
          <el-avatar :size="58" class="user-avatar">{{ displayName.charAt(0) || 'U' }}</el-avatar>
          <strong>{{ displayName }}</strong>
          <small>{{ userStore.isAdmin ? '管理员' : '志愿者' }}</small>
        </div>

        <div class="drawer-menu">
          <button
            v-for="item in menuItems"
            :key="item.path"
            type="button"
            :class="['drawer-link', isMenuActive(item.path) && 'is-active']"
            @click="handleMobileNavigate(item.path)"
          >
            {{ item.label }}
          </button>
        </div>

        <div class="drawer-divider"></div>

        <div class="drawer-actions">
          <button type="button" class="drawer-link" @click="handleCommand('profile')">个人资料</button>
          <button type="button" class="drawer-link" @click="handleCommand('myRegistrations')">我的报名</button>
          <button v-if="userStore.isAdmin" type="button" class="drawer-link" @click="handleCommand('admin')">管理后台</button>
          <button type="button" class="drawer-link danger" @click="handleCommand('logout')">退出登录</button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const drawer = ref(false)
const isMobile = ref(false)

const displayName = computed(() => userStore.userInfo.realName || userStore.userInfo.username || '用户')
const menuItems = computed(() => {
  const items = [
    { label: '首页', path: '/home' },
    { label: '活动', path: '/activities' },
    { label: '反馈', path: '/my-feedback' },
    { label: '志愿足迹', path: '/my' }
  ]

  if (userStore.isAdmin) {
    items.push({ label: '管理后台', path: '/admin/activities' })
  }
  return items
})

const checkMobile = () => {
  isMobile.value = window.innerWidth < 960
}

const isMenuActive = (path) => {
  if (path.startsWith('/admin')) {
    return route.path.startsWith('/admin')
  }
  return route.path === path
}

const handleNavigate = (path) => {
  if (route.path !== path) {
    router.push(path)
  }
}

const handleMobileNavigate = (path) => {
  drawer.value = false
  handleNavigate(path)
}

const handleCommand = (command) => {
  drawer.value = false

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
    return
  }

  if (command === 'profile') {
    router.push('/profile')
    return
  }

  if (command === 'myRegistrations') {
    router.push('/my')
    return
  }

  if (command === 'admin') {
    router.push('/admin/activities')
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
.user-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at 0% 0%, rgba(68, 139, 255, 0.14), transparent 34%),
    radial-gradient(circle at 100% 0%, rgba(117, 209, 255, 0.14), transparent 24%),
    radial-gradient(circle at 100% 100%, rgba(218, 225, 255, 0.82), transparent 30%),
    var(--cv-bg);
}

.shell-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.72);
  border-bottom: 1px solid rgba(194, 204, 229, 0.65);
  backdrop-filter: blur(22px);
}

.shell-header-inner {
  min-height: 82px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 clamp(16px, 2vw, 26px);
}

.shell-brand-group {
  display: flex;
  align-items: center;
  gap: clamp(18px, 2vw, 28px);
  min-width: 0;
}

.shell-brand {
  border: none;
  background: transparent;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.brand-mark {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  color: #fff;
  background: linear-gradient(135deg, var(--cv-primary), #18b6ff);
  box-shadow: 0 14px 32px rgba(13, 71, 217, 0.2);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.1;
}

.brand-copy strong {
  font-family: 'Plus Jakarta Sans', 'Manrope', sans-serif;
  font-size: clamp(18px, 2vw, 21px);
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--cv-text);
}

.brand-copy small {
  margin-top: 3px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--cv-text-soft);
  text-transform: uppercase;
}

.shell-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(245, 248, 255, 0.82);
  box-shadow: inset 0 0 0 1px rgba(199, 210, 236, 0.8);
}

.shell-nav-item {
  border: none;
  background: transparent;
  color: #56607f;
  font-family: 'Plus Jakarta Sans', 'Manrope', sans-serif;
  font-size: 14px;
  font-weight: 700;
  padding: 11px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.shell-nav-item:hover {
  color: var(--cv-primary);
  background: rgba(13, 71, 217, 0.08);
}

.shell-nav-item.is-active {
  color: #fff;
  background: linear-gradient(135deg, var(--cv-primary), #18b6ff);
  box-shadow: 0 12px 24px rgba(13, 71, 217, 0.18);
}

.shell-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.user-trigger {
  border: none;
  background: rgba(245, 248, 255, 0.88);
  box-shadow: inset 0 0 0 1px rgba(199, 210, 236, 0.8);
  border-radius: 999px;
  padding: 8px 10px 8px 8px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.user-avatar {
  background: linear-gradient(135deg, var(--cv-primary), #18b6ff);
  color: #fff;
  font-weight: 800;
}

.user-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: 138px;
  line-height: 1.15;
}

.user-copy strong {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--cv-text);
  font-size: 14px;
  font-weight: 800;
}

.user-copy small {
  margin-top: 3px;
  color: var(--cv-text-soft);
  font-size: 12px;
  font-weight: 600;
}

.mobile-menu-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 999px;
  color: var(--cv-primary);
  background: rgba(245, 248, 255, 0.92);
  box-shadow: inset 0 0 0 1px rgba(199, 210, 236, 0.82);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.shell-main {
  min-height: calc(100vh - 82px);
  padding: clamp(18px, 2.2vw, 28px);
}

.drawer-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 18px 10px 6px;
}

.drawer-profile {
  padding: 18px 18px 16px;
  border-radius: 24px;
  background: linear-gradient(145deg, rgba(218, 225, 255, 0.86), rgba(255, 255, 255, 0.96));
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.drawer-profile strong {
  font-size: 18px;
  font-weight: 800;
  color: var(--cv-text);
}

.drawer-profile small {
  color: var(--cv-text-soft);
  font-weight: 600;
}

.drawer-menu,
.drawer-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.drawer-link {
  border: none;
  background: transparent;
  color: var(--cv-text);
  font-family: 'Plus Jakarta Sans', 'Manrope', sans-serif;
  font-size: 14px;
  font-weight: 700;
  text-align: left;
  padding: 14px 16px;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.drawer-link:hover,
.drawer-link.is-active {
  color: var(--cv-primary);
  background: rgba(13, 71, 217, 0.08);
}

.drawer-link.danger {
  color: #d14c4c;
}

.drawer-divider {
  height: 1px;
  background: rgba(194, 204, 229, 0.8);
}

@media (max-width: 959px) {
  .shell-header-inner {
    min-height: 72px;
  }

  .shell-main {
    min-height: calc(100vh - 72px);
    padding-inline: 16px;
  }

  .brand-copy small {
    display: none;
  }

  .brand-copy strong {
    font-size: 17px;
  }

  .brand-mark {
    width: 40px;
    height: 40px;
    border-radius: 14px;
  }
}
</style>
