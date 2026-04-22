<template>
  <div v-if="attachments.length > 0" class="feedback-attachments">
    <div v-for="item in attachments" :key="item.id || item.attachmentKey" class="attachment-card">
      <button
        v-if="item.fileType === 'IMAGE'"
        class="thumb"
        type="button"
        @click="previewImage(item)"
      >
        <img v-if="imageUrls[item.attachmentKey]" :src="imageUrls[item.attachmentKey]" :alt="item.fileName">
        <span v-else>图片</span>
      </button>
      <div class="file-info">
        <strong>{{ item.fileName || '附件' }}</strong>
        <span>{{ item.fileType === 'IMAGE' ? '图片' : '文件' }} · {{ formatFileSize(item.fileSize) }}</span>
      </div>
      <el-button type="primary" link @click="handleDownload(item)">下载</el-button>
    </div>

    <el-image-viewer
      v-if="previewUrl"
      :url-list="[previewUrl]"
      @close="previewUrl = ''"
    />
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadFeedbackAttachment, getFeedbackAttachmentBlob } from '@/api/feedback'

const props = defineProps({
  attachments: {
    type: Array,
    default: () => []
  }
})

const imageUrls = ref({})
const previewUrl = ref('')

const formatFileSize = (size) => {
  const bytes = Number(size || 0)
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

const revokeImages = () => {
  Object.values(imageUrls.value).forEach(url => URL.revokeObjectURL(url))
  imageUrls.value = {}
}

const loadImages = async () => {
  revokeImages()
  const images = props.attachments.filter(item => item.fileType === 'IMAGE' && item.attachmentKey)
  for (const item of images) {
    try {
      const res = await getFeedbackAttachmentBlob(item.attachmentKey, item.fileName)
      imageUrls.value = {
        ...imageUrls.value,
        [item.attachmentKey]: URL.createObjectURL(res.data)
      }
    } catch (error) {
      console.error('加载反馈图片失败:', error)
    }
  }
}

const previewImage = (item) => {
  if (!imageUrls.value[item.attachmentKey]) {
    ElMessage.warning('图片仍在加载或无法预览')
    return
  }
  previewUrl.value = imageUrls.value[item.attachmentKey]
}

const handleDownload = async (item) => {
  try {
    await downloadFeedbackAttachment(item)
  } catch (error) {
    console.error('下载反馈附件失败:', error)
    ElMessage.error('附件下载失败')
  }
}

watch(() => props.attachments, loadImages, { deep: true, immediate: true })

onBeforeUnmount(() => {
  revokeImages()
})
</script>

<style scoped>
.feedback-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.attachment-card {
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 100%;
  padding: 8px 10px;
  border: 1px solid rgba(201, 214, 243, 0.8);
  border-radius: 8px;
  background: #fff;
}

.thumb {
  width: 54px;
  height: 54px;
  border: 1px solid #dbe5f8;
  border-radius: 6px;
  padding: 0;
  overflow: hidden;
  background: #eef4ff;
  color: #607091;
  cursor: pointer;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.file-info strong {
  max-width: 180px;
  color: #263250;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-info span {
  color: #7b8298;
  font-size: 12px;
}
</style>
