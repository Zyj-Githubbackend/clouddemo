<template>
  <div class="st-page admin-page">
    <section class="st-hero page-hero">
      <div class="st-hero-content">
        <div class="st-hero-copy">
          <p class="st-hero-eyebrow">活动签到</p>
          <h1 class="st-hero-title">活动签到</h1>
          <p class="st-hero-desc">
            选择正在进行中的活动后，可为已报名的志愿者完成签到。
          </p>
        </div>
        <div class="st-hero-actions">
          <span class="st-chip">
            <el-icon><Checked /></el-icon>
            可签到活动 {{ checkInActivities.length }} 个
          </span>
        </div>
      </div>
    </section>

    <section class="st-stat-grid">
      <article class="st-stat-card">
        <span class="label">可选活动</span>
        <strong class="value">{{ checkInActivities.length }}</strong>
        <span class="hint">当前可执行签到的活动</span>
      </article>
      <article class="st-stat-card">
        <span class="label">报名记录</span>
        <strong class="value">{{ registrations.length }}</strong>
        <span class="hint">当前活动下的报名人数</span>
      </article>
      <article class="st-stat-card">
        <span class="label">已签到</span>
        <strong class="value">{{ checkedCount }}</strong>
        <span class="hint">已完成签到的志愿者</span>
      </article>
    </section>

    <section class="st-panel table-panel">
      <div class="st-section-head">
        <div>
          <h2>签到工作台</h2>
          <p>选择活动后，可为已报名志愿者完成签到。</p>
        </div>
      </div>

      <div class="toolbar-row">
        <span class="label">选择活动</span>
        <el-select
          v-model="selectedActivityId"
          filterable
          clearable
          placeholder="请选择进行中的活动"
          style="width: min(520px, 100%)"
          :loading="activitiesLoading"
          @change="onActivityChange"
        >
          <el-option
            v-for="a in checkInActivities"
            :key="a.id"
            :label="activityOptionLabel(a)"
            :value="a.id"
          />
        </el-select>
      </div>

      <div v-if="selectedActivity" class="current-card">
        <strong>{{ selectedActivity.title }}</strong>
        <span>{{ formatDate(selectedActivity.startTime) }} ~ {{ formatDate(selectedActivity.endTime) }}</span>
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
            <el-table-column label="报名时间" width="170">
              <template #default="{ row }">{{ formatDate(row.registrationTime) }}</template>
            </el-table-column>
            <el-table-column label="签到状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.checkInStatus === 1 ? 'success' : 'info'">{{ row.checkInStatus === 1 ? '已签到' : '未签到' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="签到时间" width="170">
              <template #default="{ row }">{{ row.checkInTime ? formatDate(row.checkInTime) : '--' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.checkInStatus !== 1" type="primary" size="small" @click="handleCheckIn(row)">
                  标记签到
                </el-button>
                <el-tag v-else type="success" size="small">已签到</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <el-empty v-else-if="!activitiesLoading && checkInActivities.length === 0" description="当前没有可签到活动" />
      <el-empty v-else-if="!activitiesLoading" description="请先在上方选择一个活动" />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActivityRegistrations, getCheckInActivitiesForAdmin, adminCheckInRegistration } from '@/api/activity'
import dayjs from 'dayjs'

const activitiesLoading = ref(false)
const checkInActivities = ref([])
const selectedActivityId = ref(null)
const loading = ref(false)
const registrations = ref([])

const selectedActivity = computed(() => checkInActivities.value.find((a) => a.id === selectedActivityId.value))
const checkedCount = computed(() => registrations.value.filter((row) => row.checkInStatus === 1).length)

const formatDate = (date) => {
  if (!date) return '--'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const activityOptionLabel = (a) => {
  const start = formatDate(a.startTime)
  const end = formatDate(a.endTime)
  return `${a.title}（${start} ~ ${end}）`
}

const fetchCheckInActivities = async () => {
  activitiesLoading.value = true
  try {
    const res = await getCheckInActivitiesForAdmin()
    checkInActivities.value = res.data || []
  } catch (e) {
    console.error(e)
    checkInActivities.value = []
  } finally {
    activitiesLoading.value = false
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

const handleCheckIn = (row) => {
  const name = row.realName || '该志愿者'
  ElMessageBox.confirm(`确定将“${name}”标记签到吗？`, '活动签到', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  })
    .then(async () => {
      try {
        await adminCheckInRegistration(row.id)
        ElMessage.success('签到成功')
        fetchRegistrations()
        fetchCheckInActivities()
      } catch (e) {
        console.error(e)
      }
    })
    .catch(() => {})
}

onMounted(() => {
  fetchCheckInActivities()
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

.reg-table {
  min-width: 940px;
}

@media (max-width: 768px) {
  .table-panel {
    padding: 16px;
  }
}
</style>
