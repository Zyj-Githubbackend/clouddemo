<template>
  <Layout>
    <div class="st-page page-container home-dashboard">
      <section class="dashboard-hero st-hero">
        <div class="st-hero-content">
          <div class="st-hero-copy">
            <p class="st-hero-eyebrow">首页总览</p>
            <h1 class="st-hero-title">用志愿行动连接校园影响力</h1>
            <p class="st-hero-desc">
              在这里快速查看个人志愿时长、参与情况，以及近期仍可报名的活动。
            </p>
          </div>
          <div class="st-hero-actions">
            <span class="st-chip">
              <el-icon><DataAnalysis /></el-icon>
              聚合个人概览与活动推荐
            </span>
            <el-button type="primary" class="hero-btn" @click="$router.push('/activities')">查看活动</el-button>
          </div>
        </div>
      </section>

      <section class="st-stat-grid">
        <article class="st-stat-card">
          <span class="label">志愿时长</span>
          <strong class="value">{{ userInfo.totalVolunteerHours || 0 }}</strong>
          <span class="hint">已累计服务时长</span>
        </article>
        <article class="st-stat-card">
          <span class="label">参与活动</span>
          <strong class="value">{{ registrationCount }}</strong>
          <span class="hint">来自我的报名记录</span>
        </article>
        <article class="st-stat-card">
          <span class="label">已签到</span>
          <strong class="value">{{ completedCount }}</strong>
          <span class="hint">已完成现场签到</span>
        </article>
        <article class="st-stat-card">
          <span class="label">已核销</span>
          <strong class="value">{{ confirmedCount }}</strong>
          <span class="hint">已完成时长核销</span>
        </article>
      </section>

      <section class="st-panel home-panel">
        <div class="st-section-head">
          <div>
            <h2>最新招募活动</h2>
            <p>优先展示正在招募的最新志愿活动。</p>
          </div>
        </div>

        <div class="activity-grid">
          <article v-for="activity in activities" :key="activity.id" class="activity-card" @click="goToDetail(activity.id)">
            <div class="activity-cover st-cover">
              <img v-if="activity.imageUrl" :src="activity.imageUrl" :alt="activity.title">
              <div v-else class="cover-fallback">
                <span>{{ activity.category || '志愿活动' }}</span>
              </div>
            </div>

            <div class="activity-body">
              <div class="tag-row">
                <el-tag :type="getRecruitmentDisplay(activity).type" effect="light">{{ getRecruitmentDisplay(activity).text }}</el-tag>
                <el-tag type="info" effect="plain">{{ activity.category }}</el-tag>
              </div>
              <h3 class="text-ellipsis-2">{{ activity.title }}</h3>
              <div class="meta-row"><el-icon><Location /></el-icon>{{ activity.location }}</div>
              <div class="meta-row"><el-icon><Clock /></el-icon>{{ activity.volunteerHours }} 小时</div>
              <div class="meta-row"><el-icon><User /></el-icon>{{ activity.currentParticipants }} / {{ activity.maxParticipants }}</div>
            </div>
          </article>
        </div>
      </section>
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
  return registrations.value.filter(r => r.checkInStatus === 1).length
})
const confirmedCount = computed(() => {
  return registrations.value.filter(r => r.hoursConfirmed === 1).length
})

const goToDetail = (id) => {
  router.push(`/activity/${id}`)
}

const sortHomeActivities = (records = []) => {
  return [...records]
    .filter(activity => getRecruitmentDisplay(activity).text === '招募中')
    .sort((a, b) => {
      const maxDiff = (b.maxParticipants || 0) - (a.maxParticipants || 0)
      if (maxDiff !== 0) return maxDiff

      const currentDiff = (b.currentParticipants || 0) - (a.currentParticipants || 0)
      if (currentDiff !== 0) return currentDiff

      return new Date(a.registrationDeadline).getTime() - new Date(b.registrationDeadline).getTime()
    })
    .slice(0, 6)
}

const fetchData = async () => {
  try {
    const [actRes, regRes, userRes] = await Promise.all([
      getActivityList({ page: 1, size: 20, recruitmentPhase: 'RECRUITING' }),
      getMyRegistrations(),
      getUserInfo()
    ])

    activities.value = sortHomeActivities(actRes.data.records || [])
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
.home-dashboard {
  max-width: 1240px;
}

.dashboard-hero {
  min-height: 260px;
}

.hero-btn {
  height: 46px;
  border-radius: 999px;
}

.home-panel {
  padding: 22px;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.activity-card {
  overflow: hidden;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(201, 214, 243, 0.5);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.activity-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 34px rgba(13, 71, 217, 0.1);
}

.activity-cover {
  height: 190px;
}

.cover-fallback {
  height: 100%;
  display: flex;
  align-items: flex-end;
  padding: 18px;
  color: #41506f;
  font-weight: 800;
}

.activity-body {
  padding: 18px;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.activity-body h3 {
  margin-bottom: 12px;
  font-size: 20px;
  line-height: 1.35;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #5f647a;
  font-size: 14px;
  margin-bottom: 6px;
}

@media (max-width: 768px) {
  .home-panel {
    padding: 16px;
  }
}
</style>
