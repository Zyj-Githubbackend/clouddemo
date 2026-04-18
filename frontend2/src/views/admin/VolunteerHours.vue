<template>
  <div class="st-page admin-page">
    <section class="st-hero page-hero">
      <div class="st-hero-content">
        <div class="st-hero-copy">
          <p class="st-hero-eyebrow">志愿时长</p>
          <h1 class="st-hero-title">志愿时长查询</h1>
          <p class="st-hero-desc">
            可按姓名、学号或用户名快速检索志愿者，并查看累计服务时长。
          </p>
        </div>
        <div class="st-hero-actions">
          <span class="st-chip">
            <el-icon><Search /></el-icon>
            当前 {{ users.length }} 人
          </span>
        </div>
      </div>
    </section>

    <section class="st-stat-grid">
      <article class="st-stat-card">
        <span class="label">志愿者人数</span>
        <strong class="value">{{ users.length }}</strong>
        <span class="hint">当前查询结果</span>
      </article>
      <article class="st-stat-card">
        <span class="label">总时长</span>
        <strong class="value">{{ totalHours }}</strong>
        <span class="hint">单位：小时</span>
      </article>
      <article class="st-stat-card">
        <span class="label">平均时长</span>
        <strong class="value">{{ avgHours }}</strong>
        <span class="hint">当前结果均值</span>
      </article>
    </section>

    <section class="st-panel table-panel">
      <div class="st-section-head">
        <div>
          <h2>时长列表</h2>
          <p>支持按姓名、学号或用户名快速查找志愿者时长记录。</p>
        </div>
      </div>

      <div class="toolbar-row">
        <el-input
          v-model="keyword"
          placeholder="搜索姓名 / 学号 / 用户名"
          clearable
          style="width: min(320px, 100%)"
          @clear="fetchData"
          @keyup.enter="fetchData"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" :loading="loading" @click="fetchData">查询</el-button>
      </div>

      <div class="table-scroll">
        <el-table :data="users" v-loading="loading" stripe class="hours-table">
          <el-table-column type="index" label="#" width="55" />
          <el-table-column label="姓名" prop="realName" width="110" />
          <el-table-column label="学号" prop="studentNo" width="120" />
          <el-table-column label="用户名" prop="username" width="130" />
          <el-table-column label="手机" prop="phone" width="130" />
          <el-table-column label="邮箱" prop="email" min-width="180" />
          <el-table-column label="累计时长" width="140" sortable :sort-method="sortByHours">
            <template #default="{ row }">
              <span :class="hoursClass(row.totalVolunteerHours)">
                {{ row.totalVolunteerHours ?? 0 }} 小时
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-empty v-if="!loading && users.length === 0" description="未找到匹配的志愿者" />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getVolunteerHoursList } from '@/api/user'

const loading = ref(false)
const keyword = ref('')
const users = ref([])

const totalHours = computed(() =>
  users.value.reduce((sum, u) => sum + Number(u.totalVolunteerHours ?? 0), 0).toFixed(2)
)

const avgHours = computed(() => {
  if (users.value.length === 0) return '0.00'
  return (Number(totalHours.value) / users.value.length).toFixed(2)
})

const hoursClass = (hours) => {
  const h = Number(hours ?? 0)
  if (h >= 20) return 'hours-high'
  if (h >= 8) return 'hours-mid'
  return ''
}

const sortByHours = (a, b) => Number(b.totalVolunteerHours ?? 0) - Number(a.totalVolunteerHours ?? 0)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getVolunteerHoursList(keyword.value)
    users.value = res.data || []
  } catch (e) {
    console.error(e)
    users.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-hero {
  min-height: 220px;
}

.table-panel {
  padding: 22px;
}

.toolbar-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.hours-high {
  color: var(--el-color-success);
  font-weight: 700;
}

.hours-mid {
  color: var(--el-color-warning);
  font-weight: 600;
}

.hours-table {
  min-width: 860px;
}

@media (max-width: 768px) {
  .table-panel {
    padding: 16px;
  }
}
</style>
