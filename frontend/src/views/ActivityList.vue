<template>
  <Layout>
    <div class="activity-page page-container">
      <section class="banner">
        <p class="banner-mini">Discover Opportunities</p>
        <h1>Curated Volunteer Activities</h1>
        <p>按状态、阶段、分类快速筛选，找到适配你的志愿行动。</p>
      </section>

      <el-card class="filter-card" shadow="never">
        <div class="filter-row">
          <div class="filter-group">
            <span class="filter-label">活动状态</span>
            <div class="pills">
              <span
                v-for="s in statusOptions"
                :key="s.value"
                :class="['pill', filters.status === s.value && 'pill-active']"
                @click="setFilter('status', s.value)"
              >{{ s.label }}</span>
            </div>
          </div>

          <div class="filter-group">
            <span class="filter-label">招募阶段</span>
            <div class="pills">
              <span
                v-for="p in phaseOptions"
                :key="p.value"
                :class="['pill', filters.recruitmentPhase === p.value && 'pill-active']"
                @click="setFilter('recruitmentPhase', p.value)"
              >{{ p.label }}</span>
            </div>
          </div>

          <div class="filter-group">
            <span class="filter-label">活动类型</span>
            <div class="pills">
              <span
                v-for="c in categoryOptions"
                :key="c.value"
                :class="['pill', filters.category === c.value && 'pill-active']"
                @click="setFilter('category', c.value)"
              >{{ c.label }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <div v-loading="loading" class="grid-list">
        <article v-for="row in activities" :key="row.id" class="activity-card" @click="goToDetail(row.id)">
          <div class="cover" :class="{ empty: !row.imageUrl }">
            <img v-if="row.imageUrl" :src="row.imageUrl" :alt="row.title">
            <div v-else class="cover-fallback">
              <span>{{ row.category || '志愿活动' }}</span>
            </div>
          </div>

          <div class="card-top">
            <el-tag :type="getRecruitmentDisplay(row).type" effect="light">{{ getRecruitmentDisplay(row).text }}</el-tag>
            <el-tag :type="getActivityPhaseDisplay(row).type" effect="plain">{{ getActivityPhaseDisplay(row).text }}</el-tag>
          </div>

          <h3 class="text-ellipsis-2">{{ row.title }}</h3>
          <div class="meta">{{ row.category }}</div>

          <div class="line"><el-icon><Location /></el-icon>{{ row.location }}</div>
          <div class="line"><el-icon><Clock /></el-icon>报名截止：{{ formatDate(row.registrationDeadline) }}</div>
          <div class="line"><el-icon><Timer /></el-icon>{{ row.volunteerHours }} 小时</div>

          <div class="progress-wrap">
            <div class="progress-label">{{ row.currentParticipants }}/{{ row.maxParticipants }} 人</div>
            <el-progress
              :percentage="Math.min(Math.round((row.currentParticipants / row.maxParticipants) * 100), 100)"
              :color="progressColor(row.currentParticipants, row.maxParticipants)"
              :show-text="false"
              :stroke-width="6"
            />
          </div>
        </article>
      </div>

      <el-empty v-if="!loading && activities.length === 0" description="暂无符合条件的活动" />

      <div class="pagination-bar">
        <span class="total-tip">共 {{ pagination.total }} 条</span>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleSearch"
        />
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Layout from '@/components/Layout.vue'
import { getActivityList } from '@/api/activity'
import { getRecruitmentDisplay } from '@/utils/recruitment'
import { getActivityPhaseDisplay } from '@/utils/activityPhase'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)
const activities = ref([])

const statusOptions = [
  { label: '全部', value: '' },
  { label: '招募中', value: 'RECRUITING' },
  { label: '已结项', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]
const phaseOptions = [
  { label: '全部', value: '' },
  { label: '未开始', value: 'NOT_STARTED' },
  { label: '招募中', value: 'RECRUITING' },
  { label: '已结束', value: 'ENDED' }
]
const categoryOptions = [
  { label: '全部', value: '' },
  { label: '校园服务', value: '校园服务' },
  { label: '公益助学', value: '公益助学' },
  { label: '社区关爱', value: '社区关爱' },
  { label: '大型活动', value: '大型活动' },
  { label: '环保公益', value: '环保公益' },
  { label: '应急救援', value: '应急救援' }
]

const progressColor = (cur, max) => {
  const pct = (cur / max) * 100
  if (pct >= 90) return '#f56c6c'
  if (pct >= 60) return '#e6a23c'
  return '#67c23a'
}

const filters = reactive({ status: '', category: '', recruitmentPhase: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

const setFilter = (key, value) => {
  filters[key] = filters[key] === value ? '' : value
  pagination.page = 1
  handleSearch()
}

const handleSizeChange = () => {
  pagination.page = 1
  handleSearch()
}

const formatDate = (date) => dayjs(date).format('MM-DD HH:mm')

const goToDetail = (id) => router.push(`/activity/${id}`)

const handleSearch = async () => {
  loading.value = true
  try {
    const res = await getActivityList({
      page: pagination.page,
      size: pagination.size,
      status: filters.status,
      category: filters.category,
      recruitmentPhase: filters.recruitmentPhase
    })
    activities.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('获取活动列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => { handleSearch() })
</script>

<style scoped>
.activity-page {
  width: 100%;
  margin: 0 auto;
}

.banner {
  border-radius: 24px;
  padding: 30px 28px;
  margin-bottom: 16px;
  color: white;
  background: linear-gradient(135deg, var(--cv-primary), var(--cv-primary-weak));
}

.banner-mini {
  font-size: 12px;
  opacity: 0.88;
  text-transform: uppercase;
  letter-spacing: 1.4px;
}

.banner h1 {
  font-size: clamp(26px, 4.5vw, 36px);
  margin: 8px 0 10px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-group {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.filter-label {
  width: 64px;
  color: #6b7084;
  font-size: 13px;
  margin-top: 6px;
  flex-shrink: 0;
}

.pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pill {
  border-radius: 999px;
  padding: 5px 12px;
  border: 1px solid #d7d9e8;
  color: #5a5f75;
  cursor: pointer;
  user-select: none;
  font-size: 13px;
}

.pill-active {
  border-color: transparent;
  color: white;
  background: linear-gradient(135deg, var(--cv-primary), var(--cv-primary-weak));
}

.grid-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.activity-card {
  border-radius: 16px;
  background: white;
  padding: 14px;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(0, 20, 83, 0.06);
  transition: all 0.22s ease;
}

.activity-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 28px rgba(0, 20, 83, 0.1);
}

.card-top {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 0 2px 10px;
}

.activity-card h3 {
  font-size: 18px;
  line-height: 1.4;
  min-height: 52px;
  margin: 0 2px 6px;
}

.meta {
  font-size: 13px;
  color: #6c7187;
  margin: 0 2px 8px;
}

.line {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #4f546a;
  margin: 0 2px 6px;
}

.progress-wrap {
  margin: 10px 2px 0;
}

.progress-label {
  font-size: 12px;
  color: #878ca1;
  margin-bottom: 4px;
}

.cover {
  overflow: hidden;
  height: 188px;
  border-radius: 14px;
  margin-bottom: 12px;
  background: linear-gradient(145deg, #dde8ff, #f8efe1);
}

.cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: flex-end;
  padding: 18px;
  color: #40506e;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.total-tip {
  color: #7c8096;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .grid-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .banner {
    padding: 24px 18px;
  }

  .banner h1 {
    font-size: 26px;
  }

  .filter-group {
    flex-direction: column;
  }

  .filter-label {
    width: auto;
    margin-top: 0;
  }

  .grid-list {
    grid-template-columns: 1fr;
  }
}
</style>




