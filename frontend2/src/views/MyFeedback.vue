<template>
  <Layout>
    <div class="st-page page-container feedback-hub">
      <section class="st-hero hub-hero">
        <div class="st-hero-content">
          <div class="st-hero-copy">
            <p class="st-hero-eyebrow">反馈中心</p>
            <h1 class="st-hero-title">我的反馈工单</h1>
            <p class="st-hero-desc">
              在这里查看你提交过的反馈，跟进处理进度，并继续补充信息或确认关闭。
            </p>
          </div>

          <div class="st-hero-actions">
            <span class="st-chip">
              <el-icon><Tickets /></el-icon>
              已加载 {{ feedbackList.length }} 条
            </span>
            <el-button class="hero-btn" type="primary" @click="router.push('/feedback')">
              提交反馈
            </el-button>
          </div>
        </div>
      </section>

      <section class="st-stat-grid">
        <article class="st-stat-card">
          <span class="label">处理中</span>
          <strong class="value">{{ localStatusCount('OPEN') }}</strong>
          <span class="hint">状态来自当前分页数据</span>
        </article>
        <article class="st-stat-card">
          <span class="label">管理员已回复</span>
          <strong class="value">{{ localStatusCount('REPLIED') }}</strong>
          <span class="hint">可继续补充消息</span>
        </article>
        <article class="st-stat-card">
          <span class="label">已关闭 / 已驳回</span>
          <strong class="value">{{ localStatusCount('CLOSED') + localStatusCount('REJECTED') }}</strong>
          <span class="hint">按当前列表统计</span>
        </article>
      </section>

      <section class="st-panel hub-panel">
        <div class="st-section-head">
          <div>
            <h2>工单列表</h2>
            <p>点击任一工单进入详情页，也可按处理状态快速筛选。</p>
          </div>
        </div>

        <div class="toolbar-row">
          <el-select v-model="statusFilter" placeholder="反馈状态" clearable @change="handleFilterChange">
            <el-option label="处理中" value="OPEN" />
            <el-option label="管理员已回复" value="REPLIED" />
            <el-option label="已关闭" value="CLOSED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <span class="toolbar-tip">共 {{ pagination.total }} 条</span>
        </div>

        <div v-if="!loading && feedbackList.length === 0" class="st-empty">
          <el-empty description="暂无反馈记录" />
        </div>

        <div v-else class="ticket-list" v-loading="loading">
          <article
            v-for="row in feedbackList"
            :key="row.id"
            class="ticket-item"
            @click="router.push(`/feedback/${row.id}`)"
          >
            <div class="ticket-main">
              <h3>{{ row.title }}</h3>
              <div class="ticket-sub">
                <span><el-icon><ChatDotRound /></el-icon>{{ categoryText(row.category) }}</span>
                <span><el-icon><Clock /></el-icon>{{ formatDate(row.lastMessageTime || row.updateTime) }}</span>
              </div>
            </div>

            <div class="ticket-side">
              <span :class="['ticket-status', statusDisplay(row.status).type]">
                {{ statusDisplay(row.status).text }}
              </span>
              <span :class="['ticket-priority', priorityClass(row.priority)]">
                {{ priorityText(row.priority) }}
              </span>
            </div>
          </article>
        </div>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="sizes, prev, pager, next, jumper"
            background
            @size-change="handleSizeChange"
            @current-change="fetchFeedback"
          />
        </div>
      </section>
    </div>
  </Layout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import Layout from '@/components/Layout.vue'
import { getMyFeedback } from '@/api/feedback'

const router = useRouter()
const loading = ref(false)
const feedbackList = ref([])
const statusFilter = ref('')
const pagination = reactive({ page: 1, size: 10, total: 0 })

const statusMap = {
  OPEN: { text: '处理中', type: 'warning' },
  REPLIED: { text: '管理员已回复', type: 'success' },
  CLOSED: { text: '已关闭', type: 'info' },
  REJECTED: { text: '已驳回', type: 'danger' }
}

const categoryMap = {
  QUESTION: '问题咨询',
  SUGGESTION: '建议',
  BUG: '系统问题',
  COMPLAINT: '投诉',
  OTHER: '其他'
}

const priorityMap = {
  LOW: '低',
  NORMAL: '普通',
  HIGH: '高',
  URGENT: '紧急'
}

const statusDisplay = (status) => statusMap[status] || { text: status || '--', type: 'info' }
const categoryText = (value) => categoryMap[value] || value || '--'
const priorityText = (value) => priorityMap[value] || value || '--'
const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '--'

const priorityClass = (value) => {
  if (value === 'URGENT') return 'danger'
  if (value === 'HIGH') return 'warning'
  if (value === 'LOW') return 'muted'
  return 'normal'
}

const localStatusCount = (status) => feedbackList.value.filter(item => item.status === status).length

const fetchFeedback = async () => {
  loading.value = true
  try {
    const res = await getMyFeedback({
      page: pagination.page,
      size: pagination.size,
      status: statusFilter.value
    })
    feedbackList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('获取我的反馈失败:', error)
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  pagination.page = 1
  fetchFeedback()
}

const handleSizeChange = () => {
  pagination.page = 1
  fetchFeedback()
}

onMounted(fetchFeedback)
</script>

<style scoped>
.feedback-hub {
  max-width: 1180px;
}

.hub-hero {
  min-height: 240px;
}

.hero-btn {
  height: 46px;
  border-radius: 999px;
}

.hub-panel {
  padding: 22px;
}

.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.toolbar-tip {
  color: #7a8198;
  font-size: 13px;
}

.ticket-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ticket-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(201, 214, 243, 0.46);
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.ticket-item:hover {
  background: rgba(242, 246, 251, 0.88);
  transform: translateY(-2px);
}

.ticket-main h3 {
  margin-bottom: 8px;
  font-size: 20px;
  line-height: 1.35;
}

.ticket-sub {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #73809d;
  font-size: 13px;
}

.ticket-sub span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.ticket-side {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.ticket-status,
.ticket-priority {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.ticket-status.warning,
.ticket-priority.warning {
  background: rgba(255, 183, 3, 0.16);
  color: #b76d00;
}

.ticket-status.success {
  background: rgba(15, 139, 95, 0.12);
  color: #0f8b5f;
}

.ticket-status.info,
.ticket-priority.normal {
  background: rgba(13, 71, 217, 0.08);
  color: var(--cv-primary);
}

.ticket-status.danger,
.ticket-priority.danger {
  background: rgba(214, 54, 56, 0.12);
  color: #c73638;
}

.ticket-priority.muted {
  background: rgba(120, 131, 157, 0.12);
  color: #6f7b97;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 768px) {
  .hub-panel {
    padding: 16px;
  }

  .ticket-item {
    grid-template-columns: 1fr;
  }

  .ticket-side,
  .pagination-bar {
    justify-content: flex-start;
  }
}
</style>
