<template>
  <Layout>
    <div class="st-page page-container" v-loading="loading">
      <template v-if="announcement.id">
        <section class="detail-hero st-panel">
          <div class="hero-copy">
            <div class="hero-top">
              <span class="st-badge success">公告</span>
              <span class="hero-date">{{ formatDate(announcement.publishTime || announcement.updateTime) }}</span>
            </div>
            <h1>{{ announcement.title }}</h1>
            <p>查看公告正文、附件资料和相关活动信息。</p>
          </div>

          <div class="hero-media st-cover">
            <el-carousel v-if="galleryImages.length > 0" height="320px" indicator-position="outside">
              <el-carousel-item v-for="(imageUrl, index) in galleryImages" :key="`${announcement.id}-${index}`">
                <img :src="imageUrl" :alt="`${announcement.title}-${index + 1}`">
              </el-carousel-item>
            </el-carousel>
            <div v-else class="media-fallback">
              <span>公告</span>
              <small>暂无公告图片</small>
            </div>
          </div>
        </section>

        <div class="st-grid-2">
          <main class="content-stack">
            <section class="st-panel content-panel">
              <div class="st-section-head">
                <div>
                  <h2>公告正文</h2>
                  <p>公告正文如下，请以发布内容为准。</p>
                </div>
              </div>
              <div class="content-body">{{ announcement.content || '暂无内容' }}</div>
            </section>

            <section class="st-panel info-panel">
              <div class="st-section-head">
                <div>
                  <h2>公告摘要</h2>
                  <p>快速了解发布时间、图片、附件和关联活动情况。</p>
                </div>
              </div>
              <div class="summary-grid">
                <article class="summary-item">
                  <span>图片数</span>
                  <strong>{{ galleryImages.length }}</strong>
                </article>
                <article class="summary-item">
                  <span>附件数</span>
                  <strong>{{ attachments.length }}</strong>
                </article>
                <article class="summary-item">
                  <span>关联活动</span>
                  <strong>{{ linkedActivities.length }}</strong>
                </article>
              </div>
            </section>
          </main>

          <aside class="st-side-stack">
            <section class="st-panel side-panel">
              <div class="st-section-head compact">
                <h3>关联活动</h3>
              </div>
              <div v-if="linkedActivities.length > 0" class="side-list">
                <button
                  v-for="activity in linkedActivities"
                  :key="activity.id"
                  type="button"
                  class="side-item"
                  @click="goToActivity(activity.id)"
                >
                  <strong>{{ activity.title || `活动 #${activity.id}` }}</strong>
                  <span>{{ formatActivityMeta(activity) }}</span>
                </button>
              </div>
              <div v-else class="st-empty">
                <p class="st-muted">暂无关联活动</p>
              </div>
            </section>

            <section class="st-panel side-panel">
              <div class="st-section-head compact">
                <h3>公告附件</h3>
              </div>
              <div v-if="attachments.length > 0" class="side-list">
                <a
                  v-for="attachment in attachments"
                  :key="attachment.attachmentKey"
                  class="side-item"
                  :href="attachment.url"
                  target="_blank"
                  rel="noopener"
                >
                  <strong>{{ attachment.fileName || '附件' }}</strong>
                  <span>{{ formatFileSize(attachment.fileSize) }}</span>
                </a>
              </div>
              <div v-else class="st-empty">
                <p class="st-muted">暂无附件</p>
              </div>
            </section>

            <section class="st-panel side-panel">
              <div class="st-section-head compact">
                <h3>相关操作</h3>
              </div>
              <el-button class="full-btn" @click="router.push('/home')">返回公告首页</el-button>
              <el-button v-if="linkedActivities[0]" class="full-btn" type="primary" @click="goToActivity(linkedActivities[0].id)">
                查看首个关联活动
              </el-button>
            </section>
          </aside>
        </div>
      </template>

      <div v-else-if="!loading" class="st-panel empty-panel">
        <el-empty description="公告不存在或已下线" />
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import Layout from '@/components/Layout.vue'
import { getAnnouncementDetail } from '@/api/announcement'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const announcement = ref({})

const galleryImages = computed(() => {
  if (Array.isArray(announcement.value.imageUrls) && announcement.value.imageUrls.length > 0) {
    return announcement.value.imageUrls
  }
  return announcement.value.imageUrl ? [announcement.value.imageUrl] : []
})

const linkedActivities = computed(() => {
  if (Array.isArray(announcement.value.activities) && announcement.value.activities.length > 0) {
    return announcement.value.activities
  }
  if (announcement.value.activityId) {
    return [{ id: announcement.value.activityId, title: `活动 #${announcement.value.activityId}` }]
  }
  return []
})

const attachments = computed(() => (
  Array.isArray(announcement.value.attachments) ? announcement.value.attachments : []
))

const formatDate = (date) => {
  if (!date) return '--'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const formatActivityMeta = (activity) => {
  const parts = []
  if (activity.location) parts.push(activity.location)
  if (activity.startTime) parts.push(formatDate(activity.startTime))
  return parts.join(' · ') || '点击查看活动详情'
}

const formatFileSize = (size) => {
  const bytes = Number(size || 0)
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

const goToActivity = (id) => {
  router.push(`/activity/${id}`)
}

const fetchAnnouncement = async () => {
  loading.value = true
  try {
    const res = await getAnnouncementDetail(route.params.id)
    announcement.value = res.data || {}
  } catch (error) {
    console.error('获取公告详情失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAnnouncement()
})
</script>

<style scoped>
.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 460px);
  gap: 18px;
  padding: 22px;
}

.hero-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-date {
  color: #7c849c;
  font-size: 13px;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-copy h1 {
  margin: 16px 0 12px;
  font-size: clamp(32px, 4.8vw, 52px);
  line-height: 1.04;
  letter-spacing: -0.04em;
}

.hero-copy p {
  margin: 0;
  color: #56627f;
  line-height: 1.85;
}

.hero-media {
  min-height: 320px;
}

.hero-media :deep(.el-carousel),
.hero-media :deep(.el-carousel__container) {
  height: 320px;
}

.media-fallback {
  height: 100%;
  min-height: 320px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 22px;
  color: #41506f;
}

.media-fallback span {
  font-size: 30px;
  font-weight: 800;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.content-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-panel,
.info-panel,
.side-panel,
.empty-panel {
  padding: 22px;
}

.content-body {
  white-space: pre-wrap;
  color: #4e5877;
  line-height: 1.9;
  font-size: 15px;
}

.summary-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(13, 71, 217, 0.06);
}

.summary-item span {
  display: block;
  color: #72809c;
  font-size: 13px;
}

.summary-item strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
  font-family: 'Plus Jakarta Sans', 'Manrope', 'Noto Sans SC', sans-serif;
}

.st-section-head.compact {
  margin-bottom: 12px;
}

.side-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.side-item {
  display: block;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(201, 214, 243, 0.62);
  background: rgba(242, 246, 251, 0.82);
  color: inherit;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
}

.side-item strong,
.side-item span {
  display: block;
}

.side-item strong {
  color: var(--cv-text);
}

.side-item span {
  margin-top: 6px;
  color: #78839d;
  font-size: 12px;
}

.full-btn {
  width: 100%;
  height: 44px;
  margin-bottom: 10px;
  border-radius: 999px;
}

@media (max-width: 768px) {
  .detail-hero {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .content-panel,
  .info-panel,
  .side-panel {
    padding: 16px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
