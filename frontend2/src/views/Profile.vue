<template>
  <Layout>
    <div class="st-page page-container profile-page" v-loading="loading">
      <div class="profile-grid">
        <section class="st-panel profile-hero">
          <div class="hero-avatar">
            <el-avatar :size="112" class="avatar">{{ userInfo.realName?.charAt(0) || 'U' }}</el-avatar>
          </div>

          <div class="hero-info">
            <p class="role-label">{{ userInfo.role === 'ADMIN' ? '管理员账号' : '志愿者账号' }}</p>
            <h1>{{ userInfo.realName || '未命名用户' }}</h1>
            <p class="hero-sub">{{ userInfo.studentNo || '--' }}</p>

            <div class="hero-chips">
              <span class="chip">
                <el-icon><User /></el-icon>
                {{ userInfo.username || '--' }}
              </span>
              <span class="chip">
                <el-icon><Phone /></el-icon>
                {{ userInfo.phone || '--' }}
              </span>
              <span class="chip">
                <el-icon><Message /></el-icon>
                {{ userInfo.email || '--' }}
              </span>
            </div>

            <div class="hero-actions">
              <el-button type="primary" class="rounded-btn" @click="showEditDialog = true">
                <el-icon><Edit /></el-icon>编辑资料
              </el-button>
              <el-button class="rounded-btn" @click="showPasswordDialog = true">
                <el-icon><Lock /></el-icon>修改密码
              </el-button>
            </div>
          </div>
        </section>

        <aside class="side-stack">
          <section class="st-stat-grid mini-stats">
            <article class="st-stat-card">
              <span class="label">累计时长</span>
              <strong class="value">{{ userInfo.totalVolunteerHours || 0 }}</strong>
              <span class="hint">已核销的志愿服务时长</span>
            </article>
          </section>

          <section class="st-panel help-panel">
            <div class="st-section-head compact">
              <h3>功能说明</h3>
            </div>
            <div class="help-list">
              <article class="help-item">
                <strong>编辑资料</strong>
                <p>可在这里维护姓名、手机号和邮箱等常用资料。</p>
              </article>
              <article class="help-item">
                <strong>修改密码</strong>
                <p>修改密码后需要重新登录，请提前确认新密码。</p>
              </article>
              <article class="help-item">
                <strong>志愿时长</strong>
                <p>活动完成并核销后，累计志愿时长会同步更新到这里。</p>
              </article>
            </div>
          </section>
        </aside>
      </div>

      <el-dialog
        v-model="showEditDialog"
        title="编辑资料"
        width="min(520px, 94vw)"
        :close-on-click-modal="false"
      >
        <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="editForm.realName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="editForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="editForm.email" type="email" placeholder="请输入邮箱" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" :loading="updateLoading" @click="handleUpdateProfile">确定</el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="showPasswordDialog"
        title="修改密码"
        width="min(520px, 94vw)"
        :close-on-click-modal="false"
      >
        <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入旧密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码（6-20位）"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showPasswordDialog = false">取消</el-button>
          <el-button type="primary" :loading="updateLoading" @click="handleUpdatePassword">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import Layout from '@/components/Layout.vue'
import { getUserInfo, updateUserInfo, updatePassword } from '@/api/user'
import { ElMessage } from 'element-plus'
import { clearAuthStorage } from '@/utils/auth'

const router = useRouter()
const loading = ref(false)
const updateLoading = ref(false)
const userInfo = ref({})

const showEditDialog = ref(false)
const showPasswordDialog = ref(false)
const editFormRef = ref()
const passwordFormRef = ref()

const editForm = ref({
  realName: '',
  phone: '',
  email: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const editRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value === passwordForm.value.newPassword) {
          callback()
        } else {
          callback(new Error('两次输入的密码不一致'))
        }
      },
      trigger: 'blur'
    }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const userRes = await getUserInfo()
    userInfo.value = userRes.data || {}
    editForm.value = {
      realName: userInfo.value.realName || '',
      phone: userInfo.value.phone || '',
      email: userInfo.value.email || ''
    }
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleUpdateProfile = async () => {
  await editFormRef.value.validate()
  updateLoading.value = true
  try {
    await updateUserInfo(editForm.value)
    ElMessage.success('资料更新成功')
    showEditDialog.value = false
    userInfo.value.realName = editForm.value.realName
    userInfo.value.phone = editForm.value.phone
    userInfo.value.email = editForm.value.email
    editForm.value = {
      realName: '',
      phone: '',
      email: ''
    }
  } catch (error) {
    console.error('更新失败:', error)
  } finally {
    updateLoading.value = false
  }
}

const handleUpdatePassword = async () => {
  await passwordFormRef.value.validate()
  updateLoading.value = true
  try {
    await updatePassword(passwordForm.value)
    ElMessage.success('密码修改成功，请重新登录')
    showPasswordDialog.value = false
    clearAuthStorage()
    setTimeout(() => {
      router.push('/login')
    }, 1000)
  } catch (error) {
    console.error('密码修改失败:', error)
  } finally {
    updateLoading.value = false
  }
}

onMounted(() => { fetchData() })

watch(showEditDialog, (newValue) => {
  if (newValue && userInfo.value.realName) {
    editForm.value = {
      realName: userInfo.value.realName,
      phone: userInfo.value.phone || '',
      email: userInfo.value.email || ''
    }
  }
})
</script>

<style scoped>
.profile-page {
  max-width: 1180px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 420px);
  gap: 16px;
}

.profile-hero,
.help-panel {
  padding: 24px;
}

.profile-hero {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: 20px;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.profile-hero::after {
  content: '';
  position: absolute;
  width: 260px;
  height: 260px;
  right: -90px;
  top: -100px;
  border-radius: 999px;
  background: rgba(13, 71, 217, 0.08);
}

.hero-avatar,
.hero-info {
  position: relative;
  z-index: 1;
}

.avatar {
  background: linear-gradient(135deg, var(--cv-primary), var(--cv-primary-weak));
  color: #fff;
  font-size: 34px;
  font-weight: 800;
  border: 4px solid rgba(255, 255, 255, 0.88);
  box-shadow: 0 16px 30px rgba(13, 71, 217, 0.18);
}

.role-label {
  margin: 0 0 10px;
  color: var(--cv-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero-info h1 {
  font-size: clamp(32px, 4.2vw, 48px);
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.hero-sub {
  margin: 10px 0 0;
  color: #6a758f;
  font-size: 16px;
}

.hero-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(13, 71, 217, 0.08);
  color: #425171;
  font-size: 13px;
  font-weight: 700;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 22px;
}

.rounded-btn {
  height: 44px;
  border-radius: 999px;
}

.side-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mini-stats {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.st-section-head.compact {
  margin-bottom: 12px;
}

.help-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.help-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(13, 71, 217, 0.05);
}

.help-item strong,
.help-item p {
  display: block;
}

.help-item p {
  margin: 8px 0 0;
  color: #67728d;
  line-height: 1.75;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .profile-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .profile-hero,
  .help-panel {
    padding: 16px;
  }

  .mini-stats {
    grid-template-columns: 1fr;
  }
}
</style>
