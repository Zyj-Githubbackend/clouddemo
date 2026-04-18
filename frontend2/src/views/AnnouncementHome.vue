<template>
  <Layout>
    <div class="st-page page-container" v-loading="loading">
      <section class="st-hero home-hero">
        <div class="st-hero-content">
          <div class="st-hero-copy">
            <p class="st-hero-eyebrow">公告中心</p>
            <h1 class="st-hero-title">校园志愿服务公告</h1>
            <p class="st-hero-desc">
              查看学校最新通知、志愿活动公告，以及与公告相关的活动入口。
            </p>
          </div>

          <div class="st-hero-actions">
            <span class="st-chip">
              <el-icon><Bell /></el-icon>
              最新 {{ announcements.length }} 条
            </span>
            <el-button class="hero-action" type="primary" size="large" @click="router.push('/activities')">
              查看志愿活动
            </el-button>
          </div>
        </div>
      </section>

      <section class="st-stat-grid">
        <article class="st-stat-card">
          <span class="label">首页公告数量</span>
          <strong class="value">{{ announcements.length }}</strong>
          <span class="hint">首页展示</span>
        </article>
        <article class="st-stat-card">
          <span class="label">关联活动公告</span>
          <strong class="value">{{ linkedCount }}</strong>
          <span class="hint">可直接跳转活动</span>
        </article>
        <article class="st-stat-card">
          <span class="label">无图片公告</span>
          <strong class="value">{{ imageMissingCount }}</strong>
          <span class="hint">默认封面</span>
        </article>
      </section>

      <section class="st-panel announcements-panel">
        <div class="st-section-head">
          <div>
            <h2>最新公告</h2>
            <p>点击卡片查看公告详情，相关活动可从卡片中直接进入。</p>
          </div>
          <span class="st-muted">共 {{ announcements.length }} 条</span>
        </div>

        <div v-if="!loading && announcements.length === 0" class="st-empty">
          <el-empty description="暂无公告" />
        </div>

        <div v-else class="announcement-grid">
          <article
            v-for="item in announcements"
            :key="item.id"
            class="announcement-card"
            @click="goToAnnouncement(item.id)"
          >
            <div class="card-cover st-cover">
              <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title">
              <div v-else class="cover-fallback">
                <span>公告</span>
                <small>默认封面</small>
              </div>
            </div>

            <div class="card-body">
              <div class="card-meta">
                <span class="st-badge success">已发布</span>
                <span>{{ formatDate(item.publishTime || item.updateTime) }}</span>
              </div>

              <h3 class="text-ellipsis-2">{{ item.title }}</h3>
              <p class="text-ellipsis-3">{{ item.content }}</p>

              <div v-if="getLinkedActivities(item).length > 0" class="linked-actions">
                <button
                  v-for="activity in getLinkedActivities(item)"
                  :key="activity.id"
                  type="button"
                  class="mini-action"
                  @click.stop="goToActivity(activity.id)"
                >
                  <el-icon><Position /></el-icon>
                  <span>{{ activity.title || `活动 #${activity.id}` }}</span>
                </button>
              </div>

              <div class="card-footer">
                <button type="button" class="detail-link" @click.stop="goToAnnouncement(item.id)">
                  查看公告
                </button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </Layout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import Layout from '@/components/Layout.vue'
import { getHomeAnnouncements } from '@/api/announcement'

const router = useRouter()
const loading = ref(false)
const announcements = ref([])

const linkedCount = computed(() =>
  announcements.value.filter((item) => getLinkedActivities(item).length > 0).length
)

const imageMissingCount = computed(() =>
  announcements.value.filter((item) => !item.imageUrl).length
)

const formatDate = (date) => {
  if (!date) return '--'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const getLinkedActivities = (item) => {
  if (Array.isArray(item.activities) && item.activities.length > 0) {
    return item.activities
  }
  if (item.activityId) {
    return [{ id: item.activityId, title: `活动 #${item.activityId}` }]
  }
  return []
}

const goToAnnouncement = (id) => {
  router.push(`/announcement/${id}`)
}

const goToActivity = (id) => {
  router.push(`/activity/${id}`)
}

const fetchAnnouncements = async () => {
  loading.value = true
  try {
    const res = await getHomeAnnouncements(12)
    announcements.value = res.data || []
  } catch (error) {
    console.error('获取公告失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAnnouncements()
})
</script>

<style scoped>
.announcements-panel {
  padding: 22px;
}

.home-hero {
  min-height: 280px;
}

.hero-action {
  height: 48px;
  border-radius: 999px;
  padding-inline: 24px;
}

.announcement-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.announcement-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(201, 214, 243, 0.5);
  box-shadow: 0 16px 34px rgba(13, 71, 217, 0.08);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.announcement-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(13, 71, 217, 0.12);
}

.card-cover {
  height: 210px;
}

.cover-fallback {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 18px;
  color: #41506f;
}

.cover-fallback span {
  font-size: 30px;
  font-weight: 800;
}

.cover-fallback small {
  margin-top: 6px;
  font-size: 13px;
}

.card-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 18px;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  color: #7b8298;
  font-size: 12px;
}

.card-body h3 {
  margin: 14px 0 10px;
  font-size: 22px;
  line-height: 1.3;
}

.card-body p {
  margin: 0;
  color: #59627e;
  line-height: 1.75;
}

.linked-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.mini-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 12px;
  border: 1px solid rgba(13, 71, 217, 0.12);
  border-radius: 999px;
  background: rgba(13, 71, 217, 0.06);
  color: var(--cv-primary);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.card-footer {
  margin-top: auto;
  padding-top: 18px;
}

.detail-link {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--cv-primary);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

@media (max-width: 768px) {
  .announcements-panel {
    padding: 16px;
  }

  .card-cover {
    height: 180px;
  }
}
</style>
