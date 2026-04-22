<template>
  <Layout>
    <div class="stitch-activity-page page-container">
      <section class="hero-panel">
        <div class="hero-copy">
          <div class="hero-badge">
            <el-icon><Compass /></el-icon>
            <span>志愿活动</span>
          </div>
          <h1>
            发现下一场
            <span>值得投入的志愿行动</span>
          </h1>
          <p>按活动状态、招募阶段和活动类型筛选，快速找到适合参与的志愿服务。</p>
        </div>

        <div class="hero-actions">
          <button type="button" class="hero-primary" @click="applyRecruitingPreset">
            查看招募中活动
          </button>
          <button type="button" class="hero-secondary" :disabled="!hasActiveFilters" @click="resetFilters">
            重置筛选
          </button>
        </div>
      </section>

      <div class="activity-layout">
        <aside class="filter-panel">
          <div class="panel-head">
            <div>
              <p>筛选条件</p>
              <h2>筛选活动</h2>
            </div>
            <span>{{ activeFilterSummary }}</span>
          </div>

          <ActivityFilterGroup
            :model-value="filters.status"
            title="活动状态"
            caption="状态"
            :options="statusOptions"
            @update:model-value="value => updateFilter('status', value)"
          />

          <div class="panel-divider"></div>

          <ActivityFilterGroup
            :model-value="filters.recruitmentPhase"
            title="招募阶段"
            caption="阶段"
            :options="phaseOptions"
            @update:model-value="value => updateFilter('recruitmentPhase', value)"
          />

          <div class="panel-divider"></div>

          <ActivityFilterGroup
            :model-value="filters.category"
            title="活动类型"
            caption="分类"
            :options="categoryOptions"
            @update:model-value="value => updateFilter('category', value)"
          />

          <div class="panel-note">
            <strong>筛选说明</strong>
            <p>可按状态、招募阶段和活动类型组合筛选，结果会随条件变化实时更新。</p>
          </div>
        </aside>

        <section class="content-panel">
          <div class="content-head">
            <div>
              <p class="content-kicker">活动结果</p>
              <h2>活动广场</h2>
              <span>{{ resultSummary }}</span>
            </div>

            <div class="content-actions">
              <div class="page-size-chip">每页 {{ pagination.size }} 条</div>
            </div>
          </div>

          <div v-if="loadError" class="state-panel error">
            <div class="state-icon">
              <el-icon><WarningFilled /></el-icon>
            </div>
            <div class="state-copy">
              <strong>活动列表加载失败</strong>
              <p>{{ loadError }}</p>
            </div>
            <el-button type="primary" @click="fetchActivities">重新加载</el-button>
          </div>

          <div v-else-if="loading" class="card-grid skeleton-grid" aria-label="活动加载中">
            <div v-for="index in 6" :key="index" class="activity-skeleton">
              <div class="skeleton-cover"></div>
              <div class="skeleton-body">
                <div class="skeleton-line short"></div>
                <div class="skeleton-line"></div>
                <div class="skeleton-line"></div>
                <div class="skeleton-footer"></div>
              </div>
            </div>
          </div>

          <div v-else-if="activities.length > 0" class="card-grid">
            <ActivityCard
              v-for="activity in activities"
              :key="activity.id"
              :activity="activity"
            />
          </div>

          <div v-else class="state-panel empty">
            <el-empty description="暂无符合当前条件的活动">
              <el-button type="primary" @click="resetFilters">查看全部活动</el-button>
            </el-empty>
          </div>

          <div v-if="!loadError && pagination.total > 0" class="pagination-shell">
            <div class="pagination-copy">
              <strong>{{ pagination.total }}</strong>
              <span>条结果</span>
            </div>

            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.size"
              background
              :total="pagination.total"
              :page-sizes="[10, 20, 50]"
              layout="sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="fetchActivities"
            />
          </div>
        </section>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Compass } from '@element-plus/icons-vue'
import Layout from '@/components/Layout.vue'
import ActivityCard from '@/components/stitch/activity/ActivityCard.vue'
import ActivityFilterGroup from '@/components/stitch/activity/ActivityFilterGroup.vue'
import { getActivityList } from '@/api/activity'

const loading = ref(false)
const loadError = ref('')
const activities = ref([])

const filters = reactive({
  status: '',
  recruitmentPhase: '',
  category: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

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

const hasActiveFilters = computed(() => Object.values(filters).some(Boolean))

const activeFilterSummary = computed(() => {
  const count = Object.values(filters).filter(Boolean).length
  return count > 0 ? `已选 ${count} 项` : '未筛选'
})

const resultSummary = computed(() => {
  if (loading.value) return '正在加载活动数据...'
  if (loadError.value) return '加载失败，可重试'
  if (pagination.total === 0) return '当前没有匹配活动'
  return `第 ${pagination.page} 页，共 ${pagination.total} 条结果`
})

const fetchActivities = async () => {
  loading.value = true
  loadError.value = ''

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
    activities.value = []
    pagination.total = 0
    loadError.value = error?.message || '请稍后重试。'
    console.error('获取活动列表失败:', error)
  } finally {
    loading.value = false
  }
}

const updateFilter = (key, value) => {
  filters[key] = value
  pagination.page = 1
  fetchActivities()
}

const resetFilters = () => {
  filters.status = ''
  filters.recruitmentPhase = ''
  filters.category = ''
  pagination.page = 1
  fetchActivities()
}

const applyRecruitingPreset = () => {
  filters.status = 'RECRUITING'
  filters.recruitmentPhase = 'RECRUITING'
  pagination.page = 1
  fetchActivities()
}

const handleSizeChange = () => {
  pagination.page = 1
  fetchActivities()
}

onMounted(fetchActivities)
</script>

<style scoped>
.stitch-activity-page {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.hero-panel {
  position: relative;
  overflow: hidden;
  border-radius: 40px;
  padding: clamp(28px, 4vw, 46px);
  min-height: 320px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  color: #fff;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.32), transparent 34%),
    linear-gradient(135deg, #0d47d9 0%, #0066ff 42%, #00c1fd 100%);
  box-shadow: 0 24px 48px rgba(13, 71, 217, 0.18);
}

.hero-copy {
  max-width: 760px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-badge {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1 {
  color: #fff;
  font-size: clamp(36px, 5vw, 58px);
  line-height: 1.03;
  letter-spacing: -0.04em;
  max-width: 680px;
}

.hero-copy h1 span {
  display: block;
  color: rgba(255, 255, 255, 0.96);
}

.hero-copy p {
  max-width: 620px;
  margin: 0;
  color: rgba(255, 255, 255, 0.88);
  font-size: 16px;
  line-height: 1.75;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.hero-primary,
.hero-secondary {
  min-height: 52px;
  border: none;
  border-radius: 999px;
  padding: 0 22px;
  font-family: 'Plus Jakarta Sans', 'Manrope', sans-serif;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease, background 0.2s ease;
}

.hero-primary {
  color: var(--cv-primary);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 28px rgba(255, 255, 255, 0.16);
}

.hero-secondary {
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.22);
}

.hero-secondary:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.hero-primary:hover,
.hero-secondary:hover:not(:disabled) {
  transform: translateY(-1px);
}

.activity-layout {
  display: grid;
  grid-template-columns: minmax(280px, 324px) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.filter-panel,
.content-panel {
  border: 1px solid rgba(194, 204, 229, 0.66);
  border-radius: 36px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--cv-shadow-soft);
  backdrop-filter: blur(18px);
}

.filter-panel {
  position: sticky;
  top: 108px;
  padding: 26px;
}

.panel-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 22px;
}

.panel-head p,
.content-kicker {
  margin: 0 0 6px;
  color: #5c6a8e;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.panel-head h2,
.content-head h2 {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.panel-head span,
.content-head span {
  color: #75829f;
  font-size: 13px;
  font-weight: 700;
}

.panel-divider {
  height: 1px;
  margin: 22px 0;
  background: rgba(194, 204, 229, 0.7);
}

.panel-note {
  margin-top: 24px;
  border-radius: 24px;
  padding: 18px 18px 20px;
  background: linear-gradient(145deg, rgba(242, 246, 251, 0.96), rgba(235, 244, 255, 0.98));
}

.panel-note strong {
  display: block;
  color: var(--cv-text);
  font-size: 14px;
  font-weight: 800;
}

.panel-note p {
  margin: 10px 0 0;
  color: #617090;
  font-size: 13px;
  line-height: 1.7;
}

.content-panel {
  padding: 26px;
  min-height: 720px;
}

.content-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.page-size-chip {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  padding: 0 16px;
  border-radius: 999px;
  background: #eef2f7;
  color: #556480;
  font-size: 13px;
  font-weight: 800;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.state-panel {
  min-height: 420px;
  border-radius: 30px;
  background: #f9fbff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  text-align: center;
  padding: 28px;
}

.state-panel.error {
  border: 1px dashed rgba(233, 150, 150, 0.9);
  background: rgba(255, 244, 244, 0.88);
}

.state-icon {
  width: 64px;
  height: 64px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #d05c5c;
  font-size: 28px;
}

.state-copy {
  max-width: 420px;
}

.state-copy strong {
  display: block;
  color: var(--cv-text);
  font-size: 20px;
  font-weight: 800;
}

.state-copy p {
  margin: 10px 0 0;
  color: #687796;
  line-height: 1.7;
}

.skeleton-grid {
  align-items: stretch;
}

.activity-skeleton {
  overflow: hidden;
  border-radius: 30px;
  border: 1px solid rgba(194, 204, 229, 0.54);
  background: #fff;
}

.skeleton-cover {
  height: 224px;
  background: linear-gradient(90deg, #edf2f9 25%, #f7f9fd 37%, #edf2f9 63%);
  background-size: 400% 100%;
  animation: shimmer 1.2s infinite linear;
}

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
}

.skeleton-line,
.skeleton-footer {
  height: 16px;
  border-radius: 999px;
  background: linear-gradient(90deg, #edf2f9 25%, #f7f9fd 37%, #edf2f9 63%);
  background-size: 400% 100%;
  animation: shimmer 1.2s infinite linear;
}

.skeleton-line.short {
  width: 42%;
}

.skeleton-footer {
  height: 58px;
  margin-top: 10px;
}

.pagination-shell {
  margin-top: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding-top: 24px;
  border-top: 1px solid rgba(194, 204, 229, 0.62);
}

.pagination-copy {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  color: #617090;
}

.pagination-copy strong {
  color: var(--cv-primary);
  font-family: 'Plus Jakarta Sans', 'Manrope', sans-serif;
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
}

@keyframes shimmer {
  0% {
    background-position: 100% 0;
  }

  100% {
    background-position: 0 0;
  }
}

@media (max-width: 1279px) {
  .activity-layout {
    grid-template-columns: 1fr;
  }

  .filter-panel {
    position: static;
  }
}

@media (max-width: 960px) {
  .hero-panel {
    min-height: auto;
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .card-grid {
    grid-template-columns: 1fr;
  }

  .pagination-shell {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .hero-panel,
  .filter-panel,
  .content-panel {
    border-radius: 28px;
  }

  .content-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-copy h1 {
    font-size: 34px;
  }
}
</style>
