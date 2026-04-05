<template>
  <Layout>
    <div class="detail-page page-container" v-loading="loading">
      <template v-if="activity.id">
        <section class="hero">
          <div class="hero-copy">
            <div class="hero-tags">
              <el-tag :type="activityPhaseDisplay.type" effect="light">{{ activityPhaseDisplay.text }}</el-tag>
              <el-tag :type="recruitmentDisplay.type" effect="light">{{ recruitmentDisplay.text }}</el-tag>
              <el-tag type="info" effect="plain">{{ activity.category }}</el-tag>
            </div>
            <h1>{{ activity.title }}</h1>
            <div class="hero-meta">
              <span><el-icon><Location /></el-icon>{{ activity.location }}</span>
              <span><el-icon><Clock /></el-icon>{{ activity.volunteerHours }} 小时</span>
              <span><el-icon><User /></el-icon>{{ activity.currentParticipants }} / {{ activity.maxParticipants }}</span>
            </div>
          </div>
          <div class="hero-cover" :class="{ empty: !hasGalleryImages }">
            <el-carousel v-if="hasGalleryImages" height="240px" indicator-position="outside">
              <el-carousel-item v-for="(imageUrl, index) in galleryImages" :key="`${activity.id}-${index}`">
                <img :src="imageUrl" :alt="`${activity.title}-${index + 1}`">
              </el-carousel-item>
            </el-carousel>
            <div v-else class="hero-fallback">
              <span>{{ activity.category || '志愿活动' }}</span>
              <small>{{ activity.location || '活动封面待上传' }}</small>
            </div>
          </div>
        </section>

        <el-row :gutter="16">
          <el-col :xs="24" :md="16">
            <el-card class="panel" shadow="never">
              <template #header><h3>活动说明</h3></template>
              <div class="content">{{ activity.description || '暂无详情' }}</div>
            </el-card>

            <el-card class="panel" shadow="never">
              <template #header><h3>时间安排</h3></template>
              <div class="time-grid">
                <div class="time-item">
                  <label>招募开始</label>
                  <div>{{ formatDate(activity.registrationStartTime) }}</div>
                </div>
                <div class="time-item warn">
                  <label>报名截止</label>
                  <div>{{ formatDate(activity.registrationDeadline) }}</div>
                </div>
                <div class="time-item">
                  <label>活动开始</label>
                  <div>{{ formatDate(activity.startTime) }}</div>
                </div>
                <div class="time-item">
                  <label>活动结束</label>
                  <div>{{ formatDate(activity.endTime) }}</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :md="8">
            <el-card class="panel" shadow="never">
              <template #header><h3>报名进度</h3></template>
              <div class="slots-num">
                <strong>{{ activity.currentParticipants }}</strong> / {{ activity.maxParticipants }}
              </div>
              <el-progress
                :percentage="Math.min(Math.round((activity.currentParticipants / activity.maxParticipants) * 100), 100)"
                :color="slotsProgressColor"
                :stroke-width="8"
              />
              <div class="slots-tip" v-if="activity.availableSlots > 0">剩余 {{ activity.availableSlots }} 个名额</div>
              <div class="slots-tip danger" v-else>名额已满</div>
            </el-card>

            <el-card class="panel" shadow="never">
              <template v-if="canRegister">
                <el-button type="primary" class="action-btn" :disabled="activity.availableSlots <= 0" @click="handleRegister">
                  {{ activity.availableSlots > 0 ? '立即报名' : '名额已满' }}
                </el-button>
                <p class="hint">报名后请按时参加活动并完成签到。</p>
              </template>
              <template v-else-if="activity.isRegistered">
                <div class="state-ok">
                  <el-icon :size="28" color="#67c23a"><CircleCheck /></el-icon>
                  <span>您已成功报名</span>
                </div>
                <p class="hint">请关注活动时间，准时签到。</p>
              </template>
              <template v-else>
                <div class="state-mute">
                  <el-icon :size="22" color="#8e93a8"><InfoFilled /></el-icon>
                  <span>{{ statusHint }}</span>
                </div>
              </template>
            </el-card>

            <el-card class="panel hours" shadow="never">
              <div class="hours-value">{{ activity.volunteerHours }}</div>
              <div class="hours-label">志愿时长（小时）</div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <el-empty v-else-if="!loading" description="活动不存在或已删除" />
    </div>
  </Layout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import Layout from '@/components/Layout.vue'
import { getActivityDetail, registerActivity } from '@/api/activity'
import { getRecruitmentDisplay } from '@/utils/recruitment'
import { getActivityPhaseDisplay } from '@/utils/activityPhase'
import dayjs from 'dayjs'

const route = useRoute()
const loading = ref(false)
const activity = ref({})

const activityPhaseDisplay = computed(() => getActivityPhaseDisplay(activity.value))
const recruitmentDisplay = computed(() => getRecruitmentDisplay(activity.value))
const galleryImages = computed(() => {
  if (Array.isArray(activity.value.imageUrls) && activity.value.imageUrls.length > 0) {
    return activity.value.imageUrls
  }
  return activity.value.imageUrl ? [activity.value.imageUrl] : []
})
const hasGalleryImages = computed(() => galleryImages.value.length > 0)

const canRegister = computed(() => {
  const a = activity.value
  return !a.isRegistered && a.status === 'RECRUITING' && recruitmentDisplay.value.text === '招募中'
})

const statusHint = computed(() => {
  const a = activity.value
  if (a.status === 'CANCELLED') return '活动已取消'
  if (a.status === 'COMPLETED') return '活动已结项'
  if (recruitmentDisplay.value.text === '未开始') return '招募尚未开始'
  if (recruitmentDisplay.value.text === '已结束') return '招募已结束'
  return '活动暂不可报名'
})

const slotsProgressColor = computed(() => {
  const pct = (activity.value.currentParticipants / activity.value.maxParticipants) * 100
  if (pct >= 90) return '#f56c6c'
  if (pct >= 60) return '#e6a23c'
  return '#67c23a'
})

const formatDate = (date) => {
  if (!date) return '--'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
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
    ElMessage.success('报名成功')
    fetchActivity()
  } catch (error) {
    console.error('报名失败:', error)
  }
}

onMounted(() => { fetchActivity() })
</script>

<style scoped>
.detail-page {
  width: 100%;
  margin: 0 auto;
}

.hero {
  margin-bottom: 16px;
  border-radius: 24px;
  padding: 30px 26px;
  color: white;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 380px);
  gap: 22px;
  align-items: center;
  background: linear-gradient(130deg, var(--cv-primary), var(--cv-primary-weak));
}

.hero-copy {
  min-width: 0;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.hero h1 {
  font-size: clamp(28px, 4.6vw, 36px);
  line-height: 1.25;
  margin-bottom: 12px;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.hero-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
  opacity: 0.92;
}

.hero-cover {
  min-height: 240px;
  border-radius: 20px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.hero-cover img {
  width: 100%;
  height: 100%;
  min-height: 240px;
  object-fit: cover;
  display: block;
}

.hero-cover :deep(.el-carousel),
.hero-cover :deep(.el-carousel__container) {
  height: 240px;
}

.hero-fallback {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 22px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.35), transparent 42%),
    linear-gradient(140deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.05));
}

.hero-fallback span {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.1;
}

.hero-fallback small {
  margin-top: 8px;
  opacity: 0.85;
}

.panel {
  margin-bottom: 16px;
}

.panel h3 {
  font-size: 18px;
  font-weight: 700;
}

.content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #4f556d;
}

.time-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.time-item {
  border-radius: 12px;
  padding: 12px;
  background: #f3f2ff;
}

.time-item.warn {
  background: #fff5f5;
}

.time-item label {
  display: block;
  color: #798099;
  font-size: 12px;
  margin-bottom: 5px;
}

.time-item div {
  font-size: 14px;
  font-weight: 600;
}

.slots-num {
  margin-bottom: 10px;
  font-size: 18px;
  color: #5e647a;
}

.slots-num strong {
  font-size: 32px;
  color: #003dca;
}

.slots-tip {
  margin-top: 8px;
  color: #6f748b;
}

.slots-tip.danger {
  color: #f56c6c;
}

.action-btn {
  width: 100%;
  height: 44px;
  border-radius: 999px;
  font-weight: 600;
}

.hint {
  margin-top: 10px;
  font-size: 12px;
  color: #7d8399;
}

.state-ok,
.state-mute {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.hours {
  text-align: center;
  background: linear-gradient(135deg, rgba(0, 61, 202, 0.12), rgba(44, 88, 232, 0.18));
}

.hours-value {
  font-size: 46px;
  font-weight: 800;
  color: #003dca;
  line-height: 1;
}

.hours-label {
  margin-top: 8px;
  color: #60667f;
}

@media (max-width: 768px) {
  .hero {
    padding: 22px 16px;
    grid-template-columns: 1fr;
  }

  .hero h1 {
    font-size: 26px;
  }

  .time-grid {
    grid-template-columns: 1fr;
  }
}
</style>




