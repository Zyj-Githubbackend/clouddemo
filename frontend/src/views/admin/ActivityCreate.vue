<template>
  <div>
    <el-card>
      <template #header>
        <h3>发布志愿活动</h3>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入活动标题" />
        </el-form-item>

        <el-form-item label="活动类型" prop="category">
          <el-select v-model="form.category" placeholder="请选择活动类型">
            <el-option label="学长火炬" value="学长火炬" />
            <el-option label="书记驿站" value="书记驿站" />
            <el-option label="爱心小屋" value="爱心小屋" />
            <el-option label="校友招商" value="校友招商" />
            <el-option label="暖冬行动" value="暖冬行动" />
          </el-select>
        </el-form-item>

        <el-form-item label="服务地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入服务地点" />
        </el-form-item>

        <el-form-item label="AI生成文案">
          <el-row :gutter="10">
            <el-col :span="18">
              <el-input 
                v-model="aiKeywords" 
                placeholder="输入关键词，例如：图书馆、值班、周末"
              />
            </el-col>
            <el-col :span="6">
              <el-button type="primary" @click="handleAIGenerate" :loading="aiLoading">
                AI生成
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="活动详情" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="8"
            placeholder="请输入活动详情，包括工作内容、要求等"
          />
        </el-form-item>

        <el-form-item label="招募人数" prop="maxParticipants">
          <el-input-number v-model="form.maxParticipants" :min="1" :max="500" />
        </el-form-item>

        <el-form-item label="志愿时长" prop="volunteerHours">
          <el-input-number v-model="form.volunteerHours" :min="0.5" :max="24" :step="0.5" />
          <span style="margin-left: 10px">小时</span>
        </el-form-item>

        <el-form-item label="活动开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择活动开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="活动结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择活动结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="招募开始时间" prop="registrationStartTime">
          <el-date-picker
            v-model="form.registrationStartTime"
            type="datetime"
            placeholder="志愿招募开放报名的开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="报名截止时间" prop="registrationDeadline">
          <el-date-picker
            v-model="form.registrationDeadline"
            type="datetime"
            placeholder="选择报名截止时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading">
            发布活动
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createActivity, generateDescription } from '@/api/activity'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const aiLoading = ref(false)
const aiKeywords = ref('')

const form = reactive({
  title: '',
  category: '',
  location: '',
  description: '',
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
  maxParticipants: [{ required: true, message: '请输入招募人数', trigger: 'blur' }],
  volunteerHours: [{ required: true, message: '请输入志愿时长', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择活动开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择活动结束时间', trigger: 'change' }],
  registrationStartTime: [
    { required: true, message: '请选择招募开始时间', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !form.registrationDeadline) {
          callback()
          return
        }
        if (new Date(form.registrationDeadline) <= new Date(value)) {
          callback(new Error('招募开始须早于报名截止时间'))
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
          callback(new Error('截止时间须晚于招募开始时间'))
          return
        }
        if (form.startTime && new Date(value) > new Date(form.startTime)) {
          callback(new Error('报名截止时间不能晚于活动开始时间'))
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
      keywords: aiKeywords.value
    })
    form.description = res.data
    ElMessage.success('AI生成成功')
  } catch (error) {
    console.error('AI生成失败:', error)
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
    console.error('发布失败:', error)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  formRef.value.resetFields()
  aiKeywords.value = ''
}
</script>
