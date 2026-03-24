<template>
  <Layout>
    <div class="activity-list-container">
      <el-card>
        <template #header>
          <div class="header">
            <h2>志愿活动列表</h2>
            <div class="filters">
              <el-select v-model="filters.status" placeholder="活动状态" clearable @change="handleSearch">
                <el-option label="招募中" value="RECRUITING" />
                <el-option label="进行中" value="ONGOING" />
                <el-option label="已结项" value="COMPLETED" />
              </el-select>
              <el-select v-model="filters.category" placeholder="活动类型" clearable @change="handleSearch">
                <el-option label="学长火炬" value="学长火炬" />
                <el-option label="书记驿站" value="书记驿站" />
                <el-option label="爱心小屋" value="爱心小屋" />
                <el-option label="校友招商" value="校友招商" />
                <el-option label="暖冬行动" value="暖冬行动" />
              </el-select>
              <el-select v-model="filters.recruitmentPhase" placeholder="招募阶段" clearable @change="handleSearch">
                <el-option label="未开始" value="NOT_STARTED" />
                <el-option label="招募中" value="RECRUITING" />
                <el-option label="已结束" value="ENDED" />
              </el-select>
            </div>
          </div>
        </template>

        <el-table :data="activities" v-loading="loading">
          <el-table-column label="活动标题" prop="title" min-width="200" />
          <el-table-column label="活动类型" width="120">
            <template #default="{ row }">
              <el-tag>{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="服务地点" prop="location" width="150" />
          <el-table-column label="志愿时长" width="100">
            <template #default="{ row }">
              {{ row.volunteerHours }} 小时
            </template>
          </el-table-column>
          <el-table-column label="报名人数" width="120">
            <template #default="{ row }">
              {{ row.currentParticipants }} / {{ row.maxParticipants }}
            </template>
          </el-table-column>
          <el-table-column label="活动时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.startTime) }}
            </template>
          </el-table-column>
          <el-table-column label="招募状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getRecruitmentDisplay(row).type">
                {{ getRecruitmentDisplay(row).text }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="goToDetail(row.id)">
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSearch"
            @current-change="handleSearch"
          />
        </div>
      </el-card>
    </div>
  </Layout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Layout from '@/components/Layout.vue'
import { getActivityList } from '@/api/activity'
import { getRecruitmentDisplay } from '@/utils/recruitment'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)
const activities = ref([])

const filters = reactive({
  status: '',
  category: '',
  recruitmentPhase: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const goToDetail = (id) => {
  router.push(`/activity/${id}`)
}

const handleSearch = async () => {
  loading.value = true
  try {
    const res = await getActivityList({
      page: pagination.page,
      size: pagination.size,
      status: filters.status,
      category: filters.category,
      recruitmentPhase: filters.recruitmentPhase
    })
    activities.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('获取活动列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.activity-list-container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
}

.filters {
  display: flex;
  gap: 10px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
