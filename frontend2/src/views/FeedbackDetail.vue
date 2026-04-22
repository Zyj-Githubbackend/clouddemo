<template>
  <Layout>
    <div class="st-page page-container feedback-detail-page">
      <section class="detail-head">
        <button type="button" class="back-link" @click="router.push('/my-feedback')">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回我的反馈</span>
        </button>

        <div class="head-main">
          <h1>{{ detail.title || '反馈详情' }}</h1>
          <div class="meta-row">
            <span class="st-badge">{{ categoryText(detail.category) }}</span>
            <span :class="['st-badge', badgeClass(detail.status)]">{{ statusDisplay(detail.status).text }}</span>
            <span class="st-badge">{{ priorityText(detail.priority) }}</span>
            <span class="head-date">{{ formatDate(detail.createTime) }}</span>
          </div>
        </div>

      </section>

      <div class="detail-grid">
        <main class="timeline-panel st-panel">
          <div class="timeline-head">
            <div>
              <h2>处理会话</h2>
              <p>按时间查看反馈处理进展和双方回复记录。</p>
            </div>
          </div>

          <div class="timeline-body" v-loading="loading">
            <article
              v-for="message in detail.messages || []"
              :key="message.id"
              :class="['message-item', message.senderRole === 'ADMIN' && 'admin', message.senderRole === 'SYSTEM' && 'system']"
            >
              <div class="message-avatar">
                <span v-if="message.senderRole === 'SYSTEM'">
                  <el-icon><Notification /></el-icon>
                </span>
                <span v-else>{{ roleText(message.senderRole).slice(0, 1) }}</span>
              </div>

              <div class="message-bubble">
                <div class="message-top">
                  <strong>{{ roleText(message.senderRole) }}</strong>
                  <span>{{ formatDate(message.createTime) }}</span>
                </div>
                <p v-if="message.content">{{ message.content }}</p>
                <FeedbackAttachmentList :attachments="message.attachments || []" />
              </div>
            </article>
          </div>

          <el-card v-if="canOperate" class="reply-card" shadow="never">
            <h3>继续补充</h3>
            <el-input
              v-model="replyForm.content"
              type="textarea"
              :rows="4"
              maxlength="5000"
              show-word-limit
              placeholder="补充复现步骤、截图说明或继续追问。"
            />
            <FeedbackAttachmentUploader
              :attachments="replyForm.attachments"
              @update:attachments="value => { replyForm.attachments = value }"
            />
            <div class="reply-actions">
              <el-button type="primary" :loading="replying" @click="submitReply">发送消息</el-button>
            </div>
          </el-card>

          <el-alert
            v-else
            title="这条反馈已结束，当前不能继续回复。"
            :closable="false"
            type="info"
            show-icon
          />
        </main>

        <aside class="st-side-stack">
          <section class="st-panel side-panel">
            <div class="st-section-head compact">
              <h3>处理信息</h3>
            </div>
            <dl class="detail-list">
              <dt>状态</dt>
              <dd>{{ statusDisplay(detail.status).text }}</dd>
              <dt>最后回复方</dt>
              <dd>{{ roleText(detail.lastReplierRole) }}</dd>
              <dt>最后更新时间</dt>
              <dd>{{ formatDate(detail.lastMessageTime || detail.updateTime) }}</dd>
              <template v-if="detail.rejectReason">
                <dt>驳回原因</dt>
                <dd>{{ detail.rejectReason }}</dd>
              </template>
            </dl>
          </section>

          <section class="st-panel side-panel">
            <div class="st-section-head compact">
              <h3>当前操作</h3>
            </div>
            <el-button class="full-btn" @click="router.push('/my-feedback')">返回列表</el-button>
            <el-button v-if="canOperate" type="success" class="full-btn" :loading="closing" @click="handleClose">
              关闭工单
            </el-button>
          </section>
        </aside>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import Layout from '@/components/Layout.vue'
import FeedbackAttachmentList from '@/components/FeedbackAttachmentList.vue'
import FeedbackAttachmentUploader from '@/components/FeedbackAttachmentUploader.vue'
import { closeMyFeedback, getFeedbackDetail, replyMyFeedback } from '@/api/feedback'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const replying = ref(false)
const closing = ref(false)
const detail = ref({})
const replyForm = reactive({ content: '', attachments: [] })

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
const roleMap = {
  VOLUNTEER: '我',
  ADMIN: '管理员',
  SYSTEM: '系统'
}

const canOperate = computed(() => ['OPEN', 'REPLIED'].includes(detail.value.status))
const statusDisplay = (status) => statusMap[status] || { text: status || '--', type: 'info' }
const categoryText = (value) => categoryMap[value] || value || '--'
const priorityText = (value) => priorityMap[value] || value || '--'
const roleText = (value) => roleMap[value] || value || '--'
const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '--'

const badgeClass = (status) => {
  if (status === 'REPLIED') return 'success'
  if (status === 'OPEN') return 'warn'
  if (status === 'REJECTED') return 'danger'
  return 'st-chip-light'
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await getFeedbackDetail(route.params.id)
    detail.value = res.data || {}
  } catch (error) {
    console.error('获取反馈详情失败:', error)
  } finally {
    loading.value = false
  }
}

const submitReply = async () => {
  if (!replyForm.content.trim() && replyForm.attachments.length === 0) {
    ElMessage.warning('请填写内容或上传附件')
    return
  }
  replying.value = true
  try {
    await replyMyFeedback(route.params.id, {
      content: replyForm.content,
      attachments: replyForm.attachments
    })
    replyForm.content = ''
    replyForm.attachments = []
    ElMessage.success('消息已发送')
    fetchDetail()
  } catch (error) {
    console.error('回复反馈失败:', error)
  } finally {
    replying.value = false
  }
}

const handleClose = () => {
  ElMessageBox.confirm('确认这条反馈已经解决并关闭吗？', '关闭反馈', {
    type: 'warning',
    confirmButtonText: '确认关闭',
    cancelButtonText: '取消'
  }).then(async () => {
    closing.value = true
    try {
      await closeMyFeedback(route.params.id)
      ElMessage.success('反馈已关闭')
      fetchDetail()
    } catch (error) {
      console.error('关闭反馈失败:', error)
    } finally {
      closing.value = false
    }
  }).catch(() => {})
}

onMounted(fetchDetail)
</script>

<style scoped>
.feedback-detail-page {
  max-width: 1180px;
}

.detail-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 16px;
  align-items: start;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--cv-primary);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.head-main h1 {
  margin: 8px 0 12px;
  font-size: clamp(30px, 4.6vw, 48px);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.head-date {
  color: #7a8198;
  font-size: 13px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 340px);
  gap: 16px;
}

.timeline-panel,
.side-panel {
  padding: 22px;
}

.timeline-head {
  margin-bottom: 18px;
}

.timeline-head h2 {
  font-size: 28px;
  margin-bottom: 6px;
}

.timeline-head p {
  margin: 0;
  color: #6f7b97;
}

.timeline-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 140px;
  margin-bottom: 18px;
}

.message-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.message-item.admin {
  grid-template-columns: minmax(0, 1fr) 44px;
}

.message-item.admin .message-avatar {
  order: 2;
}

.message-item.admin .message-bubble {
  order: 1;
  background: rgba(13, 71, 217, 0.08);
}

.message-item.system .message-bubble {
  background: rgba(120, 131, 157, 0.12);
}

.message-avatar {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: rgba(13, 71, 217, 0.12);
  color: var(--cv-primary);
  font-weight: 800;
}

.message-bubble {
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(201, 214, 243, 0.52);
  box-shadow: 0 12px 24px rgba(13, 71, 217, 0.06);
}

.message-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #6a738e;
  font-size: 13px;
}

.message-bubble p {
  margin: 12px 0 0;
  color: #243152;
  white-space: pre-wrap;
  line-height: 1.8;
}

.reply-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-radius: 22px;
}

.reply-card h3 {
  margin: 0;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
}

.st-section-head.compact {
  margin-bottom: 12px;
}

.detail-list {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 10px 12px;
  margin: 0;
}

.detail-list dt {
  color: #7b839a;
}

.detail-list dd {
  margin: 0;
  color: #243152;
  font-weight: 700;
  word-break: break-word;
}

.full-btn {
  width: 100%;
  height: 44px;
  margin-bottom: 10px;
  border-radius: 999px;
}

.full-btn + .full-btn {
  margin-left: 0;
}

@media (max-width: 900px) {
  .detail-head,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .timeline-panel,
  .side-panel {
    padding: 16px;
  }
}
</style>
