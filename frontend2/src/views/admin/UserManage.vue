<template>
  <div class="st-page admin-page">
    <section class="st-hero page-hero">
      <div class="st-hero-content">
        <div class="st-hero-copy">
          <p class="st-hero-eyebrow">账号与权限</p>
          <h1 class="st-hero-title">用户管理</h1>
          <p class="st-hero-desc">
            管理用户基础信息、登录密码、账号状态和管理员授权，保证后台权限变更可控可追溯。
          </p>
        </div>
        <div class="st-hero-actions">
          <span class="st-chip">总数 {{ pagination.total }} 人</span>
          <el-button class="hero-btn" type="primary" :loading="loading" @click="fetchUsers">刷新</el-button>
        </div>
      </div>
    </section>

    <section class="st-stat-grid">
      <article class="st-stat-card">
        <span class="label">用户总数</span>
        <strong class="value">{{ pagination.total }}</strong>
        <span class="hint">按当前筛选条件统计</span>
      </article>
      <article class="st-stat-card">
        <span class="label">管理员</span>
        <strong class="value">{{ currentPageAdminCount }}</strong>
        <span class="hint">当前页管理员数量</span>
      </article>
      <article class="st-stat-card">
        <span class="label">已禁用</span>
        <strong class="value">{{ currentPageDisabledCount }}</strong>
        <span class="hint">当前页禁用账号</span>
      </article>
    </section>

    <section class="st-panel table-panel">
      <div class="st-section-head">
        <div>
          <h2>用户列表</h2>
          <p>支持按姓名、学号、用户名、角色和状态筛选，并执行资料维护与权限调整。</p>
        </div>
      </div>

      <div class="toolbar-grid">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索姓名 / 学号 / 用户名"
          clearable
          @keyup.enter="handleFilterChange"
          @clear="handleFilterChange"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filters.role" placeholder="角色" clearable @change="handleFilterChange">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="志愿者" value="VOLUNTEER" />
        </el-select>
        <el-select v-model="filters.status" placeholder="账号状态" clearable @change="handleFilterChange">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleFilterChange">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <div class="table-scroll">
        <el-table :data="users" v-loading="loading" stripe class="user-table">
          <el-table-column label="用户" min-width="190">
            <template #default="{ row }">
              <div class="user-cell">
                <span class="user-avatar">{{ avatarText(row) }}</span>
                <div>
                  <strong>{{ row.realName || row.username || '--' }}</strong>
                  <small>{{ row.username || '--' }}</small>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="学号" prop="studentNo" width="130" />
          <el-table-column label="手机" prop="phone" width="140" />
          <el-table-column label="邮箱" prop="email" min-width="190" show-overflow-tooltip />
          <el-table-column label="角色" width="110">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'success' : 'info'">{{ roleText(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="Number(row.status) === 1 ? 'success' : 'danger'">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="累计时长" width="120">
            <template #default="{ row }">{{ row.totalVolunteerHours ?? 0 }} 小时</template>
          </el-table-column>
          <el-table-column label="最近更新" width="170">
            <template #default="{ row }">{{ formatDate(row.updateTime || row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="330" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="openProfileDialog(row)">编辑</el-button>
              <el-button type="warning" size="small" link @click="openPasswordDialog(row)">重置密码</el-button>
              <el-button type="success" size="small" link @click="handleRoleChange(row)">
                {{ row.role === 'ADMIN' ? '设为志愿者' : '设为管理员' }}
              </el-button>
              <el-button :type="Number(row.status) === 1 ? 'danger' : 'success'" size="small" link @click="handleStatusChange(row)">
                {{ Number(row.status) === 1 ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-empty v-if="!loading && users.length === 0" description="未找到匹配的用户" />

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="fetchUsers"
        />
      </div>
    </section>

    <el-dialog
      v-model="profileDialogVisible"
      title="编辑用户资料"
      width="min(620px, 96vw)"
      destroy-on-close
      @closed="resetProfileForm"
    >
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="96px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="profileForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model.trim="profileForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model.trim="profileForm.studentNo" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model.trim="profileForm.phone" placeholder="请输入手机号码" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model.trim="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="profileSaving" @click="submitProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="passwordDialogVisible"
      :title="passwordDialogTitle"
      width="min(520px, 96vw)"
      destroy-on-close
      @closed="resetPasswordForm"
    >
      <el-alert
        class="password-tip"
        type="warning"
        :closable="false"
        show-icon
        title="保存后用户需使用新密码登录，请确认已完成线下告知。"
      />
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="96px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="submitPassword">重置密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  getAdminUserList,
  resetAdminUserPassword,
  updateAdminUserProfile,
  updateAdminUserRole,
  updateAdminUserStatus
} from '@/api/user'

const loading = ref(false)
const profileSaving = ref(false)
const passwordSaving = ref(false)
const profileDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const users = ref([])
const editingUser = ref(null)
const profileFormRef = ref()
const passwordFormRef = ref()

const filters = reactive({
  keyword: '',
  role: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const profileForm = reactive({
  username: '',
  realName: '',
  studentNo: '',
  phone: '',
  email: ''
})

const passwordForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const profileRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号码', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const passwordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const passwordDialogTitle = computed(() => {
  const name = editingUser.value?.realName || editingUser.value?.username || '用户'
  return `重置密码 - ${name}`
})

const currentPageAdminCount = computed(() => users.value.filter(item => item.role === 'ADMIN').length)
const currentPageDisabledCount = computed(() => users.value.filter(item => Number(item.status) !== 1).length)

const roleText = (role) => role === 'ADMIN' ? '管理员' : '志愿者'
const statusText = (status) => Number(status) === 1 ? '启用' : '禁用'
const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '--'
const avatarText = (row) => (row.realName || row.username || '?').slice(0, 1)

const buildQuery = () => {
  const params = {
    page: pagination.page,
    size: pagination.size
  }
  if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
  if (filters.role) params.role = filters.role
  if (filters.status !== '') params.status = filters.status
  return params
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await getAdminUserList(buildQuery())
    const data = res.data || {}
    users.value = data.records || []
    pagination.total = data.total || 0
    pagination.page = data.page || pagination.page
    pagination.size = data.size || pagination.size
  } catch (error) {
    console.error('获取用户列表失败:', error)
    users.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  pagination.page = 1
  fetchUsers()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.role = ''
  filters.status = ''
  handleFilterChange()
}

const handleSizeChange = () => {
  pagination.page = 1
  fetchUsers()
}

const openProfileDialog = (row) => {
  editingUser.value = row
  Object.assign(profileForm, {
    username: row.username || '',
    realName: row.realName || '',
    studentNo: row.studentNo || '',
    phone: row.phone || '',
    email: row.email || ''
  })
  profileDialogVisible.value = true
}

const resetProfileForm = () => {
  profileFormRef.value?.clearValidate()
  Object.assign(profileForm, {
    username: '',
    realName: '',
    studentNo: '',
    phone: '',
    email: ''
  })
}

const submitProfile = async () => {
  if (!editingUser.value?.id) return
  await profileFormRef.value.validate()
  profileSaving.value = true
  try {
    await updateAdminUserProfile(editingUser.value.id, { ...profileForm })
    ElMessage.success('用户资料已更新')
    profileDialogVisible.value = false
    fetchUsers()
  } catch (error) {
    console.error('更新用户资料失败:', error)
  } finally {
    profileSaving.value = false
  }
}

const openPasswordDialog = (row) => {
  editingUser.value = row
  passwordDialogVisible.value = true
}

const resetPasswordForm = () => {
  passwordFormRef.value?.clearValidate()
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const submitPassword = async () => {
  if (!editingUser.value?.id) return
  await passwordFormRef.value.validate()
  passwordSaving.value = true
  try {
    await resetAdminUserPassword(editingUser.value.id, { newPassword: passwordForm.newPassword })
    ElMessage.success('密码已重置')
    passwordDialogVisible.value = false
  } catch (error) {
    console.error('重置密码失败:', error)
  } finally {
    passwordSaving.value = false
  }
}

const handleRoleChange = (row) => {
  const nextRole = row.role === 'ADMIN' ? 'VOLUNTEER' : 'ADMIN'
  const actionText = nextRole === 'ADMIN' ? '授权为管理员' : '调整为志愿者'
  ElMessageBox.confirm(`确定将“${row.realName || row.username}”${actionText}吗？`, '调整用户角色', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    await updateAdminUserRole(row.id, { role: nextRole })
    ElMessage.success('用户角色已更新')
    fetchUsers()
  }).catch(() => {})
}

const handleStatusChange = (row) => {
  const nextStatus = Number(row.status) === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'
  ElMessageBox.confirm(`确定${actionText}“${row.realName || row.username}”吗？`, '调整账号状态', {
    type: 'warning',
    confirmButtonText: actionText,
    cancelButtonText: '取消'
  }).then(async () => {
    await updateAdminUserStatus(row.id, { status: nextStatus })
    ElMessage.success('账号状态已更新')
    fetchUsers()
  }).catch(() => {})
}

onMounted(fetchUsers)
</script>

<style scoped>
.page-hero {
  min-height: 220px;
}

.hero-btn {
  height: 46px;
  border-radius: 999px;
}

.table-panel {
  padding: 22px;
}

.toolbar-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(120px, 160px) minmax(120px, 160px) auto auto;
  gap: 10px;
  margin-bottom: 14px;
}

.user-table {
  min-width: 1120px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.user-avatar {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(13, 71, 217, 0.92), rgba(47, 126, 244, 0.92));
  color: #fff;
  font-weight: 800;
}

.user-cell strong,
.user-cell small {
  display: block;
  max-width: 126px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell small {
  margin-top: 3px;
  color: #7b849d;
  font-size: 12px;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.password-tip {
  margin-bottom: 16px;
}

@media (max-width: 900px) {
  .toolbar-grid {
    grid-template-columns: 1fr;
  }

  .table-panel {
    padding: 16px;
  }

  .pagination-bar {
    justify-content: flex-start;
  }
}
</style>
