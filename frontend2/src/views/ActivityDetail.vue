<template>
  <Layout>
    <div class="st-page page-container" v-loading="loading">
      <template v-if="activity.id">
        <section class="activity-hero st-panel">
          <div class="activity-copy">
            <div class="hero-tags">
              <el-tag :type="activityPhaseDisplay.type" effect="light">{{ activityPhaseDisplay.text }}</el-tag>
              <el-tag :type="recruitmentDisplay.type" effect="light">{{ recruitmentDisplay.text }}</el-tag>
              <el-tag type="info" effect="plain">{{ activity.category }}</el-tag>
            </div>

            <h1>{{ activity.title }}</h1>
            <p>{{ activity.description || '暂无活动说明。' }}</p>

            <div class="hero-meta">
              <span><el-icon><Location /></el-icon>{{ activity.location || '--' }}</span>
              <span><el-icon><Clock /></el-icon>{{ activity.volunteerHours }} 小时</span>
              <span><el-icon><User /></el-icon>{{ activity.currentParticipants }} / {{ activity.maxParticipants }}</span>
            </div>
          </div>

          <div class="activity-cover st-cover">
            <el-carousel v-if="hasGalleryImages" height="360px" indicator-position="outside">
              <el-carousel-item v-for="(imageUrl, index) in galleryImages" :key="`${activity.id}-${index}`">
                <img :src="imageUrl" :alt="`${activity.title}-${index + 1}`">
              </el-carousel-item>
            </el-carousel>
            <div v-else class="cover-fallback">
              <span>{{ activity.category || '志愿活动' }}</span>
              <small>{{ activity.location || '封面待配置' }}</small>
            </div>
          </div>
        </section>

        <div class="st-grid-2 activity-grid">
          <main class="content-stack">
            <section class="st-panel section-panel">
              <div class="st-section-head">
                <div>
                  <h2>活动简介</h2>
                  <p>查看活动介绍、服务内容与参与要求。</p>
                </div>
              </div>
              <div class="section-body">{{ activity.description || '暂无详情' }}</div>
            </section>

            <section class="st-panel section-panel">
              <div class="st-section-head">
                <div>
                  <h2>时间安排</h2>
                  <p>报名与活动时间请以页面展示安排为准。</p>
                </div>
              </div>
              <div class="time-grid">
                <article class="time-item">
                  <span>招募开始</span>
                  <strong>{{ formatDate(activity.registrationStartTime) }}</strong>
                </article>
                <article class="time-item warn">
                  <span>报名截止</span>
                  <strong>{{ formatDate(activity.registrationDeadline) }}</strong>
                </article>
                <article class="time-item">
                  <span>活动开始</span>
                  <strong>{{ formatDate(activity.startTime) }}</strong>
                </article>
                <article class="time-item">
                  <span>活动结束</span>
                  <strong>{{ formatDate(activity.endTime) }}</strong>
                </article>
              </div>
            </section>

            <section class="st-panel section-panel">
              <div class="st-section-head">
                <div>
                  <h2>活动概览</h2>
                  <p>集中查看活动类型、地点、名额和志愿时长等关键信息。</p>
                </div>
              </div>
              <div class="overview-grid">
                <article class="overview-item">
                  <span>活动类型</span>
                  <strong>{{ activity.category || '--' }}</strong>
                </article>
                <article class="overview-item">
                  <span>服务地点</span>
                  <strong>{{ activity.location || '--' }}</strong>
                </article>
                <article class="overview-item">
                  <span>剩余名额</span>
                  <strong>{{ activity.availableSlots ?? 0 }}</strong>
                </article>
                <article class="overview-item">
                  <span>志愿时长</span>
                  <strong>{{ activity.volunteerHours ?? 0 }} 小时</strong>
                </article>
              </div>
            </section>
          </main>

          <aside class="st-side-stack">
            <section class="st-panel register-panel">
              <div class="panel-head">
                <h3>报名进度</h3>
                <span>{{ slotsPercentage }}%</span>
              </div>

              <div class="slots-value">
                <strong>{{ activity.currentParticipants }}</strong>
                <span>/ {{ activity.maxParticipants }}</span>
              </div>

              <el-progress :percentage="slotsPercentage" :stroke-width="10" :color="slotsProgressColor" />

              <p class="slots-tip" :class="{ danger: activity.availableSlots <= 0 }">
                {{ activity.availableSlots > 0 ? `剩余 ${activity.availableSlots} 个名额` : '名额已满' }}
              </p>

              <div v-if="canRegister" class="action-stack">
                <el-button
                  type="primary"
                  class="full-btn"
                  :disabled="activity.availableSlots <= 0"
                  @click="handleRegister"
                >
                  {{ activity.availableSlots > 0 ? '立即报名' : '名额已满' }}
                </el-button>
                <p class="tip-text">报名成功后，后续签到与时长核销将进入“我的志愿足迹”。</p>
              </div>

              <div v-else-if="activity.isRegistered" class="action-stack">
                <div class="state-banner success">
                  <el-icon><CircleCheck /></el-icon>
                  <span>您已成功报名</span>
                </div>
                <el-button
                  v-if="canCancelRegistration"
                  class="full-btn"
                  :loading="cancelling"
                  @click="handleCancelRegistration"
                >
                  取消报名
                </el-button>
                <p class="tip-text">
                  {{ canCancelRegistration ? '活动开始前可取消报名。' : '当前阶段不可取消，请按时参加活动。' }}
                </p>
              </div>

              <div v-else class="action-stack">
                <div class="state-banner muted">
                  <el-icon><InfoFilled /></el-icon>
                  <span>{{ statusHint }}</span>
                </div>
              </div>
            </section>

            <section class="st-panel mini-panel">
              <div class="mini-value">{{ activity.volunteerHours ?? 0 }}</div>
              <div class="mini-label">志愿时长（小时）</div>
            </section>

            <section class="st-panel mini-panel">
              <div class="mini-value">{{ recruitmentDisplay.text }}</div>
              <div class="mini-label">当前招募阶段</div>
            </section>
          </aside>
        </div>
      </template>

      <div v-else-if="!loading" class="st-panel empty-panel">
        <el-empty description="活动不存在或已删除" />
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import Layout from '@/components/Layout.vue'
import { cancelMyRegistration, getActivityDetail, registerActivity } from '@/api/activity'
import { getRecruitmentDisplay } from '@/utils/recruitment'
import { getActivityPhaseDisplay } from '@/utils/activityPhase'

const route = useRoute()
const loading = ref(false)
const cancelling = ref(false)
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

const canCancelRegistration = computed(() => {
  const a = activity.value
  if (!a.isRegistered) return false
  if (a.status !== 'RECRUITING') return false
  if (!a.startTime) return false
  return dayjs().isBefore(dayjs(a.startTime))
})

const statusHint = computed(() => {
  const a = activity.value
  if (a.status === 'CANCELLED') return '活动已取消'
  if (a.status === 'COMPLETED') return '活动已结束'
  if (recruitmentDisplay.value.text === '未开始') return '招募尚未开始'
  if (recruitmentDisplay.value.text === '已结束') return '招募已结束'
  return '活动暂不可报名'
})

const slotsPercentage = computed(() => {
  const max = Number(activity.value.maxParticipants || 0)
  const current = Number(activity.value.currentParticipants || 0)
  if (max <= 0) return 0
  return Math.min(Math.round((current / max) * 100), 100)
})

const slotsProgressColor = computed(() => {
  if (slotsPercentage.value >= 90) return '#f56c6c'
  if (slotsPercentage.value >= 60) return '#e6a23c'
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
    await fetchActivity()
  } catch (error) {
    console.error('报名失败:', error)
  }
}

const handleCancelRegistration = async () => {
  try {
    await ElMessageBox.confirm(
      '取消报名后，你将从该活动的报名名单中移除。是否继续？',
      '确认取消报名',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '再想想'
      }
    )
    cancelling.value = true
    await cancelMyRegistration(route.params.id)
    ElMessage.success('已取消报名')
    await fetchActivity()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('取消报名失败:', error)
    }
  } finally {
    cancelling.value = false
  }
}

onMounted(() => {
  fetchActivity()
})
</script>

<style scoped>
.activity-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 480px);
  gap: 18px;
  padding: 22px;
}

.activity-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.activity-copy h1 {
  margin: 18px 0 12px;
  font-size: clamp(34px, 5vw, 54px);
  line-height: 1.02;
  letter-spacing: -0.04em;
}

.activity-copy p {
  margin: 0;
  color: #56627f;
  line-height: 1.82;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 18px;
  color: #4f5d7d;
}

.hero-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(13, 71, 217, 0.08);
  font-size: 13px;
  font-weight: 700;
}

.activity-cover {
  min-height: 360px;
}

.activity-cover :deep(.el-carousel),
.activity-cover :deep(.el-carousel__container) {
  height: 360px;
}
.cover-fallback {
  height: 100%;
  min-height: 360px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 22px;
  color: #41506f;
}

.cover-fallback span {
  font-size: 32px;
  font-weight: 800;
}

.cover-fallback small {
  margin-top: 8px;
  font-size: 13px;
}

.activity-grid {
  align-items: start;
}

.content-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-panel,
.register-panel,
.mini-panel,
.empty-panel {
  padding: 22px;
}

.section-body {
  white-space: pre-wrap;
  color: #4e5877;
  line-height: 1.9;
}

.time-grid,
.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.time-item,
.overview-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(13, 71, 217, 0.06);
}

.time-item.warn {
  background: rgba(245, 108, 108, 0.08);
}

.time-item span,
.overview-item span {
  display: block;
  color: #75809d;
  font-size: 13px;
}

.time-item strong,
.overview-item strong {
  display: block;
  margin-top: 8px;
  color: var(--cv-text);
  font-size: 18px;
  line-height: 1.5;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.panel-head h3 {
  font-size: 24px;
}

.panel-head span {
  color: #6f7b97;
  font-weight: 800;
}

.slots-value {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.slots-value strong {
  font-size: 44px;
  line-height: 1;
  font-family: 'Plus Jakarta Sans', 'Manrope', 'Noto Sans SC', sans-serif;
}

.slots-value span {
  color: #66718d;
  font-size: 18px;
}

.slots-tip {
  margin: 12px 0 0;
  color: #6f748b;
}

.slots-tip.danger {
  color: #f56c6c;
}

.action-stack {
  margin-top: 18px;
}

.full-btn {
  width: 100%;
  height: 46px;
  border-radius: 999px;
}

.tip-text {
  margin: 12px 0 0;
  color: #7d8399;
  font-size: 13px;
  line-height: 1.7;
}

.state-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 18px;
  font-weight: 700;
}

.state-banner.success {
  color: #0f8b5f;
  background: rgba(15, 139, 95, 0.1);
}

.state-banner.muted {
  color: #68738f;
  background: rgba(13, 71, 217, 0.06);
}

.mini-panel {
  text-align: center;
}

.mini-value {
  font-size: 34px;
  font-weight: 800;
  line-height: 1.2;
  color: var(--cv-primary);
}

.mini-label {
  margin-top: 8px;
  color: #72809d;
}

@media (max-width: 768px) {
  .activity-hero {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .section-panel,
  .register-panel,
  .mini-panel {
    padding: 16px;
  }

  .time-grid,
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
