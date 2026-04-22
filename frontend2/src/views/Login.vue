<template>
  <div class="login-shell">
    <div class="login-orb orb-left"></div>
    <div class="login-orb orb-right"></div>

    <section class="login-stage">
      <div class="brand-panel">
        <div class="brand-badge">
          <el-icon :size="26"><Trophy /></el-icon>
        </div>
        <p class="eyebrow">校园志愿</p>
        <h1>欢迎回到志愿服务平台</h1>
        <p class="subtitle">
          继续查看公告、报名活动、跟进反馈和沉淀个人志愿足迹。
        </p>

        <div class="brand-points">
          <article class="brand-point">
            <span class="point-icon">
              <el-icon><Bell /></el-icon>
            </span>
            <div>
              <strong>公告与活动同步</strong>
              <p>登录后优先进入当前可用的志愿活动与公告列表。</p>
            </div>
          </article>
          <article class="brand-point">
            <span class="point-icon">
              <el-icon><Tickets /></el-icon>
            </span>
            <div>
              <strong>报名状态可追踪</strong>
              <p>已报名、已签到、已核销等服务进度都能在平台内持续查看。</p>
            </div>
          </article>
        </div>
      </div>

      <div class="login-card">
        <div class="card-head">
          <div class="logo-chip">
            <el-icon :size="24"><Trophy /></el-icon>
          </div>
          <div>
            <h2>账号登录</h2>
            <p>输入当前系统已有账号进入平台。</p>
          </div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              size="large"
              clearable
              placeholder="请输入用户名"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              show-password
              placeholder="请输入密码"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>

          <button type="button" class="switch-link" @click="$router.push('/register')">
            还没有账号？立即注册
          </button>
        </el-form>

        <div class="divider">
          <span>快捷登录</span>
        </div>

        <div class="quick-grid">
          <button
            v-for="account in testAccounts"
            :key="account.username"
            type="button"
            class="quick-item"
            @click="quickLogin(account)"
          >
            <strong>{{ account.label }}</strong>
            <span>{{ account.username }}</span>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/user'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const testAccounts = [
  { username: 'admin', password: 'password123', label: '管理员' },
  { username: 'student01', password: 'password123', label: '学生 01' },
  { username: 'student02', password: 'password123', label: '学生 02' }
]

const quickLogin = (account) => {
  form.username = account.username
  form.password = account.password
  handleLogin()
}

const safeRedirect = () => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  return redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : ''
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true

  try {
    const res = await login(form)
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.userInfo)

    ElMessage.success('登录成功')

    router.push(safeRedirect() || (res.data.userInfo.role === 'ADMIN' ? '/admin/activities' : '/home'))
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  padding: clamp(18px, 4vw, 30px);
  overflow: hidden;
  background:
    radial-gradient(circle at 10% 10%, rgba(47, 126, 244, 0.14), transparent 30%),
    radial-gradient(circle at 90% 90%, rgba(15, 139, 95, 0.14), transparent 28%),
    var(--cv-bg);
}

.login-stage {
  position: relative;
  z-index: 1;
  width: min(1120px, 100%);
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 460px);
  gap: 18px;
  align-items: center;
}

.login-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(8px);
}

.orb-left {
  width: 240px;
  height: 240px;
  left: -40px;
  top: 80px;
  background: rgba(47, 126, 244, 0.14);
}

.orb-right {
  width: 320px;
  height: 320px;
  right: -100px;
  bottom: -60px;
  background: rgba(0, 193, 253, 0.16);
}

.brand-panel,
.login-card {
  border-radius: 32px;
  border: 1px solid rgba(201, 214, 243, 0.55);
  box-shadow: 0 26px 46px rgba(13, 71, 217, 0.12);
  backdrop-filter: blur(18px);
}

.brand-panel {
  position: relative;
  overflow: hidden;
  padding: clamp(28px, 5vw, 54px);
  min-height: 620px;
  color: #fff;
  background: linear-gradient(135deg, #0d47d9 0%, #2f7ef4 58%, #28b7ff 100%);
}

.brand-panel::before {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  right: -110px;
  top: -120px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
}

.brand-panel::after {
  content: '';
  position: absolute;
  width: 260px;
  height: 260px;
  left: -120px;
  bottom: -120px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
}

.brand-badge {
  position: relative;
  z-index: 1;
  width: 68px;
  height: 68px;
  display: grid;
  place-items: center;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.18);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.eyebrow {
  position: relative;
  z-index: 1;
  margin: 28px 0 10px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  opacity: 0.88;
}

.brand-panel h1 {
  position: relative;
  z-index: 1;
  color: #fff;
  font-size: clamp(34px, 5vw, 54px);
  line-height: 1.04;
  letter-spacing: -0.04em;
}

.subtitle {
  position: relative;
  z-index: 1;
  max-width: 520px;
  margin: 16px 0 0;
  font-size: 16px;
  line-height: 1.8;
  opacity: 0.92;
}

.brand-points {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 14px;
  margin-top: 42px;
}

.brand-point {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.point-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
}

.brand-point strong {
  display: block;
  margin-bottom: 6px;
  font-size: 18px;
}

.brand-point p {
  margin: 0;
  line-height: 1.7;
  opacity: 0.9;
}

.login-card {
  padding: clamp(24px, 4vw, 34px);
  background: rgba(255, 255, 255, 0.78);
}

.card-head {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 26px;
}

.logo-chip {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  color: #fff;
  background: linear-gradient(135deg, var(--cv-primary), var(--cv-primary-weak));
}

.card-head h2 {
  margin-bottom: 4px;
  font-size: 30px;
  line-height: 1.1;
}

.card-head p {
  margin: 0;
  color: var(--cv-text-soft);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
  height: 52px;
  margin-top: 4px;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 800;
}

.switch-link {
  width: 100%;
  margin-top: 14px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--cv-primary);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 26px 0 18px;
  color: #7a8198;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(201, 214, 243, 0.85);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.quick-item {
  padding: 14px 12px;
  border: 1px solid rgba(201, 214, 243, 0.72);
  border-radius: 18px;
  background: rgba(242, 246, 251, 0.9);
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.quick-item:hover {
  transform: translateY(-2px);
  border-color: rgba(13, 71, 217, 0.28);
  box-shadow: 0 12px 22px rgba(13, 71, 217, 0.08);
}

.quick-item strong,
.quick-item span {
  display: block;
}

.quick-item strong {
  color: var(--cv-text);
  font-size: 14px;
}

.quick-item span {
  margin-top: 6px;
  color: #74809b;
  font-size: 12px;
}

@media (max-width: 960px) {
  .login-stage {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: auto;
  }
}

@media (max-width: 580px) {
  .login-shell {
    padding: 12px;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
