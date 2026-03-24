<template>
  <Layout>
    <div class="activity-detail-container">
      <el-card v-loading="loading">
        <template #header>
          <div class="header">
            <h2>{{ activity.title }}</h2>
            <div class="header-tags">
              <el-tag :type="getStatusType(activity.status)" size="large">
                {{ getStatusText(activity.status) }}
              </el-tag>
              <el-tag :type="recruitmentDisplay.type" size="large">
                {{ recruitmentDisplay.text }}
              </el-tag>
            </div>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="活动类型">
            <el-tag>{{ activity.category }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="志愿时长">
            {{ activity.volunteerHours }} 小时
          </el-descriptions-item>
          <el-descriptions-item label="服务地点">
            <el-icon><Location /></el-icon>
            {{ activity.location }}
          </el-descriptions-item>
          <el-descriptions-item label="招募人数">
            {{ activity.currentParticipants }} / {{ activity.maxParticipants }}
            <el-progress 
              :percentage="Math.round((activity.currentParticipants / activity.maxParticipants) * 100)" 
              style="margin-top: 5px"
            />
          </el-descriptions-item>
          <el-descriptions-item label="活动时间">
            {{ formatDate(activity.startTime) }} 至 {{ formatDate(activity.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="招募开始">
            {{ activity.registrationStartTime ? formatDate(activity.registrationStartTime) : '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="报名截止">
            {{ formatDate(activity.registrationDeadline) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <div class="description-section">
          <h3>活动详情</h3>
          <div class="description-content">
            {{ activity.description }}
          </div>
        </div>

        <el-divider />

        <div class="action-section">
          <el-button 
            v-if="canRegister" 
            type="primary" 
            size="large"
            @click="handleRegister"
            :disabled="activity.availableSlots <= 0"
          >
            <el-icon><Check /></el-icon>
            {{ activity.availableSlots > 0 ? '立即报名' : '名额已满' }}
          </el-button>
          <el-tag v-else-if="activity.isRegistered" type="success" size="large">
            <el-icon><CircleCheck /></el-icon>
            已报名
          </el-tag>
          <el-tag v-else type="info" size="large">
            活动暂不可报名
          </el-tag>
        </div>
      </el-card>
    </div>
  </Layout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Layout from '@/components/Layout.vue'
import { getActivityDetail, registerActivity } from '@/api/activity'
import { getRecruitmentDisplay } from '@/utils/recruitment'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activity = ref({})

const recruitmentDisplay = computed(() => getRecruitmentDisplay(activity.value))
const canRegister = computed(() => {
  const a = activity.value
  return (
    !a.isRegistered
    && a.status === 'RECRUITING'
    && recruitmentDisplay.value.text === '招募中'
  )
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

const fetchActivity = async () => {
  loading.value = true
  try {
    const res = await getActivityDetail(route.params.id)
    activity.value = res.data || {}
  } catch (error) {
    console.error('获取活动详情失败:', error)
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  try {
    await registerActivity(route.params.id)
    ElMessage.success('报名成功！')
    fetchActivity()
  } catch (error) {
    console.error('报名失败:', error)
  }
}

onMounted(() => {
  fetchActivity()
})
</script>

<style scoped>
.activity-detail-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
}

.header-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.description-section {
  margin: 20px 0;
}

.description-section h3 {
  margin-bottom: 15px;
  color: #333;
}

.description-content {
  line-height: 1.8;
  color: #666;
  white-space: pre-wrap;
}

.action-section {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
