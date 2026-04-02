<template>
  <Layout>
    <div class="profile-container">
      <el-row :gutter="20">
        <!-- 左栏：个人信息 -->
        <el-col :xs="24" :sm="24" :md="8" :lg="6">
          <el-card class="profile-card" shadow="never">
            <div class="profile-gradient-header">
              <el-avatar :size="72" class="profile-avatar">
                {{ userInfo.realName?.charAt(0) }}
              </el-avatar>
              <h3 class="profile-name">{{ userInfo.realName }}</h3>
              <span class="profile-no">{{ userInfo.studentNo }}</span>
              <el-tag
                :type="userInfo.role === 'ADMIN' ? 'danger' : 'primary'"
                size="small"
                effect="light"
                class="role-tag"
              >
                {{ userInfo.role === 'ADMIN' ? '管理员' : '志愿者' }}
              </el-tag>
            </div>

            <div class="profile-fields">
              <div class="profile-field">
                <el-icon class="field-icon"><User /></el-icon>
                <span class="field-label">账号</span>
                <span class="field-value">{{ userInfo.username }}</span>
              </div>
              <div class="profile-field">
                <el-icon class="field-icon"><Phone /></el-icon>
                <span class="field-label">手机</span>
                <span class="field-value">{{ userInfo.phone || '未填写' }}</span>
              </div>
              <div class="profile-field">
                <el-icon class="field-icon"><Message /></el-icon>
                <span class="field-label">邮箱</span>
                <span class="field-value email">{{ userInfo.email || '未填写' }}</span>
              </div>
            </div>

            <div class="profile-actions">
              <el-button type="primary" size="large" block @click="showEditDialog = true">
                <el-icon><Edit /></el-icon>
                编辑资料
              </el-button>
              <el-button size="large" block @click="showPasswordDialog = true">
                <el-icon><Lock /></el-icon>
                修改密码
              </el-button>
            </div>
          </el-card>

          <div class="stat-cards">
            <div class="stat-card stat-card--primary">
              <div class="stat-icon-wrap"><el-icon :size="22"><Timer /></el-icon></div>
              <div class="stat-body">
                <div class="stat-num">{{ userInfo.totalVolunteerHours || 0 }}</div>
                <div class="stat-desc">累计时长（小时）</div>
              </div>
            </div>
            <div class="stat-card stat-card--success">
              <div class="stat-icon-wrap"><el-icon :size="22"><Flag /></el-icon></div>
              <div class="stat-body">
                <div class="stat-num">{{ registrationCount }}</div>
                <div class="stat-desc">参与活动</div>
              </div>
            </div>
            <div class="stat-card stat-card--warning">
              <div class="stat-icon-wrap"><el-icon :size="22"><Select /></el-icon></div>
              <div class="stat-body">
                <div class="stat-num">{{ checkedInCount }}</div>
                <div class="stat-desc">已签到</div>
              </div>
            </div>
            <div class="stat-card stat-card--orange">
              <div class="stat-icon-wrap"><el-icon :size="22"><Trophy /></el-icon></div>
              <div class="stat-body">
                <div class="stat-num">{{ confirmedCount }}</div>
                <div class="stat-desc">已核销</div>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 右栏：操作说明 -->
        <el-col :xs="24" :sm="24" :md="16" :lg="18">
          <el-card class="info-card" shadow="never">
            <template #header>
              <div class="info-header">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
                <span>功能说明</span>
              </div>
            </template>
            <div class="info-content">
              <div class="info-item">
                <el-icon class="item-icon"><Document /></el-icon>
                <div class="item-text">
                  <div class="item-title">编辑资料</div>
                  <div class="item-desc">您可以在这里修改您的姓名、手机号和邮箱信息</div>
                </div>
              </div>
              <div class="info-item">
                <el-icon class="item-icon"><Lock /></el-icon>
                <div class="item-text">
                  <div class="item-title">修改密码</div>
                  <div class="item-desc">为了账号安全，建议定期修改密码</div>
                </div>
              </div>
              <div class="info-item">
                <el-icon class="item-icon"><Stamp /></el-icon>
                <div class="item-text">
                  <div class="item-title">查看志愿足迹</div>
                  <div class="item-desc">点击顶部导航栏的"我的志愿足迹"查看您的报名记录</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 编辑资料对话框 -->
      <el-dialog
        v-model="showEditDialog"
        title="编辑资料"
        width="500px"
        :close-on-click-modal="false"
      >
        <el-form
          :model="editForm"
          :rules="editRules"
          ref="editFormRef"
          label-width="100px"
        >
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
          <el-button type="primary" @click="handleUpdateProfile" :loading="updateLoading">
            确定
          </el-button>
        </template>
      </el-dialog>

      <!-- 修改密码对话框 -->
      <el-dialog
        v-model="showPasswordDialog"
        title="修改密码"
        width="500px"
        :close-on-click-modal="false"
      >
        <el-form
          :model="passwordForm"
          :rules="passwordRules"
          ref="passwordFormRef"
          label-width="100px"
        >
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
          <el-button type="primary" @click="handleUpdatePassword" :loading="updateLoading">
            确定
          </el-button>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import Layout from '@/components/Layout.vue'
import { getUserInfo, updateUserInfo, updatePassword } from '@/api/user'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

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

const registrationCount = computed(() => 0)
const checkedInCount = computed(() => 0)
const confirmedCount = computed(() => 0)

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
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
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

const formatDate = (date) => {
  if (!date) return '—'
  return dayjs(date).format('MM-DD HH:mm')
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
    // 更新 userInfo 以反映更改
    userInfo.value.realName = editForm.value.realName
    userInfo.value.phone = editForm.value.phone
    userInfo.value.email = editForm.value.email
    // 清空 editForm
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
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
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

// 监听编辑对话框打开，初始化 editForm
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
.profile-container {
  max-width: 1280px;
  margin: 0 auto;
}

/* 个人卡片 */
.profile-card {
  border-radius: 14px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  margin-bottom: 16px;
}

.profile-gradient-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 28px 20px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: white;
  margin: -20px -20px 0;
}

.profile-avatar {
  background: rgba(255,255,255,0.3) !important;
  color: white !important;
  font-size: 28px;
  font-weight: 700;
  border: 3px solid rgba(255,255,255,0.5);
}

.profile-name {
  margin: 4px 0 0;
  font-size: 18px;
  font-weight: 700;
  color: white;
}

.profile-no {
  font-size: 13px;
  opacity: 0.85;
}

.role-tag {
  margin-top: 2px;
  border-radius: 12px;
}

.profile-fields {
  padding: 16px 4px 4px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.profile-field {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.field-icon { color: #909399; flex-shrink: 0; }
.field-label { color: #909399; width: 30px; flex-shrink: 0; }
.field-value { color: #303133; flex: 1; word-break: break-all; }
.field-value.email { font-size: 12px; }

.profile-actions {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-card {
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: transform 0.2s;
}

.stat-card:hover { transform: translateY(-2px); }

.stat-card--primary {
  background: linear-gradient(135deg, #667eea15, #667eea25);
  border: 1px solid #667eea30;
}
.stat-card--primary .stat-icon-wrap { color: #667eea; }
.stat-card--primary .stat-num { color: #667eea; }

.stat-card--success {
  background: linear-gradient(135deg, #67c23a15, #67c23a25);
  border: 1px solid #67c23a30;
}
.stat-card--success .stat-icon-wrap { color: #67c23a; }
.stat-card--success .stat-num { color: #67c23a; }

.stat-card--warning {
  background: linear-gradient(135deg, #e6a23c15, #e6a23c25);
  border: 1px solid #e6a23c30;
}
.stat-card--warning .stat-icon-wrap { color: #e6a23c; }
.stat-card--warning .stat-num { color: #e6a23c; }

.stat-card--orange {
  background: linear-gradient(135deg, #f5622115, #f5622125);
  border: 1px solid #f5622130;
}
.stat-card--orange .stat-icon-wrap { color: #f56221; }
.stat-card--orange .stat-num { color: #f56221; }

.stat-icon-wrap { flex-shrink: 0; }
.stat-num {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}
.stat-desc {
  font-size: 11px;
  color: #909399;
  margin-top: 3px;
}

/* 信息卡片 */
.info-card {
  border-radius: 14px;
  border: 1px solid #f0f0f0;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.info-icon { color: #667eea; }

.info-content {
  padding: 20px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  margin-bottom: 12px;
  background: #f8f9fc;
  border-radius: 8px;
  transition: background 0.2s;
}

.info-item:hover { background: #f0f2f5; }

.item-icon {
  color: #667eea;
  flex-shrink: 0;
  margin-top: 4px;
}

.item-text {
  flex: 1;
}

.item-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.item-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 480px) {
  .stat-cards { grid-template-columns: 1fr; }
}
</style>
