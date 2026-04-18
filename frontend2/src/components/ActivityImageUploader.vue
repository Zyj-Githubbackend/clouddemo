<template>
  <div class="image-uploader">
    <div class="preview-grid" :class="{ empty: previewItems.length === 0 }">
      <template v-if="previewItems.length > 0">
        <div v-for="(item, index) in previewItems" :key="item.imageKey || item.imageUrl || index" class="preview-card">
          <img :src="item.imageUrl" alt="活动图片预览" class="preview-image">
          <button
            type="button"
            class="remove-btn"
            :disabled="disabled || uploading"
            @click="removeImage(index)"
          >
            删除
          </button>
        </div>
      </template>
      <div v-else class="preview-placeholder">
        <span>活动图集</span>
        <small>建议上传横版活动图片，可多选</small>
      </div>
    </div>

    <div class="toolbar">
      <el-upload
        :show-file-list="false"
        :http-request="handleUpload"
        :disabled="disabled || uploading || previewItems.length >= limit"
        :multiple="true"
        :limit="limit"
        accept="image/png,image/jpeg,image/gif,image/webp"
        @exceed="handleExceed"
      >
        <el-button
          type="primary"
          plain
          :loading="uploading"
          :disabled="disabled || uploading || previewItems.length >= limit"
        >
          {{ previewItems.length > 0 ? '继续上传图片' : '上传活动图片' }}
        </el-button>
      </el-upload>
      <el-button
        v-if="previewItems.length > 0"
        :disabled="disabled || uploading"
        @click="clearImages"
      >
        清空图片
      </el-button>
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
  imageKeys: {
    type: Array,
    default: () => []
  },
  imageUrls: {
    type: Array,
    default: () => []
  },
  tip: {
    type: String,
    default: '支持 JPG、PNG、GIF、WEBP，单张不超过 5MB，最多上传 6 张。'
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

const emit = defineEmits(['update:imageKeys', 'update:imageUrls'])
const uploading = ref(false)

const previewItems = computed(() => {
  const keys = Array.isArray(props.imageKeys) ? props.imageKeys : []
  const urls = Array.isArray(props.imageUrls) ? props.imageUrls : []
  const maxLength = Math.max(keys.length, urls.length)
  return Array.from({ length: maxLength }, (_, index) => ({
    imageKey: keys[index] || '',
    imageUrl: urls[index] || buildActivityImageUrl(keys[index])
  })).filter(item => item.imageKey || item.imageUrl)
})

const syncImages = (items) => {
  emit('update:imageKeys', items.map(item => item.imageKey).filter(Boolean))
  emit('update:imageUrls', items.map(item => item.imageUrl).filter(Boolean))
}

const handleUpload = async ({ file }) => {
  if (previewItems.value.length >= props.limit) {
    ElMessage.warning(`最多上传 ${props.limit} 张图片`)
    return
  }

  uploading.value = true
  try {
    const res = await uploadActivityImage(file)
    syncImages([
      ...previewItems.value,
      {
        imageKey: res.data.imageKey,
        imageUrl: res.data.imageUrl
      }
    ])
    ElMessage.success('活动图片上传成功')
  } catch (error) {
    console.error('上传活动图片失败:', error)
  } finally {
    uploading.value = false
  }
}

const handleExceed = () => {
  ElMessage.warning(`最多上传 ${props.limit} 张图片`)
}

const removeImage = (index) => {
  const items = [...previewItems.value]
  items.splice(index, 1)
  syncImages(items)
}

const clearImages = () => {
  syncImages([])
}
</script>

<style scoped>
.image-uploader {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  min-height: 220px;
}

.preview-grid.empty {
  border-radius: 18px;
  background: linear-gradient(145deg, #e7eefc, #f6f2e8);
  border: 1px solid rgba(74, 90, 139, 0.12);
}

.preview-card {
  position: relative;
  overflow: hidden;
  min-height: 180px;
  border-radius: 18px;
  background: linear-gradient(145deg, #e7eefc, #f6f2e8);
  border: 1px solid rgba(74, 90, 139, 0.12);
}

.preview-image {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

.remove-btn {
  position: absolute;
  right: 10px;
  bottom: 10px;
  border: none;
  border-radius: 999px;
  padding: 6px 10px;
  color: #fff;
  background: rgba(24, 31, 55, 0.72);
  cursor: pointer;
}

.remove-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
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
