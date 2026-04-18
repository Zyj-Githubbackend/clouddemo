<template>
  <div class="st-page admin-page">
    <section class="st-hero page-hero">
      <div class="st-hero-content">
        <div class="st-hero-copy">
          <p class="st-hero-eyebrow">反馈管理</p>
          <h1 class="st-hero-title">反馈工单</h1>
          <p class="st-hero-desc">
            受理用户提交的问题、建议、Bug 和投诉，支持多轮回复、优先级调整、关闭与驳回。
          </p>
        </div>
        <div class="st-hero-actions">
          <span class="st-chip">总数 {{ pagination.total }} 条</span>
        </div>
      </div>
    </section>

    <section class="st-stat-grid">
      <article class="st-stat-card">
        <span class="label">处理中</span>
        <strong class="value">{{ localStatusCount('OPEN') }}</strong>
        <span class="hint">按当前列表统计</span>
      </article>
      <article class="st-stat-card">
        <span class="label">管理员已回复</span>
        <strong class="value">{{ localStatusCount('REPLIED') }}</strong>
        <span class="hint">按当前列表统计</span>
      </article>
      <article class="st-stat-card">
        <span class="label">高优先级</span>
        <strong class="value">{{ localPriorityCount('HIGH') + localPriorityCount('URGENT') }}</strong>
        <span class="hint">按当前列表统计</span>
      </article>
    </section>

    <section class="st-panel table-panel">
      <div class="st-section-head">
        <div>
          <h2>工单列表</h2>
          <p>支持按状态、分类、优先级和关键词筛选反馈工单。</p>
        </div>
      </div>

      <div class="toolbar-grid">
        <el-select v-model="filters.status" placeholder="状态" clearable @change="handleFilterChange">
          <el-option label="处理中" value="OPEN" />
          <el-option label="管理员已回复" value="REPLIED" />
          <el-option label="已关闭" value="CLOSED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-select v-model="filters.category" placeholder="分类" clearable @change="handleFilterChange">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.priority" placeholder="优先级" clearable @change="handleFilterChange">
          <el-option label="低" value="LOW" />
          <el-option label="普通" value="NORMAL" />
          <el-option label="高" value="HIGH" />
          <el-option label="紧急" value="URGENT" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="搜索标题/原因" clearable @keyup.enter="handleFilterChange" />
        <el-button type="primary" @click="handleFilterChange">搜索</el-button>
      </div>

      <div class="table-scroll">
        <el-table :data="feedbackList" v-loading="loading" stripe>
          <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />
          <el-table-column label="用户ID" prop="userId" width="90" />
          <el-table-column label="分类" width="110">
            <template #default="{ row }">{{ categoryText(row.category) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="statusDisplay(row.status).type">{{ statusDisplay(row.status).text }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="100">
            <template #default="{ row }">
              <el-tag :type="priorityDisplay(row.priority).type">{{ priorityDisplay(row.priority).text }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近回复" width="110">
            <template #default="{ row }">{{ roleText(row.lastReplierRole) }}</template>
          </el-table-column>
          <el-table-column label="最近更新" width="170">
            <template #default="{ row }">{{ formatDate(row.lastMessageTime || row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="openDetail(row)">查看</el-button>
              <el-button
                v-if="canOperate(row)"
                type="success"
                size="small"
                link
                @click="handleClose(row)"
              >
                关闭
              </el-button>
              <el-button
                v-if="canOperate(row)"
                type="danger"
                size="small"
                link
                @click="handleReject(row)"
              >
                驳回
              </el-button>
            </template>
          </el-table-column>
        </el-table>
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

    <el-drawer v-model="drawerVisible" size="min(92vw, 920px)" destroy-on-close title="反馈详情">
      <div v-loading="detailLoading" class="drawer-body">
        <section class="detail-title">
          <h2>{{ detail.title }}</h2>
          <div class="meta-row">
            <el-tag>{{ categoryText(detail.category) }}</el-tag>
            <el-tag :type="statusDisplay(detail.status).type">{{ statusDisplay(detail.status).text }}</el-tag>
            <el-tag :type="priorityDisplay(detail.priority).type">{{ priorityDisplay(detail.priority).text }}</el-tag>
            <span>用户 #{{ detail.userId }}</span>
          </div>
        </section>

        <div class="priority-row">
          <el-select v-model="priorityForm.priority" :disabled="!detail.id" placeholder="优先级">
            <el-option label="低" value="LOW" />
            <el-option label="普通" value="NORMAL" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
          <el-button :loading="prioritySaving" @click="savePriority">保存优先级</el-button>
          <el-button v-if="canOperate(detail)" type="success" @click="handleClose(detail)">关闭</el-button>
          <el-button v-if="canOperate(detail)" type="danger" @click="handleReject(detail)">驳回</el-button>
        </div>

        <div class="messages">
          <article
            v-for="message in detail.messages || []"
            :key="message.id"
            :class="['message', message.senderRole === 'ADMIN' && 'admin', message.senderRole === 'SYSTEM' && 'system']"
          >
            <div class="bubble">
              <div class="message-top">
                <strong>{{ roleText(message.senderRole) }}</strong>
                <span>{{ formatDate(message.createTime) }}</span>
              </div>
              <p v-if="message.content">{{ message.content }}</p>
              <FeedbackAttachmentList :attachments="message.attachments || []" />
            </div>
          </article>
        </div>

        <el-card v-if="canOperate(detail)" class="reply-card" shadow="never">
          <h3>管理员回复</h3>
          <el-input
            v-model="replyForm.content"
            type="textarea"
            :rows="4"
            maxlength="5000"
            show-word-limit
            placeholder="写下处理进展、解决方案或需要用户补充的信息。"
          />
          <FeedbackAttachmentUploader
            :attachments="replyForm.attachments"
            @update:attachments="value => { replyForm.attachments = value }"
          />
          <div class="reply-actions">
            <el-button type="primary" :loading="replying" @click="submitReply">发送回复</el-button>
          </div>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import FeedbackAttachmentList from '@/components/FeedbackAttachmentList.vue'
import FeedbackAttachmentUploader from '@/components/FeedbackAttachmentUploader.vue'
import {
  closeFeedbackAsAdmin,
  getAdminFeedbackDetail,
  getAdminFeedbackList,
  rejectFeedbackAsAdmin,
  replyFeedbackAsAdmin,
  updateFeedbackPriority
} from '@/api/feedback'

const loading = ref(false)
const detailLoading = ref(false)
const replying = ref(false)
const prioritySaving = ref(false)
const drawerVisible = ref(false)
const feedbackList = ref([])
const detail = ref({})
const filters = reactive({ status: '', category: '', priority: '', keyword: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const replyForm = reactive({ content: '', attachments: [] })
const priorityForm = reactive({ priority: 'NORMAL' })

const categoryOptions = [
  { label: '问题咨询', value: 'QUESTION' },
  { label: '建议', value: 'SUGGESTION' },
  { label: '系统问题', value: 'BUG' },
  { label: '投诉', value: 'COMPLAINT' },
  { label: '其他', value: 'OTHER' }
]
const statusMap = {
  OPEN: { text: '处理中', type: 'warning' },
  REPLIED: { text: '管理员已回复', type: 'success' },
  CLOSED: { text: '已关闭', type: 'info' },
  REJECTED: { text: '已驳回', type: 'danger' }
}
const categoryMap = Object.fromEntries(categoryOptions.map(item => [item.value, item.label]))
const priorityMap = {
  LOW: { text: '低', type: 'info' },
  NORMAL: { text: '普通', type: '' },
  HIGH: { text: '高', type: 'warning' },
  URGENT: { text: '紧急', type: 'danger' }
}
const roleMap = {
  VOLUNTEER: '用户',
  ADMIN: '管理员',
  SYSTEM: '系统'
}

const statusDisplay = (status) => statusMap[status] || { text: status || '--', type: 'info' }
const priorityDisplay = (priority) => priorityMap[priority] || { text: priority || '--', type: '' }
const categoryText = (value) => categoryMap[value] || value || '--'
const roleText = (value) => roleMap[value] || value || '--'
const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '--'
const canOperate = (row) => ['OPEN', 'REPLIED'].includes(row?.status)

const localStatusCount = (status) => feedbackList.value.filter(item => item.status === status).length
const localPriorityCount = (priority) => feedbackList.value.filter(item => item.priority === priority).length

const fetchFeedback = async () => {
  loading.value = true
  try {
    const res = await getAdminFeedbackList({
      page: pagination.page,
      size: pagination.size,
      ...filters
    })
    feedbackList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('获取反馈列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchDetail = async (id) => {
  detailLoading.value = true
  try {
    const res = await getAdminFeedbackDetail(id)
    detail.value = res.data || {}
    priorityForm.priority = detail.value.priority || 'NORMAL'
  } catch (error) {
    console.error('获取反馈详情失败:', error)
  } finally {
    detailLoading.value = false
  }
}

const openDetail = async (row) => {
  drawerVisible.value = true
  replyForm.content = ''
  replyForm.attachments = []
  await fetchDetail(row.id)
}

const submitReply = async () => {
  if (!replyForm.content.trim() && replyForm.attachments.length === 0) {
    ElMessage.warning('请填写内容或上传附件')
    return
  }
  replying.value = true
  try {
    await replyFeedbackAsAdmin(detail.value.id, {
      content: replyForm.content,
      attachments: replyForm.attachments
    })
    ElMessage.success('回复已发送')
    replyForm.content = ''
    replyForm.attachments = []
    await fetchDetail(detail.value.id)
    fetchFeedback()
  } catch (error) {
    console.error('回复反馈失败:', error)
  } finally {
    replying.value = false
  }
}

const savePriority = async () => {
  if (!detail.value.id) return
  prioritySaving.value = true
  try {
    await updateFeedbackPriority(detail.value.id, priorityForm.priority)
    ElMessage.success('优先级已更新')
    await fetchDetail(detail.value.id)
    fetchFeedback()
  } catch (error) {
    console.error('更新优先级失败', error)
  } finally {
    prioritySaving.value = false
  }
}

const handleClose = (row) => {
  ElMessageBox.confirm(`确认关闭“${row.title}”吗？`, '关闭反馈', {
    type: 'warning',
    confirmButtonText: '关闭',
    cancelButtonText: '取消'
  }).then(async () => {
    await closeFeedbackAsAdmin(row.id)
    ElMessage.success('反馈已关闭')
    if (detail.value.id === row.id) await fetchDetail(row.id)
    fetchFeedback()
  }).catch(() => {})
}

const handleReject = (row) => {
  ElMessageBox.prompt('请输入驳回原因', '驳回反馈', {
    inputType: 'textarea',
    inputPlaceholder: '例如：重复反馈，已在另一条记录中处理',
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
    inputValidator: value => Boolean(value && value.trim()),
    inputErrorMessage: '请填写驳回原因'
  }).then(async ({ value }) => {
    await rejectFeedbackAsAdmin(row.id, value)
    ElMessage.success('反馈已驳回')
    if (detail.value.id === row.id) await fetchDetail(row.id)
    fetchFeedback()
  }).catch(() => {})
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
.page-hero {
  min-height: 220px;
}

.table-panel {
  padding: 22px;
}

.toolbar-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 160px)) minmax(180px, 1fr) auto;
  gap: 10px;
  margin-bottom: 14px;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-title h2 {
  margin: 0 0 10px;
  word-break: break-word;
}

.meta-row,
.priority-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: #6d748d;
}

.messages {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.message {
  display: flex;
}

.message.admin {
  justify-content: flex-end;
}

.message.system {
  justify-content: center;
}

.bubble {
  width: min(720px, 92%);
  padding: 14px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(13, 71, 217, 0.08);
}

.admin .bubble {
  background: #eef6ff;
}

.system .bubble {
  width: auto;
  background: #f2f4f8;
  color: #666f86;
}

.message-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #69718c;
  font-size: 13px;
}

.bubble p {
  margin: 10px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: #263250;
  line-height: 1.7;
}

.reply-card h3 {
  margin: 0 0 12px;
}

.reply-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-radius: 22px;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .toolbar-grid {
    grid-template-columns: 1fr;
  }

  .table-panel {
    padding: 16px;
  }

  .pagination-bar {
    justify-content: flex-start;
  }
}
</style>
