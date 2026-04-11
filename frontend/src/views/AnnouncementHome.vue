<template>
  <Layout>
    <div class="announcement-home page-container" v-loading="loading">
      <section class="home-hero">
        <div>
          <p class="eyebrow">Campus Bulletin</p>
          <h1>校园志愿服务公告</h1>
          <p>关注最新通知、活动提醒和志愿时长核销说明。</p>
        </div>
        <el-button type="primary" class="hero-action" @click="router.push('/activities')">查看志愿活动</el-button>
      </section>

      <section class="announcement-list">
        <div class="section-head">
          <h2>最新公告</h2>
          <span>共 {{ announcements.length }} 条</span>
        </div>

        <el-empty v-if="!loading && announcements.length === 0" description="暂无公告" />

        <div v-else class="grid">
          <article
            v-for="item in announcements"
            :key="item.id"
            class="announcement-card"
            @click="goToAnnouncement(item.id)"
          >
            <div class="cover" :class="{ empty: !item.imageUrl }">
              <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title">
              <div v-else class="cover-fallback">
                <span>公告</span>
              </div>
            </div>
            <div class="card-body">
              <div class="meta-row">
                <el-tag type="success" effect="light">已发布</el-tag>
                <span>{{ formatDate(item.publishTime || item.updateTime) }}</span>
              </div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.content }}</p>
              <div class="card-actions">
                <el-button type="primary" link @click.stop="goToAnnouncement(item.id)">查看公告</el-button>
                <el-button
                  v-if="item.activityId"
                  type="success"
                  link
                  @click.stop="goToActivity(item.activityId)"
                >
                  查看关联活动
                </el-button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </Layout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import Layout from '@/components/Layout.vue'
import { getHomeAnnouncements } from '@/api/announcement'

const router = useRouter()
const loading = ref(false)
const announcements = ref([])

const formatDate = (date) => {
  if (!date) return '--'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
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
.announcement-home {
  width: 100%;
  margin: 0 auto;
}

.home-hero {
  border-radius: 24px;
  padding: 34px 30px;
  margin-bottom: 18px;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-end;
  background: linear-gradient(135deg, var(--cv-primary), var(--cv-primary-weak));
  color: #fff;
}

.eyebrow {
  opacity: 0.86;
  font-size: 12px;
  letter-spacing: 1.6px;
  text-transform: uppercase;
}

.home-hero h1 {
  font-size: clamp(28px, 4.8vw, 38px);
  line-height: 1.18;
  margin: 8px 0 10px;
}

.home-hero p:last-child {
  max-width: 560px;
  opacity: 0.92;
}

.hero-action {
  border-radius: 999px;
  min-width: 140px;
  background: rgba(255, 255, 255, 0.22);
}

.announcement-list {
  border-radius: 18px;
  padding: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 20, 83, 0.08);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.section-head h2 {
  font-size: 24px;
  font-weight: 800;
}

.section-head span {
  color: #7f859c;
  font-size: 13px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.announcement-card {
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid rgba(74, 90, 139, 0.12);
  background: #f8fbff;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.announcement-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 20px rgba(0, 20, 83, 0.1);
}

.cover {
  height: 170px;
  background: linear-gradient(145deg, #dde8ff, #f8efe1);
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-fallback {
  height: 100%;
  display: flex;
  align-items: flex-end;
  padding: 16px;
  color: #41506f;
  font-weight: 800;
  font-size: 26px;
}

.card-body {
  padding: 14px;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #7b8298;
  font-size: 12px;
}

.card-body h3 {
  min-height: 48px;
  margin: 12px 0 8px;
  font-size: 18px;
  line-height: 1.35;
}

.card-body p {
  min-height: 72px;
  margin: 0;
  color: #59627e;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .home-hero {
    padding: 24px 18px;
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-action {
    width: 100%;
  }
}
</style>
