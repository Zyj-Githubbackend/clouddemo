<template>
  <div>
    <el-card>
      <template #header>
        <h3>活动管理</h3>
      </template>

      <el-table :data="activities" v-loading="loading">
        <el-table-column label="活动标题" prop="title" min-width="200" />
        <el-table-column label="活动类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="服务地点" prop="location" width="150" />
        <el-table-column label="报名人数" width="120">
          <template #default="{ row }">
            {{ row.currentParticipants }} / {{ row.maxParticipants }}
          </template>
        </el-table-column>
        <el-table-column label="活动时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="活动状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="招募状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getRecruitmentDisplay(row).type">
              {{ getRecruitmentDisplay(row).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewDetail(row.id)">
              查看
            </el-button>
            <el-button type="success" size="small" link @click="openRegistrationList(row)">
              报名名单 ({{ row.currentParticipants ?? 0 }})
            </el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchActivities"
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
          <template #default="{ row }">
            {{ formatDate(row.registrationTime) }}
          </template>
        </el-table-column>
        <el-table-column label="签到" width="90">
          <template #default="{ row }">
            <el-tag :type="row.checkInStatus === 1 ? 'success' : 'info'" size="small">
              {{ row.checkInStatus === 1 ? '已签到' : '未签到' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核销" width="90">
          <template #default="{ row }">
            <el-tag :type="row.hoursConfirmed === 1 ? 'success' : 'warning'" size="small">
              {{ row.hoursConfirmed === 1 ? '已核销' : '待核销' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="dialog-footer-tip">共 {{ registrationList.length }} 人报名</span>
        <el-button type="primary" @click="regDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActivityList, deleteActivity, getActivityRegistrations } from '@/api/activity'
import { getRecruitmentDisplay } from '@/utils/recruitment'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)
const activities = ref([])

const regDialogVisible = ref(false)
const regLoading = ref(false)
const registrationList = ref([])
const currentActivity = ref({ id: null, title: '' })

const regDialogTitle = computed(() => {
  const t = currentActivity.value.title || '活动'
  const n = registrationList.value.length
  return `报名名单 — ${t}（${n} 人）`
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const getStatusType = (status) => {
  const map = {
    'RECRUITING': 'success',
    'ONGOING': 'warning',
    'COMPLETED': 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    'RECRUITING': '招募中',
    'ONGOING': '进行中',
    'COMPLETED': '已结项'
  }
  return map[status] || status
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
    '确定删除该活动？将同时删除其所有报名记录（已核销的用户累计时长不会回滚）。',
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
      /* 错误信息由 request 拦截器提示 */
    }
  }).catch(() => {})
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
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer-tip {
  float: left;
  line-height: 32px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
