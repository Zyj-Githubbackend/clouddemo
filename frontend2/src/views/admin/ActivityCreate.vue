<template>
  <div class="st-page admin-page create-page">
    <section class="st-hero create-hero">
      <div class="st-hero-content">
        <div class="st-hero-copy">
          <p class="st-hero-eyebrow">发布活动</p>
          <h1 class="st-hero-title">发布新的志愿活动</h1>
          <p class="st-hero-desc">
            完成活动信息填写后即可发布，便于志愿者查看详情并参与报名。
          </p>
        </div>
      </div>
    </section>

    <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="create-form">
      <div class="create-grid">
        <div class="main-stack">
          <section class="st-panel form-section">
            <div class="st-section-head compact">
              <div>
                <h2>基本信息</h2>
                <p>填写活动名称、类型、地点、人数和时长，方便志愿者了解并报名。</p>
              </div>
            </div>

            <el-form-item label="活动标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入活动标题" size="large" clearable />
            </el-form-item>

            <div class="two-col">
              <el-form-item label="活动类型" prop="category">
                <el-select v-model="form.category" placeholder="请选择活动类型" size="large" style="width:100%">
                  <el-option label="校园服务" value="校园服务" />
                  <el-option label="公益助学" value="公益助学" />
                  <el-option label="社区关爱" value="社区关爱" />
                  <el-option label="大型活动" value="大型活动" />
                  <el-option label="环保公益" value="环保公益" />
                  <el-option label="应急救援" value="应急救援" />
                </el-select>
              </el-form-item>

              <el-form-item label="服务地点" prop="location">
                <el-input v-model="form.location" placeholder="请输入服务地点" size="large" clearable>
                  <template #prefix><el-icon><Location /></el-icon></template>
                </el-input>
              </el-form-item>
            </div>

            <div class="two-col">
              <el-form-item label="招募人数" prop="maxParticipants">
                <el-input-number
                  v-model="form.maxParticipants"
                  :min="1"
                  :max="500"
                  size="large"
                  style="width:100%"
                  controls-position="right"
                />
              </el-form-item>

              <el-form-item label="志愿时长（小时）" prop="volunteerHours">
                <el-input-number
                  v-model="form.volunteerHours"
                  :min="0.5"
                  :max="24"
                  :step="0.5"
                  size="large"
                  style="width:100%"
                  controls-position="right"
                />
              </el-form-item>
            </div>
          </section>

          <section class="st-panel form-section">
            <div class="st-section-head compact">
              <div>
                <h2>活动介绍</h2>
                <p>补充活动内容、服务要求和参与注意事项。</p>
              </div>
              <div class="ai-bar">
                <el-input
                  v-model="aiKeywords"
                  placeholder="输入关键词辅助 AI 生成，例如：图书馆、值班"
                  size="small"
                  class="ai-input"
                  clearable
                />
                <el-button type="primary" size="small" :loading="aiLoading" @click="handleAIGenerate">
                  <el-icon><MagicStick /></el-icon>AI生成
                </el-button>
              </div>
            </div>

            <el-form-item prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="10"
                placeholder="描述活动内容、志愿者要求、注意事项等"
                resize="vertical"
              />
            </el-form-item>
          </section>
        </div>

        <div class="side-stack">
          <section class="st-panel form-section">
            <div class="st-section-head compact">
              <div>
                <h2>活动图片</h2>
                <p>上传活动图片，提升页面展示效果。</p>
              </div>
            </div>
            <ActivityImageUploader
              :image-keys="form.imageKeys"
              :image-urls="form.imageUrls"
              @update:image-keys="(value) => { form.imageKeys = value }"
              @update:image-urls="(value) => { form.imageUrls = value }"
            />
          </section>

          <section class="st-panel form-section">
            <div class="st-section-head compact">
              <div>
                <h2>时间安排</h2>
                <p>请按实际安排填写招募时间与活动时间。</p>
              </div>
            </div>

            <div class="time-group">
              <div class="time-title">活动时间</div>
              <el-form-item label="开始时间" prop="startTime">
                <el-date-picker
                  v-model="form.startTime"
                  type="datetime"
                  placeholder="活动开始"
                  format="YYYY-MM-DD HH:mm"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width:100%"
                />
              </el-form-item>
              <el-form-item label="结束时间" prop="endTime">
                <el-date-picker
                  v-model="form.endTime"
                  type="datetime"
                  placeholder="活动结束"
                  format="YYYY-MM-DD HH:mm"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width:100%"
                />
              </el-form-item>
            </div>

            <el-divider />

            <div class="time-group">
              <div class="time-title">招募窗口</div>
              <el-form-item label="招募开始" prop="registrationStartTime">
                <el-date-picker
                  v-model="form.registrationStartTime"
                  type="datetime"
                  placeholder="招募开始时间"
                  format="YYYY-MM-DD HH:mm"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width:100%"
                />
              </el-form-item>
              <el-form-item label="报名截止" prop="registrationDeadline">
                <el-date-picker
                  v-model="form.registrationDeadline"
                  type="datetime"
                  placeholder="报名截止时间"
                  format="YYYY-MM-DD HH:mm"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width:100%"
                />
              </el-form-item>
            </div>

            <div class="time-tip">招募开始 &lt; 报名截止 ≤ 活动开始 &lt; 活动结束</div>
          </section>

          <div class="action-bar">
            <el-button size="large" @click="handleReset" style="flex:1">重置</el-button>
            <el-button type="primary" size="large" :loading="loading" @click="handleSubmit" style="flex:2">
              发布活动
            </el-button>
          </div>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createActivity, generateDescription } from '@/api/activity'
import ActivityImageUploader from '@/components/ActivityImageUploader.vue'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const aiLoading = ref(false)
const aiKeywords = ref('')

const initialFormState = () => ({
  title: '',
  category: '',
  location: '',
  description: '',
  imageKeys: [],
  imageUrls: [],
  maxParticipants: 20,
  volunteerHours: 2,
  startTime: '',
  endTime: '',
  registrationStartTime: '',
  registrationDeadline: ''
})

const form = reactive(initialFormState())

const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  location: [{ required: true, message: '请输入服务地点', trigger: 'blur' }],
  description: [{ required: true, message: '请输入活动详情', trigger: 'blur' }],
  maxParticipants: [{ required: true, trigger: 'blur' }],
  volunteerHours: [{ required: true, trigger: 'blur' }],
  startTime: [
    { required: true, message: '请选择活动开始时间', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !form.endTime) {
          callback()
          return
        }
        if (new Date(value) >= new Date(form.endTime)) {
          callback(new Error('开始时间需早于结束时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  endTime: [
    { required: true, message: '请选择活动结束时间', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !form.startTime) {
          callback()
          return
        }
        if (new Date(value) <= new Date(form.startTime)) {
          callback(new Error('结束时间需晚于开始时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  registrationStartTime: [
    { required: true, message: '请选择招募开始时间', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !form.registrationDeadline) {
          callback()
          return
        }
        if (new Date(form.registrationDeadline) <= new Date(value)) {
          callback(new Error('招募开始需早于截止时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  registrationDeadline: [
    { required: true, message: '请选择报名截止时间', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !form.registrationStartTime) {
          callback()
          return
        }
        if (new Date(value) <= new Date(form.registrationStartTime)) {
          callback(new Error('截止需晚于招募开始'))
          return
        }
        if (form.startTime && new Date(value) > new Date(form.startTime)) {
          callback(new Error('报名截止不能晚于活动开始'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

const handleAIGenerate = async () => {
  if (!form.location || !form.category) {
    ElMessage.warning('请先填写活动类型和服务地点')
    return
  }
  aiLoading.value = true
  try {
    const res = await generateDescription({
      location: form.location,
      category: form.category,
      keywords: aiKeywords.value,
      volunteerHours: form.volunteerHours
    })
    form.description = res.data
    ElMessage.success('AI 生成成功')
  } catch (error) {
    console.error(error)
  } finally {
    aiLoading.value = false
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await createActivity({
      title: form.title,
      category: form.category,
      location: form.location,
      description: form.description,
      imageKeys: form.imageKeys,
      maxParticipants: form.maxParticipants,
      volunteerHours: form.volunteerHours,
      startTime: form.startTime,
      endTime: form.endTime,
      registrationStartTime: form.registrationStartTime,
      registrationDeadline: form.registrationDeadline
    })
    ElMessage.success('活动发布成功')
    router.push('/admin/activities')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  formRef.value.resetFields()
  Object.assign(form, initialFormState())
  aiKeywords.value = ''
}
</script>

<style scoped>
.create-hero {
  min-height: 220px;
}

.create-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 420px);
  gap: 16px;
}

.main-stack,
.side-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-section {
  padding: 22px;
}

.st-section-head.compact {
  margin-bottom: 12px;
}

.two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
}

.ai-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-input {
  width: min(320px, 100%);
}

.time-title {
  margin-bottom: 10px;
  font-size: 12px;
  color: #7f859c;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  font-weight: 700;
}

.time-tip {
  border-radius: 12px;
  padding: 8px 10px;
  font-size: 12px;
  color: #767c93;
  background: #f3f2ff;
}

.action-bar {
  display: flex;
  gap: 10px;
}

@media (max-width: 980px) {
  .create-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .form-section {
    padding: 16px;
  }

  .two-col {
    grid-template-columns: 1fr;
  }

  .action-bar {
    flex-direction: column;
  }
}
</style>
