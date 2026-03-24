<template>
  <Layout>
    <div class="home-container">
      <el-row :gutter="20">
        <!-- 数据统计卡片 -->
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-icon" style="background: #409eff20">
                <el-icon :size="30" color="#409eff"><Timer /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ userInfo.totalVolunteerHours || 0 }}</div>
                <div class="stat-label">志愿时长(h)</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-icon" style="background: #67c23a20">
                <el-icon :size="30" color="#67c23a"><Flag /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ registrationCount }}</div>
                <div class="stat-label">参与活动</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-icon" style="background: #e6a23c20">
                <el-icon :size="30" color="#e6a23c"><Trophy /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ completedCount }}</div>
                <div class="stat-label">已完成</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-icon" style="background: #f5622120">
                <el-icon :size="30" color="#f56221"><Star /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ confirmedCount }}</div>
                <div class="stat-label">已核销</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 最新活动 -->
      <el-card class="section-card">
        <template #header>
          <div class="card-header">
            <span>🔥 最新活动</span>
            <el-button type="primary" @click="$router.push('/activities')">
              查看更多
            </el-button>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" :lg="8" v-for="activity in activities" :key="activity.id">
            <el-card class="activity-card" @click="goToDetail(activity.id)">
              <div class="activity-header">
                <el-tag :type="getRecruitmentDisplay(activity).type" size="small">
                  {{ getRecruitmentDisplay(activity).text }}
                </el-tag>
                <el-tag type="info" size="small">{{ activity.category }}</el-tag>
              </div>
              <h3>{{ activity.title }}</h3>
              <div class="activity-info">
                <div class="info-item">
                  <el-icon><Location /></el-icon>
                  <span>{{ activity.location }}</span>
                </div>
                <div class="info-item">
                  <el-icon><Clock /></el-icon>
                  <span>{{ activity.volunteerHours }} 小时</span>
                </div>
                <div class="info-item">
                  <el-icon><User /></el-icon>
                  <span>{{ activity.currentParticipants }} / {{ activity.maxParticipants }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-card>
    </div>
  </Layout>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import Layout from '@/components/Layout.vue'
import { getActivityList, getMyRegistrations } from '@/api/activity'
import { getRecruitmentDisplay } from '@/utils/recruitment'
import { getUserInfo } from '@/api/user'

const router = useRouter()
const activities = ref([])
const registrations = ref([])
const userInfo = ref({})

const registrationCount = computed(() => registrations.value.length)
const completedCount = computed(() => {
  return registrations.value.filter(r => r.hoursConfirmed === 1).length
})
const confirmedCount = computed(() => {
  return registrations.value.filter(r => r.hoursConfirmed === 1).length
})

const goToDetail = (id) => {
  router.push(`/activity/${id}`)
}

const fetchData = async () => {
  try {
    const [actRes, regRes, userRes] = await Promise.all([
      getActivityList({ page: 1, size: 6, status: 'RECRUITING' }),
      getMyRegistrations(),
      getUserInfo()
    ])
    
    activities.value = actRes.data.records || []
    registrations.value = regRes.data || []
    userInfo.value = userRes.data || {}
  } catch (error) {
    console.error('获取数据失败:', error)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.home-container {
  padding: 0;
  max-width: 1400px;
  margin: 0 auto;
}

.stat-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-card :deep(.el-card__body) {
  padding: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #333;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.section-card {
  margin-top: 20px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.section-card :deep(.el-card__header) {
  padding: 20px 24px;
  background: linear-gradient(to right, #f8f9fa, #fff);
  border-bottom: 2px solid #f0f0f0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.card-header span {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.activity-card {
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  height: 100%;
}

.activity-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 28px rgba(102, 126, 234, 0.2);
}

.activity-card :deep(.el-card__body) {
  padding: 20px;
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.activity-card h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px 0;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 44px;
  line-height: 1.4;
}

.activity-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.info-item .el-icon {
  color: #909399;
  flex-shrink: 0;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .stat-value {
    font-size: 28px;
  }
  
  .stat-label {
    font-size: 12px;
  }
}

@media (max-width: 992px) {
  .stat-card :deep(.el-card__body) {
    padding: 20px;
  }
  
  .stat-icon {
    width: 50px;
    height: 50px;
  }
  
  .stat-value {
    font-size: 24px;
  }
}

@media (max-width: 768px) {
  .home-container {
    padding: 0;
  }
  
  .stat-card {
    margin-bottom: 16px;
  }
  
  .stat-card :deep(.el-card__body) {
    padding: 16px;
  }
  
  .stat-item {
    gap: 12px;
  }
  
  .stat-icon {
    width: 48px;
    height: 48px;
  }
  
  .stat-icon :deep(.el-icon) {
    font-size: 24px !important;
  }
  
  .stat-value {
    font-size: 22px;
  }
  
  .stat-label {
    font-size: 11px;
  }
  
  .section-card {
    margin-top: 16px;
  }
  
  .section-card :deep(.el-card__header) {
    padding: 16px;
  }
  
  .card-header span {
    font-size: 16px;
  }
  
  .activity-card {
    margin-bottom: 16px;
  }
  
  .activity-card h3 {
    font-size: 15px;
  }
}

@media (max-width: 576px) {
  .stat-value {
    font-size: 20px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .card-header .el-button {
    width: 100%;
  }
}
</style>
