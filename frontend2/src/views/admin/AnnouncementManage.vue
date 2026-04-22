<template>
  <div class="st-page admin-page">
    <section class="st-hero page-hero">
      <div class="st-hero-content">
        <div class="st-hero-copy">
          <p class="st-hero-eyebrow">公告运营</p>
          <h1 class="st-hero-title">公告管理</h1>
          <p class="st-hero-desc">
            支持发布首页公告、关联一个或多个活动、维护图片与附件，并切换发布/下线状态。
          </p>
        </div>
        <div class="st-hero-actions">
          <span class="st-chip">总数 {{ pagination.total }} 条</span>
          <el-button class="hero-btn" type="primary" @click="openCreateDialog">发布公告</el-button>
        </div>
      </div>
    </section>

    <section class="st-stat-grid">
      <article class="st-stat-card">
        <span class="label">已发布</span>
        <strong class="value">{{ publishedCount }}</strong>
        <span class="hint">对外展示中</span>
      </article>
      <article class="st-stat-card">
        <span class="label">已下线</span>
        <strong class="value">{{ offlineCount }}</strong>
        <span class="hint">已停止展示</span>
      </article>
      <article class="st-stat-card">
        <span class="label">有附件</span>
        <strong class="value">{{ attachmentCount }}</strong>
        <span class="hint">含下载资料</span>
      </article>
    </section>

    <section class="st-panel table-panel">
      <div class="st-section-head">
        <div>
          <h2>公告列表</h2>
          <p>可按公告状态筛选，并继续处理发布、下线和编辑操作。</p>
        </div>
      </div>

      <div class="toolbar-row">
        <el-select v-model="statusFilter" placeholder="公告状态" clearable @change="handleFilterChange">
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已下线" value="OFFLINE" />
        </el-select>
      </div>

      <div class="table-scroll">
        <el-table :data="announcements" v-loading="loading" stripe>
          <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
                {{ row.status === 'PUBLISHED' ? '已发布' : '已下线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="关联活动" min-width="220">
            <template #default="{ row }">
              <div v-if="getLinkedActivities(row).length > 0" class="activity-links">
                <el-button
                  v-for="activity in getLinkedActivities(row)"
                  :key="activity.id"
                  type="primary"
                  link
                  @click="router.push(`/activity/${activity.id}`)"
                >
                  {{ activity.title || `活动 #${activity.id}` }}
                </el-button>
              </div>
              <span v-else class="muted">未关联</span>
            </template>
          </el-table-column>
          <el-table-column label="附件" width="90">
            <template #default="{ row }">
              <el-tag v-if="Array.isArray(row.attachments) && row.attachments.length > 0" type="warning">
                {{ row.attachments.length }} 个
              </el-tag>
              <span v-else class="muted">无</span>
            </template>
          </el-table-column>
          <el-table-column label="排序" prop="sortOrder" width="80" />
          <el-table-column label="发布时间" width="170">
            <template #default="{ row }">{{ formatDate(row.publishTime || row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="router.push(`/announcement/${row.id}`)">查看</el-button>
              <el-button type="warning" size="small" link @click="openEditDialog(row)">编辑</el-button>
              <el-button
                v-if="row.status !== 'PUBLISHED'"
                type="success"
                size="small"
                link
                @click="handlePublish(row)"
              >
                发布
              </el-button>
              <el-button
                v-else
                type="info"
                size="small"
                link
                @click="handleOffline(row)"
              >
                下线
              </el-button>
              <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
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
          @current-change="fetchAnnouncements"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑公告' : '发布公告'"
      width="min(760px, 96vw)"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" clearable />
        </el-form-item>
        <el-form-item label="关联活动">
          <el-select
            v-model="form.activityIds"
            placeholder="可选，选择后公告详情会展示活动列表"
            clearable
            filterable
            multiple
            collapse-tags
            collapse-tags-tooltip
            style="width: 100%"
          >
            <el-option
              v-for="activity in activityOptions"
              :key="activity.id"
              :label="activity.title"
              :value="activity.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button label="PUBLISHED">发布</el-radio-button>
            <el-radio-button label="OFFLINE">下线</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
          <span class="tip">数值越大越靠前</span>
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="7" placeholder="请输入公告正文" />
        </el-form-item>
        <el-form-item label="公告图片">
          <AnnouncementImageUploader
            :image-keys="form.imageKeys"
            :image-urls="form.imageUrls"
            @update:image-keys="(value) => { form.imageKeys = value }"
            @update:image-urls="(value) => { form.imageUrls = value }"
          />
        </el-form-item>
        <el-form-item label="公告附件">
          <AnnouncementAttachmentUploader
            :attachments="form.attachments"
            @update:attachments="(value) => { form.attachments = value }"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  createAnnouncement,
  deleteAnnouncement,
  getAdminAnnouncementDetail,
  getAdminAnnouncementList,
  offlineAnnouncement,
  publishAnnouncement,
  updateAnnouncement
} from '@/api/announcement'
import { getActivityList } from '@/api/activity'
import AnnouncementAttachmentUploader from '@/components/AnnouncementAttachmentUploader.vue'
import AnnouncementImageUploader from '@/components/AnnouncementImageUploader.vue'

const router = useRouter()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const announcements = ref([])
const activityOptions = ref([])
const statusFilter = ref('')

const publishedCount = computed(() => announcements.value.filter((item) => item.status === 'PUBLISHED').length)
const offlineCount = computed(() => announcements.value.filter((item) => item.status === 'OFFLINE').length)
const attachmentCount = computed(() =>
  announcements.value.filter((item) => Array.isArray(item.attachments) && item.attachments.length > 0).length
)

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  title: '',
  content: '',
  imageKeys: [],
  imageUrls: [],
  activityIds: [],
  attachments: [],
  status: 'PUBLISHED',
  sortOrder: 0
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  status: [{ required: true, message: '请选择公告状态', trigger: 'change' }]
}

const formatDate = (date) => {
  if (!date) return '--'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const buildPayload = () => ({
  title: form.title,
  content: form.content,
  imageKeys: form.imageKeys,
  activityId: form.activityIds[0] || null,
  activityIds: form.activityIds,
  attachments: form.attachments.map(({ attachmentKey, fileName, contentType, fileSize }) => ({
    attachmentKey,
    fileName,
    contentType,
    fileSize
  })),
  status: form.status,
  sortOrder: form.sortOrder
})

const resetForm = () => {
  editingId.value = null
  Object.assign(form, {
    title: '',
    content: '',
    imageKeys: [],
    imageUrls: [],
    activityIds: [],
    attachments: [],
    status: 'PUBLISHED',
    sortOrder: 0
  })
}

const getLinkedActivities = (row) => {
  if (Array.isArray(row.activities) && row.activities.length > 0) {
    return row.activities
  }
  if (row.activityId) {
    return [{ id: row.activityId, title: `活动 #${row.activityId}` }]
  }
  return []
}

const getLinkedActivityIds = (data) => {
  if (Array.isArray(data.activityIds) && data.activityIds.length > 0) {
    return data.activityIds
  }
  if (Array.isArray(data.activities) && data.activities.length > 0) {
    return data.activities.map(activity => activity.id).filter(Boolean)
  }
  return data.activityId ? [data.activityId] : []
}

const fetchAnnouncements = async () => {
  loading.value = true
  try {
    const res = await getAdminAnnouncementList({
      page: pagination.page,
      size: pagination.size,
      status: statusFilter.value
    })
    announcements.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('获取公告列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchActivityOptions = async () => {
  try {
    const res = await getActivityList({ page: 1, size: 100 })
    activityOptions.value = res.data.records || []
  } catch (error) {
    console.error('获取活动选项失败:', error)
  }
}

const openCreateDialog = async () => {
  resetForm()
  dialogVisible.value = true
  await fetchActivityOptions()
  await nextTick()
  formRef.value?.clearValidate()
}

const openEditDialog = async (row) => {
  editingId.value = row.id
  dialogVisible.value = true
  await fetchActivityOptions()
  try {
    const res = await getAdminAnnouncementDetail(row.id)
    const data = res.data || {}
    Object.assign(form, {
      title: data.title || '',
      content: data.content || '',
      imageKeys: data.imageKeys || (data.imageKey ? [data.imageKey] : []),
      imageUrls: data.imageUrls || (data.imageUrl ? [data.imageUrl] : []),
      activityIds: getLinkedActivityIds(data),
      attachments: data.attachments || [],
      status: data.status || 'PUBLISHED',
      sortOrder: data.sortOrder ?? 0
    })
    await nextTick()
    formRef.value?.clearValidate()
  } catch (error) {
    console.error('获取公告详情失败:', error)
    dialogVisible.value = false
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (editingId.value) {
      await updateAnnouncement(editingId.value, buildPayload())
      ElMessage.success('公告已保存')
    } else {
      await createAnnouncement(buildPayload())
      ElMessage.success('公告已发布')
    }
    dialogVisible.value = false
    fetchAnnouncements()
  } catch (error) {
    console.error('保存公告失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const handlePublish = async (row) => {
  await publishAnnouncement(row.id)
  ElMessage.success('公告已发布')
  fetchAnnouncements()
}

const handleOffline = async (row) => {
  await offlineAnnouncement(row.id)
  ElMessage.success('公告已下线')
  fetchAnnouncements()
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除公告“${row.title}”吗？`, '删除公告', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(async () => {
    await deleteAnnouncement(row.id)
    ElMessage.success('公告已删除')
    fetchAnnouncements()
  }).catch(() => {})
}

const handleFilterChange = () => {
  pagination.page = 1
  fetchAnnouncements()
}

const handleSizeChange = () => {
  pagination.page = 1
  fetchAnnouncements()
}

onMounted(() => {
  fetchAnnouncements()
})
</script>

<style scoped>
.page-hero {
  min-height: 220px;
}

.hero-btn {
  height: 46px;
  border-radius: 999px;
}

.table-panel {
  padding: 22px;
}

.toolbar-row {
  margin-bottom: 14px;
  display: flex;
  justify-content: flex-end;
}

.activity-links {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.muted,
.tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.tip {
  margin-left: 10px;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .table-panel {
    padding: 16px;
  }

  .toolbar-row,
  .pagination-bar {
    justify-content: flex-start;
  }
}
</style>
