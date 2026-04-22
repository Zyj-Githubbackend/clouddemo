<template>
  <div class="st-page admin-page">
    <section class="st-hero page-hero">
      <div class="st-hero-content">
        <div class="st-hero-copy">
          <p class="st-hero-eyebrow">时长核销</p>
          <h1 class="st-hero-title">时长核销管理</h1>
          <p class="st-hero-desc">
            选择已结束活动后，可集中处理志愿者的时长核销工作。
          </p>
        </div>
        <div class="st-hero-actions">
          <span class="st-chip">
            <el-icon><Clock /></el-icon>
            可选活动 {{ endedActivities.length }} 个
          </span>
        </div>
      </div>
    </section>

    <section class="st-stat-grid">
      <article class="st-stat-card">
        <span class="label">待选活动</span>
        <strong class="value">{{ endedActivities.length }}</strong>
        <span class="hint">可选择的已结束活动</span>
      </article>
      <article class="st-stat-card">
        <span class="label">当前记录</span>
        <strong class="value">{{ registrations.length }}</strong>
        <span class="hint">已加载报名记录</span>
      </article>
      <article class="st-stat-card">
        <span class="label">可核销</span>
        <strong class="value">{{ confirmableCount }}</strong>
        <span class="hint">已签到且未核销</span>
      </article>
    </section>

    <section class="st-panel table-panel">
      <div class="st-section-head">
        <div>
          <h2>核销工作台</h2>
          <p>活动结束后，可在这里为已签到志愿者核销服务时长。</p>
        </div>
      </div>

      <div class="toolbar-row">
        <span class="label">选择活动</span>
        <el-select
          v-model="selectedActivityId"
          filterable
          clearable
          placeholder="请选择已结束且未结项活动"
          style="width: min(520px, 100%)"
          :loading="endedLoading"
          @change="onActivityChange"
        >
          <el-option
            v-for="a in endedActivities"
            :key="a.id"
            :label="activityOptionLabel(a)"
            :value="a.id"
          />
        </el-select>
      </div>

      <div v-if="selectedActivity" class="current-card">
        <strong>{{ selectedActivity.title }}</strong>
        <span>结束时间 {{ formatDate(selectedActivity.endTime) }}</span>
      </div>

      <template v-if="selectedActivityId">
        <div class="table-scroll">
          <el-table :data="registrations" v-loading="loading" stripe class="reg-table" empty-text="暂无报名记录">
            <el-table-column label="姓名" width="120">
              <template #default="{ row }">{{ row.realName || '-' }}</template>
            </el-table-column>
            <el-table-column label="学号" width="120">
              <template #default="{ row }">{{ row.studentNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="手机" prop="phone" width="130" />
            <el-table-column label="志愿时长" width="100">
              <template #default="{ row }">{{ row.volunteerHours }} 小时</template>
            </el-table-column>
            <el-table-column label="签到状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.checkInStatus === 1 ? 'success' : 'info'">{{ row.checkInStatus === 1 ? '已签到' : '未签到' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="核销状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.hoursConfirmed === 1 ? 'success' : 'warning'">{{ row.hoursConfirmed === 1 ? '已核销' : '待核销' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="核销时间" width="160">
              <template #default="{ row }">{{ row.confirmTime ? formatDate(row.confirmTime) : '--' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.hoursConfirmed === 0 && row.checkInStatus === 1"
                  type="primary"
                  size="small"
                  @click="handleConfirm(row)"
                >
                  核销
                </el-button>
                <el-tag v-else-if="row.hoursConfirmed === 1" type="success" size="small">已核销</el-tag>
                <span v-else class="tip-muted">需先签到</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <el-empty v-else-if="!endedLoading && endedActivities.length === 0" description="暂无待核销活动" />
      <el-empty v-else-if="!endedLoading" description="请先在上方选择一个活动" />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { confirmHours, getActivityRegistrations, getEndedActivitiesForAdmin } from '@/api/activity'
import dayjs from 'dayjs'

const endedLoading = ref(false)
const endedActivities = ref([])
const selectedActivityId = ref(null)
const loading = ref(false)
const registrations = ref([])

const selectedActivity = computed(() => endedActivities.value.find((a) => a.id === selectedActivityId.value))
const confirmableCount = computed(() =>
  registrations.value.filter((row) => row.hoursConfirmed === 0 && row.checkInStatus === 1).length
)

const formatDate = (date) => {
  if (!date) return '--'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const activityOptionLabel = (a) => `${a.title}（${formatDate(a.endTime)} 结束）`

const fetchEndedActivities = async () => {
  endedLoading.value = true
  try {
    const res = await getEndedActivitiesForAdmin()
    endedActivities.value = res.data || []
  } catch (e) {
    console.error(e)
    endedActivities.value = []
  } finally {
    endedLoading.value = false
  }
}

const onActivityChange = () => {
  registrations.value = []
  if (!selectedActivityId.value) return
  fetchRegistrations()
}

const fetchRegistrations = async () => {
  if (!selectedActivityId.value) return
  loading.value = true
  try {
    const res = await getActivityRegistrations(selectedActivityId.value)
    registrations.value = res.data || []
  } catch (e) {
    console.error(e)
    registrations.value = []
  } finally {
    loading.value = false
  }
}

const handleConfirm = (row) => {
  ElMessageBox.confirm(
    `确定核销“${row.realName || '该志愿者'}”的 ${row.volunteerHours} 小时时长吗？`,
    '确认核销',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await confirmHours(row.id)
      ElMessage.success('核销成功')
      fetchRegistrations()
      fetchEndedActivities()
    } catch (error) {
      console.error('核销失败:', error)
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchEndedActivities()
})
</script>

<style scoped>
.page-hero {
  min-height: 220px;
}

.table-panel {
  padding: 22px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.label {
  font-size: 14px;
  font-weight: 700;
  color: var(--cv-text);
}

.current-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 14px;
  border-radius: 18px;
  background: rgba(13, 71, 217, 0.06);
  color: #425171;
  flex-wrap: wrap;
}

.tip-muted {
  font-size: 12px;
  color: #8990a8;
}

.reg-table {
  min-width: 930px;
}

@media (max-width: 768px) {
  .table-panel {
    padding: 16px;
  }
}
</style>
