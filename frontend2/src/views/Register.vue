<template>
  <div class="register-shell">
    <div class="register-orb orb-left"></div>
    <div class="register-orb orb-right"></div>

    <section class="register-stage">
      <div class="register-brand">
        <div class="brand-chip">
          <el-icon :size="26"><UserFilled /></el-icon>
        </div>
        <p class="eyebrow">注册账号</p>
        <h1>创建你的志愿者账号</h1>
        <p class="subtitle">
          注册完成后即可查看公告、报名志愿活动、提交反馈，并沉淀个人服务记录。
        </p>

        <div class="brand-grid">
          <article class="brand-card">
            <strong>统一管理报名记录</strong>
            <p>注册后可随时查看活动报名进度、签到情况和时长核销状态。</p>
          </article>
          <article class="brand-card">
            <strong>反馈工单持续跟进</strong>
            <p>注册后可提交问题、建议、投诉，并在个人页中继续补充与关闭。</p>
          </article>
          <article class="brand-card">
            <strong>形成可导出的足迹</strong>
            <p>已核销记录支持导出，便于后续留存与使用。</p>
          </article>
        </div>
      </div>

      <div class="register-card">
        <div class="card-head">
          <h2>注册账号</h2>
          <p>请填写真实信息，便于后续报名、联系与消息通知。</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="register-form">
          <div class="form-grid">
            <el-form-item prop="username">
              <el-input v-model="form.username" size="large" clearable placeholder="用户名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item prop="realName">
              <el-input v-model="form.realName" size="large" clearable placeholder="真实姓名">
                <template #prefix><el-icon><Avatar /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" size="large" show-password placeholder="设置密码">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                size="large"
                show-password
                placeholder="确认密码"
                @keyup.enter="handleRegister"
              >
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item prop="studentNo">
              <el-input v-model="form.studentNo" size="large" clearable placeholder="学号">
                <template #prefix><el-icon><Tickets /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item prop="phone">
              <el-input v-model="form.phone" size="large" clearable placeholder="手机号">
                <template #prefix><el-icon><Phone /></el-icon></template>
              </el-input>
            </el-form-item>
          </div>

          <el-form-item prop="email">
            <el-input v-model="form.email" size="large" clearable placeholder="邮箱地址">
              <template #prefix><el-icon><Message /></el-icon></template>
            </el-input>
          </el-form-item>

          <div class="tip-strip">
            <el-icon><InfoFilled /></el-icon>
            <span>密码长度至少 6 位，手机号与邮箱会按现有前端规则校验。</span>
          </div>

          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleRegister">
            立即注册
          </el-button>

          <button type="button" class="switch-link" @click="$router.push('/login')">
            已有账号，返回登录
          </button>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/user'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  studentNo: '',
  phone: '',
  email: ''
})

const validatePassword = (_rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const { confirmPassword, ...data } = form
    await register(data)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-shell {
  position: relative;
  min-height: 100vh;
  padding: clamp(18px, 4vw, 30px);
  overflow: hidden;
  background:
    radial-gradient(circle at 10% 16%, rgba(47, 126, 244, 0.14), transparent 32%),
    radial-gradient(circle at 92% 92%, rgba(0, 193, 253, 0.15), transparent 28%),
    var(--cv-bg);
}

.register-stage {
  position: relative;
  z-index: 1;
  width: min(1180px, 100%);
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(0, 1fr);
  gap: 18px;
  align-items: center;
}

.register-orb {
  position: absolute;
  border-radius: 999px;
}

.orb-left {
  width: 260px;
  height: 260px;
  top: -80px;
  left: -80px;
  background: rgba(47, 126, 244, 0.14);
}

.orb-right {
  width: 340px;
  height: 340px;
  right: -130px;
  bottom: -120px;
  background: rgba(15, 139, 95, 0.13);
}

.register-brand,
.register-card {
  border-radius: 32px;
  border: 1px solid rgba(201, 214, 243, 0.55);
  box-shadow: 0 26px 46px rgba(13, 71, 217, 0.12);
  backdrop-filter: blur(18px);
}

.register-brand {
  position: relative;
  overflow: hidden;
  padding: clamp(26px, 4vw, 40px);
  min-height: 660px;
  color: #fff;
  background: linear-gradient(145deg, #0d47d9 0%, #1a66ef 56%, #00c1fd 100%);
}

.register-brand::before {
  content: '';
  position: absolute;
  width: 280px;
  height: 280px;
  top: -140px;
  right: -80px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
}

.brand-chip {
  position: relative;
  z-index: 1;
  width: 66px;
  height: 66px;
  display: grid;
  place-items: center;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.18);
}

.eyebrow {
  position: relative;
  z-index: 1;
  margin: 28px 0 10px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  opacity: 0.9;
}

.register-brand h1 {
  position: relative;
  z-index: 1;
  color: #fff;
  font-size: clamp(34px, 5vw, 50px);
  line-height: 1.04;
  letter-spacing: -0.04em;
}

.subtitle {
  position: relative;
  z-index: 1;
  margin-top: 14px;
  font-size: 16px;
  line-height: 1.8;
  opacity: 0.92;
}

.brand-grid {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 14px;
  margin-top: 38px;
}

.brand-card {
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.brand-card strong {
  display: block;
  margin-bottom: 8px;
  font-size: 18px;
}

.brand-card p {
  margin: 0;
  line-height: 1.75;
  opacity: 0.9;
}

.register-card {
  padding: clamp(24px, 4vw, 36px);
  background: rgba(255, 255, 255, 0.8);
}

.card-head {
  margin-bottom: 22px;
}

.card-head h2 {
  margin-bottom: 6px;
  font-size: 32px;
}

.card-head p {
  margin: 0;
  color: var(--cv-text-soft);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.tip-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  margin-bottom: 18px;
  border-radius: 16px;
  background: rgba(13, 71, 217, 0.08);
  color: #4e5c7f;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  height: 52px;
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

@media (max-width: 980px) {
  .register-stage {
    grid-template-columns: 1fr;
  }

  .register-brand {
    min-height: auto;
  }
}

@media (max-width: 620px) {
  .register-shell {
    padding: 12px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
