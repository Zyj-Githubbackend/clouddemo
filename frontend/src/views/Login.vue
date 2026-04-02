<template>
  <div class="login-container">
    <div class="login-background">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>
    
    <div class="login-box">
      <div class="login-header">
        <div class="logo">
          <el-icon :size="50" color="#667eea"><Trophy /></el-icon>
        </div>
        <h1>校园志愿服务平台</h1>
        <p>让爱心传递，让奉献闪光</p>
      </div>
      
      <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
        <el-form-item prop="username">
          <el-input 
            v-model="form.username" 
            placeholder="请输入用户名"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            size="large"
            class="login-button" 
            @click="handleLogin" 
            :loading="loading"
          >
            <span v-if="!loading">登 录</span>
          </el-button>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            size="large"
            class="register-button" 
            @click="$router.push('/register')"
          >
            注册新账号
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <el-divider>快速登录</el-divider>
        <div class="test-accounts">
          <el-tag 
            v-for="account in testAccounts" 
            :key="account.username"
            :type="account.type"
            effect="plain"
            class="account-tag"
            @click="quickLogin(account)"
          >
            {{ account.label }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/user'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const testAccounts = [
  { username: 'admin', password: 'password123', label: '管理员', type: 'danger' },
  { username: 'student01', password: 'password123', label: '学生01', type: 'success' },
  { username: 'student02', password: 'password123', label: '学生02', type: 'warning' }
]

const quickLogin = (account) => {
  form.username = account.username
  form.password = account.password
  handleLogin()
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  
  try {
    const res = await login(form)
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.userInfo)
    
    ElMessage.success('登录成功')
    
    // 根据角色跳转
    if (res.data.userInfo.role === 'ADMIN') {
      router.push('/admin/activities')
    } else {
      router.push('/home')
    }
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-background {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 20s infinite ease-in-out;
}

.shape-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.shape-2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  right: -50px;
  animation-delay: 5s;
}

.shape-3 {
  width: 150px;
  height: 150px;
  top: 50%;
  right: 10%;
  animation-delay: 10s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.9); }
}

.login-box {
  position: relative;
  width: 90%;
  max-width: 450px;
  padding: 50px 40px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  display: inline-block;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.login-header h1 {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 10px 0;
}

.login-header p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e0e0e0 inset;
  border-radius: 12px;
  padding: 8px 15px;
  transition: all 0.3s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #667eea inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #667eea inset;
}

.login-button {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.4);
}

.register-button {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  border: 2px solid #e0e0e0;
  background: white;
  color: #666;
  transition: all 0.3s;
}

.register-button:hover {
  border-color: #667eea;
  color: #667eea;
  background: #f5f7ff;
}

.login-footer {
  margin-top: 30px;
}

.test-accounts {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.account-tag {
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 20px;
  transition: all 0.3s;
  font-size: 13px;
}

.account-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-box {
    width: 95%;
    padding: 40px 30px;
  }
  
  .login-header h1 {
    font-size: 24px;
  }
  
  .shape {
    display: none;
  }
}

@media (max-width: 480px) {
  .login-box {
    padding: 30px 20px;
  }
  
  .login-header h1 {
    font-size: 20px;
  }
  
  .logo {
    width: 60px;
    height: 60px;
  }
  
  .test-accounts {
    flex-direction: column;
  }
}
</style>
