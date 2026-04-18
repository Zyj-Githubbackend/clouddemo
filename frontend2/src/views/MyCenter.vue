<template>
  <Layout>
    <div class="st-page page-container history-page">
      <section class="st-hero history-hero">
        <div class="st-hero-content">
          <div class="st-hero-copy">
            <p class="st-hero-eyebrow">志愿足迹</p>
            <h1 class="st-hero-title">我的志愿足迹</h1>
            <p class="st-hero-desc">
              报名记录按活动开始时间倒序展示，并保留导出已核销记录与取消报名能力。
            </p>
          </div>

          <div class="st-hero-actions">
            <span class="st-chip">
              <el-icon><Clock /></el-icon>
              已加载 {{ displayedRegistrations.length }} 条
            </span>
            <el-button
              class="hero-btn"
              type="primary"
              :loading="exporting"
              :disabled="confirmedRegistrations.length === 0"
              @click="handleExport"
            >
              导出已核销记录
            </el-button>
          </div>
        </div>
      </section>

      <section class="st-stat-grid">
        <article class="st-stat-card">
          <span class="label">活动记录</span>
          <strong class="value">{{ registrations.length }}</strong>
          <span class="hint">全部报名记录</span>
        </article>
        <article class="st-stat-card">
          <span class="label">已核销活动</span>
          <strong class="value">{{ confirmedRegistrations.length }}</strong>
          <span class="hint">已完成核销的报名记录</span>
        </article>
        <article class="st-stat-card">
          <span class="label">已核销时长</span>
          <strong class="value">{{ confirmedHours }}</strong>
          <span class="hint">单位：小时</span>
        </article>
      </section>

      <section class="st-panel history-panel">
        <div class="st-section-head">
          <div>
            <h2>志愿记录</h2>
            <p>查看报名进度、签到状态和志愿时长核销情况。</p>
          </div>
        </div>

        <div class="toolbar-row">
          <div class="toolbar-switch">
            <span>仅看已核销</span>
            <el-switch v-model="showConfirmedOnly" />
          </div>
          <span class="toolbar-tip">
            {{ showConfirmedOnly ? '仅显示已核销记录' : '显示全部记录' }}
          </span>
        </div>

        <div v-if="!loading && displayedRegistrations.length === 0" class="st-empty">
          <el-empty :description="showConfirmedOnly ? '暂无已核销记录' : '还没有报名记录'" />
        </div>

        <div v-else class="history-list" v-loading="loading">
          <article v-for="row in displayedRegistrations" :key="`${row.activityId}-${row.registrationTime}`" class="history-item">
            <div class="history-icon">
              <el-icon v-if="row.hoursConfirmed === 1"><Medal /></el-icon>
              <el-icon v-else-if="row.checkInStatus === 1"><CircleCheck /></el-icon>
              <el-icon v-else><Calendar /></el-icon>
            </div>

            <div class="history-main">
              <div class="title-row">
                <h3>{{ row.activityTitle }}</h3>
                <span :class="['status-pill', row.hoursConfirmed === 1 ? 'success' : row.checkInStatus === 1 ? 'info' : 'muted']">
                  {{ row.hoursConfirmed === 1 ? '已核销' : row.checkInStatus === 1 ? '已签到' : '待签到' }}
                </span>
              </div>

              <div class="meta-row">
                <span><el-icon><Clock /></el-icon>{{ row.volunteerHours }} 小时</span>
                <span><el-icon><Calendar /></el-icon>活动 {{ formatShortDate(row.startTime) }}</span>
                <span><el-icon><Tickets /></el-icon>报名 {{ formatShortDate(row.registrationTime) }}</span>
                <span><el-icon><Flag /></el-icon>{{ row.status === 'REGISTERED' ? '已报名' : '已取消' }}</span>
              </div>

              <div class="extra-row">
                <span>核销时间：{{ formatFullDate(row.confirmTime) }}</span>
              </div>
            </div>

            <div class="history-actions">
              <button
                v-if="canCancel(row)"
                type="button"
                class="danger-link"
                :disabled="cancellingActivityId === row.activityId"
                @click="handleCancel(row)"
              >
                {{ cancellingActivityId === row.activityId ? '处理中...' : '取消报名' }}
              </button>
              <span v-else class="action-muted">--</span>
            </div>
          </article>
        </div>
      </section>
    </div>
  </Layout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import Layout from '@/components/Layout.vue'
import { cancelMyRegistration, exportConfirmedMyRegistrations, getMyRegistrations } from '@/api/activity'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const exporting = ref(false)
const cancellingActivityId = ref(null)
const showConfirmedOnly = ref(false)
const registrations = ref([])

const confirmedRegistrations = computed(() =>
  registrations.value.filter(item => item.status === 'REGISTERED' && item.hoursConfirmed === 1)
)

const displayedRegistrations = computed(() =>
  showConfirmedOnly.value ? confirmedRegistrations.value : registrations.value
)

const confirmedHours = computed(() =>
  confirmedRegistrations.value
    .reduce((sum, item) => sum + Number(item.volunteerHours ?? 0), 0)
    .toFixed(2)
)

const formatShortDate = (date) => {
  if (!date) return '--'
  return dayjs(date).format('MM-DD HH:mm')
}

const formatFullDate = (date) => {
  if (!date) return '--'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const canCancel = (row) => {
  if (!row || row.status !== 'REGISTERED') return false
  if (row.checkInStatus === 1 || row.hoursConfirmed === 1) return false
  if (!row.startTime) return false
  return dayjs().isBefore(dayjs(row.startTime))
}

const sanitizeFilenamePart = (value) => {
  if (!value) return ''
  return String(value)
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, '')
    .trim()
}

const buildExportFileName = (serverFileName = '') => {
  const parts = [
    '志愿足迹',
    sanitizeFilenamePart(userStore.userInfo.realName),
    sanitizeFilenamePart(userStore.userInfo.studentNo),
    '已核销记录',
    dayjs().format('YYYYMMDDHHmmss')
  ].filter(Boolean)

  if (parts.length > 0) {
    return `${parts.join('-')}.xlsx`
  }
  return serverFileName || 'my-volunteer-footprint-confirmed.xlsx'
}

const downloadBlob = (blob, fileName) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

const parseFileName = (disposition) => {
  if (!disposition) return ''
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) return decodeURIComponent(utf8Match[1])
  const plainMatch = disposition.match(/filename="?([^"]+)"?/i)
  return plainMatch?.[1] || ''
}

const fetchData = async () => {
  loading.value = true
  try {
    const regRes = await getMyRegistrations()
    const data = regRes.data || []
    registrations.value = data.sort((a, b) => {
      const timeA = new Date(a.startTime || 0).getTime()
      const timeB = new Date(b.startTime || 0).getTime()
      return timeB - timeA
    })
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(`确认取消《${row.activityTitle}》的报名吗？`, '确认取消报名', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '再想想'
    })
    cancellingActivityId.value = row.activityId
    await cancelMyRegistration(row.activityId)
    ElMessage.success('已取消报名')
    await fetchData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('取消报名失败:', error)
    }
  } finally {
    cancellingActivityId.value = null
  }
}

const handleExport = async () => {
  if (confirmedRegistrations.value.length === 0) {
    ElMessage.warning('暂无已核销记录可导出')
    return
  }

  exporting.value = true
  try {
    const response = await exportConfirmedMyRegistrations()
    const serverFileName = parseFileName(response.headers['content-disposition'])
    const fileName = buildExportFileName(serverFileName)
    downloadBlob(response.data, fileName)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出已核销记录失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.history-page {
  max-width: 1180px;
}

.history-hero {
  min-height: 240px;
}

.hero-btn {
  height: 46px;
  border-radius: 999px;
}

.history-panel {
  padding: 22px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.toolbar-switch {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--cv-text);
  font-weight: 700;
}

.toolbar-tip {
  color: #7a8198;
  font-size: 13px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(201, 214, 243, 0.5);
}

.history-icon {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: rgba(13, 71, 217, 0.08);
  color: var(--cv-primary);
  font-size: 20px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.title-row h3 {
  font-size: 20px;
  line-height: 1.35;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.success {
  background: rgba(15, 139, 95, 0.12);
  color: #0f8b5f;
}

.status-pill.info {
  background: rgba(13, 71, 217, 0.08);
  color: var(--cv-primary);
}

.status-pill.muted {
  background: rgba(120, 131, 157, 0.12);
  color: #6f7b97;
}

.meta-row,
.extra-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
  color: #74819d;
  font-size: 13px;
}

.meta-row span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.history-actions {
  min-width: 92px;
  text-align: right;
}

.danger-link {
  padding: 0;
  border: none;
  background: transparent;
  color: #c73638;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

.danger-link:disabled,
.action-muted {
  color: #97a0b7;
}

@media (max-width: 768px) {
  .history-panel {
    padding: 16px;
  }

  .history-item {
    grid-template-columns: 1fr;
  }

  .history-actions {
    text-align: left;
  }
}
</style>
