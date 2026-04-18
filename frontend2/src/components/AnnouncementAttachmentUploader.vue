<template>
  <div class="attachment-uploader">
    <div v-if="items.length > 0" class="attachment-list">
      <div v-for="(item, index) in items" :key="item.attachmentKey || index" class="attachment-item">
        <div>
          <p>{{ item.fileName || '附件' }}</p>
          <span>{{ item.contentType || '文件' }} · {{ formatFileSize(item.fileSize) }}</span>
        </div>
        <div class="attachment-actions">
          <a v-if="item.url" :href="item.url" target="_blank" rel="noopener">下载</a>
          <el-button type="danger" link :disabled="disabled || uploading" @click="removeAttachment(index)">删除</el-button>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无附件" />

    <div class="toolbar">
      <el-upload
        :show-file-list="false"
        :http-request="handleUpload"
        :disabled="disabled || uploading || items.length >= limit"
        :multiple="true"
        :limit="limit"
        accept=".pdf,.xls,.xlsx,.doc,.docx,.txt,.csv"
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
      <el-button v-if="items.length > 0" :disabled="disabled || uploading" @click="clearAttachments">
        清空附件
      </el-button>
    </div>

    <p class="tip">支持 PDF、Excel、Word、TXT、CSV，请上传体积适中的附件文件。</p>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadAnnouncementAttachment } from '@/api/announcement'
import { buildAnnouncementAttachmentUrl } from '@/utils/announcementAttachment'

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
    default: 8
  }
})

const emit = defineEmits(['update:attachments'])
const uploading = ref(false)

const items = computed(() => {
  const attachments = Array.isArray(props.attachments) ? props.attachments : []
  return attachments
    .map((item) => ({
      attachmentKey: item.attachmentKey || '',
      fileName: item.fileName || '',
      contentType: item.contentType || '',
      fileSize: item.fileSize || 0,
      url: item.url || buildAnnouncementAttachmentUrl(item.attachmentKey, item.fileName)
    }))
    .filter(item => item.attachmentKey)
})

const syncAttachments = (nextItems) => {
  emit('update:attachments', nextItems.map(item => ({
    attachmentKey: item.attachmentKey,
    fileName: item.fileName,
    contentType: item.contentType,
    fileSize: item.fileSize,
    url: item.url
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
    const res = await uploadAnnouncementAttachment(file)
    syncAttachments([
      ...items.value,
      res.data
    ])
    ElMessage.success('附件上传成功')
  } catch (error) {
    console.error('上传公告附件失败:', error)
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

const clearAttachments = () => {
  syncAttachments([])
}
</script>

<style scoped>
.attachment-uploader {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attachment-item {
  border: 1px solid rgba(74, 90, 139, 0.14);
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  background: #f8fbff;
}

.attachment-item p {
  margin: 0 0 4px;
  font-weight: 700;
  color: #2d3654;
}

.attachment-item span {
  color: #7b8298;
  font-size: 12px;
}

.attachment-actions,
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.attachment-actions a {
  color: var(--el-color-primary);
  text-decoration: none;
  font-size: 14px;
}

.tip {
  margin: 0;
  color: #7b8298;
  font-size: 12px;
}

@media (max-width: 640px) {
  .attachment-item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
