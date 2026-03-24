# 🎨 前端项目文档

> 基于 Vue 3 + Vite + Element Plus 的现代化志愿服务管理系统前端

## ✨ 特性

- 🎨 **现代化UI设计** - 渐变主题 + 流畅动画
- 📱 **完全响应式** - 支持桌面/平板/手机
- ⚡ **快速开发** - Vite 极速热更新
- 🎯 **组件化** - Element Plus 丰富组件库
- 🔐 **权限控制** - 路由守卫 + 角色鉴权
- 💾 **状态管理** - Pinia 轻量状态管理
- 🚀 **API封装** - Axios 统一请求拦截

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4 | 渐进式框架 + Composition API |
| Vite | 5.4 | 下一代前端构建工具 |
| Element Plus | ^2.4 | Vue 3 UI 组件库 |
| Pinia | ^2.2 | Vue 状态管理 |
| Vue Router | ^4.2 | 官方路由管理 |
| Axios | ^1.6 | HTTP 客户端 |
| ECharts | ^5.4 | 图表（首页等） |
| dayjs | ^1.11 | 日期格式化 |

## 📁 项目结构

```
frontend/
├── src/
│   ├── views/              # 页面组件
│   │   ├── Login.vue       # 登录页（美化版，快速登录）
│   │   ├── Register.vue    # 注册页
│   │   ├── Home.vue        # 首页（数据统计）
│   │   ├── ActivityList.vue    # 活动列表
│   │   ├── ActivityDetail.vue  # 活动详情
│   │   ├── MyCenter.vue    # 个人中心
│   │   └── admin/          # 管理员页面
│   │       ├── AdminLayout.vue
│   │       ├── ActivityManage.vue
│   │       ├── ActivityCreate.vue
│   │       └── HoursConfirm.vue
│   ├── components/         # 公共组件
│   │   └── Layout.vue      # 主布局（响应式导航）
│   ├── api/               # API接口
│   │   ├── user.js        # 用户相关API
│   │   └── activity.js    # 活动相关API
│   ├── router/            # 路由配置
│   │   └── index.js       # 路由定义 + 守卫
│   ├── store/             # 状态管理
│   │   └── user.js        # 用户状态
│   ├── utils/             # 工具函数
│   │   └── request.js     # Axios封装
│   ├── style.css          # 全局样式
│   ├── App.vue            # 根组件
│   └── main.js            # 入口文件
├── public/                # 静态资源
├── index.html             # HTML模板
├── vite.config.js         # Vite配置
├── package.json           # 依赖配置
└── README.md              # 本文档
```

## 🚀 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问：http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 🎨 界面展示

### 登录页面特性

- ✨ 渐变紫色背景
- 🌊 动态浮动元素动画
- 🔐 现代化输入框（带图标）
- ⚡ **快速登录标签**（点击即填充账号密码）
  - 🔴 管理员
  - 🟢 学生01
  - 🟡 学生02
- 📱 响应式布局（自适应手机屏幕）

### 首页特性

- 📊 **数据统计卡片**（4个指标）
  - 累计志愿时长
  - 参与活动数
  - 已完成活动
  - 已核销次数
- 🎴 **活动卡片网格**
  - 悬浮效果
  - 渐变图标
  - 活动状态标签
- 📱 **响应式网格**
  - 桌面：4列
  - 平板：2列
  - 手机：1列

### 导航组件特性

- 🖥️ **桌面端**：水平导航栏 + 用户下拉菜单
- 📱 **移动端**：抽屉式侧边栏 + 汉堡菜单
- 🎯 **自动切换**：根据屏幕宽度自动切换布局
- ✨ **流畅动画**：路由切换淡入淡出

## 🎯 响应式设计

### 断点定义

```css
/* 手机 */
@media (max-width: 480px) { ... }

/* 平板 */
@media (max-width: 768px) { ... }

/* 桌面 */
@media (max-width: 1024px) { ... }

/* 大屏 */
@media (min-width: 1200px) { ... }
```

### Element Plus 响应式网格

使用 `el-col` 的响应式属性：

```vue
<el-col :xs="24" :sm="12" :md="8" :lg="6">
  <!-- 内容 -->
</el-col>
```

- `xs`: < 768px（手机）
- `sm`: ≥ 768px（平板）
- `md`: ≥ 992px（桌面）
- `lg`: ≥ 1200px（大屏）

## 🔧 配置说明

### Vite 配置 (vite.config.js)

```javascript
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')  // @ 别名指向 src
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',  // 后端网关
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

### 路由配置 (router/index.js)

- **路由守卫**：检查登录状态
- **角色权限**：管理员路由验证
- **重定向**：未登录跳转登录页

### API 请求封装 (utils/request.js)

- **请求拦截器**：自动添加 JWT Token
- **响应拦截器**：统一错误处理
- **401处理**：自动退出登录
- **消息提示**：使用 Element Plus Message

## 📊 状态管理

### User Store (store/user.js)

```javascript
// 状态
token         // JWT令牌
userInfo      // 用户信息

// 计算属性
isLogin       // 是否登录
isAdmin       // 是否管理员

// 方法
setToken()    // 设置令牌
setUserInfo() // 设置用户信息
logout()      // 退出登录
```

## 🎨 样式规范

### 全局样式 (style.css)

- **CSS Reset**：统一浏览器样式
- **滚动条美化**：自定义滚动条样式
- **主题色定制**：紫色渐变主题
- **Element Plus 定制**：按钮、卡片、输入框
- **工具类**：文本截断、动画效果

### 主题色

```css
:root {
  --el-color-primary: #667eea;
  --primary-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
```

### 常用渐变

```css
/* 紫色渐变 */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

/* 图标背景 */
background: rgba(102, 126, 234, 0.2);
```

## 🔐 权限控制

### 路由守卫

```javascript
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLogin) {
    // 需要登录但未登录 → 跳转登录页
    next('/login')
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    // 需要管理员权限但不是管理员 → 拒绝访问
    ElMessage.error('需要管理员权限')
    next('/home')
  } else {
    next()
  }
})
```

### 角色权限

```vue
<!-- 仅管理员可见 -->
<el-menu-item v-if="userStore.isAdmin" index="/admin">
  管理后台
</el-menu-item>
```

## 📱 移动端适配

### 导航组件

- 屏幕 < 768px：显示汉堡菜单按钮
- 点击按钮：打开抽屉式侧边栏
- 抽屉内容：用户信息 + 导航菜单

### 卡片网格

- 桌面：`el-col :lg="6"`（4列）
- 平板：`el-col :md="12"`（2列）
- 手机：`el-col :xs="24"`（1列）

### 表单优化

- 移动端：增大输入框和按钮
- 标签位置：`label-position="top"`
- 字体大小：适当放大

## 🐛 常见问题

### 1. 端口被占用

```bash
# Windows
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :3000
kill -9 <PID>
```

### 2. 依赖安装失败

```bash
# 清除缓存
npm cache clean --force

# 删除重装
rm -rf node_modules package-lock.json
npm install
```

### 3. Vite 启动报错

```bash
# 检查 Node 版本
node -v  # 应该 ≥ 16

# 更新 Vite
npm install vite@latest
```

### 4. 跨域问题

检查 `vite.config.js` 中的 proxy 配置：
```javascript
proxy: {
  '/api': {
    target: 'http://localhost:9000',
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/api/, '')
  }
}
```

### 5. 图标不显示

确保已注册 Element Plus 图标：
```javascript
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
```

## 📚 相关资源

- [Vue 3 文档](https://cn.vuejs.org/)
- [Vite 文档](https://cn.vitejs.dev/)
- [Element Plus 文档](https://element-plus.org/zh-CN/)
- [Pinia 文档](https://pinia.vuejs.org/zh/)
- [Vue Router 文档](https://router.vuejs.org/zh/)

## 🎯 开发规范

### 组件命名

- 页面组件：大驼峰（`ActivityList.vue`）
- 公共组件：大驼峰（`Layout.vue`）
- 单文件组件：必须有多个单词

### 代码风格

- 使用 Composition API
- 优先使用 `<script setup>`
- Props 定义使用 `defineProps`
- Emits 定义使用 `defineEmits`

### Git 提交

```bash
# 格式
<type>(<scope>): <subject>

# 示例
feat(login): 添加快速登录功能
fix(activity): 修复报名按钮状态
style(home): 优化首页卡片样式
```

## 🔨 脚本命令

```bash
# 开发
npm run dev          # 启动开发服务器
npm run build        # 构建生产版本
npm run preview      # 预览生产构建

# 代码检查（如果配置了）
npm run lint         # ESLint 检查
npm run lint:fix     # 自动修复
```

## 📝 TODO

- [ ] 添加单元测试
- [ ] 添加 E2E 测试
- [ ] 优化打包体积
- [ ] 添加 PWA 支持
- [ ] 添加国际化（i18n）

---

💡 **提示**：开发时建议使用 Vue DevTools 浏览器扩展进行调试。
