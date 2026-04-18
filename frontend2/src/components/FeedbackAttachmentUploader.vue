<template>
  <div class="feedback-uploader">
    <div v-if="items.length > 0" class="attachment-list">
      <div v-for="(item, index) in items" :key="item.attachmentKey || index" class="attachment-item">
        <div class="file-main">
          <el-tag :type="item.fileType === 'IMAGE' ? 'success' : 'info'" size="small">
            {{ item.fileType === 'IMAGE' ? '图片' : '文件' }}
          </el-tag>
          <div>
            <p>{{ item.fileName || '附件' }}</p>
            <span>{{ formatFileSize(item.fileSize) }} · {{ item.contentType || '未知类型' }}</span>
          </div>
        </div>
        <el-button type="danger" link :disabled="disabled || uploading" @click="removeAttachment(index)">
          删除
        </el-button>
      </div>
    </div>

    <el-upload
      :show-file-list="false"
      :http-request="handleUpload"
      :disabled="disabled || uploading || items.length >= limit"
      :multiple="true"
      :limit="limit"
      accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,.xls,.xlsx,.doc,.docx,.txt,.csv"
      @exceed="handleExceed"
    >
      <el-button
        type="primary"
        plain
        :loading="uploading"
        :disabled="disabled || uploading || items.length >= limit"
      >
        {{ items.length > 0 ? '继续上传附件' : '上传附件' }}
      </el-button>
    </el-upload>
    <p class="tip">每条消息最多 6 个附件，支持图片、PDF、Excel、Word、TXT、CSV。</p>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFeedbackAttachment } from '@/api/feedback'

const props = defineProps({
  attachments: {
    type: Array,
    default: () => []
  },
  disabled: {
    type: Boolean,
    default: false
  },
  limit: {
    type: Number,
    default: 6
  }
})

const emit = defineEmits(['update:attachments'])
const uploading = ref(false)

const items = computed(() => Array.isArray(props.attachments) ? props.attachments : [])

const syncAttachments = (nextItems) => {
  emit('update:attachments', nextItems.map(item => ({
    attachmentKey: item.attachmentKey,
    fileName: item.fileName,
    contentType: item.contentType,
    fileSize: item.fileSize,
    fileType: item.fileType
  })))
}

const formatFileSize = (size) => {
  const bytes = Number(size || 0)
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

const handleUpload = async ({ file }) => {
  if (items.value.length >= props.limit) {
    ElMessage.warning(`最多上传 ${props.limit} 个附件`)
    return
  }

  uploading.value = true
  try {
    const res = await uploadFeedbackAttachment(file)
    syncAttachments([...items.value, res.data])
    ElMessage.success('附件上传成功')
  } catch (error) {
    console.error('上传反馈附件失败:', error)
  } finally {
    uploading.value = false
  }
}

const handleExceed = () => {
  ElMessage.warning(`最多上传 ${props.limit} 个附件`)
}

const removeAttachment = (index) => {
  const nextItems = [...items.value]
  nextItems.splice(index, 1)
  syncAttachments(nextItems)
}
</script>

<style scoped>
.feedback-uploader {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid rgba(201, 214, 243, 0.8);
  border-radius: 8px;
  background: #f8fbff;
}

.file-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.file-main p {
  margin: 0 0 3px;
  color: #243152;
  font-weight: 700;
  word-break: break-all;
}

.file-main span,
.tip {
  color: #7b8298;
  font-size: 12px;
}

.tip {
  margin: 0;
}

@media (max-width: 560px) {
  .attachment-item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
