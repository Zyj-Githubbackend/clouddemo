<template>
  <div>
    <el-card>
      <template #header>
        <h3>时长核销管理</h3>
      </template>

      <el-table :data="registrations" v-loading="loading">
        <el-table-column label="活动名称" prop="activityTitle" min-width="200" />
        <el-table-column label="志愿者姓名" width="120">
          <template #default="{ row }">
            <span>{{ row.realName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学号" width="120">
          <template #default="{ row }">
            <span>{{ row.studentNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="服务地点" prop="location" width="150" />
        <el-table-column label="志愿时长" width="100">
          <template #default="{ row }">
            {{ row.volunteerHours }} 小时
          </template>
        </el-table-column>
        <el-table-column label="活动时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="签到状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.checkInStatus === 1 ? 'success' : 'info'">
              {{ row.checkInStatus === 1 ? '已签到' : '未签到' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核销状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.hoursConfirmed === 1 ? 'success' : 'warning'">
              {{ row.hoursConfirmed === 1 ? '已核销' : '待核销' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.hoursConfirmed === 0"
              type="primary" 
              size="small" 
              @click="handleConfirm(row)"
            >
              核销
            </el-button>
            <el-tag v-else type="success">已核销</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { confirmHours, getAdminRegistrations } from '@/api/activity'
import dayjs from 'dayjs'

const loading = ref(false)
const registrations = ref([])

const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const handleConfirm = (row) => {
  ElMessageBox.confirm(
    `确定核销 ${row.realName || '该志愿者'} 的 ${row.volunteerHours} 小时志愿时长吗？`, 
    '确认核销', 
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await confirmHours(row.id)
      ElMessage.success('核销成功')
      fetchRegistrations()
    } catch (error) {
      console.error('核销失败:', error)
    }
  })
}

const fetchRegistrations = async () => {
  loading.value = true
  try {
    const res = await getAdminRegistrations()
    registrations.value = res.data || []
  } catch (error) {
    console.error('获取报名记录失败:', error)
    registrations.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchRegistrations()
})
</script>
