<template>
  <div class="admin-shell">
    <aside v-if="!isMobile" class="admin-aside">
      <div class="aside-head">
        <div class="brand-mark">V</div>
        <div>
          <h1>校园志愿</h1>
          <p>管理后台</p>
        </div>
      </div>

      <nav class="nav-list">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          :class="['nav-item', route.path === item.path && 'active']"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="aside-foot">
        <button type="button" class="nav-item ghost" @click="router.push('/home')">
          <el-icon><House /></el-icon>
          <span>返回前台</span>
        </button>
        <button type="button" class="nav-item ghost" @click="handleCommand('logout')">
          <el-icon><SwitchButton /></el-icon>
          <span>退出登录</span>
        </button>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <el-button v-if="isMobile" text class="menu-btn" @click="menuDrawer = true">
            <el-icon :size="22"><Menu /></el-icon>
          </el-button>

          <div>
            <p class="header-label">管理工作区</p>
            <h2>{{ currentTitle }}</h2>
          </div>
        </div>

        <div class="header-right">
          <div class="header-chip desktop-only">
            <el-icon><Bell /></el-icon>
            <span>管理员工作台</span>
          </div>

          <el-dropdown @command="handleCommand">
            <button type="button" class="user-pill">
              <span class="avatar">{{ (userStore.userInfo.realName || 'A').slice(0, 1) }}</span>
              <span class="user-text">
                <strong>{{ userStore.userInfo.realName || '管理员' }}</strong>
                <small>{{ userStore.userInfo.role || 'ADMIN' }}</small>
              </span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">返回前台</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>

    <el-drawer v-model="menuDrawer" direction="ltr" size="min(86vw, 320px)" :with-header="false">
      <div class="drawer-shell">
        <div class="aside-head mobile">
          <div class="brand-mark">V</div>
          <div>
            <h1>校园志愿</h1>
            <p>管理后台</p>
          </div>
        </div>

        <nav class="nav-list">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            :class="['nav-item', route.path === item.path && 'active']"
            @click="menuDrawer = false"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </router-link>
        </nav>

        <div class="aside-foot">
          <button type="button" class="nav-item ghost" @click="menuDrawer = false; router.push('/home')">
            <el-icon><House /></el-icon>
            <span>返回前台</span>
          </button>
          <button type="button" class="nav-item ghost" @click="handleCommand('logout')">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Bell,
  Calendar,
  Checked,
  Clock,
  House,
  Menu,
  Notification,
  Plus,
  SwitchButton,
  Tickets
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const menuDrawer = ref(false)
const isMobile = ref(false)

const navItems = [
  { path: '/admin/announcements', label: '公告管理', icon: Notification },
  { path: '/admin/feedback', label: '反馈工单', icon: Tickets },
  { path: '/admin/activities', label: '活动管理', icon: Calendar },
  { path: '/admin/create', label: '发布活动', icon: Plus },
  { path: '/admin/checkin', label: '活动签到', icon: Checked },
  { path: '/admin/confirm', label: '时长核销', icon: Clock },
  { path: '/admin/hours', label: '时长查询', icon: Bell }
]

const currentTitle = computed(() => route.meta.title || '管理后台')

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

const handleCommand = (command) => {
  if (command === 'home') {
    menuDrawer.value = false
    router.push('/home')
    return
  }

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
  display: grid;
  grid-template-columns: 264px minmax(0, 1fr);
  background:
    radial-gradient(circle at 8% 10%, rgba(47, 126, 244, 0.08), transparent 30%),
    var(--cv-bg);
}

.admin-aside {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 20px 16px;
  background: rgba(250, 252, 255, 0.88);
  border-right: 1px solid rgba(201, 214, 243, 0.56);
  backdrop-filter: blur(14px);
}

.aside-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 10px 18px;
}

.aside-head.mobile {
  padding-inline: 0;
}

.brand-mark {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--cv-primary), var(--cv-primary-weak));
  color: #fff;
  font-family: 'Plus Jakarta Sans', 'Manrope', 'Noto Sans SC', sans-serif;
  font-size: 22px;
  font-weight: 800;
}

.aside-head h1 {
  margin: 0;
  font-size: 20px;
  line-height: 1.1;
}

.aside-head p {
  margin: 4px 0 0;
  color: #72809d;
  font-size: 12px;
}

.nav-list,
.aside-foot {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-list {
  flex: 1;
  margin-top: 12px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 999px;
  color: #5d6884;
  font-weight: 700;
  text-decoration: none;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.nav-item:hover {
  background: rgba(13, 71, 217, 0.08);
  color: var(--cv-primary);
  transform: translateX(2px);
}

.nav-item.active {
  background: linear-gradient(120deg, rgba(13, 71, 217, 0.12), rgba(0, 193, 253, 0.12));
  color: var(--cv-primary);
  box-shadow: inset 0 0 0 1px rgba(13, 71, 217, 0.12);
}

.nav-item.ghost {
  width: 100%;
  text-align: left;
}

.aside-foot {
  padding-top: 14px;
  border-top: 1px solid rgba(201, 214, 243, 0.6);
}

.admin-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 24px 14px;
  background: rgba(244, 248, 255, 0.72);
  backdrop-filter: blur(14px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-label {
  margin: 0 0 4px;
  color: var(--cv-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.header-left h2 {
  font-size: clamp(20px, 3vw, 30px);
  line-height: 1.05;
}

.menu-btn {
  color: var(--cv-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  color: #5f6a85;
  font-size: 12px;
  font-weight: 700;
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px 8px 8px;
  border: 1px solid rgba(201, 214, 243, 0.58);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  cursor: pointer;
}

.avatar {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--cv-primary), var(--cv-primary-weak));
  color: #fff;
  font-weight: 800;
}

.user-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.user-text strong {
  color: var(--cv-text);
  font-size: 14px;
}

.user-text small {
  color: #74809b;
  font-size: 11px;
}

.admin-content {
  flex: 1;
  padding: 0 24px 24px;
}

.drawer-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
}

@media (max-width: 991px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-header,
  .admin-content {
    padding-inline: 12px;
  }

  .desktop-only {
    display: none;
  }
}
</style>
