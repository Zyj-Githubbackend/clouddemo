<template>
  <Layout>
    <div class="st-page page-container feedback-page">
      <section class="st-hero feedback-hero">
        <div class="st-hero-content">
          <div class="st-hero-copy">
            <p class="st-hero-eyebrow">提交反馈</p>
            <h1 class="st-hero-title">我们重视你的反馈</h1>
            <p class="st-hero-desc">
              提交后将生成反馈工单，你可以在详情页继续追踪处理进展并补充回复。
            </p>
          </div>

          <div class="st-hero-actions">
            <span class="st-chip">
              <el-icon><ChatLineSquare /></el-icon>
              提交后可持续跟进处理进度
            </span>
            <el-button class="hero-link" @click="router.push('/my-feedback')">我的反馈</el-button>
          </div>
        </div>
      </section>

      <section class="st-panel form-panel">
        <div class="st-section-head">
          <div>
            <h2>提交反馈</h2>
            <p>请填写反馈主题、问题分类和具体内容，必要时可补充附件。</p>
          </div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="feedback-form">
          <el-form-item label="反馈标题" prop="title">
            <el-input
              v-model="form.title"
              maxlength="200"
              show-word-limit
              size="large"
              placeholder="例如：报名页面无法提交"
            />
          </el-form-item>

          <el-form-item label="反馈分类" prop="category">
            <div class="category-grid">
              <button
                v-for="item in categoryOptions"
                :key="item.value"
                type="button"
                :class="['category-item', form.category === item.value && 'active']"
                @click="form.category = item.value"
              >
                <strong>{{ item.label }}</strong>
                <span>{{ categoryHintMap[item.value] }}</span>
              </button>
            </div>
          </el-form-item>

          <el-form-item label="详细说明" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="8"
              maxlength="5000"
              show-word-limit
              placeholder="请描述出现了什么、你期望看到什么，以及复现步骤或补充背景。"
            />
          </el-form-item>

          <el-form-item label="附件">
            <div class="upload-panel">
              <div class="upload-copy">
                <strong>截图或文档附件</strong>
                <span>支持上传截图、图片或文档附件，帮助更快定位问题。</span>
              </div>
              <FeedbackAttachmentUploader
                :attachments="form.attachments"
                @update:attachments="value => { form.attachments = value }"
              />
            </div>
          </el-form-item>
        </el-form>

        <div class="action-bar">
          <el-button size="large" @click="router.back()">返回</el-button>
          <el-button type="primary" size="large" :loading="submitting" @click="submitForm">
            提交反馈
          </el-button>
        </div>
      </section>
    </div>
  </Layout>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Layout from '@/components/Layout.vue'
import FeedbackAttachmentUploader from '@/components/FeedbackAttachmentUploader.vue'
import { createFeedback } from '@/api/feedback'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const categoryOptions = [
  { label: '问题咨询', value: 'QUESTION' },
  { label: '建议', value: 'SUGGESTION' },
  { label: '系统问题', value: 'BUG' },
  { label: '投诉', value: 'COMPLAINT' },
  { label: '其他', value: 'OTHER' }
]

const categoryHintMap = {
  QUESTION: '业务规则、流程疑问',
  SUGGESTION: '使用体验或优化建议',
  BUG: '页面异常、接口报错',
  COMPLAINT: '服务或处理问题投诉',
  OTHER: '暂不属于以上类型'
}

const form = reactive({
  title: '',
  category: 'BUG',
  content: '',
  attachments: []
})

const rules = {
  title: [{ required: true, message: '请输入反馈标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择反馈分类', trigger: 'change' }],
  content: [{
    validator: (_rule, value, callback) => {
      if ((value && value.trim()) || form.attachments.length > 0) {
        callback()
      } else {
        callback(new Error('请填写说明或上传附件'))
      }
    },
    trigger: 'blur'
  }]
}

const submitForm = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = await createFeedback({
      title: form.title,
      category: form.category,
      content: form.content,
      attachments: form.attachments
    })
    ElMessage.success('反馈已提交')
    router.push(`/feedback/${res.data.id}`)
  } catch (error) {
    console.error('提交反馈失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.feedback-page {
  max-width: 1100px;
}

.feedback-hero {
  min-height: 240px;
}

.hero-link {
  height: 46px;
  border-radius: 999px;
}

.form-panel {
  padding: 24px;
}

.feedback-form :deep(.el-form-item__label) {
  font-weight: 700;
  color: var(--cv-text);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.category-item {
  padding: 16px 18px;
  border-radius: 22px;
  border: 1px solid rgba(201, 214, 243, 0.82);
  background: rgba(242, 246, 251, 0.82);
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-item.active {
  border-color: rgba(13, 71, 217, 0.3);
  background: rgba(13, 71, 217, 0.08);
  box-shadow: 0 10px 22px rgba(13, 71, 217, 0.08);
}

.category-item strong,
.category-item span {
  display: block;
}

.category-item strong {
  color: var(--cv-text);
  font-size: 16px;
}

.category-item span {
  margin-top: 8px;
  color: #78839d;
  font-size: 12px;
  line-height: 1.6;
}

.upload-panel {
  border-radius: 24px;
  padding: 18px;
  background: rgba(242, 246, 251, 0.7);
  border: 1px dashed rgba(183, 198, 231, 0.9);
}

.upload-copy {
  margin-bottom: 14px;
}

.upload-copy strong,
.upload-copy span {
  display: block;
}

.upload-copy span {
  margin-top: 6px;
  color: #7a8198;
  font-size: 13px;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .form-panel {
    padding: 16px;
  }

  .action-bar {
    flex-direction: column-reverse;
  }

  .action-bar .el-button {
    width: 100%;
    margin-left: 0;
  }
}
</style>
