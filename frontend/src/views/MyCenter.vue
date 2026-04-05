<template>
  <Layout>
    <div class="my-page page-container">
      <section class="my-hero">
        <div>
          <p class="mini">My Records</p>
          <h1>我的志愿足迹</h1>
          <p>按活动时间倒序展示报名记录，并支持导出已核销志愿时长与对应活动明细。</p>
        </div>
        <div class="hero-actions">
          <div class="count-chip">当前 {{ displayedRegistrations.length }} 条</div>
          <el-button
            class="export-btn"
            type="primary"
            plain
            :loading="exporting"
            :disabled="confirmedRegistrations.length === 0"
            @click="handleExport"
          >
            导出已核销记录
          </el-button>
        </div>
      </section>

      <section class="summary-grid">
        <el-card shadow="never" class="summary-card">
          <span>已核销活动数</span>
          <strong>{{ confirmedRegistrations.length }}</strong>
        </el-card>
        <el-card shadow="never" class="summary-card">
          <span>已核销志愿时长</span>
          <strong>{{ confirmedHours }} 小时</strong>
        </el-card>
      </section>

      <el-card class="table-card" shadow="never">
        <div class="table-toolbar">
          <el-switch
            v-model="showConfirmedOnly"
            inline-prompt
            active-text="仅看已核销"
            inactive-text="显示全部"
          />
          <span class="toolbar-tip">
            {{ showConfirmedOnly ? '当前仅展示已核销记录' : '当前展示全部报名记录' }}
          </span>
        </div>

        <div class="table-scroll">
          <el-table :data="displayedRegistrations" v-loading="loading" :empty-text="emptyText" stripe>
            <el-table-column label="活动名称" prop="activityTitle" min-width="200" show-overflow-tooltip />
            <el-table-column label="时长" width="90" align="center">
              <template #default="{ row }">
                <span class="hours-pill">{{ row.volunteerHours }}h</span>
              </template>
            </el-table-column>
            <el-table-column label="活动时间" width="140">
              <template #default="{ row }">{{ formatShortDate(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="报名时间" width="140">
              <template #default="{ row }">{{ formatShortDate(row.registrationTime) }}</template>
            </el-table-column>
            <el-table-column label="核销时间" width="170">
              <template #default="{ row }">{{ formatFullDate(row.confirmTime) }}</template>
            </el-table-column>
            <el-table-column label="签到" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.checkInStatus === 1 ? 'success' : 'info'" effect="light">
                  {{ row.checkInStatus === 1 ? '已签到' : '未签到' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时长核销" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="row.hoursConfirmed === 1 ? 'success' : 'warning'" effect="light">
                  {{ row.hoursConfirmed === 1 ? '已核销' : '待核销' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="96" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'REGISTERED' ? 'primary' : 'info'" effect="plain">
                  {{ row.status === 'REGISTERED' ? '已报名' : '已取消' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-empty
          v-if="!loading && displayedRegistrations.length === 0"
          :description="showConfirmedOnly ? '暂无已核销记录' : '还没有报名记录，快去参加志愿活动吧'"
        />
      </el-card>
    </div>
  </Layout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import Layout from '@/components/Layout.vue'
import { exportConfirmedMyRegistrations, getMyRegistrations } from '@/api/activity'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const exporting = ref(false)
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

const emptyText = computed(() =>
  showConfirmedOnly.value ? '暂无已核销记录' : '暂无报名记录'
)

const formatShortDate = (date) => {
  if (!date) return '--'
  return dayjs(date).format('MM-DD HH:mm')
}

const formatFullDate = (date) => {
  if (!date) return '--'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
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
      const timeA = new Date(a.startTime).getTime()
      const timeB = new Date(b.startTime).getTime()
      return timeB - timeA
    })
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
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
.my-page {
  width: 100%;
  margin: 0 auto;
}

.my-hero {
  margin-bottom: 16px;
  border-radius: 24px;
  padding: 28px 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  color: #fff;
  background: linear-gradient(135deg, var(--cv-primary), var(--cv-primary-weak));
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.mini {
  font-size: 12px;
  letter-spacing: 1.4px;
  text-transform: uppercase;
  opacity: 0.9;
}

.my-hero h1 {
  font-size: clamp(26px, 4vw, 34px);
  margin: 8px 0;
  color: #fff;
}

.my-hero p {
  margin: 0;
  opacity: 0.92;
}

.count-chip {
  border-radius: 999px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.2);
  font-weight: 600;
  white-space: nowrap;
}

.export-btn {
  border-color: rgba(255, 255, 255, 0.7);
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}

.export-btn:hover,
.export-btn:focus-visible {
  color: #fff;
  border-color: #fff;
  background: rgba(255, 255, 255, 0.2);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.summary-card {
  border-radius: 18px;
}

.summary-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-card span {
  color: #737990;
  font-size: 13px;
}

.summary-card strong {
  font-size: 28px;
  color: var(--cv-primary);
}

.table-card {
  width: 100%;
  border-radius: 18px;
}

.table-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.toolbar-tip {
  color: #737990;
  font-size: 13px;
}

.table-scroll {
  width: 100%;
}

.table-scroll :deep(.el-table) {
  width: 100%;
}

.hours-pill {
  border-radius: 999px;
  padding: 2px 8px;
  background: rgba(13, 71, 217, 0.12);
  color: var(--cv-primary);
  font-weight: 700;
}

@media (max-width: 768px) {
  .my-hero {
    padding: 22px 16px;
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
