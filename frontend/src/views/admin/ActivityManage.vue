<template>
  <div class="manage-page">
    <el-card>
      <template #header>
        <div class="head">
          <h3>活动管理</h3>
          <span>共 {{ pagination.total }} 条</span>
        </div>
      </template>

      <el-table :data="activities" v-loading="loading" :row-class-name="getRowClass">
        <el-table-column label="活动标题" prop="title" min-width="220" />
        <el-table-column label="活动类型" width="120">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="服务地点" prop="location" width="150" />
        <el-table-column label="报名人数" width="120">
          <template #default="{ row }">{{ row.currentParticipants }} / {{ row.maxParticipants }}</template>
        </el-table-column>
        <el-table-column label="活动时间" width="170">
          <template #default="{ row }">{{ formatDate(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="活动阶段" width="120">
          <template #default="{ row }">
            <template v-for="p in [getActivityPhaseDisplay(row)]" :key="p.text">
              <el-tag :type="p.type">{{ p.text }}</el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="招募状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getRecruitmentDisplay(row).type">{{ getRecruitmentDisplay(row).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="460" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewDetail(row.id)">查看</el-button>
            <el-button v-if="canEdit(row)" type="warning" size="small" link @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="canEdit(row)" type="info" size="small" link @click="handleCancelActivity(row)">取消活动</el-button>
            <el-button v-if="canEdit(row)" type="success" size="small" link @click="handleCompleteActivity(row)">结项</el-button>
            <el-button type="success" size="small" link @click="openRegistrationList(row)">
              报名名单 ({{ row.currentParticipants ?? 0 }})
            </el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="fetchActivities"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="regDialogVisible"
      :title="regDialogTitle"
      width="min(920px, 96vw)"
      destroy-on-close
      @closed="registrationList = []"
    >
      <el-table :data="registrationList" v-loading="regLoading" max-height="420" stripe>
        <el-table-column label="姓名" prop="realName" width="100" />
        <el-table-column label="学号" prop="studentNo" width="120" />
        <el-table-column label="用户名" prop="username" width="120" />
        <el-table-column label="手机" prop="phone" width="130" />
        <el-table-column label="报名时间" width="170">
          <template #default="{ row }">{{ formatDate(row.registrationTime) }}</template>
        </el-table-column>
        <el-table-column label="签到" width="90">
          <template #default="{ row }">
            <el-tag :type="row.checkInStatus === 1 ? 'success' : 'info'" size="small">{{ row.checkInStatus === 1 ? '已签到' : '未签到' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核销" width="90">
          <template #default="{ row }">
            <el-tag :type="row.hoursConfirmed === 1 ? 'success' : 'warning'" size="small">{{ row.hoursConfirmed === 1 ? '已核销' : '待核销' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="dialog-footer-tip">共 {{ registrationList.length }} 人报名</span>
        <el-button type="primary" @click="regDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑活动"
      width="min(640px, 96vw)"
      destroy-on-close
      @closed="resetEditForm"
    >
      <div v-loading="editDetailLoading" class="edit-dialog-body">
        <el-form v-if="!editDetailLoading" :model="editForm" :rules="editRules" ref="editFormRef" label-width="120px">
          <el-form-item label="活动标题" prop="title">
            <el-input v-model="editForm.title" placeholder="请输入活动标题" />
          </el-form-item>
          <el-form-item label="活动类型" prop="category">
            <el-select v-model="editForm.category" placeholder="请选择活动类型" style="width: 100%">
              <el-option label="校园服务" value="校园服务" />
              <el-option label="公益助学" value="公益助学" />
              <el-option label="社区关爱" value="社区关爱" />
              <el-option label="大型活动" value="大型活动" />
              <el-option label="环保公益" value="环保公益" />
              <el-option label="应急救援" value="应急救援" />
            </el-select>
          </el-form-item>
          <el-form-item label="服务地点" prop="location">
            <el-input v-model="editForm.location" placeholder="请输入服务地点" />
          </el-form-item>
          <el-form-item label="AI生成文案">
            <el-row :gutter="10">
              <el-col :span="18">
                <el-input v-model="editAiKeywords" placeholder="关键词，例如：图书馆、值班" />
              </el-col>
              <el-col :span="6">
                <el-button type="primary" @click="handleEditAIGenerate" :loading="editAiLoading">AI生成</el-button>
              </el-col>
            </el-row>
          </el-form-item>
          <el-form-item label="活动详情" prop="description">
            <el-input v-model="editForm.description" type="textarea" :rows="6" placeholder="活动详情" />
          </el-form-item>
          <el-form-item label="活动图片">
            <ActivityImageUploader
              :image-key="editForm.imageKey"
              :image-url="editImageUrl"
              @update:image-key="(value) => { editForm.imageKey = value }"
              @update:image-url="setEditImageUrl"
            />
          </el-form-item>
          <el-form-item label="招募人数" prop="maxParticipants">
            <el-input-number v-model="editForm.maxParticipants" :min="1" :max="500" />
            <span class="form-tip">需大于等于当前已报名人数</span>
          </el-form-item>
          <el-form-item label="志愿时长" prop="volunteerHours">
            <el-input-number v-model="editForm.volunteerHours" :min="0.5" :max="24" :step="0.5" />
            <span style="margin-left: 10px">小时</span>
          </el-form-item>
          <el-form-item label="活动开始时间" prop="startTime">
            <el-date-picker
              v-model="editForm.startTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="活动结束时间" prop="endTime">
            <el-date-picker
              v-model="editForm.endTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="招募开始时间" prop="registrationStartTime">
            <el-date-picker
              v-model="editForm.registrationStartTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="报名截止时间" prop="registrationDeadline">
            <el-date-picker
              v-model="editForm.registrationDeadline"
              type="datetime"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitLoading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getActivityList,
  deleteActivity,
  getActivityRegistrations,
  getActivityDetail,
  updateActivity,
  generateDescription,
  adminCancelActivity,
  adminCompleteActivity
} from '@/api/activity'
import { getRecruitmentDisplay } from '@/utils/recruitment'
import { getActivityPhaseDisplay } from '@/utils/activityPhase'
import ActivityImageUploader from '@/components/ActivityImageUploader.vue'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)
const activities = ref([])

const editDialogVisible = ref(false)
const editDetailLoading = ref(false)
const editSubmitLoading = ref(false)
const editFormRef = ref()
const editingId = ref(null)
const editAiKeywords = ref('')
const editAiLoading = ref(false)
const editImageUrl = ref('')

const setEditImageUrl = (value) => {
  editImageUrl.value = value
}

const editForm = reactive({
  title: '',
  category: '',
  location: '',
  description: '',
  imageKey: '',
  maxParticipants: 20,
  volunteerHours: 2,
  startTime: '',
  endTime: '',
  registrationStartTime: '',
  registrationDeadline: ''
})

const editRules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  location: [{ required: true, message: '请输入服务地点', trigger: 'blur' }],
  description: [{ required: true, message: '请输入活动详情', trigger: 'blur' }],
  maxParticipants: [{ required: true, message: '请输入招募人数', trigger: 'blur' }],
  volunteerHours: [{ required: true, message: '请输入志愿时长', trigger: 'blur' }],
  startTime: [
    { required: true, message: '请选择活动开始时间', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !editForm.endTime) { callback(); return }
        if (new Date(value) >= new Date(editForm.endTime)) {
          callback(new Error('活动开始时间须早于结束时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  endTime: [
    { required: true, message: '请选择活动结束时间', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !editForm.startTime) { callback(); return }
        if (new Date(value) <= new Date(editForm.startTime)) {
          callback(new Error('活动结束时间须晚于开始时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  registrationStartTime: [
    { required: true, message: '请选择招募开始时间', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !editForm.registrationDeadline) {
          callback()
          return
        }
        if (new Date(editForm.registrationDeadline) <= new Date(value)) {
          callback(new Error('招募开始需早于报名截止'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  registrationDeadline: [
    { required: true, message: '请选择报名截止时间', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !editForm.registrationStartTime) {
          callback()
          return
        }
        if (new Date(value) <= new Date(editForm.registrationStartTime)) {
          callback(new Error('报名截止须晚于招募开始'))
          return
        }
        if (editForm.startTime && new Date(value) > new Date(editForm.startTime)) {
          callback(new Error('报名截止不能晚于活动开始'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

const regDialogVisible = ref(false)
const regLoading = ref(false)
const registrationList = ref([])
const currentActivity = ref({ id: null, title: '' })

const regDialogTitle = computed(() => {
  const t = currentActivity.value.title || '活动'
  const n = registrationList.value.length
  return `报名名单 - ${t}（${n} 人）`
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const formatDate = (date) => dayjs(date).format('YYYY-MM-DD HH:mm')

const canEdit = (row) => row.status === 'RECRUITING'

const getRowClass = ({ row }) => {
  if (row.status === 'COMPLETED') return 'row-completed'
  if (row.status === 'CANCELLED') return 'row-cancelled'
  return ''
}

const resetEditForm = () => {
  editingId.value = null
  editAiKeywords.value = ''
  editImageUrl.value = ''
  Object.assign(editForm, {
    title: '',
    category: '',
    location: '',
    description: '',
    imageKey: '',
    maxParticipants: 20,
    volunteerHours: 2,
    startTime: '',
    endTime: '',
    registrationStartTime: '',
    registrationDeadline: ''
  })
}

const openEditDialog = async (row) => {
  if (!canEdit(row)) return
  editingId.value = row.id
  editDialogVisible.value = true
  editDetailLoading.value = true
  try {
    const res = await getActivityDetail(row.id)
    const d = res.data || {}
    Object.assign(editForm, {
      title: d.title ?? '',
      category: d.category ?? '',
      location: d.location ?? '',
      description: d.description ?? '',
      imageKey: d.imageKey ?? '',
      maxParticipants: d.maxParticipants ?? 20,
      volunteerHours: d.volunteerHours != null ? Number(d.volunteerHours) : 2,
      startTime: d.startTime ?? '',
      endTime: d.endTime ?? '',
      registrationStartTime: d.registrationStartTime ?? '',
      registrationDeadline: d.registrationDeadline ?? ''
    })
    editImageUrl.value = d.imageUrl ?? ''
    await nextTick()
    editFormRef.value?.clearValidate()
  } catch (e) {
    console.error(e)
    editDialogVisible.value = false
  } finally {
    editDetailLoading.value = false
  }
}

const handleEditAIGenerate = async () => {
  if (!editForm.location || !editForm.category) {
    ElMessage.warning('请先填写活动类型和服务地点')
    return
  }
  editAiLoading.value = true
  try {
    const res = await generateDescription({
      location: editForm.location,
      category: editForm.category,
      keywords: editAiKeywords.value
    })
    editForm.description = res.data
    ElMessage.success('AI生成成功')
  } catch (e) {
    console.error(e)
  } finally {
    editAiLoading.value = false
  }
}

const submitEdit = async () => {
  if (!editingId.value) return
  await editFormRef.value.validate()
  editSubmitLoading.value = true
  try {
    await updateActivity(editingId.value, { ...editForm })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    fetchActivities()
  } catch {
    // 错误由 request 拦截器提示
  } finally {
    editSubmitLoading.value = false
  }
}

const handleCancelActivity = (row) => {
  ElMessageBox.confirm(
    '确定取消该活动？将删除该活动下全部报名记录并释放名额，已核销时长不回滚。',
    '取消活动',
    { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '关闭' }
  )
    .then(async () => {
      await adminCancelActivity(row.id)
      ElMessage.success('已取消活动')
      fetchActivities()
    })
    .catch(() => {})
}

const handleCompleteActivity = (row) => {
  ElMessageBox.confirm(
    '确定将活动标记为已结项？结项后不可再编辑或报名。',
    '活动结项',
    { type: 'warning', confirmButtonText: '确定结项', cancelButtonText: '关闭' }
  )
    .then(async () => {
      await adminCompleteActivity(row.id)
      ElMessage.success('已结项')
      fetchActivities()
    })
    .catch(() => {})
}

const viewDetail = (id) => {
  router.push(`/activity/${id}`)
}

const openRegistrationList = async (row) => {
  currentActivity.value = { id: row.id, title: row.title }
  regDialogVisible.value = true
  regLoading.value = true
  registrationList.value = []
  try {
    const res = await getActivityRegistrations(row.id)
    registrationList.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    regLoading.value = false
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm(
    '确定删除该活动？将同时删除其所有报名记录（已核销时长不回滚）。',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteActivity(id)
      ElMessage.success('删除成功')
      fetchActivities()
    } catch {
      // 错误由 request 拦截器提示
    }
  }).catch(() => {})
}

const handleSizeChange = () => {
  pagination.page = 1
  fetchActivities()
}

const fetchActivities = async () => {
  loading.value = true
  try {
    const res = await getActivityList({
      page: pagination.page,
      size: pagination.size
    })
    activities.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('获取活动列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchActivities()
})
</script>

<style scoped>
.manage-page .head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.manage-page .head h3 {
  font-size: 22px;
}

.manage-page .head span {
  font-size: 13px;
  color: #7f859c;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer-tip {
  float: left;
  line-height: 32px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.form-tip {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.edit-dialog-body {
  min-height: 120px;
}
.manage-page :deep(.el-table__inner-wrapper) {
  min-width: 1180px;
}

.manage-page :deep(.el-table__body-wrapper),
.manage-page :deep(.el-table__header-wrapper) {
  overflow-x: auto;
}

@media (max-width: 768px) {
  .pagination-bar {
    justify-content: center;
  }
}
</style>

<style>
.el-table .row-completed td {
  background-color: #f0f9eb !important;
  color: var(--el-text-color-secondary);
}

.el-table .row-cancelled td {
  background-color: #fef0f0 !important;
  color: var(--el-text-color-placeholder);
  text-decoration: line-through;
}
</style>


