<template>
  <article class="activity-card" @click="router.push(`/activity/${activity.id}`)">
    <div class="activity-cover" :class="{ empty: !coverUrl }">
      <img v-if="coverUrl" :src="coverUrl" :alt="activity.title || '活动封面'" />
      <div v-else class="cover-fallback">
        <span>{{ activity.category || '志愿活动' }}</span>
        <small>{{ activity.location || '封面完善中' }}</small>
      </div>

      <div class="activity-badges">
        <span :class="['badge', recruitmentClass]">{{ recruitment.text }}</span>
        <span :class="['badge ghost', phaseClass]">{{ phase.text }}</span>
      </div>
    </div>

    <div class="activity-body">
      <div class="activity-top">
        <div class="category-mark">{{ activity.category || '未分类活动' }}</div>
        <div class="slots-inline">{{ participantsText }}</div>
      </div>

      <h3>{{ activity.title || '未命名活动' }}</h3>

      <div class="meta-list">
        <div class="meta-item">
          <el-icon><Location /></el-icon>
          <span>{{ activity.location || '地点信息完善中' }}</span>
        </div>
        <div class="meta-item">
          <el-icon><Clock /></el-icon>
          <span>{{ registrationText }}</span>
        </div>
        <div class="meta-item">
          <el-icon><Timer /></el-icon>
          <span>{{ hoursText }}</span>
        </div>
      </div>

      <div class="activity-footer">
        <div class="hours-panel">
          <strong>{{ hoursValue }}</strong>
          <span>小时</span>
        </div>

        <div class="progress-panel">
          <div class="progress-meta">
            <span>{{ participantsText }}</span>
            <strong>{{ percentage }}%</strong>
          </div>
          <div class="progress-track">
            <div class="progress-bar" :style="{ width: `${percentage}%`, background: progressColor }"></div>
          </div>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { getActivityPhaseDisplay } from '@/utils/activityPhase'
import { getRecruitmentDisplay } from '@/utils/recruitment'

const props = defineProps({
  activity: {
    type: Object,
    required: true
  }
})

const router = useRouter()

const phase = computed(() => getActivityPhaseDisplay(props.activity))
const recruitment = computed(() => getRecruitmentDisplay(props.activity))

const coverUrl = computed(() => {
  if (Array.isArray(props.activity.imageUrls) && props.activity.imageUrls.length > 0) {
    return props.activity.imageUrls[0]
  }
  return props.activity.imageUrl || ''
})

const percentage = computed(() => {
  const total = Number(props.activity.maxParticipants || 0)
  const current = Number(props.activity.currentParticipants || 0)
  if (total <= 0) return 0
  return Math.min(Math.round((current / total) * 100), 100)
})

const participantsText = computed(() => {
  const current = Number(props.activity.currentParticipants || 0)
  const total = Number(props.activity.maxParticipants || 0)
  return `${current}/${total} 人`
})

const hoursValue = computed(() => Number(props.activity.volunteerHours || 0))
const hoursText = computed(() => `${hoursValue.value || 0} 小时志愿时长`)

const registrationText = computed(() => {
  if (!props.activity.registrationDeadline) return '报名截止时间见详情'
  return `报名截止：${dayjs(props.activity.registrationDeadline).format('MM-DD HH:mm')}`
})

const progressColor = computed(() => {
  if (percentage.value >= 90) return '#f56c6c'
  if (percentage.value >= 60) return '#e6a23c'
  return 'linear-gradient(135deg, #0066ff, #00c1fd)'
})

const phaseClass = computed(() => ({
  success: phase.value.type === 'success',
  warning: phase.value.type === 'warning',
  danger: phase.value.type === 'danger'
}))

const recruitmentClass = computed(() => ({
  success: recruitment.value.type === 'success',
  warning: recruitment.value.type === 'warning',
  danger: recruitment.value.type === 'danger'
}))
</script>

<style scoped>
.activity-card {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  overflow: hidden;
  border: 1px solid rgba(194, 204, 229, 0.68);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 36px rgba(9, 41, 122, 0.08);
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 44px rgba(9, 41, 122, 0.14);
}

.activity-cover {
  position: relative;
  height: 224px;
  overflow: hidden;
  background: linear-gradient(145deg, #deebff, #e8f8ff 48%, #f6efe6);
}

.activity-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.35s ease;
}

.activity-card:hover .activity-cover img {
  transform: scale(1.04);
}

.cover-fallback {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 6px;
  padding: 22px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.34), transparent 40%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.08));
}

.cover-fallback span {
  color: #365282;
  font-family: 'Plus Jakarta Sans', 'Manrope', sans-serif;
  font-size: 24px;
  font-weight: 800;
}

.cover-fallback small {
  color: #5f7198;
  font-size: 13px;
  font-weight: 700;
}

.activity-badges {
  position: absolute;
  top: 18px;
  left: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: #23386b;
  font-size: 12px;
  font-weight: 800;
  backdrop-filter: blur(12px);
}

.badge.success {
  background: rgba(218, 247, 229, 0.92);
  color: #0d7a4b;
}

.badge.warning {
  background: rgba(255, 239, 212, 0.94);
  color: #ad6a00;
}

.badge.danger {
  background: rgba(255, 227, 227, 0.94);
  color: #bf4b4b;
}

.badge.ghost {
  background: rgba(255, 255, 255, 0.92);
  color: #365282;
}

.activity-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
}

.activity-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.category-mark {
  color: #5d6b8d;
  font-size: 13px;
  font-weight: 700;
}

.slots-inline {
  color: #536382;
  font-size: 12px;
  font-weight: 800;
}

.activity-body h3 {
  font-size: 23px;
  line-height: 1.25;
  font-weight: 800;
  letter-spacing: -0.02em;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  color: #576687;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
}

.meta-item :deep(.el-icon) {
  color: var(--cv-primary);
}

.activity-footer {
  margin-top: auto;
  padding-top: 18px;
  border-top: 1px solid rgba(194, 204, 229, 0.5);
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 18px;
  align-items: end;
}

.hours-panel {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.hours-panel strong {
  color: var(--cv-primary);
  font-family: 'Plus Jakarta Sans', 'Manrope', sans-serif;
  font-size: 32px;
  font-weight: 800;
  line-height: 1;
}

.hours-panel span {
  color: #5a6482;
  font-size: 13px;
  font-weight: 700;
}

.progress-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  color: #617090;
  font-size: 12px;
  font-weight: 700;
}

.progress-meta strong {
  color: var(--cv-primary);
}

.progress-track {
  width: 100%;
  height: 10px;
  border-radius: 999px;
  background: #edf2f9;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  border-radius: 999px;
}
</style>
