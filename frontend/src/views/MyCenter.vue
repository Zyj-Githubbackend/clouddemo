<template>
  <Layout>
    <div class="my-center-container">
      <el-card class="footprint-card" shadow="never">
        <template #header>
          <div class="footprint-header">
            <div class="header-left">
              <el-icon class="header-icon"><Stamp /></el-icon>
              <span>我的志愿足迹</span>
            </div>
            <span class="footprint-total">共 {{ registrations.length }} 条记录</span>
          </div>
        </template>

        <el-table
          :data="registrations"
          v-loading="loading"
          empty-text="暂无报名记录"
          stripe
        >
          <el-table-column label="活动名称" prop="activityTitle" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="activity-title-text">{{ row.activityTitle }}</span>
            </template>
          </el-table-column>
          <el-table-column label="时长" width="80" align="center">
            <template #default="{ row }">
              <span class="hours-pill">{{ row.volunteerHours }}h</span>
            </template>
          </el-table-column>
          <el-table-column label="活动时间" width="120">
            <template #default="{ row }">
              <span class="date-sm">{{ formatDate(row.startTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="报名时间" width="120">
            <template #default="{ row }">
              <span class="date-sm">{{ formatDate(row.registrationTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="签到" width="90" align="center">
            <template #default="{ row }">
              <el-tag
                :type="row.checkInStatus === 1 ? 'success' : 'info'"
                size="small"
                effect="light"
              >
                {{ row.checkInStatus === 1 ? '已签到' : '未签到' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时长核销" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                :type="row.hoursConfirmed === 1 ? 'success' : 'warning'"
                size="small"
                effect="light"
              >
                {{ row.hoursConfirmed === 1 ? '已核销' : '待核销' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 'REGISTERED' ? 'primary' : 'info'"
                size="small"
                effect="plain"
              >
                {{ row.status === 'REGISTERED' ? '已报名' : '已取消' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && registrations.length === 0" description="还没有报名记录，快去参加志愿活动吧！" />
      </el-card>
    </div>
  </Layout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Layout from '@/components/Layout.vue'
import { getMyRegistrations } from '@/api/activity'
import dayjs from 'dayjs'

const loading = ref(false)
const registrations = ref([])

const formatDate = (date) => {
  if (!date) return '—'
  return dayjs(date).format('MM-DD HH:mm')
}

const fetchData = async () => {
  loading.value = true
  try {
    const regRes = await getMyRegistrations()
    const data = regRes.data || []
    // 按活动开始时间从近到远排序（最新的在前面）
    registrations.value = data.sort((a, b) => {
      const timeA = new Date(a.startTime).getTime()
      const timeB = new Date(b.startTime).getTime()
      return timeB - timeA // 降序：大的在前（最近的活动）
    })
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.my-center-container {
  max-width: 1280px;
  margin: 0 auto;
}

/* 足迹卡片 */
.footprint-card {
  border-radius: 14px;
  border: 1px solid #f0f0f0;
}

.footprint-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.header-icon { color: #667eea; }

.footprint-total {
  font-size: 13px;
  color: #909399;
}

.activity-title-text {
  font-weight: 500;
  color: #303133;
}

.hours-pill {
  display: inline-block;
  background: linear-gradient(135deg, #667eea20, #764ba220);
  color: #667eea;
  font-weight: 600;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 6px;
}

.date-sm {
  font-size: 12px;
  color: #606266;
}

@media (max-width: 768px) {
  .footprint-card { margin-top: 16px; }
}
</style>
