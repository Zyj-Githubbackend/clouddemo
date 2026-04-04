<template>
  <div class="image-uploader">
    <div class="preview-card" :class="{ empty: !previewUrl }">
      <img v-if="previewUrl" :src="previewUrl" alt="活动图片预览" class="preview-image">
      <div v-else class="preview-placeholder">
        <span>Activity Cover</span>
        <small>16:9 cover suggested</small>
      </div>
    </div>

    <div class="toolbar">
      <el-upload
        :show-file-list="false"
        :http-request="handleUpload"
        :disabled="disabled || uploading"
        accept="image/png,image/jpeg,image/gif,image/webp"
      >
        <el-button type="primary" plain :loading="uploading" :disabled="disabled || uploading">
          {{ previewUrl ? '重新上传' : '上传图片' }}
        </el-button>
      </el-upload>
      <el-button v-if="previewUrl" :disabled="disabled || uploading" @click="clearImage">移除图片</el-button>
    </div>

    <p class="tip">{{ tip }}</p>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadActivityImage } from '@/api/activity'
import { buildActivityImageUrl } from '@/utils/activityImage'

const props = defineProps({
  imageKey: {
    type: String,
    default: ''
  },
  imageUrl: {
    type: String,
    default: ''
  },
  tip: {
    type: String,
    default: '支持 JPG、PNG、GIF、WEBP，建议上传横版封面图，大小不超过 5MB。'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:imageKey', 'update:imageUrl'])

const uploading = ref(false)

const previewUrl = computed(() => props.imageUrl || buildActivityImageUrl(props.imageKey))

const handleUpload = async ({ file }) => {
  uploading.value = true
  try {
    const res = await uploadActivityImage(file)
    emit('update:imageKey', res.data.imageKey)
    emit('update:imageUrl', res.data.imageUrl)
    ElMessage.success('活动图片上传成功')
  } catch (error) {
    console.error('上传活动图片失败:', error)
  } finally {
    uploading.value = false
  }
}

const clearImage = () => {
  emit('update:imageKey', '')
  emit('update:imageUrl', '')
}
</script>

<style scoped>
.image-uploader {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-card {
  position: relative;
  overflow: hidden;
  min-height: 220px;
  border-radius: 18px;
  background: linear-gradient(145deg, #e7eefc, #f6f2e8);
  border: 1px solid rgba(74, 90, 139, 0.12);
}

.preview-card.empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image {
  width: 100%;
  height: 220px;
  object-fit: cover;
  display: block;
}

.preview-placeholder {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #59627e;
  letter-spacing: 0.4px;
}

.preview-placeholder span {
  font-size: 18px;
  font-weight: 700;
}

.preview-placeholder small {
  margin-top: 6px;
  font-size: 12px;
}

.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.tip {
  margin: 0;
  color: #7b8298;
  font-size: 12px;
}
</style>
