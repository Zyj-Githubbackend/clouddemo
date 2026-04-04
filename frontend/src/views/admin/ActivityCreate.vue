<template>
  <div class="create-page page-container">
    <section class="hero">
      <p class="mini">Create Activity</p>
      <h1>发布新的志愿活动</h1>
      <p>填写完整信息后发布，前台用户即可查看并报名。</p>
    </section>

    <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="create-form">
      <el-row :gutter="16">
        <el-col :xs="24" :md="15">
          <el-card class="section" shadow="never">
            <template #header><h3>基本信息</h3></template>

            <el-form-item label="活动标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入活动标题" size="large" clearable />
            </el-form-item>

            <el-row :gutter="12">
              <el-col :xs="24" :sm="12">
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
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="服务地点" prop="location">
                  <el-input v-model="form.location" placeholder="请输入服务地点" size="large" clearable>
                    <template #prefix><el-icon><Location /></el-icon></template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="12">
              <el-col :xs="24" :sm="12">
                <el-form-item label="招募人数" prop="maxParticipants">
                  <el-input-number
                    v-model="form.maxParticipants"
                    :min="1" :max="500"
                    size="large"
                    style="width:100%"
                    controls-position="right"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="志愿时长（小时）" prop="volunteerHours">
                  <el-input-number
                    v-model="form.volunteerHours"
                    :min="0.5" :max="24" :step="0.5"
                    size="large"
                    style="width:100%"
                    controls-position="right"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-card>

          <el-card class="section" shadow="never">
            <template #header>
              <div class="section-head">
                <h3>活动介绍</h3>
                <div class="ai-bar">
                  <el-input
                    v-model="aiKeywords"
                    placeholder="输入关键词辅助AI生成，如：图书馆、值班"
                    size="small"
                    class="ai-input"
                    clearable
                  />
                  <el-button type="primary" size="small" :loading="aiLoading" @click="handleAIGenerate">
                    <el-icon><MagicStick /></el-icon>AI生成
                  </el-button>
                </div>
              </div>
            </template>
            <el-form-item prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="10"
                placeholder="描述活动内容、志愿者要求、注意事项等"
                resize="vertical"
              />
            </el-form-item>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="9">
          <el-card class="section" shadow="never">
            <template #header><h3>活动图片</h3></template>
            <ActivityImageUploader
              :image-key="form.imageKey"
              :image-url="imagePreviewUrl"
              @update:image-key="(value) => { form.imageKey = value }"
              @update:image-url="setImagePreviewUrl"
            />
          </el-card>

          <el-card class="section" shadow="never">
            <template #header><h3>时间安排</h3></template>

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
          </el-card>

          <div class="action-bar">
            <el-button size="large" @click="handleReset" style="flex:1">重置</el-button>
            <el-button type="primary" size="large" :loading="loading" @click="handleSubmit" style="flex:2">
              发布活动
            </el-button>
          </div>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createActivity, generateDescription } from '@/api/activity'
import ActivityImageUploader from '@/components/ActivityImageUploader.vue'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const aiLoading = ref(false)
const aiKeywords = ref('')
const imagePreviewUrl = ref('')

const setImagePreviewUrl = (value) => {
  imagePreviewUrl.value = value
}

const form = reactive({
  title: '',
  category: '',
  location: '',
  description: '',
  imageKey: '',
  maxParticipants: 20,
  volunteerHours: 2,
  startTime: '',
  endTime: '',
  registrationStartTime: '',
  registrationDeadline: ''
})

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
      validator: (_r, v, cb) => {
        if (!v || !form.endTime) { cb(); return }
        if (new Date(v) >= new Date(form.endTime)) { cb(new Error('开始时间须早于结束时间')); return }
        cb()
      },
      trigger: 'change'
    }
  ],
  endTime: [
    { required: true, message: '请选择活动结束时间', trigger: 'change' },
    {
      validator: (_r, v, cb) => {
        if (!v || !form.startTime) { cb(); return }
        if (new Date(v) <= new Date(form.startTime)) { cb(new Error('结束时间须晚于开始时间')); return }
        cb()
      },
      trigger: 'change'
    }
  ],
  registrationStartTime: [
    { required: true, message: '请选择招募开始时间', trigger: 'change' },
    {
      validator: (_r, v, cb) => {
        if (!v || !form.registrationDeadline) { cb(); return }
        if (new Date(form.registrationDeadline) <= new Date(v)) { cb(new Error('招募开始须早于截止时间')); return }
        cb()
      },
      trigger: 'change'
    }
  ],
  registrationDeadline: [
    { required: true, message: '请选择报名截止时间', trigger: 'change' },
    {
      validator: (_r, v, cb) => {
        if (!v || !form.registrationStartTime) { cb(); return }
        if (new Date(v) <= new Date(form.registrationStartTime)) { cb(new Error('截止须晚于招募开始')); return }
        if (form.startTime && new Date(v) > new Date(form.startTime)) { cb(new Error('报名截止不能晚于活动开始')); return }
        cb()
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
    await createActivity(form)
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
  aiKeywords.value = ''
  imagePreviewUrl.value = ''
}
</script>

<style scoped>
.create-page {
  width: 100%;
  margin: 0 auto;
}

.hero {
  margin-bottom: 16px;
  border-radius: 22px;
  padding: 24px 22px;
  color: #fff;
  background: linear-gradient(130deg, var(--cv-primary), var(--cv-primary-weak));
}

.hero .mini {
  font-size: 12px;
  letter-spacing: 1.2px;
  opacity: 0.9;
  text-transform: uppercase;
}

.hero h1 {
  margin: 8px 0;
  font-size: clamp(26px, 4vw, 34px);
}

.section {
  margin-bottom: 14px;
}

.section h3 {
  font-size: 20px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
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

@media (max-width: 560px) {
  .action-bar {
    flex-direction: column;
  }
}

@media (max-width: 900px) {
  .hero h1 {
    font-size: 28px;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .ai-bar {
    width: 100%;
  }
}
</style>




